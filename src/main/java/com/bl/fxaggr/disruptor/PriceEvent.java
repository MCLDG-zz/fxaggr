package com.bl.fxaggr.disruptor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * You would think that by now the Java working group would have fixed
 * the need to use getters and setters and provided a simple, declarative
 * way of declaring them; they are a complete waste of time
 *
 * WARNING: Event objects used the the Disruptor are mutable objects and 
 * will be recycled by the RingBuffer. You must take a copy of data it holds
 * before the framework recycles it.
 * 
 * TODO At some point we'll need test cases to test whether the engine still
 * continues to operate correctly once it starts reusing PriceEvents
 * 
 */
public class PriceEvent {
    private PriceEntity priceEntity;
    private EventStatus eventStatus = EventStatus.VALID;
    private EventState eventState = EventState.NEW_QUOTE;
    private AppliedSelectionScheme appliedSelectionScheme = AppliedSelectionScheme.NONE;
    private String configuredSelectionScheme = null;
    private EventLPSwitch eventLPSwitch = null;  //holds type of LP switch if a switch hapened while processing this event
    private String primaryLPWhenHandlingThisEvent = null;  //holds the primary LP when this event was handled
    private List<FilterReason> filterReasons = new ArrayList<>();
    private List<String> eventAuditTrail = new ArrayList<>();
    private Map<String, PriceEntity> bestBidAsk = null;
    public long recordNum = 0;
    
    //Store useful timing information
    private LocalDateTime quoteInstant;   //timestamp of quote - this is already stored in PRiceEntity so don't need this
    private Instant filterInstant;  //time event was processed by filter event handler
    private Instant bidAskInstant;  //time event was processed by bid/ask event handler
    private Instant sentToConsumerInstant; //time event was send to consumer
    private Instant persistInstant; //time event was persisted
    private long publishToRingBufferDelay;
    
    /**
     * Events are reused by the disruptor. A reused event will still hold the state
     * set during processing by the previous event handlers. This must be reset
     * when the event is reused on the ring buffer
     */
    public void resetEvent() {
        this.eventStatus = EventStatus.VALID;
        this.eventState = EventState.NEW_QUOTE;
        this.appliedSelectionScheme = AppliedSelectionScheme.NONE;
        this.configuredSelectionScheme = null;
        this.eventLPSwitch = null;  
        this.primaryLPWhenHandlingThisEvent = null;  
        this.filterReasons = new ArrayList<>();
        this.eventAuditTrail = new ArrayList<>();
        this.bestBidAsk = null;
        this.recordNum = 0;
    }
    public enum FilterReason {
		NEGATIVE_SPREAD,
		SPREAD_EXCEEDS_AVERAGE,
		ASK_SPIKE,
		BID_SPIKE,
		NOT_PRIMARY,
		NO_BEST_BIDASK_NOT_PRIMARY
	}
    
    public enum EventStatus {
		FILTERED,
		VALID
	}
    
    public enum EventState {
		NEW_QUOTE,
		FILTER_COMPLETED,
		COMPARISON_COMPLETED,
		FINAL_QUOTE
	}
    
    public enum EventLPSwitch {
		SWITCH_NEXT_PRIMARY,
		SWITCH_BACK_PREVIOUS_PRIMARY,
		UNABLE_TO_SWITCH
	}
    
    public enum AppliedSelectionScheme {
        NONE,
		PRIMARY_BID_ASK,
		BEST_BID_ASK
	}
    
    public void setPriceEntity(PriceEntity priceEntity) {
        this.priceEntity = priceEntity;
    }
    public PriceEntity getPriceEntity() {
        return priceEntity;
    }

    public void setEventStatus(EventStatus eventStatus) {
        this.eventStatus = eventStatus;
    }
    public EventStatus getEventStatus() {
        return eventStatus;
    }
    
    public void setEventState(EventState eventState) {
        this.eventState = eventState;
    }
    public EventState getEventState() {
        return eventState;
    }
    
    public void setEventLPSwitch(EventLPSwitch eventLPSwitch) {
        this.eventLPSwitch = eventLPSwitch;
    }
    public EventLPSwitch getEventLPSwitch() {
        return eventLPSwitch;
    }
    
    public void setAppliedSelectionScheme(AppliedSelectionScheme appliedSelectionScheme) {
        this.appliedSelectionScheme = appliedSelectionScheme;
    }
    public AppliedSelectionScheme getAppliedSelectionScheme() {
        return appliedSelectionScheme;
    }
    
    public void setConfiguredSelectionScheme(String configuredSelectionScheme) {
        this.configuredSelectionScheme = configuredSelectionScheme;
    }
    public String getConfiguredSelectionScheme() {
        return configuredSelectionScheme;
    }
    
    public boolean addFilteredReason(FilterReason filteredReason) {
        return this.filterReasons.add(filteredReason);
    }
    public List<FilterReason> getFilteredReasons() {
        return filterReasons;
    }

    public boolean addAuditEvent(String auditEvent) {
        return this.eventAuditTrail.add(auditEvent);
    }
    public List<String> getEventAuditTrail() {
        return eventAuditTrail;
    }
    
    public void setBestBidAsk(Map<String, PriceEntity> bestBidAsk) {
        this.bestBidAsk = bestBidAsk;
    }
    public Map<String, PriceEntity> getBestBidAsk() {
        return bestBidAsk;
    }
    public void setQuoteInstant(LocalDateTime timestamp) {
        this.quoteInstant = timestamp;
    }
    public void setFilterInstant() {
        this.filterInstant = Instant.now();
    }
    public void setBidAskInstant() {
        this.bidAskInstant = Instant.now();
    }
    public void setPersistInstant() {
        this.persistInstant = Instant.now();
    }
    public void setSentToConsumerInstant() {
        this.sentToConsumerInstant = Instant.now();
    }
    public void setPublishToRingBufferDelay(long publishToRingBufferDelay) {
        this.publishToRingBufferDelay = publishToRingBufferDelay;
    }
    public LocalDateTime getQuoteInstant() {
        return this.quoteInstant;
    }
    public Instant getFilterInstant() {
        return this.filterInstant;
    }
    public Instant getBidAskInstant() {
        return this.bidAskInstant;
    }
    public Instant getPersistInstant() {
        return this.persistInstant;
    }
    public Instant getSentToConsumerInstant() {
        return this.sentToConsumerInstant;
    }
    public long getPublishToRingBufferDelay() {
        return this.publishToRingBufferDelay;
    }
}