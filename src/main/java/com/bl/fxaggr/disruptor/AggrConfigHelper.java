package com.bl.fxaggr.disruptor;

import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import org.bson.Document;
import com.mongodb.Block;
import com.mongodb.client.FindIterable; 

import com.google.gson.Gson;

/**
 * This class is a helper class for the price aggregation engine config
 * 
 */
public class AggrConfigHelper {
    
	public static AggrConfig aggrConfig;
	private static MongoDatabase db = null;
	
	//the following variables contain config values from AggrConfig
	public static Map<String, AggrConfig.AggrConfigCurrency> aggrcurrencyconfigMap = new HashMap<>();
	private static String priceSelectionScheme = null;
	private static String currentPrimaryLiquidityProvider = null;
	private static String[] currentLiquidityProviders = null;
	private static String actionwhennomoreliquidityproviders;
	private static long numberconsecutivespikesfiltered;
	private static long timeintervalformatchingquotesms;
	private static long minimummatchingquotesrequired;
	private static long numberquotesbeforeswitchtoprevious;
	private static int ewmaperiods;
	
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
		if (aggrConfig == null || aggrConfig.currencyconfig == null) {
	   		System.out.println("PriceEventHelper - no Config in collection aggrconfig. Application requires a config to operate.");
		}
		for (AggrConfig.AggrConfigCurrency currencyconfig : aggrConfig.currencyconfig) {
       		aggrcurrencyconfigMap.put(currencyconfig.symbol, currencyconfig);
		}
		currentLiquidityProviders = aggrConfig.globalconfig.liquidityproviders;
		currentPrimaryLiquidityProvider = aggrConfig.globalconfig.liquidityproviders[0];
		numberconsecutivespikesfiltered = aggrConfig.globalconfig.filteringrules.numberconsecutivespikesfiltered;
		timeintervalformatchingquotesms = aggrConfig.globalconfig.bestbidask.timeintervalformatchingquotesms;
		minimummatchingquotesrequired = aggrConfig.globalconfig.bestbidask.minimummatchingquotesrequired;
		ewmaperiods = aggrConfig.globalconfig.smoothing.ewmaperiods;
		priceSelectionScheme = aggrConfig.globalconfig.scheme;
		actionwhennomoreliquidityproviders = aggrConfig.globalconfig.primarybidask.actionwhennomoreliquidityproviders;
		numberquotesbeforeswitchtoprevious = aggrConfig.globalconfig.primarybidask.numberquotesbeforeswitchtoprevious;
   		System.out.println("PriceEventHelper Pricing scheme: " + priceSelectionScheme);
   		System.out.println("PriceEventHelper Primary Liquidity Provider: " + currentPrimaryLiquidityProvider);
	}
	public static void setNumberQuotesBeforeSwitchToPrevious(long numberQuotes) {
		numberquotesbeforeswitchtoprevious = numberQuotes;
	}
	public static long getNumberQuotesBeforeSwitchToPrevious() {
		return numberquotesbeforeswitchtoprevious;
	}
	public static void setActionWhenNoMoreLiquidityProviders(String action) {
		actionwhennomoreliquidityproviders = action;
	}
	public static String getActionWhenNoMoreLiquidityProviders() {
		return actionwhennomoreliquidityproviders;
	}
	public static void setLiquidityProviders(String[] lps) {
	    currentLiquidityProviders = lps;
	}
	public static String[] getCurrentLiquidityProviders() {
		return currentLiquidityProviders;
	}
	public static void setCurrentPrimaryLiquidityProvider(String lp) {
	    currentPrimaryLiquidityProvider = lp;
	}
	public static String getCurrentPrimaryLiquidityProvider() {
	    return currentPrimaryLiquidityProvider;
	}
	public static void setPriceSelectionScheme(String scheme) {
	    priceSelectionScheme = scheme;
	}
	public static String getPriceSelectionScheme() {
	    return priceSelectionScheme;
	}
	public static void setEWMAPeriods(int periods) {
		ewmaperiods = periods;
	}
	public static int getEWMAPeriods() {
		return ewmaperiods;
	}
	public static long getNumberConsecutiveSpikesFiltered() {
	    return numberconsecutivespikesfiltered;
	}
	public static long getMinimumMatchingQuotesRequired() {
	    return minimummatchingquotesrequired;
	}
	public static long getTimeIntervalForMatchingQuotesMS() {
	    return timeintervalformatchingquotesms;
	}
}