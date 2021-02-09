#!/usr/local/bin/zsh

# ----- settings -----
## timeout
limit_time=20m

## iterations per instance
iterations=1

## test problem classes
## provide folder names that contain the performance test classes
classes=()
# classes+=crossword
# classes+=visit-all
classes+=stable-marriage

## tasks [yn]
typeset -A run
run[ground]=y
run[solve-only]=y
run[all]=n

# clean during preparations [yn]
clean_grounding=n
clean_output=n

# consume instance after tests
consume_instance=y

# create rulewerk.jar
compile=n


# ----- helper functions -----
# run and time the given command $1
# record it based on task $2, system $3, encoding $4, and instance $5
function run-test {
    comm=$1; task=$2; system=$3; encoding=$4; instance=$5
    echo $task $system $encoding $instance
    echo $comm

    for i in {1..$iterations}; do
        if [[ $task = solve-only && $run[solve-only] = y ]]; then
            (eval timeout $limit_time $comm) 2> .stderr > .stdout
            exit_code=$?
            timing=$(grep 'Solving:' .stdout)
            duration=${${(z)timing:s/(//}[5]}

        elif [[ $run[$task] = y || ( $task = ground && $run[solve-only] = y ) ]]; then
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

# preparations
if [[ $compile = y ]]; then
    cd ../rulewerk-examples
    mvn clean compile assembly:single
    cp target/rulewerk-examples-0.7.0-SNAPSHOT-jar-with-dependencies.jar ../evaluation/rulewerk-asp.jar
    cd ../evaluation
fi

# run tests
for suite in $classes; do
    dir=$suite
    output_file=$dir/results.txt

    if [[ $clean_grounding = y ]]; then
        rm $dir/groundings/*(.)
    fi
    if [[ $clean_output = y ]]; then
        rm $dir/output/*(.)
    fi

    for instance in $dir/instances/*.lp; do
        # rulewerk
        for encoding in $dir/encodings/*.rlp; do
            # ground
            comm="java -jar rulewerk-asp.jar -a -i ${instance:r:t} -s rulewerk -o ${dir}/groundings/${encoding:r:t}-${instance:r:t}.rw-aspif $encoding $instance"
            run-test $comm ground rulewerk $encoding $instance

            # solve: clasp@rulewerk
            comm="clasp ${dir}/groundings/${encoding:r:t}-${instance:r:t}.rw-aspif"
            run-test $comm solve-only clasp@rulewerk $encoding $instance

            # ground with native answer optimisation
            comm="java -jar rulewerk-asp-fast.jar -a -i ${instance:r:t} -s rulewerk -o ${dir}/groundings/${encoding:r:t}-${instance:r:t}.rwf-aspif $encoding $instance"
            run-test $comm ground rulewerk-fast $encoding $instance

            # solve: clasp@rulewerk-fast
            comm="clasp ${dir}/groundings/${encoding:r:t}-${instance:r:t}.rwf-aspif"
            run-test $comm solve-only clasp@rulewerk-fast $encoding $instance

            # ground+solve
            # comm="java -jar rulewerk-asp.jar -i ${instance:r:t} -s rulewerk -o ${dir}/output/${encoding:r:t}-${instance:r:t}.rw-solved $encoding $instance"
            # run-test $comm all rulewerk $encoding $instance
        done

        # potassco
        for encoding in $dir/encodings/*.lp; do
            # ground
            comm="gringo $encoding $instance > ${dir}/groundings/${encoding:r:t}-${instance:r:t}.gg-aspif"
            run-test $comm ground gringo $encoding $instance

            # solve: clasp@gringo
            comm="clasp ${dir}/groundings/${encoding:r:t}-${instance:r:t}.gg-aspif"
            run-test $comm solve-only clasp@gringo $encoding $instance

            # clingo: ground+solve
            # comm="clingo $encoding $instance > ${dir}/output/${encoding:r:t}-${instance:r:t}.cl-solved"
            # run-test $comm all clingo $encoding $instance

            # clingo: solving time
            # comm="clingo $encoding $instance"
            # run-test $comm solve-only clingo $encoding $instance

            # gringo|clasp
            # comm="gringo $encoding $instance | clasp > ${dir}/output/${encoding:r:t}-${instance:r:t}.cl-solved"
            # run-test $comm all gringo+clasp $encoding $instance

            # clasp@gringo-shuf
            #for i in {1..5}; do
                #./shuf-aspif-format.zsh ${dir}/groundings/${encoding:r:t}-${instance:r:t}.gg-aspif ${dir}/groundings/${encoding:r:t}-${instance:r:t}-${i}.shuf-aspif
                #comm="clasp ${dir}/groundings/${encoding:r:t}-${instance:r:t}-${i}.shuf-aspif"
                #run-test $comm solve-only clasp@shuf $encoding $instance
            #done
        done

        if [[ $consume_instance = y ]]; then
            rm $instance
        fi
    done
done

rm vlog.log
rm .stdout
rm .stderr
