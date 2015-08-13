var app = angular.module('fxaggr', ['ui.router', 'ui.grid']);

app.controller('fxaggrCtrl', ['$scope', '$timeout', '$http', 
    function($scope, $timeout, $http) {

        var socket = io.connect();

        $scope.outliers = [];
        /*
         * Handle the quote being sent from node.js server via socket.io
         */
        socket.on('outlier', function(data) {
            $scope.outliers = [];
            $scope.outliers.push(data);
            //for some reason Angular's digest does not seem to pick up scope items
            //that are updated via socket. I haven't had time to research this yet
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

        // $scope.loadBalance = function() {
        //     var httpReq = $http.get('/users/balance').
        //     success(function(data, status, headers, config) {
        //         //ensure we received a response
        //         if (data.length < 1) {
        //             return;
        //         }
        //         $scope.balance = data;
        //         $scope.balance[0].accountvalue = Number(data[0].cashbalance) + Number(data[0].assetvalue);
        //     }).
        //     error(function(data, status, headers, config) {
        //         $scope.balance = {
        //             "error retrieving balance": status
        //         };
        //     });
        // };

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
        };

        //Run the init function on startup
        $scope.init();
    }
]);
