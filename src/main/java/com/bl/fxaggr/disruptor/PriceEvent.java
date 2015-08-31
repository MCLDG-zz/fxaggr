package com.bl.fxaggr.disruptor;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
    private EventLPSwitch eventLPSwitch = null;  //holds type of LP switch if a switch hapened while processing this event
    private String primaryLPWhenHandlingThisEvent = null;  //holds the primary LP when this event was handled
    private List<FilterReason> filterReasons = new ArrayList<>();
    private List<String> eventAuditTrail = new ArrayList<>();
    private Map<String, PriceEntity> bestBidAsk = null;
    
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
    
    
}