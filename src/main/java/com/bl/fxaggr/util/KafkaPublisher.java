package com.bl.fxaggr.util;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import com.bl.fxaggr.connector.PriceQuote;

public class KafkaPublisher {
    private KafkaProducer<String, String> stringProducer = null;
    private KafkaProducer<String, Object> avroProducer = null;
    private String topic = "pricefeed";
    
    public KafkaPublisher() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());        
        stringProducer = new KafkaProducer<String, String>(props);

        props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                  io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                  io.confluent.kafka.serializers.KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        // Set any other properties
        avroProducer = new KafkaProducer<String, Object>(props);
    }
    
    public void publish(String msg) {
		stringProducer.send(new ProducerRecord<String, String>(topic, msg),
		    new Callback() {
                     public void onCompletion(RecordMetadata metadata, Exception e) {
                         if(e != null)
                             e.printStackTrace();
                         System.out.println("The offset of the record we just sent is: " + metadata.offset());
                     }
                });
        //producer.close();
    }
    public void publish(Object msg) {
        PriceQuote pq = (PriceQuote)msg;
        String priceSchema = "{\"type\":\"record\"," +
                    "\"name\":\"pricefeedschema\"," +
                    "\"fields\":[{\"name\":\"symbol\",\"type\":\"string\"}," +
                                "{\"name\":\"bid\",\"type\":\"string\"}," + 
                                "{\"name\":\"ask\",\"type\":\"string\"}]}";
Schema.Parser parser = new Schema.Parser();
Schema schema = parser.parse(priceSchema);
GenericRecord avroRecord = new GenericData.Record(schema);
avroRecord.put("symbol", pq.getSymbol());
avroRecord.put("bid", pq.getBid());
avroRecord.put("ask", pq.getAsk());

//record = new ProducerRecord<Object, Object>("topic1", key, avroRecord);

		avroProducer.send(new ProducerRecord<String, Object>(topic, avroRecord),
		    new Callback() {
                     public void onCompletion(RecordMetadata metadata, Exception e) {
                         if(e != null)
                             e.printStackTrace();
                         System.out.println("The offset of the record we just sent is: " + metadata.offset());
                     }
                });
        //producer.close();
    }
}