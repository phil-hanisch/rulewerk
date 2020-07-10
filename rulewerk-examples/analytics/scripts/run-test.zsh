#!/usr/local/bin/zsh

# some constants
limit_time=3m
iterations=3
output_file=../results/results_of_july

tests=()
tests+=crossword
tests+=graphs
#tests+=argumentation

# run and time the given command $1
# record it based on task $2, system $3, encoding $4, and instance $5
function run-test {
    comm=$1; task=$2; system=$3; encoding=$4; instance=$5
    echo $task $system $encoding $instance
    for i in {0..$iterations}; do
        echo $i of $iterations

        if [[ $task = solve ]]; then
            timing=$(eval timeout $limit_time $comm | grep 'Solving:')
            duration=${${(z)timing:s/(//}[5]}
        else
            timing=$({time (eval timeout $limit_time $comm)} |& grep total)
            duration=${${(z)timing}[7]}
        fi

        if [[ $i > 0 && $duration < $limit_time ]]; then
            print "TIMING [s] # ${encoding:r:t}:${instance:r:t} # $task:$system # ${duration//,/.}" >> $output_file
        elif [[ $i > 0 ]]; then
            echo timeout!
        fi
    done
}

alias BEGIN_COMMENT="if [[ 1 = 0 ]]; then"
alias END_COMMENT="fi"

# ----- main part -----
for suite in $tests; do
    dir=../input/$suite

    # rulewerk
    for encoding in $dir/encodings/*.rlp; do
        for instance in $dir/instances/*.lp; do
            # ground
            comm="java -jar rulewerk_asp.jar -a -i ${instance:r:t} -s rulewerk -o ${dir:s/input/grounding/}/${instance:r:t}.rw-aspif $encoding $instance"
            run-test $comm ground rulewerk $encoding $instance

            # solve
            comm="java -jar rulewerk_asp.jar -i ${instance:r:t} -s rulewerk -o ${dir:s/input/output/}/${instance:r:t}.rw-solved $encoding $instance"
            run-test $comm all rulewerk $encoding $instance

            # clasp@rulewerk
            comm="clasp ${dir:s/input/grounding/}/${instance:r:t}.rw-aspif"
            run-test $comm solve clasp@rulewerk $encoding $instance
        done
    done

    # potassco
    for encoding in $dir/encodings/*.lp; do
        for instance in $dir/instances/*.lp; do
            # ground
            comm="gringo $encoding $instance > ${dir:s/input/grounding/}/${instance:r:t}.gg-aspif"
            run-test $comm ground gringo $encoding $instance

            # clingo
            comm="clingo $encoding $instance > ${dir:s/input/grounding/}/${instance:r:t}.cl-solved"
            run-test $comm all clingo $encoding $instance

            # clingo solve
            comm="clingo $encoding $instance > ${dir:s/input/grounding/}/${instance:r:t}.cl-solved"
            run-test $comm solve clingo $encoding $instance

            # gringo|clasp
            comm="gringo $encoding $instance | clasp > ${dir:s/input/grounding/}/${instance:r:t}.cl-solved"
            run-test $comm all gringo+clasp $encoding $instance

            # clasp@gringo
            comm="clasp ${dir:s/input/grounding/}/${instance:r:t}.gg-aspif"
            run-test $comm solve clasp@gringo $encoding $instance

            # clasp@gringo-shuf
            ./shuf-aspif-format.zsh ${dir:s/input/grounding/}/${instance:r:t}.gg-aspif ${dir:s/input/grounding/}/${instance:r:t}.shuf-aspif
            comm="clasp ${dir:s/input/grounding/}/${instance:r:t}.shuf-aspif"
            run-test $comm solve clasp@shuf $encoding $instance
        done

        # shuffled instances
        for instance in $dir/instances/*.shuf-lp; do
            # clingo solve
            comm="clingo $encoding $instance > ${dir:s/input/grounding/}/${instance:r:t}.cl-solved"
            run-test $comm solve clingo $encoding $instance:shuf
        done
    done

done

