package com.bl.fxaggr.util;

import org.apache.avro.generic.GenericRecord;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.message.MessageAndMetadata;

public class ConsumerLogic implements Runnable {
  private KafkaStream stream;
  private int threadNumber;

  public ConsumerLogic(KafkaStream stream, int threadNumber) {
    this.threadNumber = threadNumber;
    this.stream = stream;
  }

  public void run() {
    ConsumerIterator<Object, Object> it = stream.iterator();

    while (it.hasNext()) {
      MessageAndMetadata<Object, Object> record = it.next();

      String topic = record.topic();
      int partition = record.partition();
      long offset = record.offset();
      Object key = record.key();
      GenericRecord message = (GenericRecord) record.message();
      System.out.println("Thread " + threadNumber +
                         " received: " + "Topic " + topic +
                         " Partition " + partition +
                         " Offset " + offset +
                         " Key " + key +
                         " Message " + message.toString());
    }
    System.out.println("Shutting down Thread: " + threadNumber);
  }
}