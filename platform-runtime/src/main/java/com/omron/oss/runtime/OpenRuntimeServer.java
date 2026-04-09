package com.omron.oss.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omron.oss.domain.api.AsyncSignalRouteFacade;
import com.omron.oss.domain.api.SignalRawPayloadParser;
import com.omron.oss.domain.common.model.FeatureTrendPoint;
import com.omron.oss.domain.common.model.SignalFeatureCatalog;
import com.omron.oss.domain.common.model.SignalFeatureDescriptor;
import com.omron.oss.domain.common.model.SignalSample;
import com.omron.oss.domain.signal.analysis.SignalFeatureExtractor;
import com.omron.oss.domain.signal.collection.SignalCollectionService;
import com.omron.oss.domain.signal.pipeline.AsyncSignalProcessingResult;
import com.omron.oss.domain.signal.pipeline.AsyncSignalPublishingService;
import com.omron.oss.domain.signal.pipeline.SignalMessageFormatter;
import com.omron.oss.domain.signal.pipeline.SignalPublishingService;
import com.omron.oss.integration.acquisition.DirectAcquisitionService;
import com.omron.oss.integration.config.DirectAcquisitionProperties;
import com.omron.oss.integration.config.SignalProcessingExecutorManager;
import com.omron.oss.integration.jms.LoggingMessagePublisher;
import com.omron.oss.integration.mongodb.SignalRecordRepository;
import com.omron.oss.integration.mysql.FeatureStoreRepository;
import com.omron.oss.integration.runtime.FeatureAnalysisService;
import com.omron.oss.integration.runtime.OpenRuntimeBootstrap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

public final class OpenRuntimeServer {

    private static final int DEFAULT_PORT = Integer.getInteger("open.runtime.port", 9730);
    private static final String AUTH_COOKIE_NAME = "demo_session";
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        Path runtimeHome = resolveRuntimeHome();
        Path etcDir = runtimeHome.resolve("etc");

        OpenRuntimeBootstrap bootstrap = new OpenRuntimeBootstrap();
        AuthSettings authSettings = loadAuthSettings(etcDir.resolve("demo-auth.properties"));
        SessionManager sessionManager = new SessionManager(authSettings);
        SignalProcessingExecutorManager executorManager =
            bootstrap.createExecutorManager(OpenRuntimeBootstrap.defaultSignalProcessingConfigPath(etcDir));
        String storageMode = System.getProperty("open.runtime.storage.mode", "mongo");
        SignalRecordRepository repository =
            bootstrap.createSignalRecordRepository(
                OpenRuntimeBootstrap.defaultMongoConfigPath(etcDir),
                storageMode,
                OpenRuntimeBootstrap.defaultJsonLineStoragePath(runtimeHome)
            );
        DirectAcquisitionService directAcquisitionService = bootstrap.createDirectAcquisitionService(
            OpenRuntimeBootstrap.defaultDirectAcquisitionConfigPath(etcDir),
            runtimeHome.resolve(Paths.get("resources", "camel", "driver"))
        );
        FeatureStoreRepository featureStoreRepository =
            bootstrap.createFeatureStoreRepository(OpenRuntimeBootstrap.defaultFeatureStoreConfigPath(etcDir));
        FeatureAnalysisService featureAnalysisService =
            new FeatureAnalysisService(new SignalFeatureExtractor(), featureStoreRepository);

        directAcquisitionService.addListener(featureAnalysisService);
        directAcquisitionService.start();

