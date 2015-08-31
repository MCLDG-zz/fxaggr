package com.bl.fxaggr.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClient;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.BsonValue;
import org.bson.BsonString;
import org.bson.BsonDocument;

import com.google.gson.Gson;

import com.bl.fxaggr.disruptor.PriceEvent;
import com.bl.fxaggr.disruptor.PriceEntity;

/**
 * StatsManager receives events (such as 'event processed', event filtered') and
 * stores useful statistics (such as 'number of events processed'). These statistics
 * are available from this class during runtime and are also persisted in the 
 * DB for later analysis. 
 * 
 * DB persistence takes place based on a configurable time period - stats are
 * not persisted as they are received. Instead they are accumulated in this class
 * and persisted regularly, so as not to overload the DB.
 */
public class StatsManager {
    private static Map <String, EventStats> symbolStats = new HashMap <> ();
    private static EventStats overallStats = new EventStats();
	private static MongoDatabase db = null;
	private static BsonValue overallStatsID;
	private static BsonValue symbolStatsID;
    
    /*
    * Setup the timer to persist the stats at regular intervals
    */
    static {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                persistStats();
            }
        };
    
        Timer timer = new Timer();
        long delay = 1000;
        long interval= 1 * 1000; 
    
        timer.scheduleAtFixedRate(task, delay, interval);
        
		MongoClient mongoClient = null;
		try {
			mongoClient = new MongoClient();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		db = mongoClient.getDatabase("fxaggr");

		//Initialise the BSON IDs used to update the stats records in Mongo
		overallStatsID = new BsonString("1");
    }

    public static void eventReceived(PriceEvent event) {
        //Get the stats for the current symbol. Create it if it doesn't exist
        EventStats symbolStat = symbolStats.get(event.getPriceEntity().getSymbol());
        if (symbolStat == null) {
            symbolStat = new EventStats();
            symbolStats.put(event.getPriceEntity().getSymbol(), symbolStat);
        }

        overallStats.totalNumberOfEvents++;
        symbolStat.totalNumberOfEvents++;
        
        //Update the filtered event counters
        if (event.getEventStatus() == PriceEvent.EventStatus.FILTERED) {
            overallStats.totalNumberOfFilteredEvents++;
            symbolStat.totalNumberOfFilteredEvents++;
            List <PriceEvent.FilterReason> filteredReasons = event.getFilteredReasons();
            for (PriceEvent.FilterReason filteredReason: filteredReasons) {
                if (overallStats.numberPerFilteredReason.containsKey(filteredReason)) {
                    overallStats.numberPerFilteredReason.replace(filteredReason, overallStats.numberPerFilteredReason.get(filteredReason) + 1);
                } 
                else {
                    overallStats.numberPerFilteredReason.put(filteredReason, new Long(1));
                }
                if (symbolStat.numberPerFilteredReason.containsKey(filteredReason)) {
                    symbolStat.numberPerFilteredReason.replace(filteredReason, symbolStat.numberPerFilteredReason.get(filteredReason) + 1);
                }
                else {
                    symbolStat.numberPerFilteredReason.put(filteredReason, new Long(1));
                }
            }
        }
        //Update the liqudity provider event counters
        if (event.getEventLPSwitch() == PriceEvent.EventLPSwitch.SWITCH_NEXT_PRIMARY) {
            overallStats.totalLiquidityProviderSwitches++;
            symbolStat.totalLiquidityProviderSwitches++;
        }
        if (event.getEventLPSwitch() == PriceEvent.EventLPSwitch.SWITCH_BACK_PREVIOUS_PRIMARY) {
            overallStats.totalLiquidityProviderSwitchBacks++;
            symbolStat.totalLiquidityProviderSwitchBacks++;
        }
        if (event.getEventLPSwitch() == PriceEvent.EventLPSwitch.UNABLE_TO_SWITCH) {
            overallStats.totalLiquidityProviderUnableToSwitch++;
            symbolStat.totalLiquidityProviderUnableToSwitch++;
        }
    }
    public static void resetStats() {
        symbolStats = new HashMap <String, EventStats> ();
        overallStats = new EventStats();
    }
    private static void persistStats() {
    	//Convert the stats to JSON and write to DB
		Gson gson = new Gson();
    	String jsonOverallStats = gson.toJson(overallStats);
		journalToMongo("overall", overallStatsID, jsonOverallStats);
    	
        for (Map.Entry<String, EventStats> entry : symbolStats.entrySet()) {
            String jsonSymbolStats = gson.toJson(entry.getValue());
            symbolStatsID = new BsonString(entry.getKey());
            
        	//Write to Mongo
    		journalToMongo("symbol", symbolStatsID, jsonSymbolStats);
        }
    }
	/**
	 * Write the event to MongoDB
	 */
	private static void journalToMongo(String whichStat, BsonValue ID, String content) {
		UpdateResult updateResult = db.getCollection("runtimestats").replaceOne(new Document("_id", ID),
		    Document.parse(content), new UpdateOptions().upsert(true));
	}
}
