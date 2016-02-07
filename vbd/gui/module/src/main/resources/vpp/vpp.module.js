/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
define(['angularAMD', 'app/routingConfig', 'ui-bootstrap', 'Restangular', 'angular-animate', 'angular-aria',
        'angular-material', 'angular-smart-table', 'angular-translate',
        'angular-translate-loader-partial', 'lodash'], function() {

  var vpp = angular.module('app.vpp', ['ui.router.state','app.core', 'ui.bootstrap', 'restangular', 'ngAnimate',
                           'ngAria', 'ngMaterial', 'smart-table', 'pascalprecht.translate']);

  vpp.register = vpp;

  vpp.config(function ($stateProvider, $compileProvider, $controllerProvider, $provide, $translateProvider, $translatePartialLoaderProvider, NavHelperProvider, $filterProvider, $mdThemingProvider) {

    $translatePartialLoaderProvider.addPart('app/vpp/assets/data/locale');

    vpp.register = {
      directive : $compileProvider.directive,
      controller : $controllerProvider.register,
      filter: $filterProvider.register,
      factory : $provide.factory,
      service : $provide.service
    };

    NavHelperProvider.addControllerUrl('app/vpp/controllers/vpp.controller');
    NavHelperProvider.addToMenu('vpp', {
      "link": "#/vpp/index",
      "active": "main.vpp",
      "title": "VPP",
      "icon": "icon-code-fork",
      "page": {
        "title": "VPP",
        "description": "VPP"
      }
    });

    var access = routingConfig.accessLevels;
      $stateProvider.state('main.vpp', {
          url: 'vpp',
          abstract: true,
          views : {
            'content' : {
              templateUrl: 'src/app/vpp/views/root.tpl.html'
            }
          }
      });

      $stateProvider.state('main.vpp.index', {
          url: '/index',
          access: access.admin,
          views: {
              '': {
                  controller: 'vppCtrl',
                  templateUrl: 'src/app/vpp/views/index.tpl.html'
              }
          }
      });

      $mdThemingProvider.definePalette('odl-gray', {
          '50': 'BDBDBD',
          '100': 'BDBDBD',
          '200': 'BDBDBD',
          '300': 'BDBDBD',
          '400': 'BDBDBD',
          '500': '414040',
          '600': '414040',
          '700': '414040',
          '800': '414040',
          '900': '414040',
          'A100': '414040',
          'A200': '414040',
          'A400': '414040',
          'A700': '414040',
          'contrastDefaultColor': 'light',    // whether, by default, text (contrast)
                                              // on this palette should be dark or light
          'contrastDarkColors': [],
          'contrastLightColors': undefined    // could also specify this if default was 'dark'
    });

    $mdThemingProvider.definePalette('odl-orange', {
        '50': 'FFA500',
        '100': 'FFA500',
        '200': 'FFA500',
        '300': 'FFA500',
        '400': 'FFA500',
        '500': 'FFA500',
        '600': 'FFA500',
        '700': 'FFA500',
        '800': 'FFA500',
        '900': 'FFA500',
        'A100': 'FFA500',
        'A200': 'FFA500',
        'A400': 'FFA500',
        'A700': 'FFA500',
        'contrastDefaultColor': 'light',    // whether, by default, text (contrast)
                                            // on this palette should be dark or light
        'contrastDarkColors': [],
        'contrastLightColors': undefined    // could also specify this if default was 'dark'
    });

    $mdThemingProvider.theme('default').dark()
        .primaryPalette('odl-gray',{
            'default': '500'
        })
        .accentPalette('odl-orange');

  });

  return vpp;
});
