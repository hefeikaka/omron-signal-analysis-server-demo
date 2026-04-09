# Initialization

This project now ships a small, open-source friendly initialization flow for the data layer.

## RC2 reference

- RC2 MongoDB default database: `db_signal`
- RC2 MongoDB connection keys: `karaf.mongodb.ip`, `karaf.mongodb.port`
- RC2 Elasticsearch URL key: `karaf.indexing.url`
- RC2 Elasticsearch index names: `signal-analysischannel-process-v1.0`, `signal-analysisindex-history-v1.0`, and related history indexes

## Open project defaults

- MongoDB database: `DB_SingalAys`
- MongoDB raw signal collection: `rawdata_signal_curve`
- Elasticsearch base URL: `http://127.0.0.1:9200`

## Files

- `platform-config/src/main/resources/etc/com.omron.gc.cm.mongodb-signal.cfg`
- `platform-config/src/main/resources/etc/custom.system.properties`
- `platform-config/src/main/resources/etc/custom.index.properties`
- `platform-config/src/main/resources/etc/indexing-channel-config.yaml`
- `platform-config/src/main/resources/etc/indexing-index-history-config.yaml`
- `platform-config/src/main/resources/init/bootstrap-datastores.ps1`
- `platform-config/src/main/resources/init/mongo-init.js`
- `platform-config/src/main/resources/init/index-catalog.json`

## How to run

After building the runtime package, run:

```powershell
.\init\bootstrap-datastores.ps1
```

The script will try `mongosh` or `mongo` for MongoDB and `curl.exe` for Elasticsearch. If the tools are missing, it prints the commands to run manually.
