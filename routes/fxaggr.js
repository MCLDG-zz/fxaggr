var express = require('express');
var router = express.Router();

/* GET the outliers - those quotes that fall outside the expected range */
router.get('/priceoutliers', function(req, res) {
    var db = req.db;
    var collection = db.get('priceoutliers');
    collection.find({}, {}, function(e, docs) {
        res.json(docs);
    });
});

module.exports = router;