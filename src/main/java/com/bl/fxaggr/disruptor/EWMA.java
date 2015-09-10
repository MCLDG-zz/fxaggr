package com.bl.fxaggr.disruptor;

public class EWMA {
    private int numPeriods = 0;
    private double factor = 0.0;

    public EWMA(int numPeriods) {
        this.numPeriods = numPeriods;
        factor = 2.0 / (numPeriods + 1.0);
    }

    public double calculate(Double[] prices) {
        double runningEMA = 0.0;
        for (int i = 0; i < prices.length; i++) {
            runningEMA = factor * prices[i] + (1 - factor) * runningEMA;
		}
        return runningEMA;
    }
}
