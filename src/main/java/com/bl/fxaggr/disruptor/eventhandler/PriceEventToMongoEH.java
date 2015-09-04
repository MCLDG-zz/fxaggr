package com.bl.fxaggr.disruptor.eventhandler;

import com.bl.fxaggr.disruptor.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.lmax.disruptor.EventHandler;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import com.mongodb.WriteConcern;
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
			//mongoClient.setWriteConcern(WriteConcern.UNACKNOWLEDGED);
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongoClient.getDatabase("fxaggr");
	}

	/**
	 * Write the event to MongoDB. 
	 */
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		event.setPersistInstant();
		
		//Convert the event and original price entity to JSON
    	String eventJson = gson.toJson(event);
    	String originalPriceJson = gson.toJson(event.getPriceEntity());

		//Store the original, raw quote
		db.getCollection("rawquotes").insertOne(
			new Document("rawpricequote", Document.parse(originalPriceJson)));
		
		//If the event contains a 'final quote', i.e. a quote that has been sent
		//to the consumer, then store the event
		if (event.getEventState() == PriceEvent.EventState.FINAL_QUOTE) {
			db.getCollection("finalquotes").insertOne(
				new Document("finalpricequote", Document.parse(eventJson)));
		}
	}
}