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

    vpp.register.service('dataService', ['$timeout', function($timeout) {

        var self = this;

        nx.graphic.Icons.registerIcon("bd", "src/app/vpp/assets/images/bd1.svg", 45, 45);
        nx.graphic.Icons.registerIcon("interf", "src/app/vpp/assets/images/interf.svg", 45, 45);

        this.bridgeDomainsTopo = new nx.graphic.Topology({
            adaptive: true,
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
                        return '#ffffff';
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
        this.bridgedomainsLoaded = false;

        this.vpps = [];
        this.tableContent = [];
        this.originalAssignments = [];
        this.interfaces = [];
        this.injectedInterfaces = [];
        this.bridgedomains = [];
        this.changedInterfaces = [];
        this.selectedBd = {
            name: ''
        };

        this.generateInterfaces = function() {
            self.interfaces.length = 0;
            _.forEach(this.vpps, function(vpp) {
                _.forEach(vpp.interfaces, function(interf) {
                    interf.vppName = vpp.name;
                    interf.label = vpp.name+'/'+interf.name;
                    interf.scale = 0.5;
                    self.interfaces.push(interf);
                });
            });
            console.log(this.interfaces);
        };

        this.buildAssignedInterfaces = function() {
            this.originalAssignments.length = 0;
            _.forEach(this.bridgedomains, function(bd){
                var bdName = bd['topology-id'];
                var nodes = bd.node;
                if (nodes) {
                    _.forEach(nodes, function(vpp) {
                        var vppName = vpp['node-id'];
                        var tps = vpp['termination-point'];
                        if (tps) {
                            _.forEach(tps, function(tp) {
                                tp.vppName = vppName;
                                tp.vbd = bdName;
                                self.originalAssignments.push(tp);
                            })
                        }
                    })
                }
            });

            console.log('Assigned Interfaces: ');
            console.log(this.originalAssignments);
        };

        this.buildTableContent = function() {
            this.tableContent.length = 0;
            angular.copy(this.interfaces,this.tableContent);


            //Makes assignements based on previously changed interfaces, or assignments retrieved from ODL
            _.forEach(this.tableContent, function(interf) {
                var matchedChangedInterface = _.find(self.changedInterfaces, {
                    name: interf.name,
                    vppName: interf.vppName
                });

                var matchedOriginalAssignment = _.find(self.originalAssignments, {
                    'vbridge-topology:user-interface': interf.name,
                    vppName: interf.vppName
                });

                if (matchedChangedInterface) {
                    interf.assigned = matchedChangedInterface.assigned;
                    interf.vbd = matchedChangedInterface.vbd;
                } else if (matchedOriginalAssignment) {
                    interf.assigned = true;
                    interf.vbd = matchedOriginalAssignment.vbd;
                } else {
                    interf.assigned = false;
                    interf.vbd = '';
                }
            });

            _.remove(self.tableContent, function(interf){
                var isAssigned = interf.assigned === true;
                var isNotCorrectBd = !(interf.vbd === self.selectedBd.name);
                return(isAssigned && isNotCorrectBd);
            });

            //_.forEach(this.originalAssignments, function(origAssignment) {
            //    if (origAssignment.vbd === self.selectedBd.name) {
            //        var matchedInterface = _.find(self.tableContent, {
            //            name: origAssignment['vbridge-topology:user-interface'],
            //            vppName: origAssignment.vppName
            //        });
            //        if (matchedInterface) {
            //            matchedInterface.assigned = true;
            //            matchedInterface.vbd = origAssignment.vbd;
            //        } else {
            //            console.error('Interface "'+origAssignment['vbridge-topology:user-interface']+'" on VPP "'+origAssignment.vppName+'" in vBD "'+origAssignment.vbd+'" was not found at mount point!');
            //        }
            //    } else {
            //        _.remove(self.tableContent, {
            //            name: origAssignment['vbridge-topology:user-interface'],
            //            vppName: origAssignment.vppName
            //        });
            //    }
            //});
            //
            //_.forEach(this.changedInterfaces, function(changedInterface) {
            //
            //    var matchedInterface = _.find(self.tableContent, {
            //        name: changedInterface.name,
            //        vppName: changedInterface.vppName
            //    });
            //
            //    if (matchedInterface) {
            //        if (changedInterface.assigned) {
            //            if (changedInterface.vbd === self.selectedBd.name) {
            //                matchedInterface.assigned = true;
            //                matchedInterface.vbd = changedInterface.vbd;
            //            }
            //            else {
            //                _.remove(self.tableContent, {
            //                    name: changedInterface.name,
            //                    vppName: changedInterface.vppName
            //                });
            //            }
            //        } else {
            //            matchedInterface.assigned = false;
            //            matchedInterface.vbd = '';
            //        }
            //    }
            //});

            //..

            //_.remove(self.tableContent, {
            //    name: origAssignment['vbridge-topology:user-interface'],
            //    vppName: origAssignment.vppName
            //});

            this.injectBridgeDomainsTopoElements();

        };


        this.clearTopology = function() {
            this.bridgeDomainsTopo.clear();
            this.injectedInterfaces.length = 0;

        };

        //this.generateUnassignedInterfaces = function() {
        //    this.unassignedInterfaces.length = 0;
        //    for (var x=0; x<this.interfaces.length; x++) {
        //        if (!this.interfaces[x]['v3po:l2']['bridge-domain']) {
        //            this.unassignedInterfaces.push(this.interfaces[x]);
        //        }
        //    }
        //};

        this.setData = function() {

            for (var x=0; x<this.tableContent.length; x++) {
                if (this.tableContent[x].assigned) {
                    this.bridgeDomainsTopo.addNode(this.tableContent[x]);
                    this.injectedInterfaces.push(this.tableContent[x]);
                }
            }

            var nodes = [{
                name : this.selectedBd.name,
                label: this.selectedBd.name,
                type:'bd',
                x: 0,
                y: 0,
                scale: 1
            }].concat(this.injectedInterfaces);

            var links = [];
            for (var x=1; x<nodes.length; x++){
                links.push({'source':0, 'target': x});
            }

            var topoData = {
                nodes: nodes,
                links: links
            };

            this.bridgeDomainsTopo.data(topoData);
        };

        this.injectBridgeDomainsTopoElements = function() {
            this.clearTopology();
            this.setData();
            self.bridgeDomainsTopo.adaptToContainer();
        };
    }]);



});