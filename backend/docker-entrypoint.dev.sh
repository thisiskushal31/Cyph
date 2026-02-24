#!/bin/sh
# Poll for source changes and restart bootRun so bind-mounted code updates apply without container restart.
# Needed because inotify often doesn't work on Docker bind mounts (e.g. Mac/Windows).

set -e
cd /app

# Build once so we have classes and a baseline mtime
./gradlew compileJava --no-daemon

# Get latest mtime of any Java/Kotlin/source file under src
get_src_mtime() {
  find src -type f \( -name '*.java' -o -name '*.kt' -o -name '*.xml' -o -name '*.properties' -o -name '*.yml' -o -name '*.yaml' \) 2>/dev/null \
    | xargs stat -c %Y 2>/dev/null | sort -n | tail -1 || echo 0
}

LAST_MTIME=$(get_src_mtime)
BOOT_PID=""

start_boot() {
  ./gradlew bootRun --no-daemon &
  BOOT_PID=$!
}

stop_boot() {
  if [ -n "$BOOT_PID" ]; then
    kill $BOOT_PID 2>/dev/null || true
    wait $BOOT_PID 2>/dev/null || true
    BOOT_PID=""
  fi
}

start_boot

while true; do
  sleep 2
  NEW_MTIME=$(get_src_mtime)
  if [ -n "$NEW_MTIME" ] && [ "$NEW_MTIME" != "$LAST_MTIME" ]; then
    echo "[dev] Source change detected, recompiling and restarting..."
    stop_boot
    if ./gradlew compileJava --no-daemon; then
      LAST_MTIME=$NEW_MTIME
      start_boot
    else
      echo "[dev] Compile failed, keeping previous run."
      start_boot
    fi
  fi
done
