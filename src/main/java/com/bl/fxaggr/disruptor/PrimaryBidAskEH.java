package com.bl.fxaggr.disruptor;

import java.util.Map;

import com.lmax.disruptor.EventHandler;

/**
 * Handles events when the pricing strategy is 'Primary Bid/Ask'. In this strategy
 * Primary bid/ask events are sent through to consumers immediately. Price quotes
 * that originate from non-primary liquidity providers are subject to a 'wait time',
 * i.e. an interval where we wait to see if a matching quote from a primary liquidity
 * provider arrives. If it does, we accept the quote from the primary liquidity provider.
 * If it doesn't, we send the non-primary quote to the consumers.
 * <p>
 * The 'wait time' is not implemented in this class, otherwise the entire disruptor would
 * end up waiting and this would impact the latency of all subsequent quotes. Instead, the
 * non-primary price quote is simply updated with a status to indicate that it must wait
 * during a later process to check if a matching primary arrives.
 * 
 * @see PrimaryBidAskHelper
 */
public class PrimaryBidAskEH implements EventHandler<PriceEvent> {
	
	private Map<String, AggrConfig.AggrConfigCurrency> aggrcurrencyconfigMap = null;
	private PriceEntity priceEntity;

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		event.setEventState(PriceEvent.EventState.COMPARISON_COMPLETED);
		if (PriceEventHelper.aggrConfig == null) {
			System.out.println("PrimaryBidAskEH cannot analyse pricing. No config in table aggrconfig. Sequence: " + sequence); 
			event.addAuditEvent("PrimaryBidAskEH. Cannot analyse pricing. No config in table aggrconfig. Sequence: " + sequence); 
			return;
		}
		//Ignore non-valid events
		if (event.getEventStatus() != PriceEvent.EventStatus.VALID) {
			return;
		}

		priceEntity = event.getPriceEntity();

		switch(PriceEventHelper.getPriceSelectionScheme()) {
			case "Primary Bid/Ask": 
				if (!this.handlePrimaryBidAskScheme(event)) {
					event.setEventStatus(PriceEvent.EventStatus.FILTERED);
					event.addFilteredReason(PriceEvent.FilterReason.NOT_PRIMARY);
				}
				break;
			case "Primary Bid/Best Ask": 
				break;
			case "Best Bid/Primary Ask": 
				break;
			case "Best Bid/Ask": 
				break;
		}

		//Log the stats
		System.out.println("Sequence: " + sequence + ". PrimaryBidAskEH event status: " + event.getEventStatus()); 
		event.addAuditEvent("PrimaryBidAskEH. Sequence: " + sequence + ". PrimaryBidAskEH event status: " + event.getEventStatus()); 
	}
	
	/**
	 * Handle this price event based on the Primary Bid/Ask scheme. In this scheme
	 * prices only continue to feed through to consumers if they are received
	 * from the primary liquidity provider. Other prices are ignored
	 * 
	 * @return	A boolean indicating whether this price quote is from the Primary 
	 * 			liquidity provider (in which case it can be sent to the consumer)
	 * 			or not (in which case it should be temporarily stored)
     *  		<code>true</code> if this price quote is from the Primary 
	 * 			liquidity provider
	 * 			<code>false</code> if the price quote is not from the Primary 
	 * 			liquidity provider
	 */
	private boolean handlePrimaryBidAskScheme(PriceEvent event) {
		int i = 0;
		/*
		* We loop twice. If we find a primary price quote we exit. If the primary has
		* not changed and we find a non-primary quote, we exit. However, if the primary
		* changes because we have not received a quote within the expected interval, then
		* the current quote must be re-evaluated as it may not be a quote from the new primary.
		* I could either hard code the checking, or loop and allow the quote to be 
		* re-evaluated against the new primary
		*/
		while (i < 2) {
			if (PriceEventHelper.getCurrentPrimaryLiquidityProvider().equals(priceEntity.getLiquidityProvider())) {
				System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - Primary Provider found. Config price scheme is: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider() + ". Price Entity is: " + priceEntity.getLiquidityProvider()); 
	
				//update the timestamp to indicate when latest primary quote received
				//TODO: I'm not sure whether we switch to secondary liquidity provider
				//for the entire feed or only for a symbol. Here I'm keep the last time
				//of a quote for the entire feed - not per symbol
				PriceEventHelper.notePrimaryPriceQuote();
				return true;
			}
			else {
				System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - Non Primary Provider found. Config price scheme is: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider() + ". Price Entity is: " + priceEntity.getLiquidityProvider()); 
				
				//Check timestamp since last primary quote received. If within the configurable
				//boundaries then the current liquidity provider remains the primary and this 
				//non-primary quote is ignore. If we have not received a primary quote within the 
				//expected time interval, change the primary liquidity provider to the next available
				//provider and re-evaluate this quote.
				//
				//Though we set the notePrimaryPriceQuote to the current instant, this may not be correct
				//since the current quote may not be for the new primary provider. However, to make this
				//correct we would have to store the quote times of all providers; then, when we set a
				//new primary we will have the correct time we received the last quote from the new primary
				if (PriceEventHelper.isIntervalSinceLastPrimaryPriceExceeded()) {
					PriceEventHelper.switchToNextBidAskLiquidityProvider();
					PriceEventHelper.notePrimaryPriceQuote();
					System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - switched to new Primary Provider: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider()); 
					continue;
				}
				return false;
			}
		}
		return false;
	}
}