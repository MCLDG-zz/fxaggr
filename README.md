#FX Price Aggregation Engine

There are two parts to this application:

* price aggregation engine
* UI for checking the progress of the engine, and for updating the configuration

Requires Java 8. Uses MongoDB, Cassandra DB and I was in the process of integrating Kafka to connect to price sources, and to
contain the topics for outputting the final prices.

* MongoDB contains the configuration and can also be used to store the output, i.e. the final prices
* Cassandra is used to store the final prices and the aggregate tick data, such as tick data per minute, hour, day, etc.

### To build:

in 'fxaggr' directory, execute 'mvn package'

### To start mongo:

* in 'fxaggr' directory, execute 'start-mongo.sh'
* The config stored in mongo can be configured using the script in 'test/java/com/bl/fxaggr/MongoPrimaryConfig.json'. There is another 
config file in the same directory - which one is used depends on what type of testing you want to carry out.

### to start cassandra:

* in 'fxaggr' directory, execute 'ccm start'. Type 'ccm' to see other commands that can be used. Type 'ccm status' to see status of the Cassandra cluster.
* Use the script in 'scripts/Cassandra.sh' to rebuild the Cassandra keyspace. This can be executed using cqlsh by typing: source 'Cassandra.sh'. This should drop and recreate the keyspace

### To prepare the UI:

* The UI uses node.js and angular.js. 
* In the root directory is package.json. So execute 'npm install' in the same directory as pacakge.json and this will install all dependencies
* Then type 'node server.js' to start the node server. It should listen on port 3001
* Use a browser to navigate to: http://192.168.9.99:3001/#/dashboard
* The UI shows various metrics on how the engine is performing. It can also be used to update the config in Mongo, either at a global level or per currency pair

### To run the engine:
There are currently two ways to run the engine:

* in test mode. This runs test cases, and is started by 'test-fxaggr.sh'
* in file mode, where prices are read from a file. 'run-fxaggr.sh'
* Currently the app does not connect to real prices feeds. Connecting the app to Kafka to retrieve prices was started but not completed. 
The idea I had was to develop adaptors that connect to the price feed (a simple telnet connector, in most cases) and publish the prices
to Kafka. The engine simply reads all prices from a Kafka topic, and publishes the final prices to another topic

### Add the following outstanding functionality:

* As part of filtering, check prices in one feed against prices in another. If the variance is too high, reject the prices. This is to
prevent a recurrence of the issue we had where one price feed was providing us the wrong prices.
* It should also check the timestamps, and check them against current system time to make sure they are aligned. I have completed the 
price stale check. Now need to check that prices from feedA are within certain time gap to prices from feedB