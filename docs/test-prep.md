# Test Prep Guide

This document summarizes what can be tested automatically in the open project, what still needs hardware, and what can be replaced with fixtures or mocks.

## Current Automated Coverage

The current test suite already covers these areas:

- Topic name compatibility in `domain-common`
- Signal collection validation and normalization
- Signal message formatting and publishing flow
- Async publishing behavior and failure wrapping
- Thread pool creation and graceful shutdown
- Local repository persistence via JSONL
- Property loading and runtime bootstrap wiring
- Mongo client factory construction
- Webapp layout and static asset packaging through Maven resources

Relevant test classes:

- `domain-common/src/test/java/com/omron/oss/domain/common/TopicNamesTest.java`
- `domain-signal-pipeline/src/test/java/com/omron/oss/domain/signal/pipeline/SignalPublishingServiceTest.java`
- `domain-signal-pipeline/src/test/java/com/omron/oss/domain/signal/pipeline/AsyncSignalPublishingServiceTest.java`
- `domain-api/src/test/java/com/omron/oss/domain/api/SignalRouteFacadeTest.java`
- `domain-api/src/test/java/com/omron/oss/domain/api/AsyncSignalRouteFacadeTest.java`
- `platform-integration/src/test/java/com/omron/oss/integration/config/PropertiesLoaderTest.java`
- `platform-integration/src/test/java/com/omron/oss/integration/config/SignalProcessingExecutorFactoryTest.java`
- `platform-integration/src/test/java/com/omron/oss/integration/config/SignalProcessingExecutorManagerTest.java`
- `platform-integration/src/test/java/com/omron/oss/integration/mongodb/JsonLineSignalRecordRepositoryTest.java`
- `platform-integration/src/test/java/com/omron/oss/integration/mongodb/MongoClientFactoryTest.java`
- `platform-integration/src/test/java/com/omron/oss/integration/runtime/OpenRuntimeBootstrapTest.java`

## Tests That Need Hardware

These scenarios are not fully replaceable by a fixture alone:

- Raw signal acquisition through the proprietary DLL path in the original RC2 runtime
- End-to-end verification of the `signal:aaa` Camel source against the actual device or acquisition driver
- Full parity validation of any encrypted or closed-source bundle that interacts with field hardware

For these tests, you need either:

- The real hardware and driver environment
- Or a recorded sample payload exported from the device side, if the goal is to verify downstream processing only

## Tests That Can Use Files Or Mocks

These scenarios can be covered without hardware:

- JSON parsing and validation
- Message formatting
- Topic naming and routing
- Async worker behavior
- Mongo repository writes through a local test implementation
- Runtime bootstrap selection logic
- Static web asset packaging

Recommended fixtures:

- `docs/test-data/sample-signal-ingest.json`
- Temporary `.properties` files created inside tests
- In-memory publishers and repositories

## Suggested Test Matrix

1. Unit tests
   - Validate parsing, transformation, repository selection, and executor lifecycle
2. Component tests
   - Run the open runtime bootstrap against fixture files
   - Use JSONL repository mode for local persistence
3. Packaging tests
   - Run `mvn clean test package`
   - Confirm `platform-runtime/target/omron-signal-analysis-server-open-runtime` is generated
4. Manual parity tests
   - Compare the open runtime outputs with RC2 captured payloads

## Input Expectations

The open ingest path expects a JSON object with:

- `machineCode`, `machineId`, or `assetId`
- `frequency` or `samplingFrequency`
- `values` as an array of numeric samples

If `values` is missing, the safe async path should return a failure result instead of throwing.

## Known Gaps

- The open project still uses a simplified local repository mode for development
- The final Karaf service lifecycle wiring is still a bootstrap-style abstraction
- Front-end parity is layout-first, not a full source rebuild of RC2 output
