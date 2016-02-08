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
            var vm = this;
            vm.rowCollection = dataService.tableContent;
            vm.displayedCollection = [].concat(vm.rowCollection);

            vm.updateAssignment = function(receivedInterface) {
                console.log(receivedInterface);
                var interf = _.find(dataService.interfaces, {name: receivedInterface.name, 'vppName': receivedInterface.vppName});
                if (receivedInterface.assigned){
                    interf.assigned = true;
                    interf.vbd = dataService.selectedBd.name;
                    receivedInterface.vbd = dataService.selectedBd.name;

                    vm.assignInterface(interf);
                } else {
                    var vbdName = receivedInterface.vbd,
                        vppName = receivedInterface.vppName;

                    interf.assigned = false;
                    interf.vbd = '';
                    receivedInterface.vbd = '';

                    vm.unassignInterface(interf, vbdName, vppName);
                }
                //dataService.buildTableContent();
                var previouslyChangedInterface = _.find(dataService.changedInterfaces, {name: interf.name, 'vppName': interf.vppName});
                if (!previouslyChangedInterface) {
                    dataService.changedInterfaces.push(interf);
                }
                console.log(dataService.changedInterfaces);
                dataService.injectBridgeDomainsTopoElements();

            };

            vm.assignInterface = function(interface) {
                var interfaceObject = bdmInterfaceService.createObj(interface.name, interface.name);

                var successCallback = function() {
                    toastService.showToast('Interface assigned');
                };

                var errorCallback = function() {
                    toastService.showToast('Unable to assign interface');
                };

                bdmInterfaceService.add(interfaceObject, interface.vbd, interface.vppName, successCallback, errorCallback);
            };

            vm.unassignInterface = function(interface, vbdname, vppName) {
                var interfaceObject = bdmInterfaceService.createObj(interface.name, interface.name);

                var successCallback = function() {
                    toastService.showToast('Interface unassigned');
                };

                var errorCallback = function() {
                    toastService.showToast('Unable to unassign interface');
                };

                bdmInterfaceService.delete(interfaceObject, vbdname, vppName, successCallback, errorCallback);

            };
    }]);

    vpp.register.controller('BridgeDomainsController', ['$scope', '$rootScope','$filter', 'dataService', 'bdmBridgeDomainService', 'toastService', '$mdDialog','bdmInterfaceService',
        function($scope, $rootScope, $filter, dataService, bdmBridgeDomainService, toastService, $mdDialog,bdmInterfaceService) {

            console.log('Bridge Domains Controller executed.');

            $scope.dataService = dataService;
            $scope.bridgedomains = dataService.bridgedomains;
            $scope.selectedBd = dataService.selectedBd;

            dataService.nextApp.container(document.getElementById('bridge-domains-next-app'));
            dataService.bridgeDomainsTopo.attach(dataService.nextApp);

            if (!dataService.bridgedomainsLoaded) {
                dataService.generateInterfaces();
                bdmBridgeDomainService.get(function(data) {
                    //success callback
                    angular.copy(data['network-topology'].topology, dataService.bridgedomains);
                    dataService.bridgedomainsLoaded = true;
                    console.log('Loaded BridgeDomains:');
                    console.log(dataService.bridgedomains);
                    dataService.buildAssignedInterfaces();
                }, function(data,status) {
                    //error callback
                    console.log(status);
                });
            }

            dataService.bridgeDomainsTopo.on('clickNode',function(topo,node) {
                console.log(node);
            });

            $scope.reload = function() {
                dataService.selectedBd.name = '';
                dataService.changedInterfaces.length = 0;
                dataService.originalAssignments.length = 0;
                dataService.interfaces.length = 0;
                dataService.tableContent.length = 0;
                dataService.bridgeDomainsTopo.clear();
                dataService.injectedInterfaces.length = 0;
                dataService.generateInterfaces();
                bdmBridgeDomainService.get(function(data) {
                    //success callback
                    angular.copy(data['network-topology'].topology, dataService.bridgedomains);
                    dataService.bridgedomainsLoaded = true;
                    console.log('Loaded BridgeDomains:');
                    console.log(dataService.bridgedomains);
                    dataService.buildAssignedInterfaces();
                }, function(data,status) {
                    //error callback
                    console.log(status);
                });
            };

            $scope.bdChanged = function() {
                dataService.buildTableContent();
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

                        vm.isDone = function(status) {
                            vm.waiting = false;
                            if (status === 'success') {
                                dataService.bridgedomains.push(vm.bd);
                                dataService.selectedBd.name = vm.bd.name;
                                $scope.reload();
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
                                    vm.isDone('success');
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


            /* FIXME: remove after testing */
            /*$scope.deploy = function() {
                var successfulRequestsRequired = dataService.changedInterfaces.length;
                var successfulRequests = 0;

                console.log('Removing previous assignments...');
                _.forEach(dataService.changedInterfaces, function(interf) {

                    //Check if previously assigned.. then DELETE
                    //....
                    var previousAssignment = _.find(dataService.originalAssignments, {
                        'vbridge-topology:user-interface': interf.name,
                        vppName: interf.vppName
                    });

                    if (previousAssignment) {
                        successfulRequestsRequired++;
                        bdmInterfaceService.delete(
                            {
                                "tp-id":previousAssignment['tp-id'],
                                "vbridge-topology:user-interface": previousAssignment['vbridge-topology:user-interface']
                            },
                            previousAssignment.vbd,
                            previousAssignment.vppName,
                            function() {
                                //success callback
                                console.log('Removed previous assignment:',previousAssignment);
                                successfulRequests++;

                                if (successfulRequests >= successfulRequestsRequired) {
                                    toastService.showToast('Deployed! Bridge Domain Validated.');
                                    dataService.changedInterfaces.length = 0;
                                    console.log('Changed interfaces tracker has been reset.');
                                    $scope.reload();
                                }
                            },
                            function() {
                                //error callback
                                console.error('ERROR removing assignment:',previousAssignment);
                            }
                        )
                    }
                    if (interf.assigned) {
                        //Send PUT to correct vBD
                        bdmInterfaceService.add(
                            {
                                "tp-id":interf.vppName+'-'+interf.name,
                                "vbridge-topology:user-interface": interf.name
                            },
                            interf.vbd,
                            interf.vppName,
                            function() {
                                //success callback
                                console.log('Added assignment:',interf);
                                successfulRequests++;

                                if (successfulRequests >= successfulRequestsRequired) {
                                    toastService.showToast('Deployed! Bridge Domain Validated.');
                                    dataService.changedInterfaces.length = 0;
                                    console.log('Changed interfaces tracker has been reset.')
                                    $scope.reload();
                                }
                            },
                            function() {
                                //error callback
                                console.error('ERROR adding assignment:',interf);
                            }
                        )
                    } else {
                        successfulRequests++;
                    }
                });
            };*/

            $scope.removeBd = function() {
                if(dataService.selectedBd.name) {
                    var successCallback = function(success) {
                        if (success) {
                            console.log($scope.bridgedomains);
                            _.remove($scope.bridgedomains, {
                                name: $scope.selectedBd.name
                            });
                            toastService.showToast('Bridge Domain Removed!');
                            $scope.selectedBd.name = '';
                            dataService.clearTopology();
                            dataService.tableContent.length = 0;
                            $scope.reload();
                        } else {
                            toastService.showToast('Error removing Bridge Domain!');
                        }
                    };
                    bdmBridgeDomainService.remove(dataService.selectedBd.name, function(){successCallback(true)}, function(){successCallback(false)});
                }
            };

            window.addEventListener('resize', function () {
                dataService.bridgeDomainsTopo.adaptToContainer();
            });

    }]);
});