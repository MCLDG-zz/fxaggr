package com.bl.fxaggr.disruptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.lmax.disruptor.EventHandler;

import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import org.bson.Document;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import com.google.gson.Gson;

/**
 * This class is a helper class of static variables and methods used by the
 * price aggregation engine
 */
public class PriceEventHelper {
    
	public static Map<String, AggrConfig.AggrConfigCurrency> aggrcurrencyconfigMap = new HashMap<>();
	public static AggrConfig aggrConfig;

	private static MongoDatabase db = null;
	private static String priceSelectionScheme = null;
	private static Instant timeOfLastPrimaryPrice = null;
	private static String currentPrimaryLiquidityProvider = null;
	private static long numberconsecutivespikesfiltered;
	
	static {
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

		//Read the config from the aggrconfig collection in Mongo
		FindIterable<Document> iterable = db.getCollection("aggrconfig").find();
	
	   	iterable.forEach(new Block<Document>() {
    		@Override
    		public void apply(final Document document) {
   				Gson gson = new Gson();
        		aggrConfig = gson.fromJson(document.toJson(), AggrConfig.class);  
    		}
		});
		for (AggrConfig.AggrConfigCurrency currencyconfig : aggrConfig.currencyconfig) {
       		aggrcurrencyconfigMap.put(currencyconfig.symbol, currencyconfig);
		}
		currentPrimaryLiquidityProvider = aggrConfig.globalconfig.primarybidask.primarysecondarytertiaryproviders[0];
		numberconsecutivespikesfiltered = aggrConfig.globalconfig.filteringrules.numberconsecutivespikesfiltered;
		priceSelectionScheme = aggrConfig.globalconfig.scheme;
   		System.out.println("PriceEventHelper Pricing scheme: " + priceSelectionScheme);
   		System.out.println("PriceEventHelper Primary Liquidity Provider: " + currentPrimaryLiquidityProvider);
	}

	public static void notePrimaryPriceQuote() {
	    timeOfLastPrimaryPrice = Instant.now();
	}
	public static boolean isIntervalSinceLastPrimaryPriceExceeded() {
	    if (timeOfLastPrimaryPrice == null) {
	        timeOfLastPrimaryPrice = Instant.now();
	    }
	    long intervalMS = timeOfLastPrimaryPrice.until(Instant.now(), ChronoUnit.MILLIS);
	    return (intervalMS > aggrConfig.globalconfig.primarybidask.timeintervalbeforeswitchingms);
	}
	public static void setCurrentPrimaryLiquidityProvider(String lp) {
	    currentPrimaryLiquidityProvider = lp;
	}
	public static String getCurrentPrimaryLiquidityProvider() {
	    return currentPrimaryLiquidityProvider;
	}
	public static String getPriceSelectionScheme() {
	    return priceSelectionScheme;
	}
	public static long getNumberConsecutiveSpikesFiltered() {
	    return numberconsecutivespikesfiltered;
	}
	/**
	 * Find current liquidity provider in the Map, then set the new
	 * primary provider to the next available provider
	 */
	public static boolean switchToNextBidAskLiquidityProvider() {
	    return true;
	}
}