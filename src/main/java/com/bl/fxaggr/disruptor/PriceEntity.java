package com.bl.fxaggr.disruptor;

import java.time.LocalDateTime;
import java.time.Instant;

public class PriceEntity {

	private String dealJSON = null;
	private long sequence;
    private String symbol;
    private String liquidityProvider;
    private double open;
    private double close;
    private double bid;
    private double ask;
    private double spread;
    private LocalDateTime datetime;
    private Instant processedTimestamp;

/*    public PriceEntity(long sequence, String symbol, double open, double close, double bid, double ask, LocalDateTime datetime) {
    	this.sequence = sequence;
        this.symbol = symbol;
        this.open = open;
        this.close = close;
        this.bid = bid;
        this.ask = ask;
        this.datetime = datetime;
    }
*/	public String getDealJSON() {
		return dealJSON;
	}
	public void setDealJSON(String dealJSON) {
		this.dealJSON = dealJSON;
	}
    public void setSequence(long sequence) {
        this.sequence = sequence;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    public void setLiquidityProvider(String liquidityProvider) {
        this.liquidityProvider = liquidityProvider;
    }
    public void setOpen(double open) {
        this.open = open;
    }
    public void setClose(double close) {
        this.close = close;
    }
    public void setBid(double bid) {
        this.bid = bid;
    }
    public void setAsk(double ask) {
        this.ask = ask;
    }
    public void setSpread(double spread) {
        this.spread = spread;
    }
    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }
    public void setProcessedTimestamp(Instant processedTimestamp) {
        this.processedTimestamp = processedTimestamp;
    }
    public long getSequence() {
        return this.sequence;
    }
    public String getSymbol() {
        return this.symbol;
    }
    public String getLiquidityProvider() {
        return this.liquidityProvider;
    }
    public double getOpen() {
        return this.open;
    }
    public double getClose() {
        return this.close;
    }
    public double getBid() {
        return this.bid;
    }
    public double getAsk() {
        return this.ask;
    }
    public double getSpread() {
        return this.spread;
    }
    public LocalDateTime getDatetime() {
        return this.datetime;
    }
    public Instant getProcessedTimestamp() {
        return this.processedTimestamp;
    }
}
