#! /bin/bash
#
# Script to reset the vpp1 Honeycomb agent.
#
# Copyright (c) 2016 Cisco Systems, Inc. and/or others.  All rights reserved.

if [ "$(id -un)" != "vagrant" ] || [ "$VM_NAME" != "(vpp1)" ]; then
    echo "ERROR: Must be run inside the vpp1 vagrant VM!"
    exit 1
fi

APP_NAME="VPP1 Honeycomb Agent"
KARAF_HOME="$(dirname /opt/honeycomb/v3po-karaf*/bin)"
BIN_DIR="$KARAF_HOME/bin"
KARAF_LOG="$KARAF_HOME/data/log/karaf.log"
CURRENT_DIR="$KARAF_HOME/etc/opendaylight/current"
DATA_DIR="$KARAF_HOME/data"
VPP2VPP_SRC_IP_ADDR="10.10.10.11"
VPP2VPP_DST_IP_ADDR="10.10.10.12"
VNI=1
VRF_ID=7
BRIDGE_ID=1
SHG=1

echo
echo "Stopping VPP..."
sudo stop vpp
echo "Done"
echo
echo "Stopping $APP_NAME"
sudo $BIN_DIR/stop
echo -n "Waiting"
cnt=0
while [ "$(ps -eaf | grep -v grep | grep karaf)" != "" ] ; do
    echo -n "."
    sleep 2
    let "cnt = cnt + 1"
    if [ $cnt -eq 10 ] ; then
        echo
        echo -n "$APP_NAME won't stop!  Killing it.."
        sudo kill $(ps -eaf | grep -v grep | grep karaf | awk '{ print $2 }')
    fi
done
echo "Done"
echo
echo "Deleting $APP_NAME persistent state."
sudo rm -rf $DATA_DIR
sudo rm -rf $CURRENT_DIR
echo
echo "Starting $APP_NAME"
sudo $BIN_DIR/start
echo "Starting VPP"
sudo start vpp
echo "Configuring vpp"
sudo vppctl set int state GigabitEthernet0/9/0 up
sudo vppctl set int state GigabitEthernet0/a/0 up
sudo vppctl set int ip table GigabitEthernet0/a/0 7
sudo vppctl set int ip address GigabitEthernet0/a/0 $VPP2VPP_SRC_IP_ADDR/24
if [ "$1" = "vxlan" ] ; then
    sudo vppctl set int l2 bridge GigabitEthernet0/9/0 $BRIDGE_ID
    sudo vppctl create vxlan tunnel src $VPP2VPP_SRC_IP_ADDR dst $VPP2VPP_DST_IP_ADDR vni $VNI encap-vrf-id $VRF_ID decap-next l2
    sudo vppctl set int l2 bridge vxlan_tunnel0 $BRIDGE_ID $SHG
fi

echo
echo "Waiting for $APP_NAME to initialize..."
while [ "$(grep VPPOPER $KARAF_LOG)" = "" ] ; do
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
echo VPP configuration:
echo
echo "show hardware:"
sudo vppctl show hard
echo
echo "show inteface:"
sudo vppctl show int
echo
echo "show ip fib:"
sudo vppctl show ip fib
if [ "$1" = "vxlan" ] ; then
    echo
    echo "show bridge $BRIDGE_ID detail:"
    sudo vppctl show bridge $BRIDGE_ID detail
fi
echo
echo "Reset $APP_NAME is complete!"

