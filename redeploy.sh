#!/usr/bin/env bash
set -e

echo "  [REDEPLOY] Stopping existing Docker containers...    "

docker compose down --remove-orphans

echo "  [REDEPLOY] Rebuilding Maven Reactor Modules...      "

mvn clean package -DskipTests

echo "  [REDEPLOY] Rebuilding & Re-launching Containers...  "

docker compose up -d --build

echo "  [REDEPLOY] Redeployment Complete! Service Status:   "
docker compose ps
