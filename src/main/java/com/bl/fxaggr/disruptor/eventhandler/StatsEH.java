package com.bl.fxaggr.disruptor.eventhandler;

import com.bl.fxaggr.disruptor.*;
import com.bl.fxaggr.stats.StatsManager;
import com.bl.fxaggr.stats.EventStats;

import com.lmax.disruptor.EventHandler;

/**
 * Persists metrics and statistics information to provide visibility into the 
 * working and performance of the price quote engine
 * 
 * @see StatsManager
 */
public class StatsEH implements EventHandler<PriceEvent> {
	
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		StatsManager.eventReceived(event);
	}
}