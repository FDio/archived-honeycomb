#!/bin/bash


add_vpp() {
    odl_ip=$1
    vpp_host=$2
    vpp_ip=$3
    vpp_port=$4

    vpp_username=admin
    vpp_password=admin

    put_data='<node xmlns="urn:TBD:params:xml:ns:yang:network-topology">
      <node-id>'$vpp_host'</node-id>
      <host xmlns="urn:opendaylight:netconf-node-topology">'$vpp_ip'</host>
      <port xmlns="urn:opendaylight:netconf-node-topology">'$vpp_port'</port>
      <username xmlns="urn:opendaylight:netconf-node-topology">admin</username>
      <password xmlns="urn:opendaylight:netconf-node-topology">admin</password>
      <tcp-only xmlns="urn:opendaylight:netconf-node-topology">false</tcp-only>
      <keepalive-delay xmlns="urn:opendaylight:netconf-node-topology">0</keepalive-delay>
      </node>
    '
curl -u admin:admin -X PUT -d "$put_data" -H 'Content-Type: application/xml' http://$odl_ip:8181/restconf/config/network-topology:network-topology/topology/topology-netconf/node/$vpp_host
}

if [ -z "$1" ] || 
   [ -z "$2" ] ||
   [ -z "$3" ] ||
   [ -z "$4" ]; then
  echo "usage: ./ncmount.sh <controllerIP_for_mount> <vpp_instance_name> <vpp_IP> <NETCONF_port>
        ie ./ncmount.sh 127.0.0.1 vpp1 192.168.10.12 2830"
  exit 1
fi
 
add_vpp $1 $2 $3 $4 
