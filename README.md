# Omron Signal Analysis Server Open

Open-source compatible reconstruction for the packaged runtime at `omron-signal-analysis-server-1.0.0-RC2`.

## Goals

- Preserve the deployment shape of the existing runtime: `bin`, `etc`, `deploy`, `system`, `webapps`, `data`, `log`, `storage`
- Replace encrypted or closed-source business jars with readable, testable modules
- Keep external integration points compatible first, then iterate internally

## Modules

- `platform-runtime`: runnable distribution, entrypoint server, assembly layout, wrapper scripts
- `platform-config`: externalized configuration templates copied into `etc`
- `platform-integration`: shared adapters for configuration, JMS, MongoDB, and open initialization helpers
- `platform-webapp`: static shell for `edge` and `login`
- `domain-common`: constants and canonical models
- `domain-signal-collection`: collection-side adapter boundary
- `domain-signal-pipeline`: normalize and publish signal messages
- `domain-signal-storage`: persistence boundary and repository contracts
- `domain-signal-analysis`: analysis contracts and starter implementations
- `domain-index`: index calculation contracts
- `domain-api`: facade DTOs and API service contracts

## Runtime Status

- Java 8 and Maven are available on this machine
- The DEMO runtime packages to `platform-runtime/target/omron-signal-analysis-server-open-runtime`
- Default landing page is `http://127.0.0.1:9730/edge/`
- Default MongoDB database is `DB_SingalAys`

## Initialization

- MongoDB and Elasticsearch setup guide: `docs/database-setup.md`
- Foolproof deployment guide for handoff: `docs/deployment-guide.md`
- One-page operator quickstart: `docs/deployment-quickstart.md`
- Packaged MongoDB init script: `platform-runtime/src/main/resources/bin/init-mongo.ps1`
- Packaged Elasticsearch init script: `platform-runtime/src/main/resources/bin/init-es.ps1`
