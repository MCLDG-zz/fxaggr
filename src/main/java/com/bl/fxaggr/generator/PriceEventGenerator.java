package com.bl.fxaggr.generator;

import com.bl.fxaggr.disruptor.*;

import com.lmax.disruptor.RingBuffer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.io.BufferedReader;
import java.io.FileReader;

public class PriceEventGenerator implements Runnable {
    private final RingBuffer < PriceEvent > ringBuffer;

    public PriceEventGenerator(RingBuffer < PriceEvent > ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void run() {
        Thread t = Thread.currentThread();
        
        try {
            FileReader reader = new FileReader("/home/ubuntu/workspace/fxaggr/src/main/java/com/bl/fxaggr/generator/AUDCAD.csv");
            BufferedReader bufferedReader = new BufferedReader(reader);
 
            String line;
            int cnt = 0;

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
            Date dateStart = new Date();
            System.out.println("Data producer 1 opening input file at: " + dateFormat.format(dateStart));
 
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
                priceEvent.getPriceEntity().setSymbol("AUDCAD");
                ringBuffer.publish(sequence);
                // System.out.println("Data producer - published sequence: " + sequence + " bid: " 
                //     + priceEvent.getPriceEntity().getBid() + " ask: " + priceEvent.getPriceEntity().getAsk()
                //     + " spread: " + (priceEvent.getPriceEntity().getAsk() - priceEvent.getPriceEntity().getBid()));  
                
                if (cnt++ > 5) {
                    System.out.println("Data producer Stopping after : " + cnt);  
                    break;
                }
            }
            reader.close();
            Date dateEnd = new Date();
            System.out.println("Data producer 1 processed input file at: " + dateFormat.format(dateEnd));
        }
        catch (IOException ex) {
            System.out.println("IOException processing input file. Exception: " + ex);
            ex.printStackTrace();
        }
    }
}