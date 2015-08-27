package com.bl.fxaggr.disruptor;

import java.util.List;
import java.util.ArrayList;

/**
 * You would think that by now the Java working group would have fixed
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
    private EventStatus eventStatus = EventStatus.VALID;
    private EventState eventState = EventState.NEW_QUOTE;
    private List<FilterReason> filterReasons = new ArrayList<>();
    private List<String> eventAuditTrail = new ArrayList<>();
    
    public enum FilterReason {
		NEGATIVE_SPREAD,
		SPREAD_EXCEEDS_AVERAGE,
		ASK_SPIKE,
		BID_SPIKE,
		NOT_PRIMARY
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
    
}