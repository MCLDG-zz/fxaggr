package com.bl.fxaggr.disruptor;
/**
 * This class and the inner classes below represent the Aggregation Config, which is stored in
 * the 'aggrconfig' collection in Mongo. These classes are used by GSON
 * to convert JSON to Java entity classes
 */
public class AggrConfig {
    public  globalconfig globalconfig;
    public  AggrConfigCurrency[] currencyconfig;

public static class globalconfig {
    public  String[] availableschemes;
    public  String scheme;
    public  String[] liquidityproviders;
    public  String pricefeedtimezone;
    public  String systemtimezone;
    public  long allowabletimeintervalfeedtosystemms;
    public  primarybidask primarybidask;
    public  bestbidask bestbidask;
    public  filteringrules filteringrules;
    public  smoothing smoothing;
}
public static class primarybidask {
    public  long timeintervalbeforeswitchingms;
    public  long numberquotesbeforeswitchtoprevious;
    public  String actionwhennomoreliquidityproviders;
}
public static class bestbidask {
    public  long timeintervalformatchingquotesms;
    public  long minimummatchingquotesrequired;
    public  String actionwhenbestnotpossible;
}
public static class smoothing {
    public  int ewmaperiods;
    public  String smoothprimaryorall;
}
public static class filteringrules {
    public  long numberconsecutivespikesfiltered;
}
public static class AggrConfigCurrency {
    public  String symbol;
    public  double pctLeewayAllowedSpread;
    public  double pctLeewayToPreviousBid;
    public  double pctLeewayToPreviousAsk;
}
}
