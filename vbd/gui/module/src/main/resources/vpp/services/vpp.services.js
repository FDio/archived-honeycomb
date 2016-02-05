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

    vpp.register.service('dataService', function() {

        nx.graphic.Icons.registerIcon("bd", "src/app/vpp/assets/images/bd1.svg", 45, 45);
        nx.graphic.Icons.registerIcon("interf", "src/app/vpp/assets/images/interf.svg", 45, 45);

        this.bridgeDomainsTopo = new nx.graphic.Topology({
            height: 350,
            width: 500,
            scalable: true,
            theme:'blue',
            enableGradualScaling:true,
            nodeConfig: {
                color: '#414040',
                label: 'model.label',
                scale: 'model.scale',
                iconType: function(vertex) {
                    var type = vertex.get().type;
                    if (type === 'bd') {
                        return 'bd'
                    } else if (type === 'vpp') {
                        return 'switch'
                    } else {
                        return 'interf';
                    }
                }
            },
            linkConfig: {
                label: 'model.label',
                linkType: 'parallel',
                color: function(link) {
                    if (link.getData().type === 'tunnel') {
                        return '#00FF00';
                    } else {
                        return '#414040';
                    }
                },
                width: function(link) {
                    if (link.getData().type === 'tunnel') {
                        return 5;
                    }
                }
            },
            showIcon: true,
            dataProcessor: 'force',
            autoLayout: true,
            enableSmartNode: false,
            tooltipManagerConfig: {
                nodeTooltipContentClass: 'TooltipNode',
                linkTooltipContentClass: 'TooltipLink'
            }
        });
        this.nextApp =  new nx.ui.Application;

        this.vpps = [];

        this.tableContent = [];
        this.unassignedInterfaces = [];
        this.interfaces = [];
        this.injectedInterfaces = [];
        this.bridgedomains = [];
        this.changedInterfaces = [];
        this.selectedBd = {
            name: ''
        };

        this.setBridgeDomains = function(data) {
            angular.copy(data['bridge-domains']['bridge-domain'], this.bridgedomains);
        };

        this.clearInjectedInterfacesInBridgeDomainTopo = function() {
            this.bridgeDomainsTopo.clear();
            this.injectedInterfaces.length = 0;

        };

        this.generateUnassignedInterfaces = function() {
            this.unassignedInterfaces.length = 0;
            for (var x=0; x<this.interfaces.length; x++) {
                if (!this.interfaces[x]['v3po:l2']['bridge-domain']) {
                    this.unassignedInterfaces.push(this.interfaces[x]);
                }
            }
        };

        this.injectBridgeDomainsTopoElements = function() {
            this.clearInjectedInterfacesInBridgeDomainTopo();
            this.injectBdIntoBridgeDomainsTopo();
            this.injectInterfacesIntoBridgeDomainsTopo();
            this.injectInterfacesLinksIntoBridgeDomainsTopo();
            this.bridgeDomainsTopo.adaptToContainer();
        };

        this.buildTableContent = function() {
            this.tableContent.length = 0;
            this.generateUnassignedInterfaces();
            angular.copy(this.unassignedInterfaces.concat(this.injectedInterfaces),this.tableContent);
        };

        this.injectBdIntoBridgeDomainsTopo = function() {
            this.bridgeDomainsTopo.addNode({
                name : this.selectedBd.name,
                label: this.selectedBd.name,
                x: 60,
                y: -50,
                scale: 5
            });
        };

        this.injectInterfacesLinksIntoBridgeDomainsTopo = function() {
            var nodes = this.bridgeDomainsTopo.getNodes();
            for (var x=1; x<nodes.length; x++){
                var target = nodes[x].get('data-id');
                this.bridgeDomainsTopo.addLink({'source':0, 'target': target});
            }
        };

        this.injectInterfacesIntoBridgeDomainsTopo = function() {
            for (var x=0; x<this.interfaces.length; x++) {
                if ((this.interfaces[x]['v3po:l2']['bridge-domain'] === this.selectedBd.name) && (this.interfaces[x].type==='iana-if-type:ethernetCsmacd')) {
                    this.interfaces[x].label = 'vpp1/'+this.interfaces[x].name;
                    this.bridgeDomainsTopo.addNode(this.interfaces[x]);
                    this.injectedInterfaces.push(this.interfaces[x]);
                    this.interfaces[x].assigned = true;
                }
            }
        };

    });



});