/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/vpp/vpp.module'], function(vpp) {
    vpp.register.factory('bdmVppService', function(VPPRestangular) {
        var s = {};

        var Vpp = function(nodeId, vppId) {
            this['node-id'] = nodeId || null;
            this['supporting-node'] = [
                {
                    'topology-ref': 'topology-netconf',
                    'node-ref': vppId
                }
            ];
            this['netconf-node-topology:pass-through'] = {};
        };

        s.createObj = function(nodeId, vppId) {
            return new Vpp(nodeId, vppId);
        };

        s.add = function(vpp, bridgeDomainId,  successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bridgeDomainId).one('node').one(vpp['node-id']);
            var dataObj = {'node': [vpp]};

            restObj.customPUT(dataObj).then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.get = function(bridgeDomainId, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bridgeDomainId);

            return restObj.get().then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.getOne = function(bridgeDomainId, vppId, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bridgeDomainId).one('node').one(vppId);

            return restObj.get().then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.delete = function(bridgeDomainId, vppId, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one(bridgeDomainId).one('node').one(vppId);

            return restObj.remove().then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.checkAndWriteVpp = function(bridgeDomainId, vppId, successCallback, errorCallback) {
             s.getOne(bridgeDomainId, vppId,
                function() {
                    successCallback();
                }, function() {
                    var vppObject = s.createObj(vppId, vppId);

                    s.add(vppObject, bridgeDomainId,  function() {
                        successCallback();
                    }, function() {
                        errorCallback();
                    });
                }
            );
        };

        s.checkAndDeleteVpp = function(bridgeDomainId, vppId, successCallback, errorCallback) {
            s.getOne(bridgeDomainId, vppId,
                function(data) {
                    if(!data['termination-point']) {
                        s.delete(bridgeDomainId, vppId,
                            function(){
                                successCallback();
                            },
                            function() {
                                errorCallback();
                            }
                        );
                    }
                },
                function() {
                    errorCallback();
                }
            );
        };

        return s;
    });
});