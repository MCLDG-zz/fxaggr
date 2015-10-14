DROP TABLE fxaggr.tickdata;

DROP TABLE fxaggr.minutedata;

DROP TABLE fxaggr.daydata;

DROP TABLE fxaggr.openclose;

DROP KEYSPACE fxaggr;

CREATE KEYSPACE fxaggr
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3 };

CREATE TABLE fxaggr.tickdata (
  symbol text,
  tick_timestamp timestamp,
  bid decimal,
  ask decimal,
  PRIMARY KEY (symbol, tick_timestamp)
);

CREATE TABLE fxaggr.minutedata (
  symbol text,
  tick_date timestamp,
  tick_minuteofday int, 
  hi decimal,
  lo decimal,
  PRIMARY KEY (symbol, tick_date, tick_minuteofday)
);

CREATE TABLE fxaggr.daydata (
  symbol text,
  tick_date timestamp,
  hi decimal,
  lo decimal,
  PRIMARY KEY (symbol, tick_date)
);

CREATE TABLE fxaggr.openclose (
  symbol text,
  tick_date timestamp,
  day_open decimal,
  day_close decimal,
  PRIMARY KEY (symbol, tick_date)
);
