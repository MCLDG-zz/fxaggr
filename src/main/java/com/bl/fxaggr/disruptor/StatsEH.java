package com.bl.fxaggr.disruptor;

import com.lmax.disruptor.EventHandler;

import com.bl.fxaggr.stats.StatsManager;
import com.bl.fxaggr.stats.EventStats;

public class StatsEH implements EventHandler<PriceEvent> {
	
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		StatsManager.eventReceived(event);
	}
}