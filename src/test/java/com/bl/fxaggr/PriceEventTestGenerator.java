package com.bl.fxaggr;

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
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.InterruptedException;
import java.lang.NumberFormatException;

public class PriceEventTestGenerator implements Runnable {
    private final RingBuffer < PriceEvent > ringBuffer;
    private File testfile = null;

    public PriceEventTestGenerator(RingBuffer < PriceEvent > ringBuffer, String filename) {
        this.ringBuffer = ringBuffer;
        testfile = new File(filename);
    }

    @Override
    public void run() {
        Thread t = Thread.currentThread();
        
        try {
                FileReader reader = new FileReader(testfile);
                BufferedReader bufferedReader = new BufferedReader(reader);
     
                String line;
                int cnt = 0;

                while ((line = bufferedReader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    //To simulate a delay between quotes, I allow the test file to contain
                    //a value such as 'wait 31000'. When I read this I will wait the 
                    //required number of MS before continuing. This is needed to test
                    //cases where the primary liquidity provider does not send quotes
                    //and the engine must switch to the secondary provider
                    if (tokens[0].startsWith("wait")) {
                        try {
                            Long waitTime = Long.parseLong(tokens[0].substring(5));
                            System.out.println("Test data generator sleeping for " + waitTime + " MS");
                            Thread.sleep(waitTime);
                        } 
                        catch (NumberFormatException nfe) {
                            //print and otherwise ignore the exception
                            System.out.println("NumberFormatException: " + nfe.getMessage());
                        }
                        catch (InterruptedException ex) {
                            //print and otherwise ignore the exception
                            System.out.println("InterruptedException: " + ex.getMessage());
                        }
                        continue;
                    }
                    long sequence = ringBuffer.next();
                    PriceEvent priceEvent = ringBuffer.get(sequence);
                    PriceEntity priceEntity = new PriceEntity();
                    priceEvent.setPriceEntity(priceEntity);
    
                    //Publish the event
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmmssSSS");
                    LocalDateTime dateTime = LocalDateTime.parse(tokens[0], formatter);
                    priceEvent.getPriceEntity().setSequence(sequence);
                    priceEvent.getPriceEntity().setQuoteTimestamp(dateTime);
                    priceEvent.getPriceEntity().setProcessedTimestamp(Instant.now());
                    priceEvent.getPriceEntity().setBid(Double.valueOf(tokens[1]));
                    priceEvent.getPriceEntity().setAsk(Double.valueOf(tokens[2]));
                    priceEvent.getPriceEntity().setSpread(priceEvent.getPriceEntity().getAsk() - priceEvent.getPriceEntity().getBid());
                    priceEvent.getPriceEntity().setLiquidityProvider(tokens[3]);
                    priceEvent.getPriceEntity().setSymbol(tokens[4]);
                    ringBuffer.publish(sequence);
                }
                reader.close();
        }
        catch (IOException ex) {
            System.out.println("IOException processing input file. Exception: " + ex);
            ex.printStackTrace();
        }
    }
}