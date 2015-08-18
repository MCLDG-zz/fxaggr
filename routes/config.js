var express = require('express');
var router = express.Router();

/* GET the aggregation engine config */
router.get('/aggrconfig', function(req, res) {
    var db = req.db;
    var collection = db.get('aggrconfig');
    collection.find({}, {}, function(e, docs) {
        res.json(docs);
    });
});

/* GET the latest price statistics */
router.get('/aggrpricestats', function(req, res) {
    var db = req.db;
    var collection = db.get('pricestats');
    collection.find({}, {}, function(e, docs) {
        res.json(docs);
    });
});

/* Update config */
router.post('/updateaggrconfig', function(req, res) {
    var db = req.db;
    var id = req.body._id;
    var body = req.body;
    delete body._id;

    var collection = db.get('aggrconfig');
    collection.findAndModify({
            "_id": id
        }, {
            "$set": body
        },
        function(err, result) {
            res.send(
                (err === null) ? {
                    msg: ''
                } : {
                    msg: err
                }
            );
        });
});


/*
 * Delete pendingorder.
 */
router.post('/delpendingorder', function(req, res) {
    var db = req.db;
    var collection = db.get('pendingorders');
    collection.remove({
        _id: req.body._id
    }, function(err, result) {
        res.send(
            (err === null) ? {
                msg: ''
            } : {
                msg: err
            }
        );
    });
});

module.exports = router;