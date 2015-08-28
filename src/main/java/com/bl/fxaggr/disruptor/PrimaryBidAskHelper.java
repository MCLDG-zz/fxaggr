package com.bl.fxaggr.disruptor;

import java.lang.String;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * NOT USED ANYMORE - SHOULD BE POSSIBLE TO REMOVE THIS CLASS
 * 
 * A helper class for the Primary Bid/Ask strategy. Instead of PrimaryBidAskEH waiting for
 * a matching quote from a primary liquidity provider, this class allows PrimaryBidAskEH to
 * store those price quotes that must wait for a matching primary quote.
 * 
 * @see PrimaryBidAskEH
 */
public class PrimaryBidAskHelper {
    //TODO might be better to use ConcurrentMap since this can be updated from 2 threads at the same time
    private static Map<String, PriceEvent> nonPrimaryCurrencyEventMap = new HashMap<>();
    
    public static void storeNonPrimaryCurrency(String currency, PriceEvent event) {
        Instant timestamp = Instant.now();
        //Since disruptor events are reused we need to clone the price event before storing
        nonPrimaryCurrencyEventMap.put(currency, clonePriceEvent(event));
    }
    public static void removeNonPrimaryCurrency(String currency) {
        nonPrimaryCurrencyEventMap.remove(currency);
    }
    public static PriceEvent getEventForNonPrimaryCurrency(String currency) {
        return nonPrimaryCurrencyEventMap.get(currency);
    }
    public static Map<String, PriceEvent> getWaitingNonPrimaryCurrencies() {
        return nonPrimaryCurrencyEventMap;
    }
    /**
     * There are numerous ways in Java to clone an object. I am taking the lazy
     * way and copying it manually, which isn't ideal but is the least effort
     * and acceptable since the object has very few variables and does not require
     * a deep copy
     */
    private static PriceEvent clonePriceEvent(PriceEvent event) {
        PriceEvent clonedEvent = new PriceEvent();
        clonedEvent.setPriceEntity(event.getPriceEntity());
        clonedEvent.setEventStatus(event.getEventStatus());
        List<PriceEvent.FilterReason> filterReasons = event.getFilteredReasons();
        filterReasons.stream().forEach((filterReason) -> {
        	clonedEvent.addFilteredReason(filterReason);
        });
        List<String> eventAuditTrail = event.getEventAuditTrail();
        eventAuditTrail.stream().forEach((eventAuditRecord) -> {
        	clonedEvent.addAuditEvent(eventAuditRecord);
        });
        return clonedEvent;
    }
}