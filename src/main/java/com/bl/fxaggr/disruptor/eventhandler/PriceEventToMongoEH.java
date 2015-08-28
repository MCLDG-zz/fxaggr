package com.bl.fxaggr.disruptor.eventhandler;

import com.bl.fxaggr.disruptor.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.lmax.disruptor.EventHandler;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import org.bson.Document;

import com.google.gson.Gson;
/**
 * Persists the price quote event to Mongo. The current state of the price event will
 * determine which collection the price event is written to in Mongo.
 */
public class PriceEventToMongoEH implements EventHandler<PriceEvent> {
	MongoDatabase db = null;
	Gson gson = new Gson();

	public PriceEventToMongoEH() {
		//Stop the logging to console
		Logger mongoLogger = Logger.getLogger( "org.mongodb.driver" );
    	mongoLogger.setLevel(Level.SEVERE); 		
    	
    	MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongoClient.getDatabase("fxaggr");
	}

	/**
	 * Write the event to MongoDB. The state of the event will
	 * determine which collection the price event is written to
	 */
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		//Convert the event to JSON
    	String json = gson.toJson(event);

		//Seems Java is clever enough to infer the type of the enum from the 
		//switch statement and won't allow it to be added. This doesn't help 
		//you (the programmer) to know which enum is referred to, so I'll 
		//let you know here:
		//
		//	PriceEvent.EventState
		//
		switch (event.getEventState()) {
			case NEW_QUOTE:
				db.getCollection("rawquotes").insertOne(
					new Document("rawpricequote", Document.parse(json)));
				break;
			case FILTER_COMPLETED:
				break;
			case COMPARISON_COMPLETED:
				break;
			case FINAL_QUOTE:
				db.getCollection("finalquotes").insertOne(
					new Document("finalpricequote", Document.parse(json)));
				break;
		}
	}
}