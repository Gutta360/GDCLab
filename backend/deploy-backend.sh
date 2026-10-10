#!/bin/bash

set -e

SERVER="${SERVER:-gutta@100.65.174.126}"
JAR="gdclab-backend-0.0.1-SNAPSHOT.jar"
SERVICE="${SERVICE:-gdclab-backend}"

LOCAL_JAR="target/$JAR"
REMOTE_NEW="/home/gutta/$JAR.new"
REMOTE_JAR="/home/gutta/$JAR"

echo "========================================"
echo " GDCLab BACKEND DEPLOYMENT"
echo "========================================"

echo ""
echo "1. Building backend..."

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
    export JAVA_HOME
    JAVA_HOME="$(/usr/libexec/java_home -v 21)"
fi

if [ -n "${JAVA_HOME:-}" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Using Java:"
java -version

mvn clean package -DskipTests

echo "Maven build successful"

echo ""
echo "2. Checking JAR..."

if [ ! -f "$LOCAL_JAR" ]; then
    echo "ERROR: $LOCAL_JAR not found."
    exit 1
fi

echo "JAR found"

echo ""
echo "2a. Verifying Flyway auto-configuration and clean foundation migration..."

if ! jar tf "$LOCAL_JAR" | grep -q 'BOOT-INF/lib/spring-boot-flyway-'; then
    echo "ERROR: spring-boot-flyway is missing from $LOCAL_JAR."
    exit 1
fi

if ! jar tf "$LOCAL_JAR" | grep -q 'BOOT-INF/lib/flyway-database-postgresql-'; then
    echo "ERROR: flyway-database-postgresql is missing from $LOCAL_JAR."
    exit 1
fi

if ! jar tf "$LOCAL_JAR" | grep -q 'BOOT-INF/classes/db/migration/V1__create_diagnostic_registration_module.sql'; then
    echo "ERROR: Diagnostic foundation migration is missing from $LOCAL_JAR."
    exit 1
fi

MIGRATION_COUNT="$(jar tf "$LOCAL_JAR" | grep -c 'BOOT-INF/classes/db/migration/.*\.sql$')"
if [ "$MIGRATION_COUNT" -ne 1 ]; then
    echo "ERROR: Expected exactly one Flyway foundation migration in $LOCAL_JAR; found $MIGRATION_COUNT."
    exit 1
fi

echo "Clean Flyway foundation migration packaged"

echo ""
echo "3. Uploading JAR..."

scp "$LOCAL_JAR" "$SERVER:$REMOTE_NEW"

echo "Upload completed"

echo ""
echo "4. Deploying on server..."

ssh "$SERVER" << EOF

set -e

mv "$REMOTE_NEW" "$REMOTE_JAR"

jar tf "$REMOTE_JAR" | grep 'BOOT-INF/classes/db/migration/V1__create_diagnostic_registration_module.sql'

sudo -n systemctl restart "$SERVICE"

for attempt in {1..30}; do
    if curl -fsS http://localhost:8090/api/v1/branches > /dev/null; then
        break
    fi

    if [ "\$attempt" -eq 30 ]; then
        echo "ERROR: GDCLab backend did not become ready within 30 seconds."
        systemctl status "$SERVICE" --no-pager -l
        exit 1
    fi

    sleep 1
done

systemctl status "$SERVICE" --no-pager -l
ss -ltn | grep ':8090'
curl -f http://localhost:8090/api/v1/branches > /dev/null

EOF

echo ""
echo "========================================"
echo " GDCLab BACKEND DEPLOYED SUCCESSFULLY"
echo "========================================"
