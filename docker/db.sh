#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<'EOSQL'
SELECT 'CREATE DATABASE certification'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'certification')\gexec
SELECT 'CREATE DATABASE keycloak'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
EOSQL
 
