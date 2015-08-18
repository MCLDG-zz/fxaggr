var fxApp = angular.module('fxaggr');

// Configure the navigation and routing - this uses ui-router
fxApp.config(function($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.otherwise('/home');

    $stateProvider

    // HOME STATES AND NESTED VIEWS ========================================
    .state('home', {
        url: '/home',
        templateUrl: 'views/outliers.html'
    })

    .state('aggrconfig', {
        url: '/aggrconfig',
        templateUrl: 'views/aggrconfig.html'
    })

    .state('pricestats', {
        url: '/pricestats',
        templateUrl: 'views/pricestats.html'
    })

    .state('runtimestats', {
        url: '/runtimestats',
        templateUrl: 'views/runtimestats.html'
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
