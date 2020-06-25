#!/usr/local/bin/zsh

# create a new snapshot
cd ~/KnowSys/rulewerk/rulewerk-examples
ls
mvn clean compile assembly:single
cd analytics
cp ../target/rulewerk-examples-0.7.0-SNAPSHOT-jar-with-dependencies.jar rulewerk_asp.jar

# for size in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15; do
for size in 8; do
    instance="size${size}"

    # create rulewerk programm
    python crosswords/crossword.py $size --rulewerk --words crosswords/words1.txt > "crosswords/instances_rulewerk/${instance}"

    # create asp programm
    python crosswords/crossword.py $size --words crosswords/words1.txt > "crosswords/instances/${instance}"

    iterations=5
    for repetion in {1..${iterations}}; do

        # rulewerk
        # execute instance and get timing
        # mvn compile exec:java -Dexec.mainClass="org.semanticweb.rulewerk.examples.AspExample" -Dexec.args=$instance | grep TIMING >> results_with_duplicates
        # rulewerk solves it, powered by clasp
        java -jar rulewerk_asp.jar -P crosswords/instances_rulewerk -a -i $instance -s improvedChoice4 -o "output_${instance}" $instance | grep TIMING >> results_with_duplicates

        # gringo
        # duration=$({time gringo crosswords/instances/${instance}} |& grep gringo | cut -f 10 -d " ")
        # print "TIMING [s] # ${instance} # Gringo # ${duration//,/.}" >> results_with_duplicates

        # clingo
        # duration=$({time clingo crosswords/instances/${instance}} |& grep '^clingo cr' | cut -f 10 -d " ")
        # print "TIMING [s] # ${instance} # Clingo # ${duration//,/.}" >> results_with_duplicates
        
        print "${repetion} / ${iterations} done"

    done

    print $instance "done"
done
