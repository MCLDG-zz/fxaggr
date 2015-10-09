package com.bl.fxaggr.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.confluent.kafka.serializers.KafkaAvroDecoder;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import kafka.utils.VerifiableProperties;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

public class KafkaSubscriber {
  private final ConsumerConnector consumer;
  private final String topic;
  private ExecutorService executor;
  private String zookeeper;
  private String groupId;
  private String url;

  public KafkaSubscriber(String zookeeper, String groupId, String topic, String url) {
    consumer = kafka.consumer.Consumer.createJavaConsumerConnector(
      new ConsumerConfig(createConsumerConfig(zookeeper, groupId, url)));
    this.topic = topic;
    this.zookeeper = zookeeper;
    this.groupId = groupId;
    this.url = url;
  }

  private Properties createConsumerConfig(String zookeeper, String groupId, String url) {
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("zookeeper.connect", zookeeper);
    props.put("group.id", groupId);
    props.put("partition.assignment.strategy", "roundrobin");
    props.put("auto.commit.enable", "false");
    props.put("auto.offset.reset", "smallest");
    props.put("schema.registry.url", url);
    return props;
  }

  public void run(int numThreads) {
    Map < String, Integer > topicCountMap = new HashMap < String, Integer > ();
    topicCountMap.put(topic, numThreads);

    Properties props = createConsumerConfig(zookeeper, groupId, url);
    VerifiableProperties vProps = new VerifiableProperties(props);

    // Create decoders for key and value
    KafkaAvroDecoder avroDecoder = new KafkaAvroDecoder(vProps);

    Map < String, List < KafkaStream < Object, Object >>> consumerMap =
      consumer.createMessageStreams(topicCountMap, avroDecoder, avroDecoder);
    List < KafkaStream < Object, Object >> streams = consumerMap.get(topic);

    // Launch all the threads
    executor = Executors.newFixedThreadPool(numThreads);

    // Create ConsumerLogic objects and bind them to threads
    int threadNumber = 0;
    for (final KafkaStream stream: streams) {
      executor.submit(new ConsumerLogic(stream, threadNumber));
      threadNumber++;
    }
  }

  public void shutdown() {
    if (consumer != null) consumer.shutdown();
    if (executor != null) executor.shutdown();
    try {
      if (!executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
        System.out.println(
          "Timed out waiting for consumer threads to shut down, exiting uncleanly");
      }
    }
    catch (InterruptedException e) {
      System.out.println("Interrupted during shutdown, exiting uncleanly");
    }
  }

  public static void main(String[] args) {
    if (args.length != 5) {
      System.out.println("Please provide command line arguments: " + "zookeeper groupId topic threads schemaRegistryUrl");
      System.exit(-1);
    }

    String zooKeeper = args[0];
    String groupId = args[1];
    String topic = args[2];
    int threads = Integer.parseInt(args[3]);
    String url = args[4];

    KafkaSubscriber example = new KafkaSubscriber(zooKeeper, groupId, topic, url);
    example.run(threads);

    // try {
    //   Thread.sleep(10000);
    // } catch (InterruptedException ie) {

    // }
    example.shutdown();


    Properties props = new Properties();
    props.put("metadata.broker.list", "localhost:9092");
    props.put("zookeeper.connect", "localhost:2181");
    props.put("bootstrap.servers", "localhost:9092");
    props.put("group.id", "test");
    props.put("partition.assignment.strategy", "roundrobin");
    props.put("session.timeout.ms", "1000");
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", "10000");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    KafkaConsumer consumer = new KafkaConsumer(props);
    consumer.subscribe("pricefeed");
    boolean isRunning = true;
    while (isRunning) {
      Map <String, ConsumerRecords> records = consumer.poll(100);
      if (records == null) {
          System.out.println("No records found: ");
      }
      else {
        process(records);
      }
    }
    consumer.close();

  }

  private static Map <TopicPartition, Long> process(Map < String, ConsumerRecords > records) {
    Map < TopicPartition, Long > processedOffsets = new HashMap < TopicPartition, Long > ();
    for (java.util.Map.Entry<String, ConsumerRecords> recordMetadata: records.entrySet()) {
      List < ConsumerRecord > recordsPerTopic = recordMetadata.getValue().records();
      for (int i = 0; i < recordsPerTopic.size(); i++) {
        ConsumerRecord record = recordsPerTopic.get(i);
        // process record
        try {
          processedOffsets.put(record.topicAndPartition(), record.offset());
          System.out.println("Received msg: " + record + " " + record.offset());

        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return processedOffsets;
  }
}