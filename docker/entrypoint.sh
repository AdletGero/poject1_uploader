#!/usr/bin/env bash
set -euo pipefail

FILE_UPLOADER_PORT="${FILE_UPLOADER_PORT:-8080}"
WORKER_PORT="${WORKER_PORT:-8081}"
APP_TMP_DIR="${APP_TMP_DIR:-/tmp/file-uploader}"

mkdir -p "${APP_TMP_DIR}"

java ${JAVA_OPTS:-} -jar /opt/app/fileUploader.jar --server.port="${FILE_UPLOADER_PORT}" &
FILE_PID=$!

java ${JAVA_OPTS_WORKER:-} -jar /opt/app/worker-service.jar --server.port="${WORKER_PORT}" &
WORKER_PID=$!

cleanup() {
  kill "${FILE_PID}" "${WORKER_PID}" 2>/dev/null || true
}

trap cleanup SIGTERM SIGINT

wait -n "${FILE_PID}" "${WORKER_PID}"
status=$?
cleanup
wait "${FILE_PID}" "${WORKER_PID}" 2>/dev/null || true
exit "${status}"