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
    private boolean filteredEvent = false;
    private boolean ignoreEvent = false;
    private boolean waitForPrimary = false;
    private List<FilterReason> filterReasons = new ArrayList<>();
    private List<String> eventAuditTrail = new ArrayList<>();
    
    public enum FilterReason {
		NEGATIVE_SPREAD,
		SPREAD_EXCEEDS_AVERAGE,
		ASK_SPIKE,
		BID_SPIKE
	}
    
    public void setPriceEntity(PriceEntity priceEntity) {
        this.priceEntity = priceEntity;
    }
    public PriceEntity getPriceEntity() {
        return priceEntity;
    }

    public void setFilteredEvent(boolean filteredEvent) {
        this.filteredEvent = filteredEvent;
    }
    public boolean isFilteredEvent() {
        return filteredEvent;
    }
    
    public void setIgnoreEvent(boolean ignoreEvent) {
        this.ignoreEvent = ignoreEvent;
    }
    public boolean isIgnoreEvent() {
        return ignoreEvent;
    }
    
    public void setWaitForPrimary(boolean waitForPrimary) {
        this.waitForPrimary = waitForPrimary;
    }
    public boolean isWaitForPrimary() {
        return waitForPrimary;
    }
    
    public boolean setFilteredReason(FilterReason filteredReason) {
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