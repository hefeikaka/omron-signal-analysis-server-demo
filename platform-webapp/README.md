# Platform Webapp

This module is now a direct source mirror of the RC2 `webapps` frontend tree, kept inside the open project so the runtime can package the same static entry points and resource layout.

## What is mirrored

- `webapps/edge` exactly matches the RC2 file tree under the same relative paths.
- `webapps/login` exactly matches the RC2 file tree under the same relative paths.
- `webapps/upload.xml` is included so the top-level Jetty configuration shape stays compatible.

## Current status

- `/edge` and `/login` use the same static entry files and assets as RC2.
- The mirrored directory structure can be bundled directly into the open runtime without additional frontend build steps.
- Any remaining differences now belong to the backend/runtime side, not this module.
