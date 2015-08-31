package com.bl.fxaggr.disruptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.EmptyStackException;

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
 * 
 * TODO refactor the AggrConfig into it's own helper class
 */
public class PriceEventHelper {
    
	public static Map<String, AggrConfig.AggrConfigCurrency> aggrcurrencyconfigMap = new HashMap<>();
	public static AggrConfig aggrConfig;

	private static MongoDatabase db = null;
	
	//the following variables contain config values from AggrConfig
	private static String priceSelectionScheme = null;
	private static String currentPrimaryLiquidityProvider = null;
	private static String[] currentLiquidityProviders = null;
	private static long numberconsecutivespikesfiltered;
	private static long timeintervalformatchingquotesms;
	private static long minimummatchingquotesrequired;
	
	//the following variables contain runtime data specifically for the 
	//Primary Bid-Ask scheme
	private static Instant timeOfLastPrimaryPrice = null;
	//after a provider switch, if we start to receive quotes from the previous
	//primary provider we can consider switching back to the old primary
	private static long numberPreviousPrimaryQuotesSinceLastProviderSwitch = 0;
	private static Stack<String> previousPrimaryLiquidityProviders = new Stack<>();
	private static String previousPrimaryLiquidityProvider = null;
	private static boolean isPreviousPrimaryLiquidityProvider = false;

