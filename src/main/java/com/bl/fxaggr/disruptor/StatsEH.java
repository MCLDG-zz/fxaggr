package com.bl.fxaggr.disruptor;

import com.lmax.disruptor.EventHandler;

import com.bl.fxaggr.stats.StatsManager;
import com.bl.fxaggr.stats.EventStats;

/**
 * Persists metrics and statistics information to provide visibility into the 
 * working and performance of the price quote engine
 */
public class StatsEH implements EventHandler<PriceEvent> {
	
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		StatsManager.eventReceived(event);
	}
}