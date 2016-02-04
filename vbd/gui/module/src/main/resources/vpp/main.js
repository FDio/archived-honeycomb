require.config({
  paths : {
    'angular-material' : 'app/vpp/bower_components/angular-material/angular-material.min',
    'angular-animate'  : 'app/vpp/bower_components/angular-animate/angular-animate.min',
    'angular-aria' :  'app/vpp/bower_components/angular-aria/angular-aria.min',
    'angular-smart-table' :  'app/vpp/bower_components/angular-smart-table/dist/smart-table',
    'lodash' : 'app/vpp/assets/js/lodash.min',
    'next': 'app/vpp/assets/js/next',
  },

  shim : {
    'angular-material' : ['angular'],
    'angular-animate' : ['angular'],
    'angular-aria' : ['angular'],
    'angular-smart-table' : ['angular'],
  },

});

define(['app/vpp/vpp.module']);
