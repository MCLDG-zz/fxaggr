package com.bl.fxaggr.disruptor;

import java.time.Instant;
import java.time.LocalDateTime;

public class PriceQuoteEntity {

    private String symbol;
    private LocalDateTime quoteDate;
    private int minuteOfDay;
    private double open;
    private double close;
    private double bidHi;
    private double bidLo;
    private double askHi;
    private double askLo;
    private long numberOfTicks;

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    public void setQuoteDate(LocalDateTime quoteDate) {
        this.quoteDate = quoteDate;
    }
    public void setMinuteOfDay(int minuteOfDay) {
        this.minuteOfDay = minuteOfDay;
    }
    public void setOpen(double open) {
        this.open = open;
    }
    public void setClose(double close) {
        this.close = close;
    }
    public void setBidHi(double bidHi) {
        this.bidHi = bidHi;
    }
    public void setBidLo(double bidLo) {
        this.bidLo = bidLo;
    }
    public void setAskHi(double askHi) {
        this.askHi = askHi;
    }
    public void setAskLo(double askLo) {
        this.askLo = askLo;
    }
    public void setNumberOfTicks(long numberOfTicks) {
        this.numberOfTicks = numberOfTicks;
    }
    public String getSymbol() {
        return this.symbol;
    }
    public LocalDateTime getQuoteDate() {
        return this.quoteDate;
    }
    public int getMinuteOfDay() {
        return this.minuteOfDay;
    }
    public double getOpen() {
        return this.open;
    }
    public double getClose() {
        return this.close;
    }
    public double getBidHi() {
        return this.bidHi;
    }
    public double getBidLo() {
        return this.bidLo;
    }
    public double getAskHi() {
        return this.askHi;
    }
    public double getAskLo() {
        return this.askLo;
    }
    public long getNumberOfTicks() {
        return this.numberOfTicks;
    }
}
