/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/vpp/vpp.module', 'next'], function(vpp) {
    vpp.register.factory('VppService', function(VPPRestangular, VPPRestangularXml, VppInterfaceService, $q) {
        var s = {};

        var Vpp = function(name, ipAddress, port, username, password, status) {
            this.name = name || null;
            this.ipAddress = ipAddress || null;
            this.port = port || null;
            this.username = username || null;
            this.password = password || null;
            this.status = status || null;
            this.interfaces = [];
        };

        s.createObj = function(name, ipAddress, port, username, password, status) {
            return new Vpp(name, ipAddress, port, username, password, status);
        };

        s.getVppList = function(successCallback, errorCallback) {
            var vppList = [];
            var promiseList = [];
            var restObj = VPPRestangular.one('restconf').one('operational').one('network-topology:network-topology').one('topology').one('topology-netconf');

            restObj.get().then(function(data) {
                //if(data.topology.length || data.topology[0].node.length) {
                    data.topology[0].node.forEach(function(n) {
                        if(n['node-id'] !== 'controller-config') {
                            //create new object
                            var vppObj = s.createObj(n['node-id'], n['netconf-node-topology:host'], n['netconf-node-topology:port'], null, null, n['netconf-node-topology:connection-status']);
                            // register a promise
                            if (vppObj.status === 'connected') {
                                var promise = VppInterfaceService.getInterfaceListByVppName(n['node-id'], function (interfaceList) {
                                    vppObj.interfaces = interfaceList;
                                });
                                // add promise to array
                                promiseList.push(promise);
                                // when promise is resolved, push vpp into vppList
                                promise.then(function () {
                                    vppList.push(vppObj);
                                })
                            }
                            else {
                                vppList.push(vppObj);
                            }

                        }
                    });
                //}
                // when all promises are resolved, call success callback
                $q.all(promiseList).then(function () {
                    successCallback(vppList);
                });
            }, function(res) {
                errorCallback(res);
            });
        };

        s.deleteVpp = function(vpp, finishedSuccessfullyCallback) {
            console.log(vpp);
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one('topology-netconf').one('node').one('controller-config').one('yang-ext:mount').one('config:modules').one('module').one('odl-sal-netconf-connector-cfg:sal-netconf-connector').one(vpp.name);

            restObj.remove().then(function() {
                finishedSuccessfullyCallback(true);
            }, function(res) {
                finishedSuccessfullyCallback(false);
            });
        };

        s.mountVpp = function(name,ip,port,un,pw,finishedSuccessfullyCallback) {

            var postData =  '\
            <module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">\
            <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">prefix:sal-netconf-connector</type>\
            <name>'+name+'</name>\
            <address xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+ip+'</address>\
            <port xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+port+'</port>\
                <username xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+un+'</username>\
                <password xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+pw+'</password>\
                <tcp-only xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">false</tcp-only>\
                <event-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:netty">prefix:netty-event-executor</type>\
            <name>global-event-executor</name>\
            </event-executor>\
            <binding-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:binding">prefix:binding-broker-osgi-registry</type>\
            <name>binding-osgi-broker</name>\
            </binding-registry>\
            <dom-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom">prefix:dom-broker-osgi-registry</type>\
            <name>dom-broker</name>\
            </dom-registry>\
            <client-dispatcher xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:config:netconf">prefix:netconf-client-dispatcher</type>\
            <name>global-netconf-dispatcher</name>\
            </client-dispatcher>\
            <processing-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:threadpool">prefix:threadpool</type>\
            <name>global-netconf-processing-executor</name>\
            </processing-executor>\
            </module>';

            var restObj = VPPRestangularXml.one('restconf').one('config').one('opendaylight-inventory:nodes').one('node').one('controller-config').one('yang-ext:mount');

            restObj.post('config:modules', postData).then(function() {
                finishedSuccessfullyCallback(true);
            }, function(res) {
                finishedSuccessfullyCallback(false);
            });
        };

        return s;
    });

    vpp.register.factory('VppInterfaceService', function(VPPRestangular, $q) {
        var s = {};

        s.getInterfaceListByVppName = function(vppName, successCallback) {
            var interfaceList = [];
            var restObj = VPPRestangular.one('restconf').one('operational').one('network-topology:network-topology').one('topology').one('topology-netconf').one('node').one(vppName).one('yang-ext:mount').one('ietf-interfaces:interfaces-state');

            return restObj.get().then(function(data) {
                if (data['interfaces-state'].interface) {
                    interfaceList = data['interfaces-state'].interface.filter(function(i) {
                        if (i.name != 'local0') {
                            return i;
                        }
                    });
                }
                successCallback(interfaceList);
            });
        };

        return s;
    });
});