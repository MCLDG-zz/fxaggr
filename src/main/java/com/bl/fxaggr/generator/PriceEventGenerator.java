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
import java.io.BufferedReader;
import java.io.FileReader;

public class PriceEventGenerator implements Runnable {
    private final RingBuffer <PriceEvent> ringBuffer;
    private String testfileDirectory = null;

    public PriceEventGenerator(RingBuffer <PriceEvent> ringBuffer, String testfileDirectory) {
        this.ringBuffer = ringBuffer;
        this.testfileDirectory = testfileDirectory;
    }

    @Override
    public void run() {
        Thread t = Thread.currentThread();
        
        File dir = new File(testfileDirectory);
        File[] csvFiles = dir.listFiles(new FilenameFilter() { 
    	         public boolean accept(File dir, String filename)
    	              { return filename.endsWith(".csv"); }
    	} );
        try {
            for (File csvFile : csvFiles) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
                Date dateStart = new Date();
                System.out.println("Data producer 1 opening input file " + csvFile.toString() + " at: " + dateFormat.format(dateStart));

                FileReader reader = new FileReader(csvFile);
                BufferedReader bufferedReader = new BufferedReader(reader);
     
                String line;
                int cnt = 0;

                while ((line = bufferedReader.readLine()) != null) {
                    cnt++;
                    String[] tokens = line.split(",");
                    long sequence = ringBuffer.next();
                    PriceEvent priceEvent = ringBuffer.get(sequence);
                    //Reset the state of the event. Since the events on the ringbuffer 
                    //are reused, processing by event handlers will leave the event
                    //in a specific state that must be reset before reusing the event
                    priceEvent.resetEvent();
                    PriceEntity priceEntity = new PriceEntity();
                    priceEvent.setPriceEntity(priceEntity);
    
                    //Publish the event
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn");
                    LocalDateTime dateTime = LocalDateTime.parse(tokens[0], formatter);
                    priceEvent.getPriceEntity().setSequence(sequence);
                    priceEvent.getPriceEntity().setDatetime(dateTime);
                    priceEvent.getPriceEntity().setBid(Double.valueOf(tokens[1]));
                    priceEvent.getPriceEntity().setAsk(Double.valueOf(tokens[2]));
                    priceEvent.getPriceEntity().setSpread(priceEvent.getPriceEntity().getAsk() - priceEvent.getPriceEntity().getBid());
                    priceEvent.getPriceEntity().setLiquidityProvider(tokens[3]);
                    priceEvent.getPriceEntity().setSymbol(tokens[4]);
                    priceEvent.recordNum = cnt; // for debugging purposes only
                    ringBuffer.publish(sequence);
                    // System.out.println("Data producer - published sequence: " + sequence + " bid: " 
                    //     + priceEvent.getPriceEntity().getBid() + " ask: " + priceEvent.getPriceEntity().getAsk()
                    //     + " spread: " + (priceEvent.getPriceEntity().getAsk() - priceEvent.getPriceEntity().getBid()));  
                    
                    // if (cnt++ > 1000) {
                    //     System.out.println("Data producer Stopping after : " + cnt);  
                    //     break;
                    // }
                }
                reader.close();
                Date dateEnd = new Date();
                System.out.println("Data producer processed " + cnt + " records from input file at: " + dateFormat.format(dateEnd));
            }
        }
        catch (IOException ex) {
            System.out.println("IOException processing input file. Exception: " + ex);
            ex.printStackTrace();
        }
    }
}