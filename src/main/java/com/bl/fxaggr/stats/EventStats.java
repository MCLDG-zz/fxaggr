package com.bl.fxaggr.stats;

import java.util.HashMap;
import java.util.Map;

import com.bl.fxaggr.disruptor.PriceEvent;

public class EventStats {
    public long totalNumberOfEvents;
    public long totalNumberOfFilteredEvents;
    public long totalLiquidityProviderSwitches;
    public long totalLiquidityProviderSwitchBacks;
    public long totalLiquidityProviderUnableToSwitch;
    public Map <PriceEvent.FilterReason, Long> numberPerFilteredReason = new HashMap <> ();
    public long maxSecondsBetweenQuoteTimeAndProcessedTime;
    public long minSecondsBetweenQuoteTimeAndProcessedTime;
    public long maxProcessingTime;
    public long minProcessingTime;
    public long avgProcessingTime;
    public long maxPersistenceTime;
    public long minPersistenceTime;
    public long avgPersistenceTime;
    
}