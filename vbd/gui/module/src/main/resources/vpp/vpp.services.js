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

    vpp.register.service('httpService', function($http, dataService, $log, toastService, $q) {
    /*
        this.requestVpps = function() {
            $http.get(vppsUrl, {
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": "Basic " + btoa(username + ":" + password)
                }
            }).then(function successCallback(response) {
                dataService.vpps = processVpps(response.data);
                var counter = 0;
                angular.forEach(dataService.vpps, function(vpp) {
                    counter++;
                    var isFinalInterfaceRequest = counter === dataService.vpps.length;
                    requestInterfaces(vpp, isFinalInterfaceRequest);
                });
            }, function errorCallback(response) {
                toastService.showToast('Error retrieving VPPs');
                $log.error(response);
            });
        };

        var requestInterfaces = function(vpp, isFinalInterfaceRequest) {
            $http.get(interfacesUrl+vpp['node-id']+interfacesUri, {
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": "Basic " + btoa(username + ":" + password)
                }
            }).then(function successCallback(response) {
                vpp.interfaces = response.data.interfaces.interface;
                //------
                $http.get(operationalUrl+vpp['node-id']+operationalUri, {
                    headers: {
                        'Content-Type': 'application/json',
                        "Authorization": "Basic " + btoa(username + ":" + password)
                    }
                }).then(function successCallback(response) {
                    vpp.operational = response.data;
                    var interfaceStates = vpp.operational['ietf-interfaces:interfaces-state'].interface;
                    _.forEach(interfaceStates, function(interf) {
                        var matchedInterface = _.find(vpp.interfaces,{name: interf.name});
                        if (matchedInterface) {
                            matchedInterface['oper-status'] = interf['oper-status'];
                            matchedInterface['admin-status'] = interf['admin-status'];
                        }
                    });
                    if (isFinalInterfaceRequest) {
                        dataService.updateInventoryTopology();
                    }
                }, function errorCallback() {
                    $log.error('Error receiving interface states for '+vpp['node-id']);
                });
                //-------
            }, function errorCallback() {
                $log.error('Error receiving interfaces for '+vpp['node-id']);
            });
        };

        var requestBridgeDomains = function() {
            $http.get(bridgedomainsUrl, {
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": "Basic " + btoa(username + ":" + password)
                }
            }).then(function successCallback(response) {
                dataService.setBridgeDomains(response.data["v3po:vpp-state"]);
                // done loading...
                dataService.requestsFinished();
            }, function errorCallback(response) {
                toastService.showToast('Error retrieving Bridge Domains');
                $log.error(response);
            });
        };

        this.deploy = function() {
            var errorOccurred = false;

            var success = function(succeeded, lastAssignment) {
                if (!errorOccurred && !succeeded) {
                    toastService.showToast('Error deploying configuration!');
                    errorOccurred = true;
                } else if (!errorOccurred && succeeded && lastAssignment){
                    toastService.showToast('Deployed! Bridge Domain Validated');
                }
            };
            for (var x = 0; x < dataService.changedInterfaces.length; x++) {
                this.updateAssignmentInODL(dataService.changedInterfaces[x], success, x === dataService.changedInterfaces.length - 1);
            }
            dataService.changedInterfaces.length = 0;
        };

        this.updateAssignmentInODL = function(interf, success, lastAssignment) {
                var postData = {
                    "interface": [
                        {
                            "name": interf.name,
                            "link-up-down-trap-enable": "disabled",
                            "v3po:ethernet": {
                                "mtu": interf['v3po:ethernet']['mtu']
                            },
                            "v3po:l2": {
                                "bridge-domain": interf['v3po:l2']['bridge-domain'],
                                "split-horizon-group": 0,
                                "bridged-virtual-interface": false
                            },
                            "type": interf.type,
                            "description": "",
                            "enabled": true
                        }
                    ]
                };
                $http.put(interfacesConfigUrl + encodeURIComponent(interf.name), postData, {
                    headers: {
                        'Content-Type': 'application/yang.data+json',
                        "Authorization": "Basic " + btoa(username + ":" + password)
                    }
                }).then(function successCallback(response) {
                    //...
                    success(true, lastAssignment);
                }, function errorCallback(response) {
                    success(false, lastAssignment);
                    $log.error(response);
                });
             };


        this.addBdToODL = function(bdName, isDone) {
            var putData = {
                "bridge-domain": [
                    {
                        "name": bdName,
                        "flood": "true",
                        "forward": "true",
                        "learn": "true",
                        "unknown-unicast-flood": "true",
                        "arp-termination": "false"
                    }
                ]
            };
            $http.put(newBridgeDomainUrl+bdName, putData,
                {
                    headers: {
                        'Content-Type': 'application/yang.data+json',
                        "Authorization": "Basic " + btoa(username + ":" + password)
                    }
                }).then(function successCallback() {
                toastService.showToast('Bridge Domain Added!');
                isDone('success');
            }, function errorCallback(response) {
                $log.error(response);
                toastService.showToast('Error adding Bridge Domain.');
                isDone('failure');
            });
        };

        this.updateInterface = function(interf, isWaiting, cancel, success) {
            $http.get(interfacesOperUrl + encodeURIComponent(interf.name), {
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": "Basic " + btoa(username + ":" + password)
                }
            }).then(function successCallback(r) {
                var receivedData = r.data.interface[0];
                var postData = {
                    "interface": [
                        {
                            "name": receivedData.name,
                            "link-up-down-trap-enable": "disabled",
                            "v3po:ethernet": {
                                "mtu": receivedData['v3po:ethernet']['mtu']
                            },
                            "v3po:l2": {
                                "bridge-domain": receivedData['v3po:l2']['bridge-domain'],
                                "split-horizon-group": 0,
                                "bridged-virtual-interface": false
                            },
                            "type": receivedData.type,
                            "description": interf.description,
                            "enabled": interf['admin-status'] === 'up' ? true : false
                        }
                    ]
                };
                $http.put(interfacesConfigUrl + encodeURIComponent(interf.name), postData,
                    {
                        headers: {
                            'Content-Type': 'application/yang.data+json',
                            "Authorization": "Basic " + btoa(username + ":" + password)
                        }
                    }).then(function successCallback() {
                    toastService.showToast('Updated!');
                    isWaiting = false;
                    success();
                }, function errorCallback(response) {
                    $log.error(response);
                    toastService.showToast('Error updating');
                    isWaiting = false;
                    cancel();
                });
            }, function errorCallback(response) {
                $log.error(response);
                toastService.showToast('Error updating');
                isWaiting = false;
                cancel();
            });
        };

        this.removeBdFromOdl = function(name,success) {
            $http.delete(newBridgeDomainUrl+name, {
                    headers: {
                        'Content-Type': 'application/json',
                        "Authorization": "Basic " + btoa(username + ":" + password)
                    }
                })
                .then(function successCallback() {
                    success(true);
                }, function errorCallback() {
                    success(false);
                });
        };

        this.configVpp = function(name,ip,port,un,pw,finishedSuccessfullyCallback) {

            var putData =  '\
            <module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">\
            <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">prefix:sal-netconf-connector</type>\
            <name>'+name+'</name>\
            <address xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+ip+'</address>\
            <port xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+port+'</port>\
                <username xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+un+'</username>\
                <password xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+pw+'</password>\
                <tcp-only xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">false</tcp-only>\
                <event-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:netty">prefix:netty-event-executor</type>\
            <name>global-event-executor</name>\
            </event-executor>\
            <binding-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:binding">prefix:binding-broker-osgi-registry</type>\
            <name>binding-osgi-broker</name>\
            </binding-registry>\
            <dom-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom">prefix:dom-broker-osgi-registry</type>\
            <name>dom-broker</name>\
            </dom-registry>\
            <client-dispatcher xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:config:netconf">prefix:netconf-client-dispatcher</type>\
            <name>global-netconf-dispatcher</name>\
            </client-dispatcher>\
            <processing-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:threadpool">prefix:threadpool</type>\
            <name>global-netconf-processing-executor</name>\
            </processing-executor>\
            </module>';

            $http.put(configVpp+name, putData, {
                headers: {
                    "Accept": 'application/xml',
                    "Content-Type": 'application/xml',
                    "Authorization": "Basic " + btoa(username + ":" + password)
                }
            }).then(function successCallback(response) {
                //...
                finishedSuccessfullyCallback(true);
            }, function errorCallback(response) {
                finishedSuccessfullyCallback(false);
                $log.error(response);
            });
        };

        this.deleteVpp = function(name, finishedSuccessfullyCallback) {
            $http.delete(configVpp+name, {
                headers: {
                    "Accept": 'application/xml',
                    "Content-Type": 'application/xml',
                    "Authorization": "Basic " + btoa(username + ":" + password)
                }
            }).then(function successCallback(response) {
                //...
                finishedSuccessfullyCallback(true);
            }, function errorCallback(response) {
                finishedSuccessfullyCallback(false);
                $log.error(response);
            }); */
        });

    vpp.register.service('dataService', function() {

        nx.graphic.Icons.registerIcon("bd", "src/app/vpp/assets/images/bd1.svg", 45, 45);
        nx.graphic.Icons.registerIcon("interf", "src/app/vpp/assets/images/interf.svg", 45, 45);
/*
        var Topo = function() {
            return new nx.graphic.Topology({
                adaptive:true,
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
        };

        this.bridgeDomainsTopo = Topo();
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
        }; */

    });

	vpp.register.factory('VppService', function(VPPRestangular, VPPRestangularXml) {
		var s = {};

        var Vpp = function(name, ipAddress, port, username, password, status) {
            this.name = name || null;
            this.ipAddress = ipAddress || null;
            this.port = port || null;
            this.username = username || null;
            this.password = password || null;
            this.status = status || null;
        };

        s.createObj = function(name, ipAddress, port, username, password, status) {
            return new Vpp(name, ipAddress, port, username, password, status);
        };

        s.getVppList = function(successCallback, errorCallback) {
        	var restObj = VPPRestangular.one('restconf').one('operational').one('network-topology:network-topology').one('topology').one('topology-netconf');

        	restObj.get().then(function(data) {
        		successCallback(data);
            }, function(res) {
                errorCallback(res);
            });
        };

        s.deleteVpp = function(vpp, finishedSuccessfullyCallback) {
            console.log(vpp);
            var restObj = VPPRestangular.one('restconf').one('config').one('network-topology:network-topology').one('topology').one('topology-netconf').one('node').one('controller-config').one('yang-ext:mount').one('config:modules').one('module').one('odl-sal-netconf-connector-cfg:sal-netconf-connector').one(vpp.name);

            restObj.remove().then(function() {
                finishedSuccessfullyCallback(true);
            }, function(res) {
                finishedSuccessfullyCallback(false);
            });
        };

        s.editVpp = function(name,ip,port,un,pw,finishedSuccessfullyCallback) {
                var putData =  '\
            <module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">\
            <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">prefix:sal-netconf-connector</type>\
            <name>'+name+'</name>\
            <address xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+ip+'</address>\
            <port xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+port+'</port>\
                <username xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+un+'</username>\
                <password xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+pw+'</password>\
                <tcp-only xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">false</tcp-only>\
                <event-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:netty">prefix:netty-event-executor</type>\
            <name>global-event-executor</name>\
            </event-executor>\
            <binding-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:binding">prefix:binding-broker-osgi-registry</type>\
            <name>binding-osgi-broker</name>\
            </binding-registry>\
            <dom-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom">prefix:dom-broker-osgi-registry</type>\
            <name>dom-broker</name>\
            </dom-registry>\
            <client-dispatcher xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:config:netconf">prefix:netconf-client-dispatcher</type>\
            <name>global-netconf-dispatcher</name>\
            </client-dispatcher>\
            <processing-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:threadpool">prefix:threadpool</type>\
            <name>global-netconf-processing-executor</name>\
            </processing-executor>\
            </module>';
            //var configVpp = '/api/restconf/config/network-topology:network-topology/topology/topology-netconf/node/controller-config/yang-ext:mount/config:modules/module/odl-sal-netconf-connector-cfg:sal-netconf-connector/';


            var restObj = VPPRestangularXml.one('restconf').one('config').one('network-topology:network-topology').one('topology').one('topology-netconf').one('node').one('controller-config').one('yang-ext:mount').one('config:modules').one('module').one('odl-sal-netconf-connector-cfg:sal-netconf-connector').one(name);

            restObj.customPUT(putData).then(function() {
                finishedSuccessfullyCallback(true);
            }, function(res) {
                finishedSuccessfullyCallback(false);
            });
        };

        s.mountVpp = function(name,ip,port,un,pw,finishedSuccessfullyCallback) {

            var postData =  '\
            <module xmlns="urn:opendaylight:params:xml:ns:yang:controller:config">\
            <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">prefix:sal-netconf-connector</type>\
            <name>'+name+'</name>\
            <address xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+ip+'</address>\
            <port xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+port+'</port>\
                <username xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+un+'</username>\
                <password xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">'+pw+'</password>\
                <tcp-only xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">false</tcp-only>\
                <event-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:netty">prefix:netty-event-executor</type>\
            <name>global-event-executor</name>\
            </event-executor>\
            <binding-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:binding">prefix:binding-broker-osgi-registry</type>\
            <name>binding-osgi-broker</name>\
            </binding-registry>\
            <dom-registry xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom">prefix:dom-broker-osgi-registry</type>\
            <name>dom-broker</name>\
            </dom-registry>\
            <client-dispatcher xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:config:netconf">prefix:netconf-client-dispatcher</type>\
            <name>global-netconf-dispatcher</name>\
            </client-dispatcher>\
            <processing-executor xmlns="urn:opendaylight:params:xml:ns:yang:controller:md:sal:connector:netconf">\
                <type xmlns:prefix="urn:opendaylight:params:xml:ns:yang:controller:threadpool">prefix:threadpool</type>\
            <name>global-netconf-processing-executor</name>\
            </processing-executor>\
            </module>';

            var restObj = VPPRestangularXml.one('restconf').one('config').one('opendaylight-inventory:nodes').one('node').one('controller-config').one('yang-ext:mount');

            restObj.post('config:modules', postData).then(function() {
                finishedSuccessfullyCallback(true);
            }, function(res) {
                finishedSuccessfullyCallback(false);
            });
        };

        return s;
	});

    vpp.register.factory('InterfaceService', function(VPPRestangular) {
        var s = {};

        s.getInterfaceList = function(vppName,successCallback, errorCallback) {
            var restObj = VPPRestangular.one('restconf').one('operational').one('network-topology:network-topology').one('topology').one('topology-netconf').one('node').one(vppName).one('yang-ext:mount').one('ietf-interfaces:interfaces-state');

            restObj.get().then(function(data) {
                successCallback(data);
            }, function(res) {
                errorCallback(res);
            });
        };

        return s;
    });

});