package com.bl.fxaggr.disruptor;

import com.bl.fxaggr.generator.PriceEventGenerator;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PriceEventMain extends Thread {
    public static long producerCount = 99998;
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

        EventHandler < PriceEvent > eh1 = new JournalToFileEventHandler();
        EventHandler < PriceEvent > eh2 = new JournalToMongoEventHandler();
        EventHandler < PriceEvent > eh3 = new PriceFilterEH();
        EventHandler < PriceEvent > eh4 = new PriceParserEventHandler();

        // Connect the handler
        disruptor.handleEventsWith(eh3).then(eh4).then(eh2);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        ringBuffer = disruptor.getRingBuffer();

        // Start the data generator
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Date dateStart = new Date();
        System.out.println("Data producer started at: " + dateFormat.format(dateStart));
        
        PriceEventGenerator priceEventGenerator = new PriceEventGenerator(ringBuffer);
        priceEventGenerator.run();
        
        Date dateEnd = new Date();
        System.out.println("Data producer completed at: " + dateFormat.format(dateEnd));
    }
}