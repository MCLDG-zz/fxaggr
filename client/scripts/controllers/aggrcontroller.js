var app = angular.module('fxaggr', ['ui.router', 'ui.grid', 'xeditable', 'chart.js']);

app.controller('fxaggrCtrl', ['$scope', '$timeout', '$http', '$state',
    function($scope, $timeout, $http, $state) {

  $scope.linechartdata = [
  ];
  $scope.onClick = function (points, evt) {
    console.log(points, evt);
  };

        var socket = io.connect();

        $scope.outliers = [];
        $scope.aggrconfig = [];
        $scope.pricestats = [];
        $scope.runtimestats = [];
        $scope.liquidityProviders = [];
        $scope.liquidityProvider;
        $scope.currencies = [];
        $scope.currencytotalstats = [];
        $scope.currencyfilterstats = [];
        /*
         * Handle the outliers being sent from node.js server via socket.io
         */
        socket.on('outlier', function(data) {
            $scope.outliers = data;
            //for some reason Angular's digest does not seem to pick up scope items
            //that are updated via socket. I haven't had time to research this yet
            $scope.$apply();
        });

        /*
         * Handle the runtimestats being sent from node.js server via socket.io
         */
        socket.on('runtimestats', function(data) {
            $scope.runtimestats = data;
            $scope.currencies = [];
            $scope.currencystats = [];
            for (var i=0; i < data.length; i++) {
                if (data[i]._id != "1") {
                    $scope.currencies.push(data[i]._id);
                    $scope.currencytotalstats.push(data[i].totalNumberOfEvents);
                    $scope.currencyfilterstats.push(data[i].totalNumberOfEvents);
                    $scope.linechartdata.push($scope.currencytotalstats);
                    $scope.linechartdata.push($scope.currencyfilterstats);
                }
            }
            $scope.$apply();
        });

        // /*
        //  * Send a request for a quote to the node.js server via socket.io
        //  */
        // $scope.send = function send() {
        //     socket.emit('ticker', $scope.newticker);
        //     //push onto the quotes array to ensure it displays on page, even if no valid quote exists for this ticker
        //     var data = {};
        //     data.ticker = $scope.newticker.toUpperCase();
        //     $scope.quotes.push(data);
        // };

        $scope.loadAggrConfig = function() {
            var httpReq = $http.get('/config/aggrconfig').
            success(function(data, status, headers, config) {
                //ensure we received a response
                if (data.length < 1) {
                    return;
                }
                $scope.aggrconfig = data[0];
            }).
            error(function(data, status, headers, config) {
                $scope.aggrconfig = {
                    "error retrieving aggrconfig": status
                };
            });
        };

        $scope.loadPriceStats = function() {
            var httpReq = $http.get('/config/aggrpricestats').
            success(function(data, status, headers, config) {
                //ensure we received a response
                if (data.length < 1) {
                    return;
                }
                $scope.pricestats = data;
            }).
            error(function(data, status, headers, config) {
                $scope.balance = {
                    "error retrieving aggrpricestatsconfig": status
                };
            });
        };

        $scope.saveCurrencyConfig = function(data, symbol) {
            //Update $scope.aggrconfig 
            var configFound = false;
            var i = 0;
            for (i = 0; i < $scope.aggrconfig.currencyconfig.length; i++) {
                if ($scope.aggrconfig.currencyconfig[i].symbol == symbol) {
                    configFound = true;
                    angular.extend($scope.aggrconfig.currencyconfig[i], data);
                    break;
                }
            }
            if (!configFound) {
                return "could not update AggrConfig - id not found: " + id;
            }
            return $http.post('/config/updateaggrconfig', $scope.aggrconfig);
        };

        /*
        * $scope.aggrconfig will have been updated directly since there is two-way binding
        * between the form and the $scope object. Here we just update the DB
        */
        $scope.saveGlobalConfig = function(data) {
            $scope.updateGlobalConfigResult = "Global Config updated successfully";
            $timeout(function() {
                $scope.updateGlobalConfigResult = false;
            }, 10000);
            return $http.post('/config/updateaggrconfig', $scope.aggrconfig);
        };

        $scope.showOutlierDetail = function() {
            $scope.selectedOutlier = this.item;
            $scope.showCurrentPrice = true;
            $scope.showPreviousPrice = true;
            $scope.showConfig = true;
            $scope.showPriceStats = true;

            $state.go('outlierdetail', {
                outlier: this.item._id
            });
        };

        // Add/remove liquidity providers
        $scope.addLiquidityProvider = function() {
            $scope.aggrconfig.globalconfig.liquidityproviders.push(this.globalconfig.addLiquidityProvider);
        }
        $scope.removeLiquidityProvider = function(provider) {
            if ($scope.aggrconfig.globalconfig.liquidityproviders.indexOf(this.globalconfigform.selectedliquidityprovider) != -1) {
                $scope.aggrconfig.globalconfig.liquidityproviders.splice($scope.aggrconfig.globalconfig.liquidityproviders.indexOf(this.globalconfigform.selectedliquidityprovider),1);
            }
        }

        // $scope.updateBalance = function() {
        //     var httpReq = $http.post('/users/updatebalance', $scope.balance[0]).
        //     success(function(data, status, headers, config) {}).
        //     error(function(data, status, headers, config) {});
        // };
        // /*
        //  * add item to watchlist
        //  */
        // $scope.addToWatchlist = function(ticker) {
        //     //If symbol is invalid, do not add to watchlist
        //     if (!$scope.isValidSymbol(ticker)) {
        //         $scope.addWatchlistResult = $sce.trustAsHtml("<strong>" + ticker + "</strong> is invalid - cannot add to watchlist");
        //         $timeout(function() {
        //             $scope.addWatchlistResult = false;
        //         }, 5000);
        //         return false;
        //     }

        //     //If ticker is already in watchlist, no action required
        //     for (var i = 0; i < $scope.tickerList.watchlist.length; i++) {
        //         if (ticker == $scope.tickerList.watchlist[i]) {
        //             $scope.addWatchlistResult = $sce.trustAsHtml("<strong>" + ticker + "</strong> is already in your watchlist");
        //             $timeout(function() {
        //                 $scope.addWatchlistResult = false;
        //             }, 5000);
        //             return false;
        //         }
        //     }

        //     //Otherwise, add to the watchlist and update the DB
        //     $scope.tickerList.watchlist.push(ticker);
        //     var httpReq = $http.post('/users/updatewatchlist', $scope.tickerList).
        //     success(function(data, status, headers, config) {
        //         //if successful, send to server to obtain a quote, and add to the quotes array 
        //         // - this will impact the display on the Watchlist page, which is bound to the quotes array
        //         //$scope.newticker = ticker;
        //         //$scope.send();
        //         $scope.addWatchlistResult = $sce.trustAsHtml("<strong>" + ticker + "</strong> successfully added to watchlist");
        //         $timeout(function() {
        //             $scope.addWatchlistResult = false;
        //         }, 5000);
        //     }).
        //     error(function(data, status, headers, config) {});
        // };
        //This function will execute once the controller is initialised. 
        $scope.init = function() {
            $scope.loadAggrConfig();
            $scope.loadPriceStats();
        };

        //Run the init function on startup
        $scope.init();
    }
]);

app.run(function(editableOptions) {
    editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
});


app.config(function (ChartJsProvider) {
    // Configure all charts
    ChartJsProvider.setOptions({
      colours: ['#97BBCD', '#DCDCDC', '#F7464A', '#46BFBD', '#FDB45C', '#949FB1', '#4D5360'],
      responsive: true
    });
    // Configure all doughnut charts
    ChartJsProvider.setOptions('Doughnut', {
      animateScale: true
    });
  });