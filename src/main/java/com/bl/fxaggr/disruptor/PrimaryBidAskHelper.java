package com.bl.fxaggr.disruptor;

import java.lang.String;
import java.util.Map;
import java.util.HashMap;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PrimaryBidAskHelper {
    private static Map<String, Instant> primaryCurrencyEventMap = new HashMap<>();
    
    public static void storePrimaryCurrencyTimestamp(String currency) {
        Instant timestamp = Instant.now();
        primaryCurrencyEventMap.put(currency, timestamp);
    }
    public static long getPrimaryCurrencyTimestampMS(String currency) {
        Instant now = Instant.now();
        Instant primaryCurrencyTimestamp = primaryCurrencyEventMap.get(currency);
        
        //Calculate the milliseconds since the primary currency price arrived
        long ms = primaryCurrencyTimestamp.until(now, ChronoUnit.MILLIS);
        return ms;
    }
}