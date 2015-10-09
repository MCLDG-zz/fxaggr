package com.bl.fxaggr.connector;


public class PriceQuote {
    private String symbol;
    private String bid;
    private String ask;
    
    public void setSymbol (String symbol) {
        this.symbol = symbol;
    }
    public String getSymbol() {
        return this.symbol;
    }
    public void setBid (String bid) {
        this.bid = bid;
    }
    public String getBid() {
        return this.bid;
    }
    public void setAsk (String ask) {
        this.ask = ask;
    }
    public String getAsk() {
        return this.ask;
    }
}