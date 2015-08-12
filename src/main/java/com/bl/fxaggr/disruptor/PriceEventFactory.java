package com.bl.fxaggr.disruptor;

import com.lmax.disruptor.EventFactory;

public class PriceEventFactory implements EventFactory<PriceEvent>
{
    public PriceEvent newInstance()
    {
        return new PriceEvent();
    }
}