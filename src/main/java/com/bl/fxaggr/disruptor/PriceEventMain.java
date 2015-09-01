package com.bl.fxaggr.disruptor;

import com.bl.fxaggr.generator.PriceEventGenerator;
import com.bl.fxaggr.disruptor.eventhandler.*;

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

        EventHandler < PriceEvent > eh3 = new PriceFilterEH();
        EventHandler < PriceEvent > eh5 = new StatsEH();
        EventHandler < PriceEvent > eh6 = new PrimaryBidAskEH();
        EventHandler < PriceEvent > eh20 = new PriceEventToMongoEH();
        EventHandler < PriceEvent > eh21 = new PriceToConsumerEH();

        /******************************************************************************************
         * BIG WARNING
         * 
         * when configuring events to execute concurrently, using <code>handleEventsWith(eh1,eh2)</code>
         * each event handler will receive a reference to the same event instance. Therefore you CANNOT
         * mutate / modify the event state. For instance, if you modify the state of the event in eh1,
         * then at some point while eh2 is processing the same event, the state that eh2 sees will change.
         *
         * So when handling an event concurrently make sure the event handlers DO NOT mutate the
         * event instance. All mutations should be done on events that are handled independently,
         * not currently. I.e. using <code>handleEventsWith(eh1)</code>
         * 
         * For example, it is OK to replicate an event and persist an event concurrently (using
         * <code>handleEventsWith(replicateEH, persistEH)</code>) since neither of these event 
         * handlers mutate the state. However, <code>handleEventsWith(updateEH, persistEH)</code>
         * would not work since updateEH is updating the state while persistEH is attempting to store
         * 
         *******************************************************************************************/
        
        // Connect the handler
        // TODO - I'm sure we can do this more efficiently. For instance, PriceEventToMongoEH
        // does not have to complete before PriceFilterEH starts. We sort of want a copy of 
        // the event to be persisted to Mongo asynchronously while we start on the filtering.
               disruptor.handleEventsWith(eh20).then(eh3).then(eh6).then(eh21).then(eh5,eh20);
        //disruptor.handleEventsWith(eh20).then(eh3);

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