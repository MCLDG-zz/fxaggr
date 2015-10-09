package com.bl.fxaggr.generator;

import com.bl.fxaggr.disruptor.*;

import com.lmax.disruptor.RingBuffer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.io.BufferedReader;
import java.io.FileReader;

public class KafkaPriceEventGenerator {
    private final RingBuffer <PriceEvent> ringBuffer;
    private int cnt = 0;

    public KafkaPriceEventGenerator(RingBuffer <PriceEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        Date dateStart = new Date();
        System.out.println("Data producer processing Kafka topic at: " + dateFormat.format(dateStart));
    }
    
    public void WriteToDisruptor(String msg) {
        cnt++;
        String[] tokens = msg.split(" ");
        
        //Track the time taken to get the next slot in the RingBuffer. If there
        //are no slots the producer will wait - and this is undesirable, since
        //it means processing of the quote will be delayed
 	    Instant start = Instant.now();
        long sequence = ringBuffer.next();
        PriceEvent priceEvent = ringBuffer.get(sequence);
        long ns = start.until(Instant.now(), ChronoUnit.MILLIS);
	    priceEvent.setPublishToRingBufferDelay(ns);
		if (ns > 100) {
 			System.out.println("PriceEventGenerator - delay getting next ringbuffer entry for sequence: " + sequence + ". MS delay: " + ns);
		}
        //Reset the state of the event. Since the events on the ringbuffer 
        //are reused, processing by event handlers will leave the event
        //in a specific state that must be reset before reusing the event
        priceEvent.resetEvent();
        PriceEntity priceEntity = new PriceEntity();
        priceEvent.setPriceEntity(priceEntity);

        //Publish the event
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn");
        //LocalDateTime dateTime = LocalDateTime.parse(tokens[0], formatter);
        priceEvent.getPriceEntity().setSequence(sequence);
        //I will set the date to the current instant, to mimic a real price feed
        //priceEvent.getPriceEntity().setQuoteTimestamp(dateTime);
        priceEvent.getPriceEntity().setProcessedTimestamp(Instant.now());
        priceEvent.getPriceEntity().setSymbol(tokens[0]);
        priceEvent.getPriceEntity().setBid(Double.valueOf(tokens[1]));
        priceEvent.getPriceEntity().setAsk(Double.valueOf(tokens[2]));
        priceEvent.getPriceEntity().setSpread(priceEvent.getPriceEntity().getAsk() - priceEvent.getPriceEntity().getBid());
        priceEvent.getPriceEntity().setLiquidityProvider("HKD-DB");
        priceEvent.recordNum = cnt; // for debugging purposes only
        System.out.println("Publish to Disruptor: " + msg + " sequence: " + sequence);
        ringBuffer.publish(sequence);
        // System.out.println("Data producer - published sequence: " + sequence + " bid: " 
        //     + priceEvent.getPriceEntity().getBid() + " ask: " + priceEvent.getPriceEntity().getAsk()
        //     + " spread: " + (priceEvent.getPriceEntity().getAsk() - priceEvent.getPriceEntity().getBid()));  
    }
}