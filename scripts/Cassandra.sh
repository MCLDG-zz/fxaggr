DROP TABLE fxaggr.rawtickdatabysymbol;

DROP TABLE fxaggr.rawtickdatabylp;

DROP TABLE fxaggr.tickdata;

DROP TABLE fxaggr.minutedata;

DROP TABLE fxaggr.daydata;

DROP TABLE fxaggr.openclose;

DROP KEYSPACE fxaggr;

CREATE KEYSPACE fxaggr
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

CREATE TABLE fxaggr.rawtickdatabysymbol (
  symbol text,
  tick_timestamp timestamp,
  liquidity_provider text,
  bid decimal,
  ask decimal,
  PRIMARY KEY (symbol, tick_timestamp)
);

CREATE TABLE fxaggr.rawtickdatabylp (
  liquidity_provider text,
  symbol text,
  tick_timestamp timestamp,
  bid decimal,
  ask decimal,
  PRIMARY KEY (liquidity_provider, symbol, tick_timestamp)
);

CREATE TABLE fxaggr.tickdata (
  symbol text,
  tick_timestamp timestamp,
  liquidity_provider text,
  bid decimal,
  ask decimal,
  PRIMARY KEY (symbol, tick_timestamp)
);

CREATE TABLE fxaggr.minutedata (
  symbol text,
  tick_date timestamp,
  tick_minuteofday int, 
  bid_hi decimal,
  bid_lo decimal,
  ask_hi decimal,
  ask_lo decimal,
  number_of_ticks bigint,
  PRIMARY KEY (symbol, tick_date, tick_minuteofday)
);

CREATE TABLE fxaggr.daydata (
  symbol text,
  tick_date timestamp,
  bid_hi decimal,
  bid_lo decimal,
  ask_hi decimal,
  ask_lo decimal,
  number_of_ticks bigint,
  PRIMARY KEY (symbol, tick_date)
);

CREATE TABLE fxaggr.openclose (
  symbol text,
  tick_date timestamp,
  day_open decimal,
  day_close decimal,
  PRIMARY KEY (symbol, tick_date)
);
