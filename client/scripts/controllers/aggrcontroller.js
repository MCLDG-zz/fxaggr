var app = angular.module('fxaggr', ['ui.router', 'ui.grid', 'xeditable', 'chart.js']);

app.controller('fxaggrCtrl', ['$scope', '$timeout', '$http', '$state',
    function($scope, $timeout, $http, $state) {

        var socket = io.connect();

        $scope.outliers = [];
        $scope.aggrconfig = [];
        $scope.pricestats = [];
        $scope.runtimestats = [];
        $scope.liquidityProviders = [];
        $scope.liquidityProvider;
        $scope.countryToCurrencyMap = null;

        /*
         * Scope items used for graphing statistics
         */
         //Unbelievable. After hours of wondering why my line chart doesn't work it seems
         //the charting package required a multi-dimensional array for a single line chart
        $scope.chartBestBidAskLabels = ["Applied Best Bid/Ask", "Applied Primary Bid/Ask"];
        $scope.chartBestBidAsk = [];
        $scope.totalNumberConfiguredBestBidAskEvents;
        $scope.totalNumberAppliedBestBidAskEvents;
        $scope.totalNumberAppliedPrimaryBidAskEvents;
        $scope.chartavgprocessingtime = [];
        $scope.avgprocessingtime = [];
        $scope.avgprocessingtimelabels = [1,2,3,4,5,6,7,8,9,10];
        $scope.currencies = [];
        $scope.filtertype = [];
        $scope.filtertypecount = [];
        $scope.filtertypechartdata = [];
        $scope.currencychartdata = [];
        $scope.currencyevents = [];
        $scope.currencyfilteredevents = [];
        $scope.totalchartdata = [];
        $scope.totalevents = [];
        $scope.totalfilteredevents = [];
        $scope.totaltrendevents = [];
        $scope.totaltrendfilteredevents = [];
        $scope.totaltrendchartdata = [];
        $scope.totaltrendchartdata[0] = $scope.totaltrendevents;
        $scope.totaltrendchartdata[1] = $scope.totaltrendfilteredevents;
        $scope.totalchartlabel = ['Total vs. Filtered'];
        $scope.onClick = function(points, evt) {
            console.log(points, evt);
        };


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
            $scope.currencyevents = [];
            $scope.currencyfilteredevents = [];
            $scope.totalchartdata = [];
            $scope.totalevents = [];
            $scope.totalfilteredevents = [];
            for (var i = 0; i < data.length; i++) {
                if (data[i]._id != "1") {
                    $scope.currencies.push(data[i]._id);
                    $scope.currencyevents.push(data[i].totalNumberOfEvents);
                    $scope.currencyfilteredevents.push(data[i].totalNumberOfFilteredEvents);
                }
                else {
                    $scope.avgprocessingtime.push(data[i].avgProcessingTime);
                    $scope.avgprocessingtime.splice(0, $scope.avgprocessingtime.length - 10);
                    $scope.chartavgprocessingtime[0] = $scope.avgprocessingtime;
                    $scope.totalNumberConfiguredBestBidAskEvents = data[i].totalNumberConfiguredBestBidAskEvents;
                    $scope.totalNumberAppliedBestBidAskEvents = data[i].totalNumberAppliedBestBidAskEvents;
                    $scope.totalNumberAppliedPrimaryBidAskEvents = data[i].totalNumberAppliedPrimaryBidAskEvents;
                    $scope.chartBestBidAsk[0] = $scope.totalNumberAppliedBestBidAskEvents;
                    $scope.chartBestBidAsk[1] = $scope.totalNumberAppliedPrimaryBidAskEvents;
                    $scope.filtertype = [];
                    $scope.filtertypecount = [];
                    for (var key in data[i].numberPerFilteredReason) {
                        if (data[i].numberPerFilteredReason.hasOwnProperty(key)) {
                            $scope.filtertype.push(key);
                            $scope.filtertypecount.push(data[i].numberPerFilteredReason[key]);
                            $scope.filtertypechartdata[0] = $scope.filtertypecount;
                        }
                    }
                    $scope.totalevents.push(data[i].totalNumberOfEvents);
                    $scope.totalfilteredevents.push(data[i].totalNumberOfFilteredEvents);
                    $scope.totalchartdata[0] = $scope.totalevents;
                    $scope.totalchartdata[1] = $scope.totalfilteredevents;
                    $scope.totaltrendevents.push(data[i].totalNumberOfEvents);
                    $scope.totaltrendfilteredevents.push(data[i].totalNumberOfFilteredEvents);
                }
            }
            $scope.currencychartdata[0] = $scope.currencyevents;
            $scope.currencychartdata[1] = $scope.currencyfilteredevents;
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
            if (!(typeof this.lp === "undefined" || this.lp == null)) {
                $scope.aggrconfig.globalconfig.liquidityproviders.push(this.lp.newProvider);
                this.lp = null;
            }
        }
        $scope.removeLiquidityProvider = function(providerIndex) {
            if ($scope.aggrconfig.globalconfig.liquidityproviders.length >= providerIndex) {
                $scope.aggrconfig.globalconfig.liquidityproviders.splice(providerIndex, 1);
            }
        }
        $scope.selectLiquidityProvider = function(providerIndex) {
            $scope.selectedLiquidityProvider = providerIndex;
        }
        $scope.moveLiquidityProvider = function(direction) {
            var tmp;
            if (direction == "up") {
                if ($scope.selectedLiquidityProvider > 0) {
                    tmp = $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider];
                    $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider] = 
                        $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider - 1];
                    $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider - 1] = tmp;
                    $scope.selectedLiquidityProvider-=1;
                }
            }
            else if (direction == "down") {
                if ($scope.selectedLiquidityProvider < $scope.aggrconfig.globalconfig.liquidityproviders.length - 1) {
                    tmp = $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider];
                    $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider] = 
                        $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider + 1];
                    $scope.aggrconfig.globalconfig.liquidityproviders[$scope.selectedLiquidityProvider + 1] = tmp;
                    $scope.selectedLiquidityProvider+=1;
                }
            }
        }

        $scope.loadCountryToCurrency = function() {
            var httpReq = $http.get('/ref/countrytocurrency').
            success(function(data, status, headers, config) {
                //ensure we received a response
                if (data.length < 1) {
                    return;
                }
                $scope.countryToCurrencyMap = data[0];
            }).
            error(function(data, status, headers, config) {
                $scope.countryToCurrencyMap = {
                    "error retrieving country to currency mapping": status
                };
            });
        };

        $scope.getCountryForCurrency = function(currency) {
            for (var prop in $scope.countryToCurrencyMap) {
                if ($scope.countryToCurrencyMap.hasOwnProperty(prop)) {
                    if ($scope.countryToCurrencyMap[prop] === currency) {
                        return prop;
                    }
                }
            }
            return null;
        };

        $scope.getFlagForFirstCurrency = function(symbol) {
            var firstCurrency = symbol.substr(0, 3);
            if (firstCurrency) {
                return "views/images/flags/" + $scope.getCountryForCurrency(firstCurrency) + ".png";
            }
        };

        $scope.getFlagForSecondCurrency = function(symbol) {
            var secondCurrency = symbol.substr(3, 3);
            if (secondCurrency) {
                return "views/images/flags/" + $scope.getCountryForCurrency(secondCurrency) + ".png";
            }
        };


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
            $scope.loadCountryToCurrency();
        };

        //Run the init function on startup
        $scope.init();
    }
]);

app.run(function(editableOptions) {
    editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
});


app.config(function(ChartJsProvider) {
    // Configure all charts
    ChartJsProvider.setOptions({
        colours: ['#97BBCD', '#DCDCDC', '#F7464A', '#46BFBD', '#FDB45C', '#949FB1', '#4D5360'],
        responsive: true
    });
    // Configure all doughnut charts
    ChartJsProvider.setOptions('Doughnut', {
        animateScale: true
    });
    // Configure all doughnut charts
    ChartJsProvider.setOptions('Line', {
        animateScale: false,
        animation : false
    });
});