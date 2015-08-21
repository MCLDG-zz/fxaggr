To test this application do the following:

1) Make sure your test data is in TestFilter.csv
2) Run MongoDB
3) Execute the script in MongoTest.JSON against the DB to populate it with test data
4) Execute test-fxaggr.sh in the root folder. This will run the PriceEventTestMain.java class
5) The results in ExpectedTestResults.json should match the contents of the 'runtimestats' collection in Mongo - these will be automatically compared

To test the filter function (as defined in class PriceFilterEH.java) make sure you have these 
Event Handlers setup in PriceEventTestMain.java in sequential order:

1) PriceFilterEH();
2) JournalToMongoEventHandler();
