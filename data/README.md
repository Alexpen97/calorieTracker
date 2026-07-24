# NEVO data directory (legacy)

The real NEVO-online 2025/9.0 export lives in:

`services/nevo-service/src/main/resources/nevo/NEVO2025_v9.0.csv`

`nevo-service` imports that classpath resource by default. You do not need a
file here unless you override `NEVO_CSV_PATH` to a filesystem path.
