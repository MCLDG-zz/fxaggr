package com.bl.fxaggr.disruptor.eventhandler;

import com.bl.fxaggr.disruptor.*;

import java.util.Map;

import com.lmax.disruptor.EventHandler;

/**
 * Handles events when the pricing strategy is 'Primary Bid/Ask'. In this strategy
 * price quotes from the primary liquidity provider are sent through to consumers 
 * immediately. Price quotes from non-primary liquidity providers are filtered.
 * <p>
 * The timestamp of the last price quote from the primary liquidity provider is stored.
 * If the interval between the timestamp and the current time exceeds a configurable
 * threshold (@see AggrConfig), the primary liquidity provider is switched to the next
 * available liquidity provider.
 * <p>
 * If, after switching, we start receiving quotes for the previous primary, we will 
 * switch back to the previous primary after a configurable number of quotes have
 * been received (@see AggrConfig).
 * 
 * @see AggrConfig
 */
public class PrimaryBidAskEH implements EventHandler<PriceEvent> {
	
	private Map<String, AggrConfig.AggrConfigCurrency> aggrcurrencyconfigMap = null;
	private PriceEntity priceEntity;

	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		//Store the latest price quote
		priceEntity = event.getPriceEntity();
		PriceEventHelper.storeLatestPriceQuote(priceEntity);
		event.setEventState(PriceEvent.EventState.COMPARISON_COMPLETED);
		
		//Ensure we have a valid config
		if (PriceEventHelper.aggrConfig == null) {
			System.out.println("PrimaryBidAskEH cannot analyse pricing. No config in table aggrconfig. Sequence: " + sequence); 
			event.addAuditEvent("PrimaryBidAskEH. Cannot analyse pricing. No config in table aggrconfig. Sequence: " + sequence); 
			return;
		}
		//Ignore non-valid events
		if (event.getEventStatus() != PriceEvent.EventStatus.VALID) {
			return;
		}

		/**
		 * TODO
		 * 
		 * Handle the case where best bid/ask is not able to create a best price.
		 * In this case, revert to the primary bid/ask
		 * 
		 */
		 
		//Now process the event based on the current price selection scheme as 
		//defined in the config
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
				if (!this.handleBestBidAskScheme(event)) {
					event.setEventStatus(PriceEvent.EventStatus.FILTERED);
					event.addFilteredReason(PriceEvent.FilterReason.NO_BEST_BIDASK_NOT_PRIMARY);
				}
				break;
		}

		//Log the stats
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

		event.setAppliedSelectionScheme(PriceEvent.AppliedSelectionScheme.PRIMARY_BID_ASK);
		
		//If this price count is from the previous primary liquidity provider, increment
		//the counter and check against the config. If it exceeds the config we will
		//switch back to the previous primary provider
		if (PriceEventHelper.isPreviousPrimaryLiquidityProvider() &&
			PriceEventHelper.getPreviousPrimaryLiquidityProvider().equals(priceEntity.getLiquidityProvider())) {
				
			PriceEventHelper.incrementPriceCounterForPreviousPrimaryProvider();
			if (PriceEventHelper.isRequiredNumberPreviousPrimaryQuotesReceived()) {
				
				if (PriceEventHelper.switchToPreviousBidAskLiquidityProvider()) {
					event.setEventLPSwitch(PriceEvent.EventLPSwitch.SWITCH_BACK_PREVIOUS_PRIMARY);
					System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - switched to previous Primary Provider: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider()); 
				}
				else {
					event.setEventLPSwitch(PriceEvent.EventLPSwitch.UNABLE_TO_SWITCH);
					System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - UNABLE to switch to previous Primary Provider"); 
					return false;
				}
			}
		}
		
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

				//update the timestamp to indicate when latest primary quote received
				//TODO: I'm not sure whether we switch to secondary liquidity provider
				//for the entire feed or only for a symbol. Here I'm keep the last time
				//of a quote for the entire feed - not per symbol
				PriceEventHelper.notePrimaryPriceQuote();
				event.setAppliedSelectionScheme(PriceEvent.AppliedSelectionScheme.PRIMARY_BID_ASK);
				return true;
			}
			else {

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
				//
				//TODO - should I switch back if quotes are received from previous primary???
				if (PriceEventHelper.isIntervalSinceLastPrimaryPriceExceeded()) {
					System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - no quotes received for primary. About to switch to next Primary Provider. Current provider is: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider()); 
					
					if (PriceEventHelper.switchToNextBidAskLiquidityProvider()) {
						event.setEventLPSwitch(PriceEvent.EventLPSwitch.SWITCH_NEXT_PRIMARY);
						PriceEventHelper.notePrimaryPriceQuote();
						System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - switched to new Primary Provider: " + PriceEventHelper.getCurrentPrimaryLiquidityProvider()); 
						continue;
					}
					else {
						event.setEventLPSwitch(PriceEvent.EventLPSwitch.UNABLE_TO_SWITCH);
						System.out.println("Sequence: " + priceEntity.getSequence() + ". PrimaryBidAskEH - UNABLE to switch to new Primary Provider"); 
						return false;
					}
				}
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Handle this price event based on the Best Bid/Ask scheme. In this scheme
	 * the best bid/ask based on the price quotes available at this moment in time
	 * is created and fed through to consumers.
	 * 
	 * <p>
	 * For a best bid/ask to be created there must be a current quote from a
	 * quorum of liquidity providers. A 'quorum' is defined as the minimum number
	 * of liquidity provider who have provided us with a current quote. Usually,
	 * a minimum of at least 3 quotes must exist for a symbol, each from unique
	 * liqudity providers, and each provided within the past 'n' seconds. The number
	 * of quotes required and the interval within which they must be provided are 
	 * defined in the config under the section 'globalconfig.bestbidask'.
	 * 
	 * @return	A boolean indicating whether a best bid-ask price quote could be
	 * 			created and may therefore be sent to the consumer
     *  		<code>true</code> a best bid-ask price quote could be
	 * 			created and may therefore be sent to the consumer
	 * 			<code>false</code> a best bid-ask price quote could not be
	 * 			created and may therefore not be sent to the consumer
	 */
	private boolean handleBestBidAskScheme(PriceEvent event) {
		String symbol = priceEntity.getSymbol();
		double bestBid = 0;
		double bestAsk = 0;
		
		event.setAppliedSelectionScheme(PriceEvent.AppliedSelectionScheme.BEST_BID_ASK);
		/*
		* Get the best bid/ask combination
		*/
		Map<String, PriceEntity> bestBidAsk = PriceEventHelper.getBestBidAskForSymbol(symbol);
		if (bestBidAsk == null) {
			return false;
		}
		
		//Assume a best bid/ask has been created
		event.setBestBidAsk(bestBidAsk);
		return true;
	}
}