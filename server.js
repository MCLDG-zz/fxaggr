var http = require('http');
var async = require('async');
var socketio = require('socket.io');
var express = require('express');
var path = require('path');
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var mongo = require('mongodb');
var monk = require('monk');
var db = monk('localhost:27017/fxaggr');
var xml2js = require('xml2js');

var routes = require('./routes/index');
var config = require('./routes/config');
var fxaggr = require('./routes/fxaggr');
var referencedata = require('./routes/ref');

var app = express();
var server = http.createServer(app);
var io = socketio.listen(server);

// view engine setup
//app.set('views', path.join(__dirname, 'client/views'));
//app.set('view engine', 'jade');

// uncomment after placing your favicon in /public
//app.use(favicon(__dirname + '/public/favicon.ico'));
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded());
app.use(cookieParser());
//app.use(express.static(path.join(__dirname, 'public')));
app.use(express.static(path.resolve(__dirname, 'client')));

// Make our db accessible to our router
app.use(function(req, res, next) {
	req.db = db;
	next();
});

app.use('/', routes);
app.use('/config', config);
app.use('/fxaggr', fxaggr);
app.use('/ref', referencedata);

/// catch 404 and forwarding to error handler
app.use(function(req, res, next) {
	var err = new Error('Not Found');
	err.status = 404;
	next(err);
});

module.exports = app;

var tickers = [];
var sockets = [];
var FETCH_INTERVAL = 3000;
var OUTLIER_LIMIT = 100;

io.on('connection', function(socket) {

	sockets.push(socket);

	socket.on('ticker', function(msg) {});

	sendOutliersToClients(socket);
	sendRuntimeStatsToClients(socket);

	//Every N seconds
	var timer = setInterval(function() {
		sendOutliersToClients(socket);
		sendRuntimeStatsToClients(socket);
	}, FETCH_INTERVAL);

	socket.on('error', function(msg) {
		console.log("Socket.io error. Message: " + msg);
	});
});

function sendOutliersToClients(socket) {
	var collection = db.get('priceoutliers');
	collection.find({}, {
			limit: 100
		},
		function(e, docs) {
			//ensure the response contains a valid document
			if (!docs) {
				return;
			}
			socket.emit('outlier', docs);
		});
}

function sendRuntimeStatsToClients(socket) {
	var collection = db.get('runtimestats');
	collection.find({}, {
			limit: 100
		},
		function(e, docs) {
			//ensure the response contains a valid document
			if (!docs) {
				return;
			}
			socket.emit('runtimestats', docs);
		});
}

server.listen(process.env.PORT || 3000, process.env.IP || "0.0.0.0", function() {
	var addr = server.address();
	console.log("Server listening at", addr.address + ":" + addr.port);
});
