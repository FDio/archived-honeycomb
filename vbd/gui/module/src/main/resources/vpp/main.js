/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
require.config({
  paths : {
    'angular-material' : 'app/vpp/bower_components/angular-material/angular-material.min',
    'angular-animate'  : 'app/vpp/bower_components/angular-animate/angular-animate.min',
    'angular-aria' :  'app/vpp/bower_components/angular-aria/angular-aria.min',
    'angular-smart-table' :  'app/vpp/bower_components/angular-smart-table/dist/smart-table',
    'lodash' : 'app/vpp/assets/js/lodash.min',
    'next': 'app/vpp/assets/js/next',
    'angular-ui-grid': 'app/vpp/bower_components/angular-ui-grid/ui-grid.min',
  },

  shim : {
    'angular-material' : ['angular'],
    'angular-animate' : ['angular'],
    'angular-aria' : ['angular'],
    'angular-smart-table' : ['angular'],
    'angular-ui-grid' : ['angular'],
    'lodash' : {exports: '_'}
  },

});

define(['app/vpp/vpp.module']);
