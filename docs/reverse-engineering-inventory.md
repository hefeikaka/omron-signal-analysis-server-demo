# Reverse Engineering Inventory

## Known Destinations

- `sourceSiganlCurve`
- `machine_curve.save`
- `signal_machine_index_history_store`
- `sigsta_etl`
- `sigfft_etl`
- `sigbff_etl`
- `envspectrum_etl`
- `bff_etl`
- `signal_ws_etl`

## Reconstruction Sequence

1. Freeze topic names and storage names
2. Capture DTOs and canonical model
3. Replace collection route with open Java service and Camel wiring
4. Replace MongoDB repositories
5. Replace analysis workers
6. Replace API façade
7. Reconnect `edge` and `login`
