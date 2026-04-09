This directory hosts the DEMO runtime wrapper scripts.
The current package builds a runnable DEMO distribution with compatible directory shape.
Scripts included:
- start-demo.ps1
- status-demo.ps1
- stop-demo.ps1
- start-open.ps1
- status-open.ps1
- stop-open.ps1
- smoke-test.ps1
- init-mongo.ps1
- init-es.ps1

Usage:
- start-demo.ps1 launches the DEMO runtime in background and writes logs to ..\log
- status-demo.ps1 checks whether the DEMO runtime is listening on port 9730
- stop-demo.ps1 stops the DEMO runtime process bound to port 9730
- start-open.ps1 launches the runtime in background and writes logs to ..\log
- status-open.ps1 checks whether the runtime is listening on port 9730
- stop-open.ps1 stops the runtime process bound to port 9730
- smoke-test.ps1 verifies /healthz, /edge/ and sample ingest
- init-mongo.ps1 initializes MongoDB collections for DB_SingalAys
- init-es.ps1 initializes the default Elasticsearch index mapping
