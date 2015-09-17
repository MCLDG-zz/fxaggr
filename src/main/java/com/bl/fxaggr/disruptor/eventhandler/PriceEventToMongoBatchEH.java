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

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.mongodb.WriteConcern;
import org.bson.Document;

import com.google.gson.Gson;
/**
 * Persists the price quote event to Mongo. Uses Disruptor batch handling
 * to batch writes to Mongo
 */
public class PriceEventToMongoBatchEH implements EventHandler<PriceEvent> {
	MongoDatabase db = null;
	Gson gson = new Gson();
	
	//for the time being I'm using a synchronised list. This is because the onEvent method
	//below and the 'timer' both access and update the list, and List is not 
	//thread safe. I will research a better way to do this in future
	// List<Document> events = Collections.synchronizedList(new ArrayList<>());

	List<Document> eventsList = new ArrayList<>();
	static final int BATCH_SIZE = 10000;

	public PriceEventToMongoBatchEH() {
		//Stop the logging to console
		Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
    	mongoLogger.setLevel(Level.SEVERE); 		
    	
    	//Setup a timer to persist data to Mongo
        // TimerTask task = new TimerTask() {
        //     @Override
        //     public void run() {
        //         persistEvents();
        //     }
        // };
    
        // Timer timer = new Timer();
        // long delay = 1000;
        // long interval= 1 * 1000; 
    
        // timer.scheduleAtFixedRate(task, delay, interval);

    	MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient();
			//mongoClient.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongoClient.getDatabase("fxaggr");
	}

	/**
	 * Store the events. They will be written to Mongo in batch mode
	 */
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		event.setPersistInstant();
		
		//Convert the event to a Mongo document
    	String eventJson = gson.toJson(event);
		eventsList.add(new Document("finalpricequote", Document.parse(eventJson)));

		if (eventsList.size() > BATCH_SIZE || endOfBatch) {
            persistEvents();
        }
	}
	private void persistEvents() {
		if (eventsList.size() > 0) {
 			Instant start = Instant.now();
			db.getCollection("finalquotes").insertMany(eventsList);
			long ns = start.until(Instant.now(), ChronoUnit.MILLIS);
 			//System.out.println("PriceEventToMongoBatchEH - bulk insert. Inserting: " + eventsList.size() + " events. Insert took: " + ns + " MS");
			eventsList.clear();
		}
	}
}