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
import java.io.BufferedReader;
import java.io.FileReader;

public class PriceEventTestGenerator implements Runnable {
    private final RingBuffer < PriceEvent > ringBuffer;

    public PriceEventTestGenerator(RingBuffer < PriceEvent > ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void run() {
        Thread t = Thread.currentThread();
        
        File testfile = new File("/home/ubuntu/workspace/fxaggr/src/test/java/com/bl/fxaggr/TestFilter.csv");
        try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
                Date dateStart = new Date();
                System.out.println("Test data generator opening input file " + testfile.toString() + " at: " + dateFormat.format(dateStart));

                FileReader reader = new FileReader(testfile);
                BufferedReader bufferedReader = new BufferedReader(reader);
     
                String line;
                int cnt = 0;

                while ((line = bufferedReader.readLine()) != null) {
                    String[] tokens = line.split(",");
                    long sequence = ringBuffer.next();
                    PriceEvent priceEvent = ringBuffer.get(sequence);
                    PriceEntity priceEntity = new PriceEntity();
                    priceEvent.setPriceEntity(priceEntity);
    
                    //Publish the event
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmmssSSS");
                    LocalDateTime dateTime = LocalDateTime.parse(tokens[0], formatter);
                    priceEvent.getPriceEntity().setSequence(sequence);
                    priceEvent.getPriceEntity().setDatetime(dateTime);
                    priceEvent.getPriceEntity().setBid(Double.valueOf(tokens[1]));
                    priceEvent.getPriceEntity().setAsk(Double.valueOf(tokens[2]));
                    priceEvent.getPriceEntity().setSpread(priceEvent.getPriceEntity().getAsk() - priceEvent.getPriceEntity().getBid());
                    priceEvent.getPriceEntity().setSymbol(tokens[4]);
                    ringBuffer.publish(sequence);
                }
                reader.close();
                Date dateEnd = new Date();
                System.out.println("Data generator processed input file at: " + dateFormat.format(dateEnd));
        }
        catch (IOException ex) {
            System.out.println("IOException processing input file. Exception: " + ex);
            ex.printStackTrace();
        }
    }
}