#!/usr/local/bin/zsh

for size in 1 2 3 4 5 6 7 8 9 10; do
    instance="size${size}"

    # RULEWERK
    # create rulewerk programm
    python crosswords/crossword.py $size --rulewerk --words crosswords/words1.txt > "src/main/data/input/asp/${instance}"

    # execute instance and get timing
    mvn compile exec:java -Dexec.mainClass="org.semanticweb.rulewerk.examples.AspExample" -Dexec.args=$instance | grep TIMING >> results.txt

    # ASP
    # create asp programm
    # python crosswords/crossword.py $size --words crosswords/words1.txt > "crosswords/instances/${instance}"

    # duration=$({time gringo crosswords/instances/${instance}} |& grep gringo | cut -f 10 -d " ")
    # print "TIMING [s] # ${instance} # Gringo # ${duration//,/.}" >> results.txt

    print $instance "done"
done
