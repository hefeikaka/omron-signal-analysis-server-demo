# Compatibility Matrix

| Area | Packaged Runtime Signal | Open Project Replacement |
| --- | --- | --- |
| Karaf startup | `bin/*.bat`, `etc/org.apache.karaf.features.cfg` | `platform-runtime`, `platform-config` |
| JMS topic naming | `sourceSiganlCurve`, `machine_curve.save`, ETL topics | `domain-common.TopicNames`, `platform-integration` |
| Signal collection route | `deploy/dc-signal.xml` | `domain-signal-collection`, `domain-signal-pipeline` |
| MongoDB signal storage | `etc/com.omron.gc.cm.mongodb-signal.cfg` with `db_signal` | `platform-config`, `domain-signal-storage`, default `DB_SingalAys` |
| Elasticsearch indexing | `etc/indexing-channel-config.yaml`, `etc/indexing-index-history-config.yaml` | `platform-config/init`, `platform-integration.search` |
| Web entrypoints | `webapps/edge`, `webapps/login` | `platform-webapp` |
| API facade | CXF XML in `deploy` | `domain-api` plus future Blueprint/CXF wiring |
