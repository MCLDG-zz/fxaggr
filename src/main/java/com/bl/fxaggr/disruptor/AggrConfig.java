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
    public  primarybidask primarybidask;
    public  bestbidask bestbidask;
    public  filteringrules filteringrules;
}
public static class primarybidask {
    public  String[] primarysecondarytertiaryproviders;
    public  long timeintervalbeforeswitchingms;
}
public static class bestbidask {
    public  String[] primarysecondarytertiaryproviders;
    public  long timeintervalbeforeswitchingms;
    public  long timeintervalformatchingquotesms;
    public  long minimummatchingquotesrequired;
}
public static class filteringrules {
    public  long numberconsecutivespikesfiltered;
}
public static class AggrConfigCurrency {
    public  String symbol;
    public  double pctLeewayAllowedSpread;
    public  double pctLeewayToPreviousBid;
    public  double pctLeewayToPreviousAsk;
    public  double pctAboveMaxSpread;
    public  double pctBelowMinSpread;
}
}
