/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

define(['app/vpp/vpp.module'], function(vpp) {
    vpp.register.controller('bdmCtrl', ['$scope', '$rootScope','$filter', 'dataService', 'toastService', function ($scope, $rootScope, filter, dataService, toastService) {

    }]);

    vpp.register.controller('TableController', ['$scope', '$rootScope','$filter', 'dataService', 'toastService', 'bdmInterfaceService',
        function ($scope, $rootScope, filter, dataService, toastService, bdmInterfaceService) {
            $scope.interfaceList = [];
            $scope.unassignedInterfaceList = [];
            $scope.assignedInterfaces = [];

            var vm = this;

            vm.updateAssignment = function(receivedInterface) {
                if (receivedInterface.assigned){
                    receivedInterface.vbdName = $scope.selectedBd['topology-id'];
                    vm.assignInterface($scope.selectedBd, receivedInterface);

                } else {
                    vm.unassignInterface(receivedInterface);
                }
            };

            vm.assignInterface = function(bridgeDomain, interface) {
                var interfaceObject = bdmInterfaceService.createObj(interface['tp-id'], interface['tp-id']);

                var successCallback = function() {
                    toastService.showToast('Interface assigned');
                    $scope.assignedInterfaces.push(interface);

                    $scope.$emit('INTERFACE_CHANGED', interface);
                };

                var errorCallback = function() {
                    toastService.showToast('Unable to assign interface');
                };

                bdmInterfaceService.add(interfaceObject, bridgeDomain['topology-id'], interface.vppName, successCallback, errorCallback);
            };

            vm.unassignInterface = function(interface) {
                var interfaceObject = bdmInterfaceService.createObj(interface['tp-id'], interface['tp-id']);

                var successCallback = function() {
                    toastService.showToast('Interface unassigned');
                    $scope.assignedInterfaces.splice($scope.assignedInterfaces.indexOf(interface), 1);
                    interface.vbdName = '';

                    $scope.$emit('INTERFACE_CHANGED', interface);
                };

                var errorCallback = function() {
                    toastService.showToast('Unable to unassign interface');
                };

                bdmInterfaceService.delete(interfaceObject, interface.vbdName, interface.vppName, successCallback, errorCallback);

            };

            $scope.$on('BUILD_INTERFACES_TABLE', function(event) {
                $scope.interfaceList = [];
                $scope.unassignedInterfaceList = [];
                $scope.assignedInterfaces = $scope.getAssignedInterfaces();

                $scope.assignedInterfacesFlat = [];

                var getAssignedInterfacesFlat = function() {
                    var keys = Object.keys($scope.assignedInterfaces);

                    if(keys.length) {
                        keys.forEach(function (k) {
                            if($scope.assignedInterfaces[k]) {
                                $scope.assignedInterfaces[k].forEach(function(ai) {
                                    checkAndPushIntoArray($scope.assignedInterfacesFlat, ai);
                                });
                            }
                        });
                    }
                };

                var checkAndPushIntoArray = function(array, item) {
                    var check = array.some(function(i) {
                        return i['tp-id'] === item['tp-id'] && i.vppName === item.vppName;
                    });

                    if(!check) {
                        array.push(item);
                    }
                };

                getAssignedInterfacesFlat();

                dataService.vpps.forEach(function(vpp){
                     vpp.interfaces.forEach(function(interface){
                         var interfaceObject = bdmInterfaceService.createObj(interface.name, interface.name);

                         var check = $scope.assignedInterfacesFlat.some(function (ai) {
                             return interfaceObject['tp-id'] === ai['tp-id'] && vpp.name === ai.vppName;
                         });

                         if(!check) {
                             interfaceObject.vppName = vpp.name;
                             checkAndPushIntoArray($scope.unassignedInterfaceList, interfaceObject);
                         }
                    });
                });

                if($scope.selectedBd) {
                    $scope.interfaceList = $scope.assignedInterfaces[$scope.selectedBd['topology-id']] ? $scope.assignedInterfaces[$scope.selectedBd['topology-id']].concat($scope.unassignedInterfaceList) : $scope.unassignedInterfaceList;
                }

                $scope.interfaceDisplayList = [].concat($scope.interfaceList);
            });

            $scope.$on('INIT_INTERFACES_TABLE', function(event) {
                $scope.interfaceList = [];
                $scope.unassignedInterfaceList = [];
                $scope.assignedInterfaces = [];
                $scope.assignedInterfacesFlat = [];
            });



    }]);

    vpp.register.controller('BridgeDomainsController', ['$scope', '$rootScope','$filter', 'dataService', 'bdmBridgeDomainService', 'toastService', '$mdDialog', 'bdmTunnelService',
        function($scope, $rootScope, $filter, dataService, bdmBridgeDomainService, toastService, $mdDialog, bdmTunnelService) {
            $scope.bridgeDomainList = [];
            $scope.showOverlay = true;

            $scope.loadBridgeDomains = function(bridgeDomain, successCallback) {
                bdmBridgeDomainService.get(function(data) {
                    $scope.bridgeDomainList = data;

                    if(bridgeDomain) {
                        $scope.selectedBd = $scope.bridgeDomainList.filter(function(bd) {
                           return bd['topology-id'] ===  bridgeDomain['topology-id'];
                        })[0];

                        $scope.showTopology($scope.selectedBd);
                    }

                    successCallback();

                }, function(data,status) {
                    //error callback
                    console.log(status);
                });
            };

            $scope.getInterfacesForBridgeDomain = function(bridgeDomain) {
                var interfaceList = [];

                if(bridgeDomain.node) {
                    bridgeDomain.node.forEach(function (n) {
                        if (n['termination-point']) {
                            n['termination-point'].forEach(function (tp) {
                                tp.vppName = n['node-id'];
                                tp.vbdName = bridgeDomain['topology-id'];
                                tp.assigned = true;

                                interfaceList.push(tp);
                            });
                        }
                    });
                }

                return interfaceList;
            };

            $scope.getAssignedInterfaces = function() {
                var interfaces = [];

                $scope.bridgeDomainList.forEach(function(bd) {
                    var bdCopy = {};
                    angular.copy(bd, bdCopy);

                    interfaces[bdCopy['topology-id']] = $scope.getInterfacesForBridgeDomain(bdCopy);
                });

                return interfaces;
            };

            $scope.addBd = function() {
                //show dialog
                $mdDialog.show({
                    controller: function() {
                        var vm = this;
                        vm.bd = {};
                        vm.waiting = false;

                        //function called when the cancel button ( 'x' in the top right) is clicked
                        vm.close = function() {
                            $mdDialog.cancel();
                        };

                        vm.isDone = function(status, bridgeDomain) {
                            vm.waiting = false;
                            if (status === 'success') {
                                $scope.reload(bridgeDomain);
                                vm.close();
                            }
                        };

                        //function called when the update button is clicked
                        vm.updateConfig = function() {
                            vm.waiting = true;
                            //send a POST with the entered content in the form field

                            var obj = bdmBridgeDomainService.createObj(vm.bd.name);

                            bdmBridgeDomainService.add(obj,
                                function(data) {
                                    vm.isDone('success', obj);
                                },
                                function() {
                                    vm.isDone('failed');
                                });

                        };
                    },
                    controllerAs: 'NewBdDialogCtrl',
                    templateUrl: $scope.view_path + 'new-bd-dialog.html',
                    parent: angular.element(document.body),
                    clickOutsideToClose:false
                })
            };

            $scope.removeBd = function() {
                if($scope.selectedBd['topology-id']) {
                    var successCallback = function(success) {
                        $scope.selectedBd = null;
                        $scope.loadBridgeDomains(null, function() {
                            $scope.$broadcast('INIT_INTERFACES_TABLE');
                            $scope.clearTopologies();
                        });

                    };
                    bdmBridgeDomainService.remove($scope.selectedBd['topology-id'], function(){successCallback(true)}, function(){successCallback(false)});
                }
            };

            $scope.bdChanged = function() {
                $scope.loadBridgeDomains($scope.selectedBd, function() {
                    $scope.$broadcast('BUILD_INTERFACES_TABLE');

                    $scope.showTopology($scope.selectedBd);
                });

            };

            nx.graphic.Icons.registerIcon("bd", "src/app/vpp/assets/images/bd1.svg", 45, 45);
            nx.graphic.Icons.registerIcon("interf", "src/app/vpp/assets/images/interf.svg", 45, 45);

            $scope.showOverlayTopology = function(bridgeDomain) {
                var bdCopy = {};
                angular.copy(bridgeDomain, bdCopy);

                $scope.bridgeDomainsTopo = new nx.graphic.Topology({
                    adaptive: true,
                    scalable: true,
                    theme: 'blue',
                    enableGradualScaling: true,
                    nodeConfig: {
                        color: '#414040',
                        label: 'model.label',
                        scale: 'model.scale',
                        iconType: function (vertex) {
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
                        color: function (link) {
                            if (link.getData().type === 'tunnel') {
                                return '#00FF00';
                            } else {
                                return '#ffffff';
                            }
                        },
                        width: function (link) {
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

                $scope.overlayNextApp =  new nx.ui.Application;

                var bdNode = {
                    "data": bdCopy,
                    "type": "bd",
                    "label": bdCopy['topology-id']
                };

                var nodes = [].concat(bdNode);
                var links = [];

                _.forEach($scope.getInterfacesForBridgeDomain(bdCopy), function(tp, index){
                    var ifNode = {
                        "data": tp,
                        "type": "interf",
                        "label": tp['tp-id']
                    };
                    nodes.push(ifNode);
                    links.push({source: 0, target: nodes.length-1});
                });


                $scope.bridgeDomainsTopo.data({
                    nodes: nodes,
                    links: links
                });

                $scope.overlayNextApp.container(document.getElementById('overlay-next-app'));
                $scope.bridgeDomainsTopo.attach($scope.overlayNextApp);
            };

            $scope.fillOverlayTopology = function(bridgeDomain) {
                var bdCopy = {};
                angular.copy(bridgeDomain, bdCopy);

                var bdNode = {
                    "data": bdCopy,
                    "type": "bd",
                    "label": bdCopy['topology-id']
                };

                var nodes = [].concat(bdNode);
                var links = [];

                _.forEach($scope.getInterfacesForBridgeDomain(bdCopy), function(tp, index){
                    var ifNode = {
                        "data": tp,
                        "type": "interf",
                        "label": tp['tp-id']
                    };
                    nodes.push(ifNode);
                    links.push({source: 0, target: nodes.length-1});
                });


                $scope.bridgeDomainsTopo.data({
                    nodes: nodes,
                    links: links
                });

            };

            $scope.showUnderTopology = function(bridgeDomain) {
                //var bdCopy = {};
                //angular.copy(bridgeDomain, bdCopy);

                $scope.underlayTopo = new nx.graphic.Topology({
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
                                return 'bd';
                            } else if (type==='vpp') {
                                return 'switch';
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
                    enableSmartNode: false
                });

                $scope.underlayNextApp =  new nx.ui.Application;

                $scope.fillUnderlayTopology(bridgeDomain);

                $scope.underlayNextApp.container(document.getElementById('underlay-next-app'));
                $scope.underlayTopo.attach($scope.underlayNextApp);
            };

            $scope.fillUnderlayTopology = function(bridgeDomain) {
                var bdCopy = {};
                angular.copy(bridgeDomain, bdCopy);

                var nodes = [];
                var links = [];

                _.forEach(bdCopy.node, function(node, index){
                    var i = index + 1;

                    nodes.push({
                        label: node['node-id'],
                        x: (-1+(2*(i%2)))*((i+1)/2 * 500),
                        y: 700,
                        scale: 1.25,
                        type: 'vpp'
                    });


                    bdmTunnelService.get(
                        bdCopy['topology-id'],
                        function(data) {
                            //success
                            console.log(data);

                            var link = data;
                            var sourceNode = link[0].source['source-node'];
                            var targetNode = link[0].destination['dest-node'];

                            links.push({
                                source: _.findIndex(nodes, {label: sourceNode, type: 'vpp'}),
                                target: _.findIndex(nodes, {label: targetNode, type: 'vpp'}),
                                type: 'tunnel'
                            });

                            $scope.underlayTopo.data({
                                nodes: nodes,
                                links: links
                            });

                        }, function(res) {
                            $scope.underlayTopo.data({
                                nodes: nodes,
                                links: links
                            });
                        });
                });


                $scope.underlayTopo.data({
                    nodes: nodes,
                    links: links
                });
            };

            $scope.reload = function(bridgeDomain) {
                $scope.loadBridgeDomains(bridgeDomain, function() {
                    $scope.$broadcast('BUILD_INTERFACES_TABLE');

                    $scope.showTopology($scope.selectedBd);
                });

            };

            $scope.toggleUnderlay = function() {
                $scope.showOverlay = !$scope.showOverlay;

                $scope.reload($scope.selectedBd);
            };

            $scope.showTopology = function(bridgeDomain) {
                if($scope.showOverlay) {
                    if(!$scope.bridgeDomainsTopo) {
                        $scope.showOverlayTopology(bridgeDomain);
                    }
                    else {
                        $scope.fillOverlayTopology(bridgeDomain);
                    }
                } else {
                    if(!$scope.underlayTopo) {
                        $scope.showUnderTopology(bridgeDomain);
                    }
                    else {
                        $scope.fillUnderlayTopology(bridgeDomain);
                    }
                }
            };

            $scope.clearTopologies = function() {
                if($scope.bridgeDomainsTopo) {
                    $scope.bridgeDomainsTopo.data({
                        nodes: [],
                        links: []
                    });
                }

                if($scope.bridgeDomainsTopo) {
                    $scope.underlayTopo.data({
                        nodes: [],
                        links: []
                    });
                    }
            };

            $scope.$on('INTERFACE_CHANGED', function(event, data) {
                bdmBridgeDomainService.getOne($scope.selectedBd['topology-id'],
                    function(bdData) {
                        $scope.fillOverlayTopology(bdData);
                    },
                    function() {
                        console.log('error getting vbd');
                    });
            });

            $scope.loadBridgeDomains(null, function() {});
        }]);
});