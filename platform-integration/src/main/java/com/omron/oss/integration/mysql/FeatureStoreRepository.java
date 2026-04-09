package com.omron.oss.integration.mysql;

import com.omron.oss.domain.common.model.FeatureTrendPoint;
import com.omron.oss.domain.common.model.SignalFeatureSnapshot;

import java.util.List;

public interface FeatureStoreRepository {

    void initialize();

    void save(SignalFeatureSnapshot snapshot);

    List<FeatureTrendPoint> findSeries(String machineId, String featureKey, long fromInclusive, long toInclusive, int limit);
}
