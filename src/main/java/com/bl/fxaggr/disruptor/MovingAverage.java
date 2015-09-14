package com.bl.fxaggr.disruptor;

public class MovingAverage {
    private int numPeriods = 0;
    private double factor = 0.0;

    public MovingAverage(int numPeriods) {
        this.numPeriods = numPeriods;
        factor = 2.0 / (numPeriods + 1.0);
    }

    public double calculateEWMA(Double[] prices) {
        double runningEMA = prices[0];
        
        //Since we set the runningEMA to the first price in the 'prices' array,
        //we can start the calculation from the 2nd element in the array
        for (int i = 1; i < prices.length; i++) {
            runningEMA = factor * prices[i] + (1 - factor) * runningEMA;
		}
        return runningEMA;
    }
    /**
     * SMA is simply the average of all the prices
     */
    public double calculateSMA(Double[] prices) {
        double totalPrice = 0.0;
        
        for (int i = 0; i < prices.length; i++) {
            totalPrice += prices[i];
		}
        return totalPrice / prices.length;
    }
    /**
     * Makes the assumption that the most recent price is 
     * first in the array. This means the first price is assigned
     * the highest weighting
     * 
     * WMA is calculated as follows:
     * 
     * price1*6+price2*5+price4*4+price3*3+price2*2+price1*1)/21
     */
    public double calculateWMA(Double[] prices) {
        double totalPrice = 0.0;
        int noItems = prices.length;
        
        for (int i = 0; i < noItems; i++) {
            totalPrice += prices[i] * (noItems - i);
		}
		//Now get the sum of the count of prices using Gauss formula
		int gauss = noItems * (noItems + 1) / 2;
        return totalPrice / gauss;
    }
    /**
     * Makes the assumption that the most recent price is 
     * first in the array. This means the first price is assigned
     * the highest weighting
     * 
     * EMA is calculated as follows:
     * 
     * runningEMA + (2/(price periods + 1))*(price - runningEMA)
     */
    public double calculateEMA(Double[] prices) {
        double totalPrice = 0.0;
        double runningEMA = prices[0];
        int noItems = prices.length;
        
        for (int i = 0; i < noItems; i++) {
            runningEMA = runningEMA + (2 / (noItems + 1)) * (prices[i] * runningEMA);
		}
        return runningEMA;
    }
}
