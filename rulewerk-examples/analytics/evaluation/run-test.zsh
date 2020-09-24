#!/usr/local/bin/zsh

# general settings
## timeout
limit_time=10m

## iterations per instance
iterations=1

## test problem classes
## provide folder names that contain the performance test classes
classes=()
#classes+=crossword
#classes+=labyrinth-O24
#classes+=stable-marriage
classes+=visit-all

## tasks [yn]
typeset -A run
run[ground]=y
run[solve]=y
run[all]=n


# run and time the given command $1
# record it based on task $2, system $3, encoding $4, and instance $5
function run-test {
    comm=$1; task=$2; system=$3; encoding=$4; instance=$5
    echo $task $system $encoding $instance
    echo $comm
    for i in {1..$iterations}; do
        #if [[ $task = solve && $run[solve] = y ]]; then
            #(eval timeout $limit_time $comm) 2> .stderr > .stdout
            #exit_code=$?
            #timing=$(grep 'Solving:' .stdout)
            #duration=${${(z)timing:s/(//}[5]}
        if [[ $run[$task] = y || ( $task = ground && $run[solve] = y ) ]]; then
            {time (eval timeout $limit_time $comm)} 2> .stderr > .stdout
            exit_code=$?
            cp .stdout "$dir/output/${system}-${task}-${instance:r:t}.log"
            timing=$(grep total .stderr)
            # timing=$({time (eval timeout $limit_time $comm)} |& grep total)
            duration=${${(z)timing}[7]}
        fi

        if [[ ($exit_code = 0 || $exit_code = 10 || $exit_code = 20) && $run[$task] = y ]]; then
        # clasp has exit code 10 if the ground asp program is satisfiable
        # clasp has exit code 20 if the ground asp program is unsatisfiable
            if [[ $i > 0 ]]; then
                print "$system,$task,${instance:r:t},${duration//,/.}" >> $output_file
            fi
            echo $i of "$iterations : $duration"
        elif [[ $run[$task] = y ]]; then
            if [[ $exit_code = 124 ]]; then
                echo "Timeout!"
            else
                echo "Error!"
            fi
            echo $i of "$iterations : $duration"
        fi
    done
    echo
}

alias BEGIN_COMMENT="if [[ 1 = 0 ]]; then"
alias END_COMMENT="fi"

# ----- main part -----

for suite in $classes; do
    dir=$suite
    output_file=$dir/results.txt

    # some dynamic properties
    vared -c -p "Clean grounding folder? [yn]: " clean_grounding
    if [[ $clean_grounding = y ]]; then
        rm $dir/groundings/*(.)
    fi

    vared -c -p "Clean output folder? [yn]: " clean_output
    if [[ $clean_output = y ]]; then
        rm $dir/output/*(.)
    fi

    for instance in $dir/instances/*.lp; do
        # rulewerk
        for encoding in $dir/encodings/*.rlp; do
            # ground
            comm="java -jar rulewerk-asp.jar -a -i ${instance:r:t} -s rulewerk -o ${dir}/groundings/${encoding:r:t}-${instance:r:t}.rw-aspif $encoding $instance"
            run-test $comm ground rulewerk $encoding $instance

            # ground+solve
            # comm="java -jar rulewerk-asp.jar -i ${instance:r:t} -s rulewerk -o ${dir}/output/${encoding:r:t}-${instance:r:t}.rw-solved $encoding $instance"
            # run-test $comm all rulewerk $encoding $instance

            # solve: clasp@rulewerk
            comm="clasp ${dir}/groundings/${encoding:r:t}-${instance:r:t}.rw-aspif"
            run-test $comm solve clasp@rulewerk $encoding $instance
        done

        # potassco
        for encoding in $dir/encodings/*.lp; do
            # ground
            comm="gringo $encoding $instance > ${dir}/groundings/${encoding:r:t}-${instance:r:t}.gg-aspif"
            run-test $comm ground gringo $encoding $instance

            # clingo: ground+solve
            # comm="clingo $encoding $instance > ${dir}/output/${encoding:r:t}-${instance:r:t}.cl-solved"
            # run-test $comm all clingo $encoding $instance

            # clingo: solving time
            # comm="clingo $encoding $instance"
            # run-test $comm solve clingo $encoding $instance

            # gringo|clasp
            # comm="gringo $encoding $instance | clasp > ${dir}/output/${encoding:r:t}-${instance:r:t}.cl-solved"
            # run-test $comm all gringo+clasp $encoding $instance

            # solve: clasp@gringo
            comm="clasp ${dir}/groundings/${encoding:r:t}-${instance:r:t}.gg-aspif"
            run-test $comm solve clasp@gringo $encoding $instance

            # clasp@gringo-shuf
            # ./shuf-aspif-format.zsh ${dir}/groundings/${encoding:r:t}-${instance:r:t}.gg-aspif ${dir}/groundings/${encoding:r:t}-${instance:r:t}.shuf-aspif
            # comm="clasp ${dir}/groundings/${encoding:r:t}-${instance:r:t}.shuf-aspif"
            # run-test $comm solve clasp@shuf $encoding $instance
        done

        rm $instance
    done
done

rm vlog.log
rm .stdout
rm .stderr
