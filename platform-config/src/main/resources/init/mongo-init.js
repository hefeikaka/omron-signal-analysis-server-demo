var databaseName = (typeof process !== "undefined" && process.env && process.env.OPEN_MONGO_DATABASE)
  ? process.env.OPEN_MONGO_DATABASE
  : "DB_SingalAys";

var targetDb = db.getSiblingDB(databaseName);
var collections = [
  "id-sequence-coll",
  "rawdata_signal_curve",
  "rawdata_signal_anormaly_source",
  "rawdata_index_source",
  "rawdata_production_info_source",
  "signal_machine_index_history"
];

collections.forEach(function(name) {
  if (!targetDb.getCollectionNames().includes(name)) {
    targetDb.createCollection(name);
  }
});

if (targetDb.rawdata_signal_curve.getIndexes().length <= 1) {
  targetDb.rawdata_signal_curve.createIndex({ machineId: 1, collectedAt: -1 });
}

if (targetDb.signal_machine_index_history.getIndexes().length <= 1) {
  targetDb.signal_machine_index_history.createIndex({ machineId: 1, indexName: 1, collectedAt: -1 });
}

printjson({
  database: targetDb.getName(),
  collections: targetDb.getCollectionNames()
});
