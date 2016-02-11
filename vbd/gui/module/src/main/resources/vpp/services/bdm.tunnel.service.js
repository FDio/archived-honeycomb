/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/vpp/vpp.module'], function(vpp) {
    vpp.register.factory('bdmTunnelService', function(VPPRestangular) {
        var s = {};

        s.get = function(bridgeDomainId, successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('operational').one('network-topology:network-topology')
                .one('topology').one(bridgeDomainId);

            restObj.get().then(function(data) {
                successCallback(data.topology[0].link);
            }, function(res) {
                errorCallback(res.data, res.status);
            });
        };

        return s;
    });
});