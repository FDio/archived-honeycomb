#! /bin/bash
#
# Script to reset the ODL Virtual Bridge Domain application
#
# Copyright (c) 2016 Cisco Systems, Inc. and/or others.  All rights reserved.

if [ "$(id -un)" != "vagrant" ] || [ "$VM_NAME" != "(vbd)" ]; then
    echo "ERROR: Must be run inside the vbd vagrant VM!"
    exit 1
fi

APP_NAME="ODL VBD application"
KARAF_HOME="$(dirname /opt/honeycomb/vbd-karaf*/bin)"
BIN_DIR="$KARAF_HOME/bin"
KARAF_LOG="$KARAF_HOME/data/log/karaf.log"
CURRENT_DIR="$KARAF_HOME/etc/opendaylight/current"
DATA_DIR="$KARAF_HOME/data"
PERSISTENT_STATE="$CURRENT_DIR $DATA_DIR"

echo
echo "Stopping $APP_NAME"
sudo $BIN_DIR/stop
echo -n "Waiting"
cnt=0
while [ "$(ps -eaf | grep -v grep | grep karaf)" != "" ] ; do
    echo -n "."
    sleep 2
    let "cnt = cnt + 1"
    if [ "$cnt" -eq "10" ] ; then
        echo
        echo -n "$APP_NAME won't stop!  Killing it.."
        sudo kill $(ps -eaf | grep -v grep | grep karaf | awk '{ print $2 }')
    fi
done
echo "Done"
echo
echo "Deleting $APP_NAME persistent state."
sudo rm -rf $CURRENT_DIR
sudo rm -rf $DATA_DIR
echo
echo "Starting $APP_NAME"
sudo $BIN_DIR/start
sleep 2
echo -n "Waiting"
while  [ "$(grep xsql $KARAF_LOG | grep Successfully)" = "" ] ; do
    echo -n "."
    sleep 2
done
cnt=0
while [ "$cnt" -lt "5" ] ; do
    echo -n "."
    sleep 2
    let "cnt = cnt + 1"
done
echo
echo "Reset $APP_NAME is complete!"

