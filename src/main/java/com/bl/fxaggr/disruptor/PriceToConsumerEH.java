package com.bl.fxaggr.disruptor;

import com.lmax.disruptor.EventHandler;

/**
 * Sends quote to consumer
 */
public class PriceToConsumerEH implements EventHandler<PriceEvent> {

	public PriceToConsumerEH() {
	}

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		event.setEventState(PriceEvent.EventState.FINAL_QUOTE);
		//Ignore non-valid events
		if (event.getEventStatus() != PriceEvent.EventStatus.VALID) {
			return;
		}
		System.out.println("Sequence: " + sequence + " sent to consumer. QuoteToConsumerEH"); 
	}
}