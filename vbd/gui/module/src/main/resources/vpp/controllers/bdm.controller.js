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

    vpp.register.controller('TableController', ['$scope', '$rootScope','$filter', 'dataService', 'toastService', function ($scope, $rootScope, filter, dataService, toastService) {
        var vm = this;
        vm.rowCollection = dataService.tableContent;
        vm.displayedCollection = [].concat(vm.rowCollection);
        vm.updateAssignment = function(receivedInterface) {
            var interf = _.find(dataService.interfaces, {name: receivedInterface.name, 'phys-address': receivedInterface['phys-address']});
            angular.copy(receivedInterface, interf);
            if (interf.assigned){
                interf['v3po:l2']['bridge-domain'] = dataService.selectedBd.name;
            } else {
                interf['v3po:l2']['bridge-domain'] = '';
            }
            dataService.injectBridgeDomainsTopoElements();
            dataService.buildTableContent();
            var previouslyChangedInterface = _.find(dataService.changedInterfaces, {name: interf.name, 'phys-address': interf['phys-address']});
            if (!previouslyChangedInterface) {
                dataService.changedInterfaces.push(interf);
            }
            console.log(dataService.changedInterfaces);
        };
    }]);

    vpp.register.controller('BridgeDomainsController', ['$scope', '$rootScope','$filter', 'dataService', 'bdmBridgeDomainService', 'toastService',
        function($scope, $rootScope, $filter, dataService, bdmBridgeDomainService, toastService) {
            $scope.addBd = function() {
                var obj = bdmBridgeDomainService.createObj('vBD' + ((Math.random() * 100) + 1) );

                bdmBridgeDomainService.add(obj,
                    function(data) {
                        console.log('successadding vbd');
                    },
                    function() {
                        console.warn('add bd failed');
                    });
            };

            $scope.dataService = dataService;

            dataService.nextApp.container(document.getElementById('bridge-domains-next-app'));
            dataService.bridgeDomainsTopo.attach(dataService.nextApp);

            window.addEventListener('resize', function () {
                if ($location.path() === '/bridgedomains') {
                    dataService.topo.adaptToContainer();
                }
            });

            $scope.bridgedomains = dataService.bridgedomains;
            $scope.selectedBd = dataService.selectedBd;

            dataService.bridgeDomainsTopo.on('clickNode',function(topo,node) {
                console.log(node);
            });

            $scope.bdChanged = function() {
                dataService.injectBridgeDomainsTopoElements();
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
                                dataService.injectBridgeDomainsTopoElements();
                                dataService.buildTableContent();
                                vm.close();
                            }
                        };

                        //function called when the update button is clicked
                        vm.updateConfig = function() {
                            vm.waiting = true;
                            //send a POST with the entered content in the form field

                        };
                    },
                    controllerAs: 'NewBdDialogCtrl',
                    templateUrl: 'templates/new-bd-dialog.html',
                    parent: angular.element(document.body),
                    clickOutsideToClose:false
                })
            };



            $scope.deploy = function() {

            };

            $scope.removeBd = function() {
                if($scope.selectedBd.name) {
                    var successCallback = function(success) {
                        if (success) {
                            console.log($scope.bridgedomains);
                            _.remove($scope.bridgedomains, {
                                name: $scope.selectedBd.name
                            });
                            toastService.showToast('Bridge Domain Removed!');
                            $scope.selectedBd.name = '';
                            dataService.clearInjectedInterfacesInBridgeDomainTopo();
                            dataService.injectBdIntoBridgeDomainsTopo();
                            dataService.tableContent.length = 0;
                        } else {
                            toastService.showToast('Error removing Bridge Domain!');

                        }
                    };

                    //... removeBdFromOdl(vm.selectedBd.name, successCallback);
                }
            };

    }]);
});