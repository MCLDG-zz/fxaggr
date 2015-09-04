package com.bl.fxaggr.disruptor.eventhandler;

import com.bl.fxaggr.disruptor.*;

import com.lmax.disruptor.EventHandler;

/**
 * Sends the final quote to consumer
 */
public class PriceToConsumerEH implements EventHandler<PriceEvent> {

	public PriceToConsumerEH() {
	}

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		event.setEventState(PriceEvent.EventState.FINAL_QUOTE);
		event.setSentToConsumerInstant();
		
		//Ignore non-valid events
		if (event.getEventStatus() != PriceEvent.EventStatus.VALID) {
			return;
		}
	}
}