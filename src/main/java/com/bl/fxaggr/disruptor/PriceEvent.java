package com.bl.fxaggr.disruptor;

/**
 * You would think that by now the useless Java group would have fixed
 * the need to use getters and setters and provided a simple, declarative
 * way of declaring them; they are a complete waste of time
 *
 * WARNING: Event objects used the the Disruptor are mutable objects and 
 * will be recycled by the RingBuffer. You must take a copy of data it holds
 * before the framework recycles it.
 * 
 */
public class PriceEvent {
    private PriceEntity priceEntity;
    
    public void setPriceEntity(PriceEntity priceEntity) {
        this.priceEntity = priceEntity;
    }
    public PriceEntity getPriceEntity() {
        return priceEntity;
    }
}