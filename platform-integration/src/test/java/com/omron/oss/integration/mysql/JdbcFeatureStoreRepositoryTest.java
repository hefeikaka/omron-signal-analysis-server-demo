package com.omron.oss.integration.mysql;

import com.omron.oss.domain.common.model.FeatureTrendPoint;
import com.omron.oss.domain.common.model.SignalFeatureCatalog;
import com.omron.oss.domain.common.model.SignalFeatureSnapshot;
import com.omron.oss.domain.common.model.SignalFeatureValue;
import com.omron.oss.integration.config.FeatureStoreProperties;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcFeatureStoreRepositoryTest {

    @Test
    void shouldStoreAndQueryFeatureSeries() {
        FeatureStoreProperties properties = new FeatureStoreProperties(
            true,
            "jdbc:h2:mem:featuredb;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "jdbc:h2:mem:featuredb;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "sa",
            "",
            "org.h2.Driver",
            "DB_SingalAys"
        );
        JdbcFeatureStoreRepository repository = new JdbcFeatureStoreRepository(properties);
        repository.initialize();
        repository.save(new SignalFeatureSnapshot(
            "M001-1",
            1000L,
            5000,
            5000,
            Arrays.asList(
                new SignalFeatureValue(SignalFeatureCatalog.byKey("time_rms"), 1.23d),
                new SignalFeatureValue(SignalFeatureCatalog.byKey("freq_dominant_frequency"), 60.0d)
            )
        ));

        List<FeatureTrendPoint> points = repository.findSeries("M001-1", "time_rms", 0L, 2000L, 10);
        assertEquals(1, points.size());
        assertEquals(1.23d, points.get(0).getValue(), 0.0001d);
    }
}
