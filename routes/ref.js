var express = require('express');
var router = express.Router();

/* GET country to currency mapping */
router.get('/countrytocurrency', function(req, res) {
    var db = req.db;
    var collection = db.get('refcountrytocurrency');
    collection.find({},{},function(e,docs){
        res.json(docs);
    });
});

module.exports = router;