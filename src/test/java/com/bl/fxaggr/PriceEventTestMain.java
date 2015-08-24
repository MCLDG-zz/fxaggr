package com.bl.fxaggr;

import com.bl.fxaggr.disruptor.*;

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

        EventHandler < PriceEvent > eh1 = new JournalToMongoEventHandler();
        EventHandler < PriceEvent > eh3 = new PriceFilterEH();
        EventHandler < PriceEvent > eh4 = new PriceCompareEH();
        EventHandler < PriceEvent > eh5 = new StatsEH();
        EventHandler < PriceEvent > eh6 = new PrimaryBidAskEH();
        EventHandler < PriceEvent > eh7 = new PrimaryBidAskWaitEH();

        // Connect the handler
        disruptor.handleEventsWith(eh3).then(eh6).then(eh7).then(eh1, eh5);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        ringBuffer = disruptor.getRingBuffer();

        // Start the data generator
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Date dateStart = new Date();
        System.out.println("Data producer started at: " + dateFormat.format(dateStart));

        PriceEventTestGenerator priceEventGenerator = new PriceEventTestGenerator(ringBuffer);
        priceEventGenerator.run();

        Date dateEnd = new Date();
        System.out.println("Data producer completed at: " + dateFormat.format(dateEnd));

        //Check test results
        Thread.sleep(5000);
        checkTestResults();
    }
    /**
     * Check the test results
     */
    private static boolean checkTestResults() {
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
        File testfile = new File("/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/ExpectedTestResults.json");
        try {
            System.out.println("Test data result checker opening input file " + testfile.toString());

            FileReader reader = new FileReader(testfile);
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
                if (!o1.equals(o2)) {
                    System.out.println("Test data result checker - expected results DO NOT MATCH!! Actual: " + actualResult + " Expected: " + line + ". Equal? " + actualResult.equals(line));
                    return false;
                }
            }
            reader.close();
            System.out.println("Test data result checker processed input file");
        }
        catch (IOException ex) {
            System.out.println("IOException in Test data result checker processing input file. Exception: " + ex);
            ex.printStackTrace();
        }
        System.out.println("Test data result checker - all expected results are correct!!");
        return true;
    }
}