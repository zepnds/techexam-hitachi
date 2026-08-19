#!/usr/bin/env bash
set -e

echo "======================================================="
echo "  [DEPLOY] Building Notification Platform Modules      "
echo "======================================================="

mvn clean package -DskipTests

echo "======================================================="
echo "  [DEPLOY] Launching Docker Compose Services           "
echo "======================================================="

docker compose up -d --build

echo "======================================================="
echo "  [DEPLOY] Docker Services Successfully Deployed!      "
echo "======================================================="
docker compose ps
