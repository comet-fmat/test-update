#!/bin/bash -e
THISDIR=`dirname "$0"`
BASEDIR=`dirname "$THISDIR"`
THISDIR=`readlink -f "$THISDIR"`
BASEDIR=`readlink -f "$BASEDIR"`

TEMPLATE="$THISDIR/template.txt"
SCRIPT_NAME=tmc-comet
TARGET="/etc/init.d/$SCRIPT_NAME"
RUN_AS=`stat -c '%U' "$BASEDIR"`
CONFIG_FILE=$BASEDIR/config.properties

echo "Installing into $TARGET."
echo "Configuring it to run as $RUN_AS (the current owner of the directory)."
echo "The config file will be read from $CONFIG_FILE."
echo

cat "$TEMPLATE" |
  sed "s@__BASEDIR__@$BASEDIR@" |
  sed "s@__NAME__@$SCRIPT_NAME@" |
  sed "s@__USER__@$RUN_AS@" |
  sed "s@__CONFIG_FILE__@$CONFIG_FILE@" > $TARGET
chmod a+x "$TARGET"

echo "Setting to start/stop by default"
update-rc.d "$SCRIPT_NAME" defaults 90 10