	//the following variables contain runtime data specifically for the 
	//Best Bid-Ask scheme
	private static Map<String, PriceEntity> latestPriceQuotes = new HashMap<>();
	
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
		currentLiquidityProviders = aggrConfig.globalconfig.primarybidask.primarysecondarytertiaryproviders;
		currentPrimaryLiquidityProvider = aggrConfig.globalconfig.primarybidask.primarysecondarytertiaryproviders[0];
		numberconsecutivespikesfiltered = aggrConfig.globalconfig.filteringrules.numberconsecutivespikesfiltered;
		timeintervalformatchingquotesms = aggrConfig.globalconfig.bestbidask.timeintervalformatchingquotesms;
		minimummatchingquotesrequired = aggrConfig.globalconfig.bestbidask.minimummatchingquotesrequired;
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
	public static boolean isRequiredNumberPreviousPrimaryQuotesReceived() {
	    return (numberPreviousPrimaryQuotesSinceLastProviderSwitch > aggrConfig.globalconfig.primarybidask.numberquotesbeforeswitchtoprevious);
	}
	public static void setLiquidityProviders(String[] lps) {
	    currentLiquidityProviders = lps;
	}
	public static void setCurrentPrimaryLiquidityProvider(String lp) {
	    currentPrimaryLiquidityProvider = lp;
	}
	public static String getCurrentPrimaryLiquidityProvider() {
	    return currentPrimaryLiquidityProvider;
	}
	public static String getPreviousPrimaryLiquidityProvider() {
	    return previousPrimaryLiquidityProvider;
	}
	public static boolean isPreviousPrimaryLiquidityProvider() {
	    return isPreviousPrimaryLiquidityProvider;
	}
	public static long incrementPriceCounterForPreviousPrimaryProvider() {
	    return numberPreviousPrimaryQuotesSinceLastProviderSwitch++;
	}
	public static String getPriceSelectionScheme() {
	    return priceSelectionScheme;
	}
	public static void setPriceSelectionScheme(String scheme) {
	    priceSelectionScheme = scheme;
	}
	public static long getNumberConsecutiveSpikesFiltered() {
	    return numberconsecutivespikesfiltered;
	}
	/**
	 * Find current liquidity provider in the array, then set the new
	 * primary provider to the next available provider.
	 * <p>
	 * A couple of things can go wrong here:
	 * <ul>
	 * <li>There are no more liquidity providers available. This is handled based on
	 * the globalconfig.primarybidask.actionwhennomoreliquidityproviders config
	 * setting
	 * <li>The current liquidity provider cannot be found. This could only be due to
	 * a bug, so throw an exception
	 * </ul>
	 * 
	 * @return	A boolean indicating whether the primary liquidity provider has 
	 * 			successfully switched
     *  		<code>true</code> primary liquidity provider has 
	 * 			successfully switched
	 * 			<code>false</code> primary liquidity provider has not
	 * 			successfully switched
	 */
	public static boolean switchToNextBidAskLiquidityProvider() {
		for (int i = 0; i < currentLiquidityProviders.length; i++) {
        	if (currentPrimaryLiquidityProvider.equals(currentLiquidityProviders[i])) {
        		//Check if there are more liquidity providers available
				System.out.println("PriceEventHelper - about to switch providers. If no providers available, scheme is: " + aggrConfig.globalconfig.primarybidask.actionwhennomoreliquidityproviders + " i == (currentLiquidityProviders.length - 1)" + (i == (currentLiquidityProviders.length - 1))); 
        		if (i == (currentLiquidityProviders.length - 1)) {
        			if (aggrConfig.globalconfig.primarybidask.actionwhennomoreliquidityproviders.equals("Stay with current provider")) {
        				//Do nothing. Do not change providers
						System.out.println("PriceEventHelper - could not switch providers. No providers available. Current is: " + getCurrentPrimaryLiquidityProvider()); 
        				return false;
        			}
        		}
        		//Switch providers
        		previousPrimaryLiquidityProviders.push(currentPrimaryLiquidityProvider);
        		previousPrimaryLiquidityProvider = currentPrimaryLiquidityProvider;
        		isPreviousPrimaryLiquidityProvider = true;
				numberPreviousPrimaryQuotesSinceLastProviderSwitch = 0;
				currentPrimaryLiquidityProvider = currentLiquidityProviders[i + 1];
				
				System.out.println("PriceEventHelper - after switching LP. New provider is: " + getCurrentPrimaryLiquidityProvider() + ". Previous provider was: " + previousPrimaryLiquidityProvider); 
				return true;
        	}
		}
		//If we get this far it means we did not find the current provider, and were
		//unable to switch
	    return false;
	}
	/**
	 * Find previous liquidity provider in the array, then set the new
	 * primary provider to the previous provider.
	 * <p>
	 * One things can go wrong here:
	 * <ul>
	 * <li>There is no previous liquidity provider. This could only be due to
	 * a bug, so throw an exception
	 * </ul>
	 * 
	 * @return	A boolean indicating whether the primary liquidity provider has 
	 * 			successfully switched to the previous
     *  		<code>true</code> primary liquidity provider has 
	 * 			successfully switched
	 * 			<code>false</code> primary liquidity provider has not
	 * 			successfully switched
	 */
	public static boolean switchToPreviousBidAskLiquidityProvider() {
		boolean successfulSwitch = false;
		//First switch to previous provider
		try {
	       	String prevLP = previousPrimaryLiquidityProviders.pop();
			currentPrimaryLiquidityProvider = prevLP;
			numberPreviousPrimaryQuotesSinceLastProviderSwitch = 0;
			System.out.println("PriceEventHelper - after switching LP to previous. New provider is: " + getCurrentPrimaryLiquidityProvider()); 
			successfulSwitch = true;
		}
		catch (EmptyStackException ex) {
			System.out.println("PriceEventHelper - could not switch to previous provider. No previous provider available: " + ex.toString()); 
		}
		//Then set the new previous provider
		//I do this is two try-catch blocks so I can treat the same exception EmptyStackException
		//differently
		try {
       		previousPrimaryLiquidityProvider = previousPrimaryLiquidityProviders.peek();
       		isPreviousPrimaryLiquidityProvider = true;
			System.out.println("PriceEventHelper - after switching LP to previous, new previous is: " + previousPrimaryLiquidityProvider); 
		}
		catch (EmptyStackException ex) {
			//There is no previous provider, so set this to false
       		isPreviousPrimaryLiquidityProvider = false;
		}
	    return successfulSwitch;
	}
	/**
	 * Get the latest price quotes that match the current symbol
	 * 
	 * @return	
     *  		<code>Map containing two PriceEntity objects. The first PriceEntity 
     * 			object is keyed 'BestBid' and contains the PriceEntity that provided
     * 			the best bid price. The second PriceEntity 
     * 			object is keyed 'BestAsk' and contains the PriceEntity that provided
     * 			the best ask price. These are only returned IF each quote is within
     * 			the time threshold configured in 'bestbidask.timeintervalformatchingquotesms' 
     * 			and the number of liquidity providers providing quotes is at least
     * 			equal to 'bestbidask.minimummatchingquotesrequired'
	 * 			<code>null</code> where the above conditions are not met
	 */
	public static Map<String, PriceEntity> getBestBidAskForSymbol(String symbol) {
		double bestBid = 0;
		double bestAsk = Double.MAX_VALUE;
		int numValidQuotes = 0;
		Map<String, PriceEntity> bestBidAsk = new HashMap<>();
		List<PriceEntity> priceQuotes = new ArrayList<>();
		
		for (int i = 0; i < currentLiquidityProviders.length; i++) {
			PriceEntity pe = latestPriceQuotes.get(currentLiquidityProviders[i] + symbol);
			if (pe != null) {
				//check the timestamp, which must be within the bounds defined in
				//the config, in 'bestbidask.timeintervalformatchingquotesms'
				long msBetween = pe.getProcessedTimestamp().until(Instant.now(), ChronoUnit.MILLIS);
				if (msBetween <= timeintervalformatchingquotesms) {
					priceQuotes.add(pe);
					numValidQuotes++;
					//update the best bid/ask
					if (pe.getBid() > bestBid) {
						bestBid = pe.getBid();
						bestBidAsk.put("BestBid", pe);
					}
					if (pe.getAsk() < bestAsk) {
						bestAsk = pe.getAsk();
						bestBidAsk.put("BestAsk", pe);
					}
				}
			}
		}
		//check the number of valid quotes, which must be at least equal to value specified in
		//the config, in 'bestbidask.minimummatchingquotesrequired'
		if (numValidQuotes >= minimummatchingquotesrequired) {
			return bestBidAsk;
		}
		else {
			return null;
		}
	}
	/**
	 * Take a copy of the Price Entity and store so we can track the 
	 * latest pricess for each liquidity provider and symbol
	 */
	public static void storeLatestPriceQuote(PriceEntity pe) {
	
        PriceEntity priceEntity = new PriceEntity();
        priceEntity.setSequence(pe.getSequence());
        priceEntity.setBid(pe.getBid());
        priceEntity.setAsk(pe.getAsk());
        priceEntity.setSpread(pe.getSpread());
        priceEntity.setOpen(pe.getOpen());
        priceEntity.setClose(pe.getClose());
        priceEntity.setSymbol(pe.getSymbol());
        priceEntity.setLiquidityProvider(pe.getLiquidityProvider());
        priceEntity.setDatetime(pe.getDatetime());
        priceEntity.setProcessedTimestamp(Instant.now());
        
        //Store the price entity in the tracking Map
        //The key to the Map is made up of liquidity provider plus symbol, 
        //for example: 
        //		BloombergUSDCAD
        latestPriceQuotes.put(pe.getLiquidityProvider() + pe.getSymbol(), priceEntity);
	}
}