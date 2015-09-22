Add the following:

As part of filtering, check prices in one feed against prices in another. If the variance is too high, reject the prices. This is to
prevent a recurrence of the issue we had where one price feed was providing us the wrong prices.
It should also check the timestamps, and check them against current system time to make sure they are aligned. I have completed the 
price stale check. Now need to check that prices from feedA are within certain time gap to prices from feedB