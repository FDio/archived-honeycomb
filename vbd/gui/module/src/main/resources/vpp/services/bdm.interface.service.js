/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/vpp/vpp.module'], function(vpp) {
    vpp.register.factory('bdmInterfaceService', function(VPPRestangular) {
        var s = {};

        var Interface = function(tpId, interfaceName) {
            this['tp-id'] = tpId || null;
            this['vbridge-topology:user-interface'] = interfaceName;
        };

        s.createObj = function(tpId, interfaceName) {
            return new Interface(tpId, interfaceName);
        };

        s.add = function(interface, bridgeDomainId, vppId, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology')
                .one('topology').one(bridgeDomainId).one('node').one(vppId).one('termination-point').one(interface['tp-id']);
            var dataObj = {'termination-point': [interface]};

            restObj.customPUT(dataObj).then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        s.get = function(bridgeDomainId, vppId, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology')
                .one('topology').one(bridgeDomainId).one('node').one(vppId);

            restObj.get().then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };


        return s;
    });
});