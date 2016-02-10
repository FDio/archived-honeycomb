#! /bin/bash
#
# Script to configure host1.
#
# Copyright (c) 2016 Cisco Systems, Inc. and/or others.  All rights reserved.

if [ "$(id -un)" != "vagrant" ] || [ "$VM_NAME" != "(host1)" ]; then
    echo "ERROR: Must be run inside the vpp1 vagrant VM!"
    exit 1
fi

HOST1_IP_ADDR="172.16.10.11"
HOST2_IP_ADDR="172.16.10.12"
echo
echo "Configuring eth1 with $HOST1_IP_ADDR"
sudo ifconfig eth1 $HOST1_IP_ADDR/24 up
echo
echo "ifconfig eth1:"
ifconfig eth1
echo
echo "Run the following command to ping host2:"
echo "  ping $HOST2_IP_ADDR"
echo
