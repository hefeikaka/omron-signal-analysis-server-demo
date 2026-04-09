db = db.getSiblingDB("DB_SingalAys");

db.createCollection("rawdata_signal_curve");
db.createCollection("rawdata_signal_anormaly_source");
db.createCollection("rawdata_index_source");
db.createCollection("signal_machine_index_history_store");
db.createCollection("id-sequence-coll");

db.rawdata_signal_curve.createIndex({ machineId: 1, collectedAt: -1 }, { name: "idx_machine_collectedAt" });
db.rawdata_signal_anormaly_source.createIndex({ machineId: 1, collectedAt: -1 }, { name: "idx_anomaly_machine_collectedAt" });
db.rawdata_index_source.createIndex({ machineId: 1, collectedAt: -1 }, { name: "idx_index_machine_collectedAt" });
db["signal_machine_index_history_store"].createIndex({ machineId: 1, collectedAt: -1 }, { name: "idx_history_machine_collectedAt" });

print("MongoDB initialized for DB_SingalAys");
