var fxApp = angular.module('fxaggr');

// Configure the navigation and routing - this uses ui-router
fxApp.config(function($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.otherwise('/dashboard');

    $stateProvider

    // HOME STATES AND NESTED VIEWS ========================================
    .state('home', {
        url: '/dashboard',
        templateUrl: 'views/dashboard.html'
    })

    .state('aggrconfig', {
        url: '/aggrconfig',
        templateUrl: 'views/aggrconfig.html'
    })

    .state('globalconfig', {
        url: '/globalconfig',
        templateUrl: 'views/globalconfig.html'
    })

    .state('currencyconfig', {
        url: '/currencyconfig',
        templateUrl: 'views/currencyconfig.html'
    })

    .state('providerconfig', {
        url: '/providerconfig',
        templateUrl: 'views/providerconfig.html'
    })

    .state('pricestats', {
        url: '/pricestats',
        templateUrl: 'views/pricestats.html'
    })

    .state('runtimestats', {
        url: '/runtimestats',
        templateUrl: 'views/runtimestats.html'
    })

    .state('dashboard', {
        url: '/dashboard',
        templateUrl: 'views/dashboard.html'
    })

    .state('outlierdetail', {
        url: '/outlierdetail/:outlierID',
        templateUrl: 'views/outlierdetail.html',
        controller: function($scope, $stateParams) {
            $scope.outlierID = $stateParams.outlierID;
        }
    })

    // .state('showsymbol', {
    //     url: '/showsymbol/:symbolID',
    //     templateUrl: 'views/partials/showsymbol.html',
    //     controller: function($scope, $stateParams) {
    //         $scope.symbolID = $stateParams.symbolID;
    //     }
    // });
});
