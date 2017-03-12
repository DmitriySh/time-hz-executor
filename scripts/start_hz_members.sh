#!/bin/sh

java -jar -Dnode=1 hz-node/build/libs/hz-node-all-*.jar &
sleep 0.1
java -jar -Dnode=2 hz-node/build/libs/hz-node-all-*.jar &
sleep 0.1
java -jar -Dnode=3 hz-node/build/libs/hz-node-all-*.jar &
