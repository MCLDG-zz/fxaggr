package com.bl.fxaggr.disruptor.intervalproducer;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import java.util.HashMap;
import java.util.Map;
import java.time.Instant;
import java.lang.InterruptedException;

import com.bl.fxaggr.disruptor.PriceEvent;
import com.bl.fxaggr.disruptor.PriceEntity;
import com.bl.fxaggr.disruptor.PriceEventFactory;
import com.bl.fxaggr.disruptor.PrimaryBidAskHelper;

public class PriceEventIntervalProducer extends Thread {
    public static RingBuffer < PriceEvent > ringBuffer;

    public static void main(String[] args) throws Exception {
        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // The factory for the event
        PriceEventFactory factory = new PriceEventFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = (int) Math.pow(2, 16);

        // Construct the Disruptor
        Disruptor < PriceEvent > disruptor = new Disruptor < > (factory, bufferSize,
            executor, ProducerType.SINGLE,
            new com.lmax.disruptor.BlockingWaitStrategy());

        // EventHandler < PriceEvent > eh1 = new JournalToFileEventHandler();
        // EventHandler < PriceEvent > eh2 = new JournalToMongoEventHandler();
        // EventHandler < PriceEvent > eh3 = new PriceFilterEH();
        // EventHandler < PriceEvent > eh4 = new PriceParserEventHandler();

        // Connect the handler
        //disruptor.handleEventsWith(eh3).then(eh4).then(eh2);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        ringBuffer = disruptor.getRingBuffer();

        // Start the data generator
        System.out.println("PriceEventIntervalProducer started at: " + Instant.now().toString());
        
        Map<String, PriceEvent> waitingNonPrimaryCurrencies = null;
        while (true) {
            System.out.println("PriceEventIntervalProducer checking for waiting currencies");
            waitingNonPrimaryCurrencies = PrimaryBidAskHelper.getWaitingNonPrimaryCurrencies();
            
            //Create an event on the ring buffer for each waiting currency
            waitingNonPrimaryCurrencies.forEach((k,v)->{
                long sequence = ringBuffer.next();
                PriceEvent priceEvent = ringBuffer.get(sequence);
                priceEvent = v;
                System.out.println("PriceEventIntervalProducer published waiting currency at sequence: " + sequence);
            });
            try {
                Thread.sleep(5000);
            }
            catch (InterruptedException ex) {
                System.out.println("PriceEventIntervalProducer thread interrupted: " + ex);
            }
        }
    }
}