package com.bl.fxaggr.disruptor;

public class MovingAverage {
    private int numPeriods = 0;
    private double factor = 0.0;

    public MovingAverage(int numPeriods) {
        this.numPeriods = numPeriods;
        factor = 2.0 / (numPeriods + 1.0);
    }

    public double calculateEWMA(Double[] prices) {
        double lambda = 0.94; //update this to get it from a config setting
        double periodicReturn = 0.0;
        double weighting = 0.0;
        double weightedReturn = 0.0;
        double runningEWMA = 0.0;
        double simpleVariance = 0.0;
        double ewma = 0.0;

        for (int i = 0; i < prices.length; i++) {
            if (i + 1 < prices.length) {
                //calculate the periodic return as the natural log of the ratio of 
                //price (i.e., currebt price divided by previous price)
                periodicReturn = Math.log(prices[i] / prices [i + 1]);
                //simple variance is the sum of the periodic returns, so we keep a 
                //running total
                simpleVariance += Math.pow(periodicReturn, 2);
                //calculate the weighting. The first (most recent) squared periodic 
                //return is weighted by (1-0.94)(.94)0 = 6%. The next squared return 
                //is simply a lambda-multiple of the prior weight; in this case 6% 
                //multiplied by 94% = 5.64%. And the third prior day's weight equals 
                //(1-0.94)(0.94)2 = 5.30%.
                weighting = (1 - lambda) * Math.pow(lambda, i);
                //apply the weighting to the square of the periodic return
                weightedReturn = simpleVariance * weighting;
                //sum the weighted returns to arrive at the EWMA
                runningEWMA += weightedReturn; 
        		System.out.println("MovingAverage - periodicReturn: " + periodicReturn + " weighting: " + weighting + " weightedReturn: " + weightedReturn);
        		System.out.println("MovingAverage - runningEWMA: " + runningEWMA + " SQRT of runningEWMA: " + Math.sqrt(runningEWMA));
            }
		}
		//Calculate the simple volatility. This is the SQRT of the simple variance -
		//i.e. the standard deviation
		double simpleVolatility = Math.sqrt(simpleVariance);
		//Apply the EWMA to the price. It should be applied to the price prior
		//to the current price, since we are using the EWMA to smooth the current
		//price
		if (prices[0] >= prices[1]) {
		    ewma = prices[1] * (1 + runningEWMA);
		}
		else {
		    ewma = prices[1] * -(1 + runningEWMA);
		}
        return ewma;
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
