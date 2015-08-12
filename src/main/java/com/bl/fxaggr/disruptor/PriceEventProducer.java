package com.bl.fxaggr.disruptor;

import com.lmax.disruptor.RingBuffer;
import java.nio.ByteBuffer;

public class PriceEventProducer {
    private final RingBuffer < PriceEvent > ringBuffer;

    public PriceEventProducer(RingBuffer < PriceEvent > ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void onData(ByteBuffer bb) {
        long sequence = ringBuffer.next(); // Grab the next sequence
        try {
            PriceEvent event = ringBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence
            event.getPriceEntity().setSequence(bb.getLong(0)); // Fill with data
        }
        finally {
            ringBuffer.publish(sequence);
        }
    }
}