#!/bin/bash -e
cd `dirname "$0"`
CONFIG_FILE="$1"
RUN_AS="$2"
LOG_FILE="$3"
JAR_FILE="tmc-comet-server/target/tmc-comet-server-1.0.0-SNAPSHOT-all.jar"

COMMAND=$(printf 'java -jar "%q" "%q"' "$JAR_FILE" "$CONFIG_FILE")

if [ -n "$LOG_FILE" ]; then
  COMMAND=$(printf '%s >> "%q" 2>&1' "$COMMAND" "$LOG_FILE")
fi

if [ -n "$RUN_AS" ]; then
  COMMAND=$(printf 'sudo -n -u "%q" %s' "$RUN_AS" "$COMMAND")
fi

exec /bin/sh -c "exec $COMMAND"
