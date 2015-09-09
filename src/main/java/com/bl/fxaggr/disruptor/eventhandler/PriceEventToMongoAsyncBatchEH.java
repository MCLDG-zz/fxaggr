package com.bl.fxaggr.disruptor.eventhandler;

import com.bl.fxaggr.disruptor.*;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.time.temporal.ChronoUnit;
import java.time.Instant;

import com.lmax.disruptor.EventHandler;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.util.JSON;
import org.bson.Document;

import com.google.gson.Gson;
/**
 * Persists the price quote event to Mongo. The current state of the price event will
 * determine which collection the price event is written to in Mongo.
 */
public class PriceEventToMongoAsyncBatchEH implements EventHandler<PriceEvent> {
	MongoDatabase db = null;
	Gson gson = new Gson();
	
	//for the time being I'm using a synchronised list. This is because the onEvent method
	//below and the 'timer' both access and update the list, and List is not 
	//thread safe. I will research a better way to do this in future
	List<Document> events = Collections.synchronizedList(new ArrayList<>());

	public PriceEventToMongoAsyncBatchEH() {
		//Stop the logging to console
		Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
    	mongoLogger.setLevel(Level.SEVERE); 		
    	
    	//Setup a timer to persist data to Mongo
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                persistEvents();
            }
        };
    
        Timer timer = new Timer();
        long delay = 1000;
        long interval= 1 * 1000; 
    
        timer.scheduleAtFixedRate(task, delay, interval);

    	MongoClient mongoClient = null;
		try {
			mongoClient = MongoClients.create();
			//mongoClient.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongoClient.getDatabase("fxaggr");
	}

	/**
	 * Store the events. They will be written to Mongo in batch mode, based
	 * on a timer setting (i.e. a batch insert every <n> seconds)
	 */
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		event.setPersistInstant();
		
		//Convert the event to a Mongo document
    	String eventJson = gson.toJson(event);
		events.add(new Document("finalpricequote", Document.parse(eventJson)));
	}
	private void persistEvents() {
		if (events.size() > 0) {
	 		synchronized (events) {
	 			Instant start = Instant.now();
				db.getCollection("finalquotes").insertMany(events, new SingleResultCallback<Void>() {
				   @Override
				   public void onResult(final Void result, final Throwable t) {
						long ns = start.until(Instant.now(), ChronoUnit.NANOS);
			 			System.out.println("PriceEventToMongoBatchEH - bulk insert. Inserting: " + events.size() + " events. Insert took: " + ns + " nanoseconds");
						events.clear();
				   }
				});
	  		}		
		}
	}
}