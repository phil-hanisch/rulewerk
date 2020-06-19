#!/usr/local/bin/zsh

for size in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
    instance="size${size}"

    # create rulewerk programm
    python crosswords/crossword.py $size --rulewerk --words crosswords/words1.txt > "src/main/data/input/asp/${instance}"

    # create asp programm
    python crosswords/crossword.py $size --words crosswords/words1.txt > "crosswords/instances/${instance}"

    for repetion in {1..5}; do

        # rulewerk
        # execute instance and get timing
        # mvn compile exec:java -Dexec.mainClass="org.semanticweb.rulewerk.examples.AspExample" -Dexec.args=$instance | grep TIMING >> results_with_duplicates
        # rulewerk solves it, powered by clasp
        # java -jar rulewerk_asp.jar -p -i $instance -s rulewerk -o output asp/$instance | grep TIMING >> results_with_duplicates

        # gringo
        # duration=$({time gringo crosswords/instances/${instance}} |& grep gringo | cut -f 10 -d " ")
        # print "TIMING [s] # ${instance} # Gringo # ${duration//,/.}" >> results_with_duplicates

        # clingo
        duration=$({time clingo crosswords/instances/${instance}} |& grep '^clingo cr' | cut -f 10 -d " ")
        print "TIMING [s] # ${instance} # Clingo # ${duration//,/.}" >> results_with_duplicates
    done

    print $instance "done"
done
