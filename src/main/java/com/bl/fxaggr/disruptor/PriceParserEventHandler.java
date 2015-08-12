package com.bl.fxaggr.disruptor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.lmax.disruptor.EventHandler;

/*
 * To cut a long story short - the ringBuffer is initialised with a size that is
 * usually less than the number of events to be processed. For instance, I initialise
 * it with 65536 and process 10M events. In this case the buffer is reused, and the events
 * in the buffer are reused. This means that any instance variables in the event will
 * retain their existing values. 
 * 
 * In my case I create the event by passing a String containing a Price. However,
 * if the event is being reused there will already be a PriceEntity object in the
 * event which will refer to a different Price. This means that the event should
 * really be reset before being reused. If the event contains any variables, such
 * as 'boolean matched', and this is set by some later event handler, it may contain
 * a value of true for this new event (since the event is reused)
 * 
 * See the use of the reset method below
 */
public class PriceParserEventHandler implements EventHandler < PriceEvent > {
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		if (event.getPriceEntity() != null) {
			if (event.getPriceEntity().getProcessed()) {}
		}

		if (sequence == PriceEventMain.producerCount) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
			Date dateEnd = new Date();
			System.out.println("PriceParserEventHandler processed: " + PriceEventMain.producerCount +
				"events. Complete time: " + dateFormat.format(dateEnd));
		}
	}
}