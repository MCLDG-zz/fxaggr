package com.bl.fxaggr.disruptor;

import java.nio.ByteBuffer;

import com.lmax.disruptor.EventTranslator;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.EventTranslatorOneArg;

public class PriceEventProducerWithTranslator {
	private final RingBuffer<PriceEvent> ringBuffer;

	public PriceEventProducerWithTranslator(RingBuffer<PriceEvent> ringBuffer) {
		this.ringBuffer = ringBuffer;
	}

	private static final EventTranslatorOneArg<PriceEvent, String> TRANSLATORO = new EventTranslatorOneArg<PriceEvent, String>() {
		public void translateTo(PriceEvent event, long sequence, String msg) {
//			event.setPriceMsg(msg);
//			event.setPriceJSON(msg);
//			if (sequence > 65530)
//				System.out.println("Publisher Sequence: " + sequence);
		}
	};

	// public void onData() {
	// ringBuffer.publishEvent(TRANSLATOR);
	// }

	public void onData(String msg) {
		ringBuffer.publishEvent(TRANSLATORO, msg);
//		if (ringBuffer.getCursor() > 65530)
//		System.out.println("Buffer cursor: " + ringBuffer.getCursor()
//				+ " Remain Capacity: " + ringBuffer.remainingCapacity());

	}
}