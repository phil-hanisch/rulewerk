#!/usr/local/bin/zsh

# some constants
limit_time=2m
iterations=1

# some dynamic properties
vared -c -p "Clean grounding folder? [yn]: " clean_grounding
if [[ $clean_grounding = y ]]; then
    rm ../grounding/**/*(.)
fi
vared -c -p "Clean output folder? [yn]: " clean_output
if [[ $clean_output = y ]]; then
    rm ../output/**/*(.)
fi

vared -c -p "Output file name: " name
output_file=../results/$name
echo

vared -c -p 'Run crossword tests? [yn]: ' run_cw
vared -c -p 'Run graph tests? [yn]: ' run_gr
vared -c -p 'Run abstract argumentation tests? [yn]: ' run_aa
echo

typeset -A run
vared -c -p 'Run "solve" tasks? [yn]: ' run_solve
run[solve]=$run_solve
vared -c -p 'Run "ground" tasks? [yn]: ' run_ground
run[ground]=$run_ground
vared -c -p 'Run "all" tasks? [yn]: ' run_all
run[all]=$run_all
echo

vared -c -p 'Run tests on shuffled instances? [yn]: ' shuffled_instances
echo

# run and time the given command $1
# record it based on task $2, system $3, encoding $4, and instance $5
function run-test {
    comm=$1; task=$2; system=$3; encoding=$4; instance=$5
    echo $task $system $encoding $instance
    echo $comm
    for i in {0..$iterations}; do
        if [[ $task = solve && $run[solve] = y ]]; then
            timing=$(eval timeout $limit_time $comm | grep 'Solving:')
            duration=${${(z)timing:s/(//}[5]}
        elif [[ $run[$task] = y || ( $task = ground && $run[solve] = y ) ]]; then
            timing=$({time (eval timeout $limit_time $comm)} |& grep total)
            duration=${${(z)timing}[7]}
        fi

        if [[ $i > 0 && $run[$task] = y ]]; then
        # if [[ $i > 0 && $duration < $limit_time ]]; then
            print "TIMING [s] # ${encoding:r:t}:${instance:r:t} # $task:$system # ${duration//,/.}" >> $output_file
        elif [[ $i > 0 && $run[$task] = y ]]; then
            echo timeout!
        fi

        echo $i of "$iterations : $duration"
    done
}

alias BEGIN_COMMENT="if [[ 1 = 0 ]]; then"
alias END_COMMENT="fi"

# ----- main part -----


tests=()
if [[ $run_cw = y ]]; then
    tests+=crossword
fi
if [[ $run_gr = y ]]; then
    tests+=graphs
fi
if [[ $run_aa = y ]]; then
    tests+=argumentation
fi

for suite in $tests; do
    dir=../input/$suite

    # rulewerk
    for encoding in $dir/encodings/*.rlp; do
        for instance in $dir/instances/*.lp; do
            # ground
            comm="java -jar rulewerk_asp.jar -a -i ${instance:r:t} -s rulewerk -o ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.rw-aspif $encoding $instance"
            run-test $comm ground rulewerk $encoding $instance

            # solve
            comm="java -jar rulewerk_asp.jar -i ${instance:r:t} -s rulewerk -o ${dir:s/input/output/}/${encoding:r:t}-${instance:r:t}.rw-solved $encoding $instance"
            run-test $comm all rulewerk $encoding $instance

            # clasp@rulewerk
            comm="clasp ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.rw-aspif"
            run-test $comm solve clasp@rulewerk $encoding $instance
        done
    done

    # potassco
    for encoding in $dir/encodings/*.lp; do
        for instance in $dir/instances/*.lp; do
            # ground
            comm="gringo $encoding $instance > ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.gg-aspif"
            run-test $comm ground gringo $encoding $instance

            # clingo
            comm="clingo $encoding $instance > ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.cl-solved"
            run-test $comm all clingo $encoding $instance

            # clingo solve
            comm="clingo $encoding $instance"
            run-test $comm solve clingo $encoding $instance

            # gringo|clasp
            comm="gringo $encoding $instance | clasp > ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.cl-solved"
            run-test $comm all gringo+clasp $encoding $instance

            # clasp@gringo
            comm="clasp ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.gg-aspif"
            run-test $comm solve clasp@gringo $encoding $instance

            # clasp@gringo-shuf
            ./shuf-aspif-format.zsh ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.gg-aspif ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.shuf-aspif
            comm="clasp ${dir:s/input/grounding/}/${encoding:r:t}-${instance:r:t}.shuf-aspif"
            run-test $comm solve clasp@shuf $encoding $instance
        done

        # shuffled instances
        if [[ $shuffled_instances = y ]]; then
            for instance in $dir/instances/*.shuf-lp; do
                # clingo solve
                comm="clingo $encoding $instance"
                run-test $comm solve clingo $encoding shuf-${instance:t}
            done
        fi
    done
done

