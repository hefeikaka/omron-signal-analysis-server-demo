# Initialization

This folder contains the open-source bootstrap assets for MongoDB and Elasticsearch.

## RC2 alignment

- MongoDB default database: `DB_SingalAys`
- MongoDB signal collection: `rawdata_signal_curve`
- RC2-style Elasticsearch index names are kept in `index-catalog.json`

## Usage

Run `bootstrap-datastores.ps1` from the packaged runtime `init` directory after the database services are available.
