/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

var modules = [
    // module
    'app/vpp/vpp.module',
    // services
    'app/vpp/services/vpp.services',
    'app/vpp/services/inventory.service',
    'app/vpp/services/bdm.service',
    'app/vpp/services/bdm.bridgedomain.service',
    'app/vpp/services/bdm.interface.service',
    'app/vpp/services/bdm.vpp.service',
    //controllers
    'app/vpp/controllers/inventory.controller',
    'app/vpp/controllers/bdm.controller',
    'app/vpp/controllers/bdm.bridgedomain.controller',
    'app/vpp/controllers/bdm.vpp.controller',
    'app/vpp/controllers/bdm.interface.controller'
];


define(modules, function(vpp) {

    vpp.controller('vppCtrl', ['$scope', '$rootScope', '$timeout', 'toastService', '$mdSidenav', '$mdDialog',
        function ($scope, $rootScope, $timeout, toastService, $mdSidenav, $mdDialog) {

            $rootScope['section_logo'] = 'src/app/vpp/assets/images/vpp.gif';
            $scope.view_path =  'src/app/vpp/views/';
    		
    	    $scope.mainView = "inventory";

    	    $scope.setMainView = function(viewName) {
    	    	$scope.mainView = viewName;
    	    };


	}]);


});


