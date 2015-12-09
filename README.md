There are two parts to this application:

price aggregation engine
UI for checking the progress of the engine

Requires Java 8. Uses MongoDB, Cassandra DB and I was in the process of integrating Kafka to connect to price sources, and to
contain the topics for outputting the final prices.

MongoDB contains the configuration and can also be used to store the output, i.e. the final prices
Cassandra is used to store the final prices and the aggregate tick data, such as tick data per minute, hour, day, etc.

To build:

in 'fxaggr' directory, execute 'mvn package'

To start mongo:

in 'fxaggr' directory, execute 'start-mongo.sh'
The config stored in mongo can be configured using the script in 'test/java/com/bl/fxaggr/MongoPrimaryConfig.json'. There is another 
config file in the same directory - which one is used depends on what type of testing you want to carry out.

to start cassandra:

in 'fxaggr' directory, execute 'ccm start'. Type 'ccm' to see other commands that can be used. Type 'ccm status' to see status of the Cassandra cluster.
Use the script in 'scripts/Cassandra.sh' to rebuild the Cassandra keyspace. This can be executed using cqlsh by typing: source 'Cassandra.sh'. This should drop and recreate the keyspace


To TAR the app:

in 'fxaggr' directory, execute ./mktar.sh

Add the following outstanding functionality:

As part of filtering, check prices in one feed against prices in another. If the variance is too high, reject the prices. This is to
prevent a recurrence of the issue we had where one price feed was providing us the wrong prices.
It should also check the timestamps, and check them against current system time to make sure they are aligned. I have completed the 
price stale check. Now need to check that prices from feedA are within certain time gap to prices from feedB