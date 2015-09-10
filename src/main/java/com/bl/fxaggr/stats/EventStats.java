package com.bl.fxaggr.stats;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

import com.bl.fxaggr.disruptor.PriceEvent;

public class EventStats {
    public long totalNumberOfEvents;
    public long totalNumberOfFilteredEvents;
    public long eventsPerSecond;
    public long publishToRingBufferDelay;
    public LocalDateTime publishToRingBufferDelayTS;
    public long totalLiquidityProviderSwitches;
    public long totalLiquidityProviderSwitchBacks;
    public long totalLiquidityProviderUnableToSwitch;
    public long totalNumberConfiguredBestBidAskEvents;
    public long totalNumberAppliedBestBidAskEvents;
    public long totalNumberAppliedPrimaryBidAskEvents;
    public Map <PriceEvent.FilterReason, Long> numberPerFilteredReason = new HashMap <> ();
    public long avgTimeBetweenQuoteAndProcessStartTime;
    public long maxTimeBetweenQuoteAndProcessStartTime;
    public long minTimeBetweenQuoteAndProcessStartTime = Long.MAX_VALUE;
    public long avgProcessingTime;
    public long maxProcessingTime;
    public long minProcessingTime = Long.MAX_VALUE;
    public long avgPersistenceTime;
    public long maxPersistenceTime;
    public long minPersistenceTime = Long.MAX_VALUE;
}