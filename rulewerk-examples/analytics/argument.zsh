#!/usr/local/bin/zsh

cd ..
echo task instance parsing vlog grounding rulewerk gringo gringo+clasp clingo solving

# for instance in $(ls -S1 argumentation/instances | grep 'apx$'); do
for instance in Medium-result-b23.apx C-2-afinput_exp_cycles_depvary_step4_batch_yyy05.apx A-4-afinput_exp_acyclic_indvary1_step2_batch_yyy07.apx; do
    for task in $(ls -S1 argumentation/rulewerk); do

        # mvn compile exec:java -Dexec.mainClass="org.semanticweb.rulewerk.examples.AspExample" -Dexec.args="-o output/grounding -P argumentation/ instances/${instance} rulewerk/${task} filter.lp" > log_file
        java -jar rulewerk_asp.jar -o output/grounding -P argumentation/ instances/${instance} rulewerk/${task} filter.lp > log_file
        parsing=$(grep Parsing log_file | cut -d " " -f 8)
        vlog=$(grep VLog log_file | cut -d " " -f 8)
        grounding=$(grep 'Grounding ' log_file | cut -d " " -f 8)
        rulewerk=$(grep Overall log_file | cut -d " " -f 8)

        gringo=$({time gringo argumentation/filter.lp argumentation/all/${task} argumentation/instances/${instance}} |& grep gringo | cut -f 12 -d " ")
        clingo=$({time clingo argumentation/filter.lp argumentation/all/${task} argumentation/instances/${instance}} |& grep '^clingo a' | cut -f 12 -d " ")
        solving=$(clingo argumentation/filter.lp argumentation/all/${task} argumentation/instances/${instance} |& grep '^Time' | tr -s ' ' | cut -f 5 -d " ")

        echo $task '--' $instance '--' $parsing '--' $vlog '--' $grounding '--' $rulewerk '--' $gringo '--' $clingo '--' $solving
    done;

    echo
done;
