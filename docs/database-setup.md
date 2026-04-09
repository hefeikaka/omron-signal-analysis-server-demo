# Database Setup

This open runtime keeps the RC2 configuration style but uses an open initialization flow.

## MongoDB

Reference sources from RC2:
- `etc/com.omron.gc.cm.mongodb-signal.cfg`
- `etc/deployment.yaml`

Default open runtime database:
- `DB_SingalAys`

Collections created by the packaged initialization script:
- `rawdata_signal_curve`
- `rawdata_signal_anormaly_source`
- `rawdata_index_source`
- `signal_machine_index_history_store`
- `id-sequence-coll`

Packaged script:
- `bin/init-mongo.ps1`

Mongo init payload:
- `data/init/mongo/init-db.js`

## Elasticsearch

Reference source from RC2:
- `etc/indexing-channel-config.yaml`

Default index URL:
- `http://127.0.0.1:9200/signal-analysischannel-process-v1.0`

Packaged script:
- `bin/init-es.ps1`

Elasticsearch mapping payload:
- `data/init/es/signal-analysischannel-process-v1.0.mapping.json`

## Notes

- RC2 uses MongoDB as the signal raw data store and Elasticsearch for indexing/search channel configuration.
- The open runtime keeps RC2 naming where practical, but switches the default Mongo database to `DB_SingalAys` as requested.
- Current open runtime writes signal samples to `rawdata_signal_curve` when `-Dopen.runtime.storage.mode=mongo` is enabled.
