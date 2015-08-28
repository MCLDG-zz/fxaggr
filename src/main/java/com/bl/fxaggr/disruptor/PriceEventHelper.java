package com.bl.fxaggr.disruptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.Stack;
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
	
	//the following variables contain runtime data
	private static Instant timeOfLastPrimaryPrice = null;
	//after a provider switch, if we start to receive quotes from the previous
	//primary provider we can consider switching back to the old primary
	private static long numberPreviousPrimaryQuotesSinceLastProviderSwitch = 0;
	private static Stack<String> previousPrimaryLiquidityProviders = new Stack<>();
	private static String previousPrimaryLiquidityProvider = null;
	private static boolean isPreviousPrimaryLiquidityProvider = false;
	
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
		System.out.println("PriceEventHelper - isRequiredNumberPreviousPrimaryQuotesReceived: " + numberPreviousPrimaryQuotesSinceLastProviderSwitch + " config: " + aggrConfig.globalconfig.primarybidask.numberquotesbeforeswitchtoprevious); 
	    return (numberPreviousPrimaryQuotesSinceLastProviderSwitch > aggrConfig.globalconfig.primarybidask.numberquotesbeforeswitchtoprevious);
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
		System.out.println("PriceEventHelper - number quotes for previous providers: " + numberPreviousPrimaryQuotesSinceLastProviderSwitch); 
	    return numberPreviousPrimaryQuotesSinceLastProviderSwitch++;
	}
	public static String getPriceSelectionScheme() {
	    return priceSelectionScheme;
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
}