package com.bl.fxaggr.disruptor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.lang.reflect.Type;

import com.lmax.disruptor.EventHandler;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

import com.google.gson.Gson;
/*
 * Persists the event to the file system
 */
public class JournalToMongoEventHandler implements EventHandler<PriceEvent> {
	DB db = null;
	DBCollection PriceColl;

	public JournalToMongoEventHandler() {
		MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongoClient.getDB("fxaggr");
		PriceColl = db.getCollection("PriceColl");
		System.out.println("Connection to Mongo created: " + PriceColl.hashCode());
	}

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		//Convert the PriceEntity to JSON
		PriceEntity priceEntity = event.getPriceEntity();
		Gson gson = new Gson();
    	String json = gson.toJson(priceEntity);

    	//Write to Mongo
		this.journalToMongo(json);
		
		if (sequence == PriceEventMain.producerCount) {
	        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	        Date dateEnd = new Date();
			System.out.println("JournalToMongoEventHandler processed: " + PriceEventMain.producerCount +
				"events. Complete time: " + dateFormat.format(dateEnd)); 
		}
	}

	/**
	 * Write the event to MongoDB
	 */
	private void journalToMongo(String content) {
		DBObject dbObject = (DBObject)JSON.parse(content);
		db.getCollection("PriceColl").insert(dbObject);
	}
}