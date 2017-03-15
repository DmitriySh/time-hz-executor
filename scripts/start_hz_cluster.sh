#!/bin/sh

java -jar -Dnode=1 -Dfile.encoding=UTF-8 ../hz-node/build/libs/hz-node-all-*.jar &
sleep 0.1
java -jar -Dnode=2 -Dfile.encoding=UTF-8 ../hz-node/build/libs/hz-node-all-*.jar &
sleep 0.1
java -jar -Dnode=3 -Dfile.encoding=UTF-8 ../hz-node/build/libs/hz-node-all-*.jar &
