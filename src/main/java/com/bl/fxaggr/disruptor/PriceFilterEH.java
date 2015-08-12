package com.bl.fxaggr.disruptor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import com.lmax.disruptor.EventHandler;

import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import org.bson.Document;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import com.google.gson.Gson;

public class PriceFilterEH implements EventHandler<PriceEvent> {
	
	private Map<String, AggrConfig> aggrConfigMap = new HashMap<>();
	private Map<String, PriceStats> priceStatsMap = new HashMap<>();
	private Map<String, PreviousPrice> previousPriceMap = new HashMap<>();
	private AggrConfig aggrConfig;
	private PriceStats priceStats;
	private PreviousPrice previousPrice = null;
	private PriceEntity priceEntity;
	
	PriceFilterEH() {
		System.out.println("PriceFilterEH created. Object ID:: " + this.toString()); 
		MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		MongoDatabase db = mongoClient.getDatabase("fxaggr");
		System.out.println("Connection to Mongo created: " + db.hashCode());
		
		//Read the config from the aggrconfig collection in Mongo
		FindIterable<Document> iterable = db.getCollection("aggrconfig").find();
	
	   	iterable.forEach(new Block<Document>() {
    		@Override
    		public void apply(final Document document) {
   				Gson gson = new Gson();
        		aggrConfig = gson.fromJson(document.toJson(), AggrConfig.class);  
        		aggrConfigMap.put(aggrConfig.symbol, aggrConfig);
    		}
		});
   		System.out.println("Number of Config items: " + aggrConfigMap.size());

		//Read the prices stats from the pricestats collection in Mongo
		iterable = db.getCollection("pricestats").find();
	
	   	iterable.forEach(new Block<Document>() {
    		@Override
    		public void apply(final Document document) {
   				Gson gson = new Gson();
        		priceStats = gson.fromJson(document.toJson(), PriceStats.class);  
        		
        		//manually set the 'symbol' since it is a sub-item in the JSON
        		Document subDoc = document.get("_id", Document.class);
        		priceStats.symbol = subDoc.get("symbol", String.class);
        		priceStats.hour = subDoc.get("hour", Integer.class);
        		priceStatsMap.put(priceStats.symbol + priceStats.hour, priceStats);
    		}
		});
   		System.out.println("Number of Stats items: " + priceStatsMap.size());
	}

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		priceEntity = event.getPriceEntity();
		
		String currency = priceEntity.getSymbol();
		int hour = priceEntity.getDatetime().getHour();
		double spread = priceEntity.getAsk() - priceEntity.getBid();
		
		priceStats = priceStatsMap.get(currency + hour);
		aggrConfig = aggrConfigMap.get(currency);
		previousPrice = previousPriceMap.get(currency);

		//Check if the bid/ask prices fall within the acceptable range
		if (spread > priceStats.averageSpread + (priceStats.averageSpread * aggrConfig.pctLeewayAllowedSpread)) {
			System.out.println("PriceFilterEH skipping quote for currency: " + currency + ". Spread of: " + spread + " is outside bounds of "
				+  priceStats.averageSpread + " + " + aggrConfig.pctLeewayAllowedSpread + "%, which equals: " 
				+  (priceStats.averageSpread + (priceStats.averageSpread * aggrConfig.pctLeewayAllowedSpread))); 
		}
		
		//Check if the bid/ask has spiked/dropped
		if (previousPrice != null) {
				System.out.println("PriceFilterEH quote for currency: " + currency + ". Ask of: " + priceEntity.getAsk() + "  "
					+  previousPrice.ask + " + " + aggrConfig.pctLeewayToPreviousAsk + "%, which equals: " 
					+  (previousPrice.ask + (previousPrice.ask * aggrConfig.pctLeewayToPreviousAsk))); 
			if (priceEntity.getAsk() > (previousPrice.ask + (previousPrice.ask * aggrConfig.pctLeewayToPreviousAsk))) {
				System.out.println("PriceFilterEH skipping quote for currency: " + currency + ". Ask has spiked/dropped. Ask of: " + priceEntity.getAsk() + " is outside bounds of "
					+  previousPrice.ask + " + " + aggrConfig.pctLeewayToPreviousAsk + "%, which equals: " 
					+  previousPrice.ask + (previousPrice.ask * aggrConfig.pctLeewayToPreviousAsk)); 
			} else {
				//Update the previous price
				previousPrice.symbol = currency;
				previousPrice.datetime = priceEntity.getDatetime();
				previousPrice.ask = priceEntity.getAsk();
				previousPrice.bid = priceEntity.getBid();
				previousPrice.spread = spread;
			}
		} else {
			previousPrice = new PreviousPrice();
			previousPrice.symbol = currency;
			previousPrice.datetime = priceEntity.getDatetime();
			previousPrice.ask = priceEntity.getAsk();
			previousPrice.bid = priceEntity.getBid();
			previousPrice.spread = spread;
			previousPriceMap.put(currency, previousPrice);
	  		System.out.println("Number of Previous Price items: " + previousPriceMap.size());
		}
		
		
		if (sequence == PriceEventMain.producerCount) {
	        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	        Date dateEnd = new Date();
			System.out.println("PriceFilterEH processed: " + PriceEventMain.producerCount +
				"events. Complete time: " + dateFormat.format(dateEnd)); 
		}
	}
	
	private class AggrConfig {
		public String symbol;
		public double pctLeewayAllowedSpread;
		public double pctLeewayToPreviousBid;
		public double pctLeewayToPreviousAsk;
	}
	
	private class PriceStats {
		public String symbol;
		public int hour;
		public double averageAsk;
		public double averageBid;
		public double averageSpread;
		public double maxSpread;
		public double minSpread;
	}
	private class PreviousPrice {
		public String symbol;
		public LocalDateTime datetime;
		public double ask;
		public double bid;
		public double spread;
	}
	
}