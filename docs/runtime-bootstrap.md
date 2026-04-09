# Runtime Bootstrap

## Goal

Turn the open modules into a real runtime by loading `etc` configuration, creating worker threads, selecting the persistence implementation, and shipping data-layer initialization assets.

## Current Bootstrap Path

- Load `signal-processing.properties`
- Create a bounded worker pool through `SignalProcessingExecutorFactory`
- Load `com.omron.gc.cm.mongodb-signal.cfg`
- Initialize MongoDB database `DB_SingalAys`
- Initialize RC2-style Elasticsearch indexes from `index-catalog.json`
- Choose repository mode:
  - `jsonl` for local open development
  - `mongo` for a MongoDB-backed runtime

## Key Classes

- `OpenRuntimeBootstrap`
- `SignalProcessingExecutorManager`
- `MongoClientFactory`
- `MongoSignalRecordRepository`
- `JsonLineSignalRecordRepository`
- `SignalStorageCollections`
- `SignalSearchIndexCatalog`

## Current Limitation

`MongoSignalRecordRepository` is ready as a direct integration class, but it is not yet fully wired into a Karaf service lifecycle or externalized repository selector command.