        AsyncSignalRouteFacade asyncFacade = new AsyncSignalRouteFacade(
            new SignalRawPayloadParser(),
            new SignalCollectionService(),
            new AsyncSignalPublishingService(
                new SignalPublishingService(new SignalMessageFormatter(), new LoggingMessagePublisher(), repository),
                executorManager.getExecutorService()
            )
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(DEFAULT_PORT), 0);
        server.createContext("/", exchange -> redirectByAuth(exchange, sessionManager));
        server.createContext("/healthz", exchange -> writeJson(exchange, 200, "{\"status\":\"UP\"}"));
        server.createContext("/api/gateway/", new LoginGatewayHandler(sessionManager));
        server.createContext("/api/logout", new LogoutHandler(sessionManager));
        server.createContext("/api/signal/ingest", authenticated(sessionManager, new SignalIngestHandler(asyncFacade), false));
        server.createContext("/api/realtime-monitor", authenticated(sessionManager, new RealtimeMonitorHandler(directAcquisitionService), false));
        server.createContext("/api/acquisition/status", authenticated(sessionManager, new AcquisitionStatusHandler(directAcquisitionService), false));
        server.createContext("/api/acquisition/settings", authenticated(sessionManager, new AcquisitionSettingsHandler(directAcquisitionService), false));
        server.createContext("/api/features/options", authenticated(sessionManager, new FeatureOptionsHandler(), false));
        server.createContext("/api/features/realtime", authenticated(sessionManager, new FeatureSeriesHandler(directAcquisitionService, featureStoreRepository, false), false));
        server.createContext("/api/features/trend", authenticated(sessionManager, new FeatureSeriesHandler(directAcquisitionService, featureStoreRepository, true), false));
        server.createContext("/api/features/debug", authenticated(sessionManager, new FeatureDebugHandler(featureAnalysisService), false));
        server.createContext("/edge", authenticated(sessionManager, new StaticResourceHandler(runtimeHome.resolve(Paths.get("webapps", "edge"))), true));
        server.createContext("/login", exchange -> {
            if (sessionManager.isAuthenticated(exchange)) {
                exchange.getResponseHeaders().set("Location", defaultLandingPath());
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }
            new StaticResourceHandler(runtimeHome.resolve(Paths.get("webapps", "login"))).handle(exchange);
        });
        server.setExecutor(Executors.newCachedThreadPool());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executorManager.close();
            featureAnalysisService.close();
            directAcquisitionService.close();
        }));
        server.start();

        System.out.println("Open runtime server started on port " + DEFAULT_PORT);
        System.out.println("Runtime home: " + runtimeHome);
        System.out.println("Storage mode: " + storageMode);
    }

    static Path resolveRuntimeHome() {
        String configured = System.getProperty("open.runtime.home");
        if (configured != null && !configured.trim().isEmpty()) {
            return Paths.get(configured);
        }
        return Paths.get(".").toAbsolutePath().normalize();
    }

    static String defaultLandingPath() {
        return "/edge/";
    }

    static String loginPath() {
        return "/login/";
    }

    static String buildRealtimeMonitorResponse(List<SignalSample> samples) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"count\":").append(samples.size()).append(",");
        builder.append("\"machines\":[");
        for (int index = 0; index < samples.size(); index++) {
            SignalSample sample = samples.get(index);
            if (index > 0) {
                builder.append(",");
            }
            builder.append("{");
            builder.append("\"machineId\":\"").append(escape(sample.getMachineId())).append("\",");
            builder.append("\"collectedAt\":").append(sample.getCollectedAtEpochMillis()).append(",");
            builder.append("\"collectedAtText\":\"")
                .append(escape(TIME_FORMATTER.format(Instant.ofEpochMilli(sample.getCollectedAtEpochMillis()))))
                .append("\",");
            builder.append("\"samplingFrequency\":").append(sample.getSamplingFrequency()).append(",");
            builder.append("\"valueCount\":").append(sample.getValues().size()).append(",");
            builder.append("\"latestValue\":").append(formatDouble(lastValue(sample))).append(",");
            builder.append("\"minValue\":").append(formatDouble(minValue(sample))).append(",");
            builder.append("\"maxValue\":").append(formatDouble(maxValue(sample))).append(",");
            builder.append("\"previewValues\":").append(buildPreviewValues(sample));
            builder.append("}");
        }
        builder.append("]}");
        return builder.toString();
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static void redirectByAuth(HttpExchange exchange, SessionManager sessionManager) throws IOException {
        exchange.getResponseHeaders().set("Location", sessionManager.isAuthenticated(exchange) ? defaultLandingPath() : loginPath());
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static HttpHandler authenticated(SessionManager sessionManager, HttpHandler delegate, boolean browserPage) {
        return exchange -> {
            if (!sessionManager.isAuthenticated(exchange)) {
                if (browserPage) {
                    exchange.getResponseHeaders().set("Location", loginPath());
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                    return;
                }
                writeJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            delegate.handle(exchange);
        };
    }

    private static final class LoginGatewayHandler implements HttpHandler {
        private final SessionManager sessionManager;

        private LoginGatewayHandler(SessionManager sessionManager) {
            this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> form = parseForm(readBody(exchange.getRequestBody()));
            String username = form.get("username");
            String password = form.get("password");
            if (isBlank(username) || isBlank(password)) {
                writeJson(exchange, 401, "{\"message\":\"401\"}");
                return;
            }
            if (!sessionManager.authenticate(username, password)) {
                writeJson(exchange, 401, "{\"error\":\"Invalid username or password\"}");
                return;
            }

            String sessionId = sessionManager.createSession(username);
            byte[] bytes = ("{\"location\":\"/edge/\",\"username\":\"" + escape(username) + "\"}")
                .getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            headers.set("Location", "/edge/");
            headers.add("Set-Cookie", AUTH_COOKIE_NAME + "=" + sessionId + "; Path=/; HttpOnly; SameSite=Lax");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }

    private static final class LogoutHandler implements HttpHandler {
        private final SessionManager sessionManager;

        private LogoutHandler(SessionManager sessionManager) {
            this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sessionManager.clearSession(exchange);
            exchange.getResponseHeaders().add("Set-Cookie", AUTH_COOKIE_NAME + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
            writeJson(exchange, 200, "{\"location\":\"/login/\"}");
        }
    }

    private static final class SignalIngestHandler implements HttpHandler {
        private final AsyncSignalRouteFacade asyncFacade;

        private SignalIngestHandler(AsyncSignalRouteFacade asyncFacade) {
            this.asyncFacade = Objects.requireNonNull(asyncFacade, "asyncFacade");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            String rawBody = readBody(exchange.getRequestBody());
            try {
                AsyncSignalProcessingResult result = asyncFacade.ingestRawPayloadAsyncSafely(rawBody).join();
                if (result.isSuccess()) {
                    writeJson(exchange, 200, result.getMessage().getPayloadJson());
                } else {
                    writeJson(exchange, 400, "{\"error\":\"" + escape(result.getFailureReason()) + "\"}");
                }
            } catch (RuntimeException ex) {
                writeJson(exchange, 500, "{\"error\":\"" + escape(ex.getMessage()) + "\"}");
            }
        }
    }

    private static final class StaticResourceHandler implements HttpHandler {
        private final Path baseDir;

        private StaticResourceHandler(Path baseDir) {
            this.baseDir = baseDir;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            String relative = requestPath.startsWith("/edge") ? requestPath.substring("/edge".length()) :
                requestPath.startsWith("/login") ? requestPath.substring("/login".length()) : "";
            if (relative.isEmpty() || "/".equals(relative)) {
                relative = "/index.html";
            }

            Path target = baseDir.resolve(relative.substring(1)).normalize();
            if (!target.startsWith(baseDir) || Files.isDirectory(target)) {
                target = baseDir.resolve("index.html");
            } else if (!Files.exists(target)) {
                if (relative.contains(".") || "XMLHttpRequest".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("X-Requested-With"))) {
                    writeJson(exchange, 404, "{\"error\":\"Not Found\"}");
                    return;
                }
                target = baseDir.resolve("index.html");
            }

            byte[] bytes = Files.readAllBytes(target);
            exchange.getResponseHeaders().set("Content-Type", contentType(target));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }

    private static final class RealtimeMonitorHandler implements HttpHandler {
        private final DirectAcquisitionService service;

        private RealtimeMonitorHandler(DirectAcquisitionService service) {
            this.service = Objects.requireNonNull(service, "service");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            int limit = parseLimit(exchange.getRequestURI().getQuery());
            List<SignalSample> recentSignals = service.getRecentSamples();
            if (recentSignals.size() > limit) {
                recentSignals = recentSignals.subList(0, limit);
            }
            writeJson(exchange, 200, buildRealtimeMonitorResponse(recentSignals));
        }
    }

    private static final class AcquisitionStatusHandler implements HttpHandler {
        private final DirectAcquisitionService service;

        private AcquisitionStatusHandler(DirectAcquisitionService service) {
            this.service = Objects.requireNonNull(service, "service");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            writeJson(exchange, 200, buildAcquisitionStatusResponse(service));
        }
    }

    private static final class AcquisitionSettingsHandler implements HttpHandler {
        private final DirectAcquisitionService service;

        private AcquisitionSettingsHandler(DirectAcquisitionService service) {
            this.service = Objects.requireNonNull(service, "service");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 200, buildAcquisitionStatusResponse(service));
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                Map<String, Object> body = OBJECT_MAPPER.readValue(
                    readBody(exchange.getRequestBody()),
                    new TypeReference<Map<String, Object>>() { }
                );
                service.updateSettings(
                    toBoolean(body.get("enabled")),
                    null,
                    toLong(body.get("acquisitionIntervalMillis")),
                    toLong(body.get("acquisitionDurationMillis"))
                );
                writeJson(exchange, 200, buildAcquisitionStatusResponse(service));
            } catch (RuntimeException ex) {
                writeJson(exchange, 400, "{\"error\":\"" + escape(ex.getMessage()) + "\"}");
            }
        }
    }

    private static final class FeatureOptionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            List<Map<String, Object>> features = new ArrayList<Map<String, Object>>();
            for (SignalFeatureDescriptor descriptor : SignalFeatureCatalog.descriptors()) {
                features.add(buildFeatureDescriptorMap(descriptor));
            }
            payload.put("features", features);
            payload.put("rangeOptions", buildRangeOptions());
            writeJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(payload));
        }
    }

    private static final class FeatureSeriesHandler implements HttpHandler {
        private final DirectAcquisitionService acquisitionService;
        private final FeatureStoreRepository repository;
        private final boolean trendMode;

        private FeatureSeriesHandler(
            DirectAcquisitionService acquisitionService,
            FeatureStoreRepository repository,
            boolean trendMode
        ) {
            this.acquisitionService = acquisitionService;
            this.repository = repository;
            this.trendMode = trendMode;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            DirectAcquisitionProperties properties = acquisitionService.getProperties();
            String machineId = isBlank(query.get("machineId")) ? properties.getEquipmentNo() : query.get("machineId");
            String featureKey = isBlank(query.get("featureKey")) ? "time_rms" : query.get("featureKey");
            String rangeKey = isBlank(query.get("rangeKey")) ? (trendMode ? "1d" : "1h") : query.get("rangeKey");
            long window = resolveRangeMillis(rangeKey);
            long now = System.currentTimeMillis();
            List<FeatureTrendPoint> points = repository.findSeries(machineId, featureKey, now - window, now, 1000);
            SignalFeatureDescriptor descriptor = SignalFeatureCatalog.byKey(featureKey);

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("machineId", machineId);
            payload.put("featureKey", featureKey);
            payload.put("rangeKey", rangeKey);
            payload.put("mode", trendMode ? "trend" : "realtime");
            payload.put("feature", buildFeatureDescriptorMap(descriptor));
            payload.put("points", buildTrendPoints(points));
            payload.put("latestValue", points.isEmpty() ? Double.valueOf(0.0d) : Double.valueOf(points.get(points.size() - 1).getValue()));
            payload.put("pointCount", Integer.valueOf(points.size()));
            writeJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(payload));
        }
    }

    private static final class FeatureDebugHandler implements HttpHandler {
        private final FeatureAnalysisService featureAnalysisService;

        private FeatureDebugHandler(FeatureAnalysisService featureAnalysisService) {
            this.featureAnalysisService = Objects.requireNonNull(featureAnalysisService, "featureAnalysisService");
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("submittedCount", Long.valueOf(featureAnalysisService.getSubmittedCount()));
            payload.put("successCount", Long.valueOf(featureAnalysisService.getSuccessCount()));
            payload.put("latestSubmittedAt", Long.valueOf(featureAnalysisService.getLatestSubmittedAt()));
            payload.put(
                "latestSubmittedAtText",
                formatTimestamp(featureAnalysisService.getLatestSubmittedAt())
            );
            payload.put("latestProcessedAt", Long.valueOf(featureAnalysisService.getLatestProcessedAt()));
            payload.put(
                "latestProcessedAtText",
                formatTimestamp(featureAnalysisService.getLatestProcessedAt())
            );
            payload.put("lastError", featureAnalysisService.getLastError());
            payload.put("latestSnapshot", buildFeatureSnapshotMap(featureAnalysisService.getLatestSnapshot()));
            writeJson(exchange, 200, OBJECT_MAPPER.writeValueAsString(payload));
        }
    }

    private static String buildAcquisitionStatusResponse(DirectAcquisitionService service) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        DirectAcquisitionProperties settings = service.getProperties();
        payload.put("enabled", Boolean.valueOf(settings.isEnabled()));
        payload.put("connected", Boolean.valueOf(service.isConnected()));
        payload.put("initialized", Boolean.valueOf(service.isInitialized()));
        payload.put("lastError", service.getLastError());
        payload.put("lastCollectedAt", Long.valueOf(service.getLastCollectedAtEpochMillis()));
        payload.put(
            "lastCollectedAtText",
            service.getLastCollectedAtEpochMillis() > 0
                ? TIME_FORMATTER.format(Instant.ofEpochMilli(service.getLastCollectedAtEpochMillis()))
                : ""
        );
        payload.put("settings", buildSettingsMap(settings));
        payload.put("latest", buildSampleMap(service.getLatestSample(), true));

        List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();
        for (SignalSample sample : service.getRecentSamples()) {
            history.add(buildSampleMap(sample, false));
        }
        payload.put("history", history);

        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize acquisition status", ex);
        }
    }

    private static Map<String, Object> buildSettingsMap(DirectAcquisitionProperties settings) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("samplingFrequency", Integer.valueOf(settings.getSamplingFrequency()));
        map.put("acquisitionIntervalMillis", Long.valueOf(settings.getAcquisitionIntervalMillis()));
        map.put("acquisitionDurationMillis", Long.valueOf(settings.getAcquisitionDurationMillis()));
        map.put("samplingNum", Integer.valueOf(settings.computeSamplingNum()));
        map.put("equipmentNo", settings.getEquipmentNo());
        map.put("tcpPort", Integer.valueOf(settings.getTcpPort()));
        return map;
    }

    private static Map<String, Object> buildSampleMap(SignalSample sample, boolean includeValues) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (sample == null) {
            map.put("machineId", "");
            map.put("collectedAt", Long.valueOf(0L));
            map.put("collectedAtText", "");
            map.put("samplingFrequency", Integer.valueOf(0));
            map.put("valueCount", Integer.valueOf(0));
            map.put("latestValue", Double.valueOf(0.0d));
            map.put("minValue", Double.valueOf(0.0d));
            map.put("maxValue", Double.valueOf(0.0d));
            map.put("previewValues", Collections.emptyList());
            map.put("values", Collections.emptyList());
            return map;
        }

        map.put("machineId", sample.getMachineId());
        map.put("collectedAt", Long.valueOf(sample.getCollectedAtEpochMillis()));
        map.put("collectedAtText", TIME_FORMATTER.format(Instant.ofEpochMilli(sample.getCollectedAtEpochMillis())));
        map.put("samplingFrequency", Integer.valueOf(sample.getSamplingFrequency()));
        map.put("valueCount", Integer.valueOf(sample.getValues().size()));
        map.put("latestValue", Double.valueOf(lastValue(sample)));
        map.put("minValue", Double.valueOf(minValue(sample)));
        map.put("maxValue", Double.valueOf(maxValue(sample)));

        List<Double> preview = new ArrayList<Double>();
        int previewSize = Math.min(sample.getValues().size(), 12);
        for (int index = 0; index < previewSize; index++) {
            preview.add(sample.getValues().get(index));
        }
        map.put("previewValues", preview);
        map.put(
            "values",
            includeValues ? new ArrayList<Double>(sample.getValues()) : Collections.<Double>emptyList()
        );
        return map;
    }

    private static Map<String, Object> buildFeatureDescriptorMap(SignalFeatureDescriptor descriptor) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("key", descriptor.getKey());
        map.put("domain", descriptor.getDomain().name());
        map.put("labelZh", descriptor.getLabelZh());
        map.put("labelEn", descriptor.getLabelEn());
        return map;
    }

    private static Map<String, Object> buildFeatureSnapshotMap(com.omron.oss.domain.common.model.SignalFeatureSnapshot snapshot) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (snapshot == null) {
            map.put("machineId", "");
            map.put("collectedAt", Long.valueOf(0L));
            map.put("collectedAtText", "");
            map.put("samplingFrequency", Integer.valueOf(0));
            map.put("sampleCount", Integer.valueOf(0));
            map.put("featureCount", Integer.valueOf(0));
            return map;
        }
        map.put("machineId", snapshot.getMachineId());
        map.put("collectedAt", Long.valueOf(snapshot.getCollectedAtEpochMillis()));
        map.put("collectedAtText", formatTimestamp(snapshot.getCollectedAtEpochMillis()));
        map.put("samplingFrequency", Integer.valueOf(snapshot.getSamplingFrequency()));
        map.put("sampleCount", Integer.valueOf(snapshot.getSampleCount()));
        map.put("featureCount", Integer.valueOf(snapshot.getFeatureValues().size()));
        return map;
    }

    private static List<Map<String, Object>> buildTrendPoints(List<FeatureTrendPoint> points) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (FeatureTrendPoint point : points) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("collectedAt", Long.valueOf(point.getCollectedAtEpochMillis()));
            item.put("collectedAtText", TIME_FORMATTER.format(Instant.ofEpochMilli(point.getCollectedAtEpochMillis())));
            item.put("value", Double.valueOf(point.getValue()));
            items.add(item);
        }
        return items;
    }

    private static List<Map<String, Object>> buildRangeOptions() {
        List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
        options.add(rangeOption("1h", "最近1小时", "Last 1 hour"));
        options.add(rangeOption("2h", "最近2小时", "Last 2 hours"));
        options.add(rangeOption("6h", "最近6小时", "Last 6 hours"));
        options.add(rangeOption("12h", "最近12小时", "Last 12 hours"));
        options.add(rangeOption("1d", "最近1天", "Last 1 day"));
        options.add(rangeOption("2d", "最近2天", "Last 2 days"));
        options.add(rangeOption("7d", "最近7天", "Last 7 days"));
        options.add(rangeOption("15d", "最近15天", "Last 15 days"));
        options.add(rangeOption("30d", "最近30天", "Last 30 days"));
        options.add(rangeOption("60d", "最近60天", "Last 60 days"));
        return options;
    }

    private static Map<String, Object> rangeOption(String key, String labelZh, String labelEn) {
        Map<String, Object> option = new LinkedHashMap<String, Object>();
        option.put("key", key);
        option.put("labelZh", labelZh);
        option.put("labelEn", labelEn);
        return option;
    }

    private static String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return "";
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }

    private static long resolveRangeMillis(String rangeKey) {
        if ("1h".equalsIgnoreCase(rangeKey)) {
            return 60L * 60L * 1000L;
        }
        if ("2h".equalsIgnoreCase(rangeKey)) {
            return 2L * 60L * 60L * 1000L;
        }
        if ("6h".equalsIgnoreCase(rangeKey)) {
            return 6L * 60L * 60L * 1000L;
        }
        if ("12h".equalsIgnoreCase(rangeKey)) {
            return 12L * 60L * 60L * 1000L;
        }
        if ("2d".equalsIgnoreCase(rangeKey)) {
            return 2L * 24L * 60L * 60L * 1000L;
        }
        if ("7d".equalsIgnoreCase(rangeKey)) {
            return 7L * 24L * 60L * 60L * 1000L;
        }
        if ("15d".equalsIgnoreCase(rangeKey)) {
            return 15L * 24L * 60L * 60L * 1000L;
        }
        if ("30d".equalsIgnoreCase(rangeKey)) {
            return 30L * 24L * 60L * 60L * 1000L;
        }
        if ("60d".equalsIgnoreCase(rangeKey)) {
            return 60L * 24L * 60L * 60L * 1000L;
        }
        return 24L * 60L * 60L * 1000L;
    }

    private static String buildPreviewValues(SignalSample sample) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int previewSize = Math.min(sample.getValues().size(), 8);
        for (int index = 0; index < previewSize; index++) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append(formatDouble(sample.getValues().get(index)));
        }
        builder.append("]");
        return builder.toString();
    }

    private static double lastValue(SignalSample sample) {
        if (sample.getValues().isEmpty()) {
            return 0.0d;
        }
        return sample.getValues().get(sample.getValues().size() - 1).doubleValue();
    }

    private static double minValue(SignalSample sample) {
        double min = 0.0d;
        boolean first = true;
        for (Double value : sample.getValues()) {
            if (first || value.doubleValue() < min) {
                min = value.doubleValue();
                first = false;
            }
        }
        return min;
    }

    private static double maxValue(SignalSample sample) {
        double max = 0.0d;
        boolean first = true;
        for (Double value : sample.getValues()) {
            if (first || value.doubleValue() > max) {
                max = value.doubleValue();
                first = false;
            }
        }
        return max;
    }

    private static String formatDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        String text = String.format(Locale.US, "%.4f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text.isEmpty() ? "0" : text;
    }

    private static int parseLimit(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 12;
        }
        Map<String, String> values = parseQuery(query);
        try {
            return Math.max(1, Math.min(Integer.parseInt(values.getOrDefault("limit", "12")), 50));
        } catch (NumberFormatException ignored) {
            return 12;
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.US);
        if (name.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        return "application/octet-stream";
    }

    private static AuthSettings loadAuthSettings(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                properties.load(inputStream);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to load auth settings from " + path, ex);
            }
        }
        String username = properties.getProperty("auth.username", "admin").trim();
        String password = properties.getProperty("auth.password", "admin").trim();
        return new AuthSettings(isBlank(username) ? "admin" : username, isBlank(password) ? "admin" : password);
    }

    private static Map<String, String> parseCookies(String headerValue) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (isBlank(headerValue)) {
            return values;
        }
        String[] cookies = headerValue.split(";");
        for (String cookie : cookies) {
            String[] pair = cookie.trim().split("=", 2);
            if (pair.length == 2) {
                values.put(pair[0].trim(), pair[1].trim());
            }
        }
        return values;
    }

    private static String escape(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String readBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (body == null || body.isEmpty()) {
            return values;
        }

        String[] parts = body.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = decodeFormComponent(pair[0]);
            String value = pair.length > 1 ? decodeFormComponent(pair[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (query == null || query.trim().isEmpty()) {
            return values;
        }

        String[] parts = query.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = decodeFormComponent(pair[0]);
            String value = pair.length > 1 ? decodeFormComponent(pair[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String decodeFormComponent(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decode form value", ex);
        }
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        return Long.valueOf(Long.parseLong(String.valueOf(value)));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class AuthSettings {
        private final String username;
        private final String password;

        private AuthSettings(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    private static final class SessionManager {
        private final AuthSettings authSettings;
        private final ConcurrentMap<String, String> sessions = new ConcurrentHashMap<String, String>();

        private SessionManager(AuthSettings authSettings) {
            this.authSettings = Objects.requireNonNull(authSettings, "authSettings");
        }

        private boolean authenticate(String username, String password) {
            return authSettings.username.equals(username) && authSettings.password.equals(password);
        }

        private String createSession(String username) {
            String sessionId = UUID.randomUUID().toString().replace("-", "");
            sessions.put(sessionId, username);
            return sessionId;
        }

        private boolean isAuthenticated(HttpExchange exchange) {
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            String sessionId = parseCookies(cookieHeader).get(AUTH_COOKIE_NAME);
            return !isBlank(sessionId) && sessions.containsKey(sessionId);
        }

        private void clearSession(HttpExchange exchange) {
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            String sessionId = parseCookies(cookieHeader).get(AUTH_COOKIE_NAME);
            if (!isBlank(sessionId)) {
                sessions.remove(sessionId);
            }
        }
    }
}
