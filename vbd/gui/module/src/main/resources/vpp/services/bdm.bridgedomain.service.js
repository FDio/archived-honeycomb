/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/vpp/vpp.module'], function(vpp) {
    vpp.register.factory('bdmBridgeDomainService', function(VPPRestangular) {
        var s = {};

        var BridgeDomain = function(topologyId) {
            this['topology-id'] = topologyId || null;
            this['topology-types'] = {
                'vbridge-topology:vbridge-topology': {}
            };
            this['underlay-topology'] = [
                {
                    'topology-ref': 'topology-netconf'
                }
            ];
            this['vbridge-topology:tunnel-type'] = 'tunnel-type-vxlan';
            this['vbridge-topology:vxlan'] = {
                'vni': '1'
            };
            this['vbridge-topology:flood'] = "true",
            this['vbridge-topology:forward'] = "true",
            this['vbridge-topology:learn'] = "true",
            this['vbridge-topology:unknown-unicast-flood'] = "true",
            this['vbridge-topology:arp-termination'] = "false"
        };

        s.createObj = function(topologyId) {
            return new BridgeDomain(topologyId);
        };

        s.add = function(bridgeDomain, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bridgeDomain['topology-id']);
            var dataObj = {'topology': [bridgeDomain]};

            restObj.customPUT(dataObj).then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.get = function(successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology');
            var bridgeDomainList = [];

            restObj.get().then(function(data) {
                if(data['network-topology'].topology) {
                    bridgeDomainList = data['network-topology'].topology.filter(function (topology) {
                        if (topology['topology-types'] && topology['topology-types']['vbridge-topology:vbridge-topology']) {
                            return topology['topology-types']['vbridge-topology:vbridge-topology'] !== undefined;
                        }
                    });
                }
                successCallback(bridgeDomainList);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.getOne = function(bdId, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bdId);

            restObj.get().then(function(data) {
                successCallback(data.topology[0]);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.remove = function(bdName,successCallback,errorCallback) {
            //http://localhost:8181/restconf/config/network-topology:network-topology/topology/testBD
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bdName);

            restObj.remove().then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        return s;
    });
});