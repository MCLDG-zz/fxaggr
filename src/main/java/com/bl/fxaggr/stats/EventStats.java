package com.bl.fxaggr.stats;

import java.util.HashMap;
import java.util.Map;

import com.bl.fxaggr.disruptor.PriceEvent;

public class EventStats {
    public long totalNumberOfEvents;
    public long totalNumberOfFilteredEvents;
    public Map <PriceEvent.FilterReason, Long> numberPerFilteredReason = new HashMap <> ();
    public long maxSecondsBetweenQuoteTimeAndProcessedTime;
    public long minSecondsBetweenQuoteTimeAndProcessedTime;
}