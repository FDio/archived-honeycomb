var modules = ['app/vpp/vpp.module',
               'app/vpp/vpp.services',
               ];


define(modules, function(vpp) {

    vpp.register.controller('vppCtrl', ['$scope', '$rootScope', '$timeout', 'httpService' ,'dataService', 'toastService', '$mdSidenav', '$mdDialog',
        function ($scope, $rootScope, $timeout, httpService ,dataService, toastService, $mdSidenav, $mdDialog) {
            $rootScope['section_logo'] = 'src/app/vpp/assets/images/vpp.gif';
            $scope.view_path =  'src/app/vpp/views/';
    		
    	    $scope.mainView = "inventory";

    	    $scope.setMainView = function(viewName) {
    	    	$scope.mainView = viewName;
    	    };
	}]);

    vpp.register.controller('InventoryTableController', ['$scope', '$rootScope','$filter', 'toastService', 'VppService', '$mdDialog', 'dataService', 'InterfaceService', function($scope, $rootScope, filter, toastService, VppService, $mdDialog, dataService, InterfaceService) {
        
        $scope.getInterfaces = function(index) {
            InterfaceService.getInterfaceList(
                $scope.vppList[index].name,
                //success callback
                function(data) {
                    var interfaces = data['interfaces-state'].interface;
                    var vpp = $scope.vppList[index];
                    vpp.interfaces = [];

                    interfaces.forEach(function(i){
                        if (i.name != 'local0') {
                            vpp.interfaces.push(i);
                        }
                    });

                    console.log($scope.vppList);
                },
                //error callback
                function(res) {
                    console.error(res);
                }
            )
        };

        $scope.getVppList = function() {
            $scope.initVppList();

            VppService.getVppList(
                // success callback
                function(data) {
                    if(data.topology.length || data.topology[0].node.length) {
                        data.topology[0].node.forEach(function(n) {
                            if(n['node-id'] !== 'controller-config') {
                                var vppObj = VppService.createObj(n['node-id'], n['netconf-node-topology:host'], n['netconf-node-topology:port'], null, null, n['netconf-node-topology:connection-status']);
                                $scope.vppList.push(vppObj);
                                $scope.getInterfaces($scope.vppList.length - 1); //pass index.

                            }
                        });
                    }
                },
                // error callback
                function(res) {
                    console.error(res);
                }
            );
        };

        $scope.initVppList = function() {
            $scope.vppList = [];
        };

        $scope.viewTopology = function(vpp) {
            $mdDialog.show({
                controller: function() {
                    var vm = this;

                    $scope.topo = new nx.graphic.Topology({
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
                    $scope.app =  new nx.ui.Application;

                    vm.vpp = vpp;
                    vm.vpp.type = 'vpp';
                    vm.vpp.label = vm.vpp.name;

                    var nodes = [].concat(vm.vpp);
                    var links = [];

                    _.forEach(vm.vpp.interfaces, function(interf, index){
                        interf.label = interf.name;
                        interf.scale = 0.5;
                        nodes.push(interf);
                        links.push({source: 0, target: index + 1});
                    });

                    console.log(vpp);
                    console.log(nodes);
                    console.log(links);

                    $scope.topo.data({
                        nodes: nodes,
                        links: links
                    });

                    this.close = function() {
                        $mdDialog.cancel();
                    };

                },
                onComplete: function() {
                    console.log(document.getElementById('next-vpp-topo'));
                    $scope.app.container(document.getElementById('next-vpp-topo'));
                    $scope.topo.attach($scope.app);

                },
                templateUrl: $scope.view_path + 'vpp-topo.html',
                controllerAs: 'VppTopoCtrl',
                parent: angular.element(document.body),
                clickOutsideToClose:true
            })
        };

        $scope.addVppShowForm = function() {
            $mdDialog.show({
                controller: function() {
                    var vm = this;
                    vm.vpp = {};
                    //function called when the cancel button ( 'x' in the top right) is clicked
                    vm.close = function() {
                        $mdDialog.cancel();
                    };

                    vm.finished = function(successful) {
                        if (successful) {
                            vm.close();
                            vm.waiting = false;
                            toastService.showToast('New VPP added!');
                            $scope.getVppList();
                        } else {
                            vm.waiting = false;
                            toastService.showToast('Error adding new VPP');
                        }
                    };

                    //function called when the update button is clicked
                    vm.updateConfig = function() {
                        vm.waiting = true;
                        VppService.mountVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finished);
                    };
                },
                controllerAs: 'NewVppDialogCtrl',
                templateUrl: $scope.view_path + 'new-vpp-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose:true
            })
        };

        $scope.editVppShowForm = function(vppObject) {

            $mdDialog.show({
                controller: function() {
                    var vm = this;

                    vm.vpp = {
                        name: vppObject.name,
                        status: vppObject.status,
                        ip: vppObject.ipAddress,
                        port: vppObject.port
                    };

                    //function called when the cancel button ( 'x' in the top right) is clicked
                    vm.close = function() {
                        $mdDialog.cancel();
                    };

                    vm.finishedUpdating = function(successful) {
                        if (successful) {
                            vm.close();
                            vm.waiting = false;
                            toastService.showToast('VPP configuration updated!');
                            $scope.getVppList();
                        } else {
                            vm.waiting = false;
                            toastService.showToast('Error configuring VPP');
                        }
                    };

                    vm.finishedDeleting = function(successful) {
                        if (successful) {
                            VppService.mountVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finishedUpdating);
                            $scope.getVppList();
                        } else {
                            vm.waiting = false;
                            toastService.showToast('Error configuring VPP');
                        }
                    };

                    //function called when the update button is clicked
                    vm.updateConfig = function() {
                        //VppService.editVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finishedUpdating);
                        VppService.deleteVpp(vm.vpp, vm.finishedDeleting);
                    };
                },
                controllerAs: 'ConfigVppDialogCtrl',
                templateUrl: $scope.view_path + 'config-vpp-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose:true
            });
        };

        $scope.deleteVpp = function(vppObject) {

            var finished = function(successful) {
                if (successful) {
                    toastService.showToast('Removed VPP!');
                    $scope.getVppList();
                } else {
                    toastService.showToast('Error removing VPP');
                }
            };

            VppService.deleteVpp(vppObject, finished);
        };

        $scope.getVppList();
    }]);

    vpp.register.controller('TableController', ['$scope', '$rootScope','$filter', 'dataService', 'httpService', 'toastService', function ($scope, $rootScope, filter, dataService, httpService, toastService) {
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

	vpp.register.controller('InventoryController', function(dataService, $mdDialog, httpService, $location, toastService, $window) {
        var vm = this;
        vm.dataService = dataService;

        var nextApp =  new nx.ui.Application;

        nextApp.container(document.getElementById('inventory-next-app'));
        dataService.vppTopo.attach(nextApp);
        dataService.vppTopo.tooltipManager().nodeTooltip().close();
        dataService.bridgeDomainsTopo.tooltipManager().nodeTooltip().close();

        window.addEventListener('resize', function() {
            if ($location.path() === '/inventory') {
                dataService.vppTopo.adaptToContainer();
            }
        });

        dataService.vppTopo.on("topologyGenerated", function() { //on topology generated...
            dataService.vppTopo.stage().on('dblclick', function(sender, event) {
                var target = event.target;
                var nodesLayerDom = dataService.vppTopo.getLayer('nodes').dom().$dom;
                var id;

                //db click node
                if (nodesLayerDom.contains(target)) {
                    while (!target.classList.contains('node')) {
                        target = target.parentElement;
                    }
                    id = target.getAttribute('data-id');
                    dataService.vppTopo.tooltipManager().nodeTooltip().close();

                    if(dataService.vppTopo.getNode(id).model().get().type ==='iana-if-type:ethernetCsmacd') {
                        vm.configInterface(dataService.vppTopo.getData().nodes[id]);
                    } else {
                        vm.configVpp(dataService.vppTopo.getNode(id));
                    }
                }
            }); //Double click handler

            dataService.vppTopo.on('clickNode',function(topo, node){
                if(node.model().get().type!='iana-if-type:ethernetCsmacd') {
                    dataService.vppTopo.tooltipManager().nodeTooltip().close();
                }
                console.log(node.model().get());
            });
        });

        vm.configVpp = function(node) {
            console.log(node.model().get());
            $mdDialog.show({
                controller: function() {
                    var vm = this;
                    var nodeModel = node.model().get();
                    vm.vpp = {
                        name: nodeModel['node-id'],
                        status: nodeModel['netconf-node-topology:connection-status'],
                        ip: nodeModel['netconf-node-topology:host'],
                        port: nodeModel['netconf-node-topology:port']
                    };

                    //function called when the cancel button ( 'x' in the top right) is clicked
                    vm.close = function() {
                        $mdDialog.cancel();
                    };

                    vm.finishedUpdating = function(successful) {
                        if (successful) {
                            vm.close();
                            vm.waiting = false;
                            toastService.showToast('VPP configuration updated!');
                            $window.location.reload(true);
                        } else {
                            vm.waiting = false;
                            toastService.showToast('Error configuring VPP');
                        }
                    };

                    vm.finishedDeleting = function(successful) {
                        if (successful) {
                            vm.close();
                            vm.waiting = false;
                            toastService.showToast('VPP deleted!');
                            $window.location.reload(true);
                        } else {
                            vm.waiting = false;
                            toastService.showToast('Error deleting VPP');
                        }
                    };

                    vm.deleteVpp = function() {
                        vm.waiting = true;
                        httpService.deleteVpp(vm.vpp.name, vm.finishedDeleting);
                    };

                    //function called when the update button is clicked
                    vm.updateConfig = function() {
                        //TODO: Function to send a POST with the entered content in the form field
                        httpService.configVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finishedUpdating);
                    };
                },
                controllerAs: 'ConfigVppDialogCtrl',
                templateUrl: 'templates/config-vpp-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose:false
            })
        };

        vm.addVpp = function() {
            //show dialog
            $mdDialog.show({
                controller: function() {
                    var vm = this;
                    vm.vpp = {};
                    //function called when the cancel button ( 'x' in the top right) is clicked
                    vm.close = function() {
                        $mdDialog.cancel();
                    };

                    vm.finished = function(successful) {
                        if (successful) {
                            vm.close();
                            vm.waiting = false;
                            toastService.showToast('New VPP added!');
                            $window.location.reload(true);
                        } else {
                            vm.waiting = false;
                            toastService.showToast('Error adding new VPP');
                        }
                    };

                    //function called when the update button is clicked
                    vm.updateConfig = function() {
                        vm.waiting = true;
                        //TODO: Function to send a POST with the entered content in the form field
                        httpService.mountVpp(vm.vpp.name, vm.vpp.ip, vm.vpp.port, vm.vpp.un, vm.vpp.pw, vm.finished);
                    };
                },
                controllerAs: 'NewVppDialogCtrl',
                templateUrl: 'templates/new-vpp-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose:false
            })
        };

        vm.configInterface = function(interf) {
            //show dialog
            $mdDialog.show({
                controller: function() {
                    var vm = this;
                    var oldDescription = interf.description;
                    var oldAdminStatus = interf['admin-status'];
                    vm.interf = interf;
                    //function called when the cancel button ( 'x' in the top right) is clicked
                    vm.cancel = function() {
                        $mdDialog.cancel();
                        vm.revert();
                    };
                    vm.revert = function() {
                        interf['admin-status'] = oldAdminStatus;
                        interf.description = oldDescription
                    };
                    vm.success = function() {
                        $mdDialog.cancel();
                    };
                    //function called when the update button is clicked
                    vm.updateConfig = function() {
                        vm.waiting = true;

                        //send a POST with the entered content in the form field
                        httpService.updateInterface(vm.interf,vm.waiting,vm.cancel, vm.success);
                    };
                },
                controllerAs: 'ConfigInterfaceDialogCtrl',
                templateUrl: 'templates/config-interface-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose:false
            })
        };
        dataService.vppTopo.adaptToContainer();
    });

	vpp.register.controller('BridgeDomainsController', function(dataService, $location, $mdDialog, httpService, toastService) {
                var vm = this;
                vm.dataService = dataService;
                var nextApp =  new nx.ui.Application;
                nextApp.container(document.getElementById('bridge-domains-next-app'));
                dataService.bridgeDomainsTopo.attach(nextApp);

                window.addEventListener('resize', function () {
                        if ($location.path() === '/bridgedomains') {
                                dataService.bridgeDomainsTopo.adaptToContainer();
                        }
                });

                dataService.vppTopo.tooltipManager().nodeTooltip().close();
                dataService.bridgeDomainsTopo.tooltipManager().nodeTooltip().close();

                vm.bridgedomains = dataService.bridgedomains;
                vm.selectedBd = dataService.selectedBd;

                dataService.bridgeDomainsTopo.on('clickNode',function(topo,node) {
                    console.log(node);
                });

                vm.bdChanged = function() {
                        dataService.injectBridgeDomainsTopoElements();
                        dataService.buildTableContent();
                };

                vm.addBd = function() {
                        //show dialog
                        $mdDialog.show({
                                controller: function(httpService) {
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
                                                httpService.addBdToODL(vm.bd.name, vm.isDone);
                                        };
                                },
                                controllerAs: 'NewBdDialogCtrl',
                                templateUrl: 'templates/new-bd-dialog.html',
                                parent: angular.element(document.body),
                                clickOutsideToClose:false
                        })
                };

                vm.deploy = function() {
                    httpService.deploy();
                };

                vm.removeBd = function() {
                    if(vm.selectedBd.name) {
                            var successCallback = function(success) {
                                if (success) {
                                    console.log(vm.bridgedomains);
                                    _.remove(vm.bridgedomains, {
                                        name: vm.selectedBd.name
                                    });
                                    toastService.showToast('Bridge Domain Removed!');
                                    vm.selectedBd.name = '';
                                    dataService.clearInjectedInterfacesInBridgeDomainTopo();
                                    dataService.injectBdIntoBridgeDomainsTopo();
                                    dataService.tableContent.length = 0;
                                } else {
                                    toastService.showToast('Error removing Bridge Domain!');

                                }
                            };

                        httpService.removeBdFromOdl(vm.selectedBd.name, successCallback);
                    }
                };
    });
});


