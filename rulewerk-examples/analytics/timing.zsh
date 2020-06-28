#!/usr/local/bin/zsh

# some constants
start_size=1
end_size=10
iterations=3
output_file='june_results'

# create a new snapshot
cd ~/KnowSys/rulewerk
mvn install
cd rulewerk-examples
ls
mvn clean compile assembly:single
cd analytics
cp ../target/rulewerk-examples-0.7.0-SNAPSHOT-jar-with-dependencies.jar rulewerk_asp.jar

for size in {${start_size}..${end_size}}; do
    instance="size${size}"

    # create rulewerk programm
    python crosswords/crossword.py $size --rulewerk --words crosswords/words1.txt > "crosswords/instances_rulewerk/${instance}"

    # create asp programm
    python crosswords/crossword.py $size --words crosswords/words1.txt > "crosswords/instances/${instance}"

    for repetion in {1..${iterations}}; do

        # rulewerk grounding
        duration=$({time java -jar rulewerk_asp.jar -P crosswords/instances_rulewerk -a -i $instance -s rulewerk -o "${instance}.aspif" $instance} |& grep 'java -jar' | tr -s " " | cut -f 16 -d " ")
        print "TIMING [s] # ${instance} # rulewerk # ${duration//,/.}" >> $output_file

        # rulewerk solving
        duration=$({time java -jar rulewerk_asp.jar -P crosswords/instances_rulewerk -i $instance -s rulewerk -o "${instance}.solved" $instance} |& grep 'java -jar' | tr -s " " | cut -f 15 -d " ")
        print "TIMING [s] # ${instance} # rulewerkSolving # ${duration//,/.}" >> $output_file

        # clasp by rulewerk
        duration=$({time clasp crosswords/instances_rulewerk/${instance}.aspifi} |& grep 'clasp cross' | tr -s " " | cut -f 9 -d " ")
        print "TIMING [s] # ${instance} # clasp@rulewerk # ${duration//,/.}" >> $output_file

        # gringo
        duration=$({time gringo crosswords/instances/${instance} > crosswords/instances/${instance}.aspif} |& grep gringo | tr -s " " | cut -f 10 -d " ")
        print "TIMING [s] # ${instance} # Gringo # ${duration//,/.}" >> $output_file

        # clasp by gringo
        duration=$({time clasp crosswords/instances/${instance}.aspifi} |& grep 'clasp cross' | tr -s " " | cut -f 9 -d " ")
        print "TIMING [s] # ${instance} # clasp@gringo # ${duration//,/.}" >> $output_file

        # gringo | clasp
        duration=$({time (gringo crosswords/instances/${instance} | clasp)} |& grep '( gringo' | tr -s " " | cut -f 13 -d " ")
        print "TIMING [s] # ${instance} # Gringo+Clasp # ${duration//,/.}" >> $output_file

        # clingo
        duration=$({time clingo crosswords/instances/${instance}} |& grep '^clingo cr' | cut -f 10 -d " ")
        print "TIMING [s] # ${instance} # Clingo # ${duration//,/.}" >> $output_file

        print "${repetion} / ${iterations} done"

    done

    print $instance "done"
done
