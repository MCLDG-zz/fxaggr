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

public class PriceCompareEH implements EventHandler<PriceEvent> {
	
	private Map<String, AggrConfigCurrency> aggrcurrencyconfigMap = new HashMap<>();
	private String currency = null;
	private AggrConfig aggrConfig;
	private AggrConfigCurrency aggrConfigCurrency;
	private PriceEntity priceEntity;
	private MongoDatabase db = null;
	private String priceSelectionScheme = null;
	
	public PriceCompareEH() {
		System.out.println("PriceCompareEH created. Object ID:: " + this.toString()); 
		MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongoClient.getDatabase("fxaggr");

		//Read the config from the aggrconfig collection in Mongo
		//TODO: since I call this in both EH classes, refactor to its own class/method
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
	}

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		if (aggrConfig == null) {
			System.out.println("PriceCompareEH cannot analyse pricing. No config in table aggrconfig. Sequence: " + sequence); 
			event.addAuditEvent("PriceCompareEH. Cannot analyse pricing. No config in table aggrconfig. Sequence: " + sequence); 
			return;
		}
		//Ignore filtered events
		if (event.isFilteredEvent()) {
			return;
		}

		priceEntity = event.getPriceEntity();
		currency = priceEntity.getSymbol();
		aggrConfigCurrency = aggrcurrencyconfigMap.get(currency);
		priceSelectionScheme = aggrConfig.globalconfig.scheme;

		switch(priceSelectionScheme) {
			case "Primary Bid/Ask": 
				event.setIgnoreEvent(this.handlePrimaryBidAskScheme());
				break;
			case "Primary Bid/Best Ask": 
				break;
			case "Best Bid/Primary Ask": 
				break;
			case "Best Bid/Ask": 
				break;
		}

		//Log the stats
		System.out.println("Sequence: " + sequence + ". PriceCompareEH ignore event?" + event.isIgnoreEvent()); 
		event.addAuditEvent("PriceCompareEH. Sequence: " + sequence + ". PriceCompareEH ignore event?" + event.isIgnoreEvent()); 
		
		if (sequence == PriceEventMain.producerCount) {
	        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	        Date dateEnd = new Date();
			System.out.println("PriceCompareEH processed: " + PriceEventMain.producerCount +
				"events. Complete time: " + dateFormat.format(dateEnd)); 
		}
	}
	
	/**
	 * Handle this price event based on the Primary Bid/Ask scheme. In this scheme
	 * prices only continue to feed through to consumers if they are received
	 * from the primary liquidity provider. Other prices are ignored
	 * 
	 * @return	A boolean indicating whether this price quote should be ignored
     *  		<code>true</code> if this price quote should be ignored
	 * 			<code>false</code> if the price quote should continue to be processed and 
	 * 			possibly sent to consumer
	 */
	private boolean handlePrimaryBidAskScheme() {
		System.out.println("Sequence: " + priceEntity.getSequence() + ". Compare price scheme. Config is: " + aggrConfig.globalconfig.primaryliquidityprovider + ". Price Entity is: " + priceEntity.getLiquidityProvider() + aggrConfig.globalconfig.primaryliquidityprovider.equals(priceEntity.getLiquidityProvider())); 
		if (aggrConfig.globalconfig.primaryliquidityprovider.equals(priceEntity.getLiquidityProvider())) {
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * The inner classes below represent this structure, and are used by GSON
	 * to convert JSON to Java entity classes
	 * 
	*/
	private class AggrConfig {
		public globalconfig globalconfig;
		public AggrConfigCurrency[] currencyconfig;
	}
	private class globalconfig {
		public int numberconsecutivespikesfiltered;
		public String[] liquidityproviders;
		public String[] availableschemes;
		public String primaryliquidityprovider;
		public String scheme;
	}
	private class AggrConfigCurrency {
		public String symbol;
		public double pctLeewayAllowedSpread;
		public double pctLeewayToPreviousBid;
		public double pctLeewayToPreviousAsk;
		public double pctAboveMaxSpread;
		public double pctBelowMinSpread;
	}
}