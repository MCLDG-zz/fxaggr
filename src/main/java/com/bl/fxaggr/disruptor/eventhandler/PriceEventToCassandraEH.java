package com.bl.fxaggr.disruptor.eventhandler;

import com.bl.fxaggr.disruptor.*;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lmax.disruptor.EventHandler;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

/**
 * Persists the price quote event to Cassandra.
 */
public class PriceEventToCassandraEH implements EventHandler<PriceEvent> {
	private Cluster cluster;
	private Session session;
	private PriceQuoteEntity priceQuoteEntity;
	private Map<String, PriceQuoteEntity> priceQuoteMap = new HashMap<>();

	public PriceEventToCassandraEH() {
		// Connect to the cluster and keyspace 
		cluster = Cluster.builder().addContactPoint("127.0.0.1").build();
		session = cluster.connect("fxaggr");
	}

	/**
	 * Write the event to Cassandra 
	 */
	public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) {
		event.setPersistInstant();
		PriceEntity priceEntity = event.getPriceEntity();
		
		this.writeRawQuote(priceEntity);
		//If the event contains a 'final quote', i.e. a quote that has been sent
		//to the consumer, then store the event
		if (event.getEventState() == PriceEvent.EventState.FINAL_QUOTE) {
			writeFinalQuote(priceEntity);
		}
		this.handleMinuteData(priceEntity);
	}
	
	private void writeRawQuote(PriceEntity pe) {
		//Store the original, raw quote
		session.execute("INSERT INTO rawtickdatabysymbol (symbol, tick_timestamp, liquidity_provider, bid, ask) " +
			" VALUES ('" + pe.getSymbol() + "','" + pe.getQuoteTimestamp() + "','" + pe.getLiquidityProvider() + "'," + pe.getBid() + "," + pe.getAsk() + ")");
		session.execute("INSERT INTO rawtickdatabylp (symbol, tick_timestamp, liquidity_provider, bid, ask) " +
			" VALUES ('" + pe.getSymbol() + "','" + pe.getQuoteTimestamp() + "','" + pe.getLiquidityProvider() + "'," + pe.getBid() + "," + pe.getAsk() + ")");
	}
	
	private void writeFinalQuote(PriceEntity pe) {
		//Store the original, raw quote
		session.execute("INSERT INTO tickdata (symbol, tick_timestamp, liquidity_provider, bid, ask) " +
			" VALUES ('" + pe.getSymbol() + "','" + pe.getQuoteTimestamp() + "','" + pe.getLiquidityProvider() + "'," + pe.getBid() + "," + pe.getAsk() + ")");
	}
	
	private void handleMinuteData(PriceEntity pe) {
		LocalDate priceQuoteDate = pe.getQuoteTimestamp().toLocalDate();
		LocalDateTime timestampMinutesOnly = pe.getQuoteTimestamp();
		timestampMinutesOnly = timestampMinutesOnly.withSecond(0);
		timestampMinutesOnly = timestampMinutesOnly.withNano(0);
		int hour = pe.getQuoteTimestamp().getHour();
		int minute = pe.getQuoteTimestamp().getMinute();
		int minuteOfDay = 0;
		
		//Calculate the minute of the day
		if (hour == 0) {
			minuteOfDay = minute;
		}
		else {
			minuteOfDay = hour * 60 + minute;
		}
		
		priceQuoteEntity = priceQuoteMap.get(pe.getSymbol());
		if (priceQuoteEntity == null) {
			priceQuoteEntity = new PriceQuoteEntity();
			priceQuoteEntity.setSymbol(pe.getSymbol());
			priceQuoteEntity.setQuoteDate(timestampMinutesOnly);
		    priceQuoteEntity.setMinuteOfDay(minuteOfDay);
		    priceQuoteEntity.setBidHi(pe.getBid());
		    priceQuoteEntity.setBidLo(pe.getBid());
		    priceQuoteEntity.setAskHi(pe.getAsk());
		    priceQuoteEntity.setAskLo(pe.getAsk());
		    priceQuoteEntity.setNumberOfTicks(1);
		    priceQuoteMap.put(pe.getSymbol(), priceQuoteEntity);
		}
		else {
			//Check if the price quote we are storing is for the current minute. If it is
			//update it, otherwise assume the current minute of time is complete and write
			//the minute date to the DB
			if (minuteOfDay == priceQuoteEntity.getMinuteOfDay()) {
				priceQuoteEntity.setNumberOfTicks(priceQuoteEntity.getNumberOfTicks() + 1);
				if (pe.getBid() > priceQuoteEntity.getBidHi()) {
					priceQuoteEntity.setBidHi(pe.getBid());
				}
				else {
					if (pe.getBid() < priceQuoteEntity.getBidLo()) {
						priceQuoteEntity.setBidLo(pe.getBid());
					}
				}
				if (pe.getAsk() > priceQuoteEntity.getAskHi()) {
					priceQuoteEntity.setAskHi(pe.getAsk());
				}
				else {
					if (pe.getAsk() < priceQuoteEntity.getAskLo()) {
						priceQuoteEntity.setAskLo(pe.getAsk());
					}
				}
			} else {
				WriteMinuteData(priceQuoteEntity);
				priceQuoteEntity.setQuoteDate(timestampMinutesOnly);
			    priceQuoteEntity.setMinuteOfDay(minuteOfDay);
			    priceQuoteEntity.setBidHi(pe.getBid());
			    priceQuoteEntity.setBidLo(pe.getBid());
			    priceQuoteEntity.setAskHi(pe.getAsk());
			    priceQuoteEntity.setAskLo(pe.getAsk());
			    priceQuoteEntity.setNumberOfTicks(1);
			}
		}
	}
	private void WriteMinuteData(PriceQuoteEntity pqe) {
		session.execute("INSERT INTO minutedata (symbol, tick_date, tick_minuteofday, bid_hi, bid_lo, ask_hi, ask_lo, number_of_ticks) " +
			" VALUES ('" + pqe.getSymbol() + "','" + pqe.getQuoteDate() + "'," + pqe.getMinuteOfDay() + "," + pqe.getBidHi() + "," + pqe.getBidLo() + "," + pqe.getAskHi() + "," + pqe.getAskLo() + "," + pqe.getNumberOfTicks() + ")");
	}
	
}