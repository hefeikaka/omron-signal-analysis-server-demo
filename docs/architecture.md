# Architecture Overview

## Runtime Shape

The packaged RC2 directory is a Karaf-based runtime distribution. This open project keeps the same deployment shape while moving source of truth into readable modules.

```text
webapps -> API / service facade -> signal pipeline -> JMS / storage -> analysis / index
                              \-> config / security / runtime adapters
```

## Compatibility Boundaries

- Topic names remain stable, including `sourceSiganlCurve`, `machine_curve.save`, and `sigfft_etl`
- MongoDB defaults to `DB_SingalAys` in the open runtime, while preserving RC2-style collection naming
- Elasticsearch index names follow RC2 naming, including `signal-analysischannel-process-v1.0` and `signal-analysisindex-history-v1.0`
- Existing configuration file names are preserved where practical
- `edge` and `login` stay deployed as separate web roots
