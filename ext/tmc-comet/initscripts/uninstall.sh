#!/bin/bash -e
SCRIPT_NAME=tmc-comet
TARGET="/etc/init.d/$SCRIPT_NAME"

echo "Removing startup links"
update-rc.d -f "$SCRIPT_NAME" remove

echo "Removing initscript"
rm -f $TARGET
