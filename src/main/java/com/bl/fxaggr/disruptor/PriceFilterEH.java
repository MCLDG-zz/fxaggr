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
	
	private Map<String, AggrConfigCurrency> aggrcurrencyconfigMap = new HashMap<>();
	private Map<String, PriceStats> priceStatsMap = new HashMap<>();
	private Map<String, PreviousPrice> previousPriceMap = new HashMap<>();
	private Map<String, Long> consecutiveFilteredPriceMap = new HashMap<>();
	private Map<String, Long> askSpikeHiCounter = new HashMap<>();
	private Map<String, Long> bidSpikeHiCounter = new HashMap<>();
	private Map<String, Long> askSpikeLoCounter = new HashMap<>();
	private Map<String, Long> bidSpikeLoCounter = new HashMap<>();
	private String currency = null;
	private AggrConfig aggrConfig;
	private AggrConfigCurrency aggrConfigCurrency;
	private PriceStats priceStats;
	private PreviousPrice previousPrice = null;
	private PriceEntity priceEntity;
	private MongoDatabase db = null;

	
	public PriceFilterEH() {
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
		   		System.out.println("aggrConfig JSON: " + document.toJson());
        		aggrConfig = gson.fromJson(document.toJson(), AggrConfig.class);  
    		}
		});
		for( AggrConfigCurrency currencyconfig : aggrConfig.currencyconfig) {
       		aggrcurrencyconfigMap.put(currencyconfig.symbol, currencyconfig);
		}
   		System.out.println("Number of CurrencyConfig items: " + aggrcurrencyconfigMap.size());

		//Read the prices stats from the pricestats collection in Mongo
		iterable = db.getCollection("pricestats").find();
	
	   	iterable.forEach(new Block<Document>() {
    		@Override
    		public void apply(final Document document) {
   				Gson gson = new Gson();
        		priceStats = gson.fromJson(document.toJson(), PriceStats.class);  
        		priceStatsMap.put(priceStats._id.symbol + priceStats._id.hour, priceStats);
    		}
		});
   		System.out.println("Number of PriceStats items: " + priceStatsMap.size());
	}

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		priceEntity = event.getPriceEntity();
		double spread = priceEntity.getSpread();

		//Filter if negative spread. Do NOT continue to process this quote
		if (spread < 0) {
			System.out.println("Sequence: " + sequence + ". Negative spread"); 
			event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Negative spread. Spread is: " + spread); 
			event.setFilteredEvent(true);
			event.setFilteredReason(PriceEvent.FilterReason.NEGATIVE_SPREAD);
			StatsManager.eventReceived(event);
			return;
		}
		
		currency = priceEntity.getSymbol();
		int hour = priceEntity.getDatetime().getHour();
		priceStats = priceStatsMap.get(currency + hour);
		aggrConfigCurrency = aggrcurrencyconfigMap.get(currency);
		previousPrice = previousPriceMap.get(currency);

		//Check if the spread falls within the acceptable range
		if (priceStats == null || aggrConfigCurrency == null) {
			System.out.println("PriceFilterEH cannot analyse pricing. No price stats in table pricestats, or no config in table aggrconfig. Currency: " + currency + " hour: " + hour + ". Sequence: " + sequence); 
			event.addAuditEvent("PriceFilterEH. Cannot analyse pricing. No price stats in table pricestats, or no config in table aggrconfig. Currency: " + currency + " hour: " + hour + ". Sequence: " + sequence); 
		}
		else {
			if (spread > Math.abs(priceStats.averageSpread + (priceStats.averageSpread * aggrConfigCurrency.pctLeewayAllowedSpread / 100))) {
				System.out.println("Sequence: " + sequence + ". exceeds average spread"); 
				event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Exceeds average spread. Spread is: " + spread + ". Expected range: " + Math.abs(priceStats.averageSpread + (priceStats.averageSpread * aggrConfigCurrency.pctLeewayAllowedSpread / 100))); 
				event.setFilteredEvent(true);
				event.setFilteredReason(PriceEvent.FilterReason.SPREAD_EXCEEDS_AVERAGE);
			}
		}
		
		//Check if the bid/ask has spiked/dropped
		if (aggrConfigCurrency != null) {
			if (previousPrice != null) {
				boolean priceSpike = false;
				/*
				* Check if ASK has spiked/dropped
				*/
				//Calculate the difference between current price and previous price, and check if it exceeds the Leeway
				double priceDiff = Math.abs(priceEntity.getAsk() - previousPrice.ask);
				double leewayHi = previousPrice.ask + (previousPrice.ask * aggrConfigCurrency.pctLeewayToPreviousAsk / 100);
				double leewayLo = previousPrice.ask - (previousPrice.ask * aggrConfigCurrency.pctLeewayToPreviousAsk / 100);

				/**
				 * The logic here is as follows:
				 * 
				 * The number of consecutive spikes is tracked
				 * If the number exceeds the limit (as determined by 
				 * AggrConfig.globalconfig.numberconsecutivespikesfiltered), then treat the 
				 * current spike as the new price trend and accept the quote
				 * Note that prices must spike in the same direction (either up or down);
				 * a spike upwards followed by a spike downwards is not treated as consecutive
				 */
				if (priceEntity.getAsk() > leewayHi) {
					if (askSpikeHiCounter.containsKey(currency)) {
						askSpikeHiCounter.replace(currency, askSpikeHiCounter.get(currency) + 1);
					}
					else {
						askSpikeHiCounter.put(currency, new Long(1));
					}
					if (askSpikeHiCounter.get(currency) > aggrConfig.globalconfig.numberconsecutivespikesfiltered) {
						System.out.println("Sequence: " + sequence + ". Treating ask price spike as new normal."); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Treating ask price spike as new normal."); 
						askSpikeHiCounter.replace(currency, new Long(0));
					}
					else {
						System.out.println("Sequence: " + sequence + ". FilterEH Ask Hi spike."); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Ask Hi Spike. Ask exceeds: " + leewayHi); 
						priceSpike = true;
						event.setFilteredEvent(true);
						event.setFilteredReason(PriceEvent.FilterReason.ASK_SPIKE);
					}
				} 
				if (priceEntity.getAsk() < leewayLo) {
					if (askSpikeLoCounter.containsKey(currency)) {
						askSpikeLoCounter.replace(currency, askSpikeLoCounter.get(currency) + 1);
					}
					else {
						askSpikeLoCounter.put(currency, new Long(1));
					}
					if (askSpikeLoCounter.get(currency) > aggrConfig.globalconfig.numberconsecutivespikesfiltered) {
						System.out.println("Sequence: " + sequence + ". Treating ask price spike as new normal."); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Treating ask price spike as new normal."); 
						askSpikeLoCounter.replace(currency, new Long(0));
					}
					else {
						System.out.println("Sequence: " + sequence + ". FilterEH Ask Lo spike."); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Ask Lo Spike. Ask below: " + leewayLo); 
						priceSpike = true;
						event.setFilteredEvent(true);
						event.setFilteredReason(PriceEvent.FilterReason.ASK_SPIKE);
					}
				} 
				/*
				* Check if BID has spiked/dropped
				*/
				//Calculate the difference between current price and previous price, and check if it exceeds the Leeway
				priceDiff = Math.abs(priceEntity.getBid() - previousPrice.bid);
				leewayHi = previousPrice.bid + (previousPrice.bid * aggrConfigCurrency.pctLeewayToPreviousBid / 100);
				leewayLo = previousPrice.bid - (previousPrice.bid * aggrConfigCurrency.pctLeewayToPreviousBid / 100);

				if (priceEntity.getBid() > leewayHi) {
					if (bidSpikeHiCounter.containsKey(currency)) {
						bidSpikeHiCounter.replace(currency, bidSpikeHiCounter.get(currency) + 1);
					}
					else {
						bidSpikeHiCounter.put(currency, new Long(1));
					}
					if (bidSpikeHiCounter.get(currency) > aggrConfig.globalconfig.numberconsecutivespikesfiltered) {
						System.out.println("Sequence: " + sequence + ". Treating bid price spike as new normal."); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Treating bid price spike as new normal."); 
						bidSpikeHiCounter.replace(currency, new Long(0));
					}
					else {
						System.out.println("Sequence: " + sequence + ". FilterEH Bid Hi spike. Prev price: " + previousPrice.bid); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Bid Hi Spike. Bid exceeds: " + leewayHi); 
						priceSpike = true;
						event.setFilteredEvent(true);
						event.setFilteredReason(PriceEvent.FilterReason.BID_SPIKE);
					}
				}
				if (priceEntity.getBid() < leewayLo) {
					if (bidSpikeLoCounter.containsKey(currency)) {
						bidSpikeLoCounter.replace(currency, bidSpikeLoCounter.get(currency) + 1);
					}
					else {
						bidSpikeLoCounter.put(currency, new Long(1));
					}
					if (bidSpikeLoCounter.get(currency) > aggrConfig.globalconfig.numberconsecutivespikesfiltered) {
						System.out.println("Sequence: " + sequence + ". Treating bid price spike as new normal."); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Treating bid price spike as new normal."); 
						bidSpikeLoCounter.replace(currency, new Long(0));
					}
					else {
						System.out.println("Sequence: " + sequence + ". FilterEH Bid Lo spike."); 
						event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". Bid Lo Spike. Bid below: " + leewayLo); 
						priceSpike = true;
						event.setFilteredEvent(true);
						event.setFilteredReason(PriceEvent.FilterReason.BID_SPIKE);
					}
				}
				//Do not store price spikes. They should be treated as anomalies (faulty) and future
				//prices should not be compared to them
				if (!priceSpike) {
					askSpikeHiCounter.replace(currency, new Long(0));
					bidSpikeHiCounter.replace(currency, new Long(0));
					askSpikeLoCounter.replace(currency, new Long(0));
					bidSpikeLoCounter.replace(currency, new Long(0));

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
			event.addAuditEvent("PriceFilterEH. Cannot analyse pricing. No config in table aggrconfig. Currency: " + currency + ". Sequence: " + sequence); 
		}
		
		//Log the stats
		System.out.println("Sequence: " + sequence + ". PriceFilterEH filtered event?" + event.isFilteredEvent()); 
		event.addAuditEvent("PriceFilterEH. Sequence: " + sequence + ". PriceFilterEH filtered event?" + event.isFilteredEvent()); 
		StatsManager.eventReceived(event);
		
		if (sequence == PriceEventMain.producerCount) {
	        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	        Date dateEnd = new Date();
			System.out.println("PriceFilterEH processed: " + PriceEventMain.producerCount +
				"events. Complete time: " + dateFormat.format(dateEnd)); 
		}
	}
	
	/**
	 * Write the potential outlying quotes to MongoDB. NOT USED ATM. Since most details are in priceevents collection.
	 * Might be required in future when running in PROD, since we will not have all config details in priceevents for audit
	 * 
	 * We will also persist the state of the helper classes which contain the 
	 * current and previous quotes to aid troubleshooting
	 */
	private void persistOutliers(String message) {
		Gson gson = new Gson();
    	String jsonMessage = gson.toJson(message);
    	String jsonAggrConfig = gson.toJson( (aggrConfig == null) ? new AggrConfig() : aggrConfig );
    	String jsonPriceStats = gson.toJson( (priceStats == null) ? new PriceStats() : priceStats );
    	String jsonPreviousPrice = gson.toJson( (previousPrice == null) ? new PreviousPrice() : previousPrice );
    	String jsonPriceEntity = gson.toJson( (priceEntity == null) ? new PriceEntity() : priceEntity );
    	// System.out.println("GSON objects null: " + (aggrConfig == null) + (priceStats == null) + (previousPrice == null) + (priceEntity == null));
    	// System.out.println("jsonAggrConfig: " + jsonAggrConfig);
    	// System.out.println("jsonPriceStats: " + jsonPriceStats);
    	// System.out.println("jsonPreviousPrice: " + jsonPreviousPrice);
    	// System.out.println("jsonPriceEntity: " + jsonPriceEntity);
    	// System.out.println("GSON objects null: " + (jsonAggrConfig == null) + (jsonPriceStats == null) + (jsonPreviousPrice == null) + (jsonPriceEntity == null));
		db.getCollection("priceoutliers").insertOne(
			new Document("priceoutlier",
				new Document()
					.append("message", message)
					.append("currentpriceentity", Document.parse(jsonPriceEntity))
					.append("previousprice", Document.parse(jsonPreviousPrice))
					.append("config", Document.parse(jsonAggrConfig))
					.append("pricestats", Document.parse((jsonPriceStats == null) ? "{}" : jsonPriceStats))
			)
		);
	}
	
	/**
	 * The inner classes below represent this structure, and are used by GSON
	 * to convert JSON to Java entity classes
	 * 
    "globalconfig": {
        "numberconsecutivespikesfiltered": 3
    },
    "currencyconfig": [{
        "symbol": "AUDCAD",
        "pctLeewayAllowedSpread": 10.0,
        "pctLeewayToPreviousBid": 10.0,
        "pctLeewayToPreviousAsk": 5.5,
        "pctAboveMaxSpread": 5.0,
        "pctBelowMinSpread": 2.0
    }, {
        "symbol": "EURNZD",
        "pctLeewayAllowedSpread": 10.0,
        "pctLeewayToPreviousBid": 10.0,
        "pctLeewayToPreviousAsk": 1.5,
        "pctAboveMaxSpread": 5.0,
        "pctBelowMinSpread": 2.0
    }]
})
	*/

	private class AggrConfig {
		public globalconfig globalconfig;
		public AggrConfigCurrency[] currencyconfig;
	}
	private class globalconfig {
		public int numberconsecutivespikesfiltered;
	}
	private class AggrConfigCurrency {
		public String symbol;
		public double pctLeewayAllowedSpread;
		public double pctLeewayToPreviousBid;
		public double pctLeewayToPreviousAsk;
		public double pctAboveMaxSpread;
		public double pctBelowMinSpread;
	}
	private class PriceStats {
		public PriceID _id;
		public double averageAsk;
		public double averageBid;
		public double averageSpread;
		public double maxSpread;
		public double minSpread;
	}
	private class PriceID {
		public String symbol;
		public int hour;
	}
	private class PreviousPrice {
		public String symbol;
		public LocalDateTime datetime;
		public double ask;
		public double bid;
		public double spread;
	}
}