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

import com.bl.fxaggr.stats.StatsManager;
import com.bl.fxaggr.stats.EventStats;

public class PriceFilterEH implements EventHandler<PriceEvent> {
	
	private Map<String, AggrConfig> aggrConfigMap = new HashMap<>();
	private Map<String, PriceStats> priceStatsMap = new HashMap<>();
	private Map<String, PreviousPrice> previousPriceMap = new HashMap<>();
	private AggrConfig aggrConfig;
	private PriceStats priceStats;
	private PreviousPrice previousPrice = null;
	private PriceEntity priceEntity;
	private MongoDatabase db = null;
	
	
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
		
		db = mongoClient.getDatabase("fxaggr");
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
		double spread = priceEntity.getSpread();

		priceStats = priceStatsMap.get(currency + hour);
		aggrConfig = aggrConfigMap.get(currency);
		previousPrice = previousPriceMap.get(currency);

		// System.out.println("PriceFilterEH processing sequence: " + sequence + " currency " + currency + " hour " + hour
		// 	+ " spread of: " + spread + ". Found matching aggrConfig: " +  (aggrConfig != null)
		// 	+ ". Found matching pricestats: " +  (priceStats != null)); 
		
		//Check if the spread falls within the acceptable range
		if (priceStats == null || aggrConfig == null) {
			System.out.println("PriceFilterEH cannot analyse pricing. No price stats in table pricestats, or no config in table aggrconfig. Currency: " + currency + ". Sequence: " + sequence); 
		}
		else {
			if (spread > Math.abs(priceStats.averageSpread + (priceStats.averageSpread * aggrConfig.pctLeewayAllowedSpread / 100))) {
				this.persistOutliers("Spread of: " + spread + " exceeds average spread for this time period"); 
				event.setFilteredEvent(true);
				event.setFilteredReason(PriceEvent.FilterReason.SPREAD_EXCEEDS_AVERAGE);
			}
		}
		
		//Check if the bid/ask has spiked/dropped
		if (aggrConfig != null) {
			if (previousPrice != null) {
				boolean priceSpike = false;
				/*
				* Check if ASK has spiked/dropped
				*/
				//Calculate the difference between current price and previous price, and check if it exceeds the Leeway
				double priceDiff = Math.abs(priceEntity.getAsk() - previousPrice.ask);
				double leewayHi = previousPrice.ask + (previousPrice.ask * aggrConfig.pctLeewayToPreviousAsk / 100);
				double leewayLo = previousPrice.ask - (previousPrice.ask * aggrConfig.pctLeewayToPreviousAsk / 100);
				// System.out.println("PriceFilterEH check for spikes for sequence: " + sequence 
				// 	+ " Ask of: " + priceEntity.getAsk() + " hi: "
				// 	+  leewayHi + " lo: " + leewayLo + ", based on a " + aggrConfig.pctLeewayToPreviousAsk 
				// 	+ "movement in price"); 
				if (priceEntity.getAsk() > leewayHi || priceEntity.getAsk() < leewayLo) {
					priceSpike = true;
					this.persistOutliers("Ask of: " + priceEntity.getAsk() + " has spiked/dropped compared to previous price"); 
					event.setFilteredEvent(true);
					event.setFilteredReason(PriceEvent.FilterReason.ASK_SPIKE);
				} 
				/*
				* Check if BID has spiked/dropped
				*/
				//Calculate the difference between current price and previous price, and check if it exceeds the Leeway
				priceDiff = Math.abs(priceEntity.getBid() - previousPrice.bid);
				leewayHi = previousPrice.bid + (previousPrice.bid * aggrConfig.pctLeewayToPreviousBid / 100);
				leewayLo = previousPrice.bid - (previousPrice.bid * aggrConfig.pctLeewayToPreviousBid / 100);
				// System.out.println("PriceFilterEH check for spikes for sequence: " + sequence 
				// 	+ " Bid of: " + priceEntity.getBid() + " hi: "
				// 	+  leewayHi + " lo: " + leewayLo + ", based on a " + aggrConfig.pctLeewayToPreviousBid 
				// 	+ "movement in price"); 
				if (priceEntity.getBid() > leewayHi || priceEntity.getBid() < leewayLo) {
					priceSpike = true;
					this.persistOutliers("Bid of: " + priceEntity.getBid() + " has spiked/dropped compared to previous price"); 
					event.setFilteredEvent(true);
					event.setFilteredReason(PriceEvent.FilterReason.ASK_SPIKE);
				}
				//Do not store price spikes. They should be treated as anomalies (faulty) and future
				//prices should not be compared to them
				if (!priceSpike) {
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
		}
		else {
			System.out.println("PriceFilterEH cannot analyse pricing. No config in table aggrconfig. Currency: " + currency + ". Sequence: " + sequence); 
		}
		
		//Log the stats
		StatsManager.eventReceived(event);
		
		if (sequence == PriceEventMain.producerCount) {
	        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	        Date dateEnd = new Date();
			System.out.println("PriceFilterEH processed: " + PriceEventMain.producerCount +
				"events. Complete time: " + dateFormat.format(dateEnd)); 
		}
	}
	
	/**
	 * Write the potential outlying quotes to MongoDB
	 * 
	 * We will also persist the state of the helper classes which contain the 
	 * current and previous quotes to aid troubleshooting
	 */
	private void persistOutliers(String message) {
		Gson gson = new Gson();
    	String jsonAggrConfig = gson.toJson(aggrConfig);		
    	String jsonPriceStats = gson.toJson(priceStats);		
    	String jsonPreviousPrice = gson.toJson(previousPrice);		
    	String jsonPriceEntity = gson.toJson(priceEntity);		
		db.getCollection("priceoutliers").insertOne(
			new Document("priceoutlier",
				new Document()
					.append("message", message)
					.append("currentpriceentity", Document.parse(jsonPriceEntity))
					.append("previousprice", Document.parse(jsonPreviousPrice))
					.append("config", Document.parse(jsonAggrConfig))
					.append("pricestats", Document.parse(jsonPriceStats))
			)
		);
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