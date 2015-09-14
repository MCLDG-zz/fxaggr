package com.bl.fxaggr;

import com.bl.fxaggr.disruptor.*;
import com.bl.fxaggr.disruptor.eventhandler.*;
import com.bl.fxaggr.stats.StatsManager;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import org.bson.Document;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Test framework for the FX Aggregation Engine
 */
public class PriceEventTestMain extends Thread {
    public static long producerCount = 99998;
    public static RingBuffer < PriceEvent > ringBuffer;
    private static String actualResult = null;

    public static void main(String[] args) throws Exception {
        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // The factory for the event
        PriceEventFactory factory = new PriceEventFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = (int) Math.pow(2, 16);

        // Construct the Disruptor
        Disruptor < PriceEvent > disruptor = new Disruptor < > (factory, bufferSize,
            executor, ProducerType.SINGLE,
            new com.lmax.disruptor.BlockingWaitStrategy());

        EventHandler < PriceEvent > eh3 = new PriceFilterEH();
        EventHandler < PriceEvent > eh4 = new PrimaryBidAskEH();
        EventHandler < PriceEvent > eh10 = new PriceToConsumerEH();
        EventHandler < PriceEvent > eh20 = new StatsEH();
        EventHandler < PriceEvent > eh30 = new PriceEventToMongoEH();
        EventHandler < PriceEvent > eh31 = new PriceEventToMongoBatchEH();

        /******************************************************************************************
         * BIG WARNING
         * 
         * when configuring events to execute concurrently, using <code>handleEventsWith(eh1,eh2)</code>
         * each event handler will receive a reference to the same event instance. Therefore you CANNOT
         * mutate / modify the event state. For instance, if you modify the state of the event in eh1,
         * then at some point while eh2 is processing the same event, the state that eh2 sees will change.
         *
         * So when handling an event concurrently make sure the event handlers DO NOT mutate the
         * event instance. All mutations should be done on events that are handled independently,
         * not currently. I.e. using <code>handleEventsWith(eh1)</code>
         * 
         * For example, it is OK to replicate an event and persist an event concurrently (using
         * <code>handleEventsWith(replicateEH, persistEH)</code>) since neither of these event 
         * handlers mutate the state. However, <code>handleEventsWith(updateEH, persistEH)</code>
         * would not work since updateEH is updating the state while persistEH is attempting to store
         * 
         *******************************************************************************************/
        
        // Connect the handler
        // TODO - I'm sure we can do this more efficiently. For instance, PriceEventToMongoEH
        // does not have to complete before PriceFilterEH starts. We sort of want a copy of 
        // the event to be persisted to Mongo asynchronously while we start on the filtering.
        disruptor.handleEventsWith(eh3).then(eh4).then(eh10).then(eh20, eh31);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        ringBuffer = disruptor.getRingBuffer();

        /**********************************************************************************
        * Execute the test set. These test price filtering
        **********************************************************************************/
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Date dateStart = new Date();
        System.out.println("Data producer started processing TestFilter at: " + dateFormat.format(dateStart));

        //Setup the config in PriceEventHelper
        String[] lps = {"Bloomberg", "Deutsche Bank", "Citibank"};
        PriceEventHelper.setCurrentPrimaryLiquidityProvider("Bloomberg");
        PriceEventHelper.setLiquidityProviders(lps);
        PriceEventHelper.setPriceSelectionScheme("Primary Bid/Ask");
        System.out.println("Pricing scheme updated to: " + PriceEventHelper.getPriceSelectionScheme());

        clearRuntimeResults();
        PriceEventTestGenerator priceEventGenerator = new PriceEventTestGenerator(ringBuffer,
            "/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestFilter.csv");
        priceEventGenerator.run();

        Date dateEnd = new Date();
        System.out.println("Data producer completed processing TestFilter at: " + dateFormat.format(dateEnd));

        //Check test results
        Thread.sleep(5000);
        checkTestResults("/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestFilterExpectedResults.json");
        Thread.sleep(2000);

        /**********************************************************************************
        * Execute the test set. These test Liquidity Provider switching
        **********************************************************************************/
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        dateStart = new Date();
        System.out.println("Data producer started processing TestSwitchLP at: " + dateFormat.format(dateStart));

        StatsManager.resetStats();
        clearRuntimeResults();
        priceEventGenerator = new PriceEventTestGenerator(ringBuffer,
            "/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestSwitchLP.csv");
        priceEventGenerator.run();

        dateEnd = new Date();
        System.out.println("Data producer completed processing TestSwitchLP at: " + dateFormat.format(dateEnd));

        //Check test results
        Thread.sleep(5000);
        checkTestResults("/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestSwitchLPExpectedResults.json");
        Thread.sleep(2000);

        /**********************************************************************************
        * Execute the test set. These test the Best Bid/Ask Scheme
        **********************************************************************************/
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        dateStart = new Date();
        System.out.println("Data producer started processing Best Bid/Ask at: " + dateFormat.format(dateStart));

        //Setup the config in PriceEventHelper
        PriceEventHelper.setCurrentPrimaryLiquidityProvider("Bloomberg");
        PriceEventHelper.setLiquidityProviders(lps);
        PriceEventHelper.setPriceSelectionScheme("Best Bid/Ask");
        System.out.println("Pricing scheme updated to: " + PriceEventHelper.getPriceSelectionScheme() + ". Primary Provider is: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider());

        StatsManager.resetStats();
        clearRuntimeResults();
        priceEventGenerator = new PriceEventTestGenerator(ringBuffer,
            "/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestBestBidAsk.csv");
        priceEventGenerator.run();

        dateEnd = new Date();
        System.out.println("Data producer completed processing BestBidAsk at: " + dateFormat.format(dateEnd));

        //Check test results
        Thread.sleep(5000);
        checkTestResults("/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestBestBidAskExpectedResults.json");
        Thread.sleep(2000);
        /**********************************************************************************
        * Execute the test set. These test the Smoothing Scheme
        **********************************************************************************/
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        dateStart = new Date();
        System.out.println("Data producer started processing Smoothing at: " + dateFormat.format(dateStart));

        //Setup the config in PriceEventHelper
        PriceEventHelper.setCurrentPrimaryLiquidityProvider("Bloomberg");
        PriceEventHelper.setLiquidityProviders(lps);
        PriceEventHelper.setPriceSelectionScheme("Smoothing");
        PriceEventHelper.setEWMAPeriods(3);
        System.out.println("Pricing scheme updated to: " + PriceEventHelper.getPriceSelectionScheme() + ". Primary Provider is: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider());

        StatsManager.resetStats();
        clearRuntimeResults();
        priceEventGenerator = new PriceEventTestGenerator(ringBuffer,
            "/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestSmoothing.csv");
        priceEventGenerator.run();

        dateEnd = new Date();
        System.out.println("Data producer completed processing Smoothing at: " + dateFormat.format(dateEnd));

        //Check test results
        Thread.sleep(5000);
        checkTestResults("/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestSmoothingExpectedResults.json");
        Thread.sleep(2000);
    }
    /**
     * Check the test results
     */
    private static boolean checkTestResults(String resultsFilename) {
        MongoClient mongoClient = null;
    	MongoDatabase db = null;

        try {
            mongoClient = new MongoClient();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        db = mongoClient.getDatabase("fxaggr");

        //Loop through each line in Expected Results; check against the runtimestats collection in Mongo.
        //They should be identical
        File testfile = new File(resultsFilename);
        System.out.println("Test data result checker opening input file " + testfile.toString());
        try (FileReader reader = new FileReader(testfile)) {
        	
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split(",");

                //Read the runtimestats from Mongo
                FindIterable < Document > iterable = db.getCollection("runtimestats").find(Document.parse( tokens[0] + "}" ));

                iterable.forEach(new Block < Document > () {
                    @Override
                    public void apply(final Document document) {
                        actualResult = document.toJson();
                    }
                });
                
                //Compare actual vs. expected results
                JsonParser parser = new JsonParser();
                JsonElement o1 = parser.parse(actualResult);
                JsonElement o2 = parser.parse(line);
                boolean match = true;
                if (o1.isJsonObject() && o2.isJsonObject()) {
                    JsonObject jso1 = o1.getAsJsonObject();
                    JsonObject jso2 = o2.getAsJsonObject();
                    JsonElement jse1 = jso1.get("totalNumberOfEvents");
                    JsonElement jse2 = jso2.get("totalNumberOfEvents");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalNumberOfEvents do not match");
                        match = false;
                    }
                    jse1 = jso1.get("totalNumberOfFilteredEvents");
                    jse2 = jso2.get("totalNumberOfFilteredEvents");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalNumberOfFilteredEvents do not match");
                        match = false;
                    }
                    jse1 = jso1.get("totalLiquidityProviderSwitches");
                    jse2 = jso2.get("totalLiquidityProviderSwitches");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalLiquidityProviderSwitches do not match");
                        match = false;
                    }
                    jse1 = jso1.get("totalLiquidityProviderSwitchBacks");
                    jse2 = jso2.get("totalLiquidityProviderSwitchBacks");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalLiquidityProviderSwitchBacks do not match");
                        match = false;
                    }
                    jse1 = jso1.get("totalLiquidityProviderUnableToSwitch");
                    jse2 = jso2.get("totalLiquidityProviderUnableToSwitch");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalLiquidityProviderUnableToSwitch do not match");
                        match = false;
                    }
                    jse1 = jso1.get("totalNumberConfiguredBestBidAskEvents");
                    jse2 = jso2.get("totalNumberConfiguredBestBidAskEvents");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalNumberConfiguredBestBidAskEvents do not match");
                        match = false;
                    }
                    jse1 = jso1.get("totalNumberAppliedBestBidAskEvents");
                    jse2 = jso2.get("totalNumberAppliedBestBidAskEvents");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalNumberAppliedBestBidAskEvents do not match");
                        match = false;
                    }
                    jse1 = jso1.get("totalNumberAppliedPrimaryBidAskEvents");
                    jse2 = jso2.get("totalNumberAppliedPrimaryBidAskEvents");
                    if (jse1 != null && jse2 != null && jse1.getAsLong() != jse2.getAsLong()) {
                        System.out.println("Test data result checker - totalNumberAppliedPrimaryBidAskEvents do not match");
                        match = false;
                    }
                    JsonObject jsosub1 = jso1.getAsJsonObject("numberPerFilteredReason");
                    JsonObject jsosub2 = jso2.getAsJsonObject("numberPerFilteredReason");
                    if (jsosub1 != null && jsosub2 != null && jsosub1.getAsJsonObject().equals(jsosub2.getAsJsonObject())) {
                        System.out.println("Test data result checker - numberPerFilteredReason do not match. Actual: " + jsosub1.toString() + " Expected: " + jsosub2.toString());
                        match = false;
                    }
                }
                if (!match) {
                    System.out.println("Test data result checker - expected results DO NOT MATCH!! Actual: " + actualResult + " Expected: " + line + ". Equal? " + actualResult.equals(line));
                    return false;
                }
                else {
                    System.out.println("Test data result checker - results MATCH!! " + actualResult);
                }
            }
            System.out.println("Test data result checker processed input file");
        }
        catch (IOException ex) {
            System.out.println("IOException in Test data result checker processing input file. Exception: " + ex);
            ex.printStackTrace();
        }
        System.out.println("Test data result checker - all expected results are correct!!");
        return true;
    }
    /**
     * Each test step above will write results to the 'runtimestats' collection in Mongo,
     * then compare that against the expected results. So clear 'runtimestats' prior
     * to executing a test step.
     */
    private static void clearRuntimeResults() {
        MongoClient mongoClient = null;
    	MongoDatabase db = null;

        try {
            mongoClient = new MongoClient();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        db = mongoClient.getDatabase("fxaggr");

        //Delete the runtimestats from Mongo
        db.getCollection("runtimestats").drop();
    }
}