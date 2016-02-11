/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['app/vpp/vpp.module', 'next'], function(vpp) {

    vpp.register.factory('VPPRestangular', function(Restangular, ENV) {
        return Restangular.withConfig(function(RestangularConfig) {
            RestangularConfig.setBaseUrl(ENV.getBaseURL("MD_SAL"));
        });
    });

    vpp.register.factory('VPPRestangularXml', function(Restangular, ENV) {
        return Restangular.withConfig(function(RestangularConfig) {
            RestangularConfig.setBaseUrl(ENV.getBaseURL("MD_SAL"));
            RestangularConfig.setDefaultHeaders({ "Content-Type": "application/xml" }, { "Accept": "application/xml" });
        });
    });

    vpp.register.service('toastService', function($mdToast) {
        this.showToast = function(content) {
            var toast = $mdToast.simple()
                .content(content)
                .action('OK')
                .position('bottom right');
            $mdToast.show(toast);
        }
    });

    vpp.register.service('dataService', ['$timeout', 'bdmTunnelService',function($timeout, bdmTunnelService) {

        var self = this;

        this.vpps = [];
        this.interfaces = [];
        this.selectedBd = {
            name: ''
        };


    }]);



});