#!/usr/local/bin/zsh

# ----- helper functions -----

# transform the file $1 in asp encoding to file $2 in rulewerk encoding
# if $2 is not given, print the rulewerk encoding to stdout
aspFileToRulewerkFile () {
    if [[ ! -z $2 ]]; then
        sed -E 's/(\,) *([A-Z])/\1?\2/g' $1 | sed -E 's/(\() *([A-Z])/\1?\2/g' >$2
    else
        sed -E 's/(\,) *([A-Z])/\1?\2/g' $1 | sed -E 's/(\() *([A-Z])/\1?\2/g' 
    fi
}

# ----- main part -----
dir=../input
# delete instances, if requested
vared -c -p 'Should the current data (instances only) be deleted? [yn]: ' delete
if [[ $delete = y ]]; then
    rm $dir/**/instances/*(.)
fi
echo

# create rulewerk encodings, if requested
vared -c -p 'Should the encoding be transformed to rulewerk files? [yn]: ' transform
if [[ $transform = y ]]; then
    for file in $dir/**/encodings/*.lp; do
        aspFileToRulewerkFile $file ${file:r}.rlp
    done
fi
echo

# generate asp data
# --- crossword data
vared -c -p 'Should crossword data be created? [yn]: ' cw_create
if [[ $cw_create == y ]]; then
    vared -c -p 'Minimal crossword size: ' cw_start_size
    vared -c -p 'Maximal crossword size: ' cw_end_size

    for size in {$cw_start_size..$cw_end_size}; do
        python3 create-crossword-instance.py $size $dir/crossword/words/words1.txt > $dir/crossword/instances/size$size.lp
    done
fi
echo

# --- abstract argumentation data
vared -c -p 'Should abstract argumentation data be created? [yn]: ' aa_create
echo

# --- graph data
vared -c -p 'Should graph data be created? [yn]: ' gr_create
if [[ $gr_create = y ]]; then
    vared -c -p 'Which graph type should be used? (er - erdos-renyi | pl - power law) ' gr_type

    vared -c -p 'Minimum exponent for vertices number: ' gr_start_exp
    vared -c -p 'Maximum exponent for vertices number: ' gr_end_exp

    vared -c -p 'Minimum edge percentage: ' gr_p_min
    vared -c -p 'Maximum edge percentage: ' gr_p_max
    vared -c -p 'Edge percentage step size: ' gr_p_step

    if [[ $gr_type = pl ]]; then
        vared -ac -p 'Power law exponent: ' pl_k
    fi

    for exp in {$gr_start_exp..$gr_end_exp}; do
        (( vertices = 2 ** exp ))
        for perc in {$gr_p_min..$gr_p_max..$gr_p_step}; do
            (( p = 0.01 * perc))
            typeset -i 10 edges
            (( edges = p * (vertices ** 2)))
            if [[ $gr_type = er ]]; then
                python3 create-graph.py -er $p -f $dir/graphs/instances/er_v${vertices}_e${edges}.lp $vertices
            elif [[ $gr_type = pl ]]; then
                for k in ${(z)pl_k}; do
                    python3 create-graph.py -pl $k -e $edges -f $dir/graphs/instances/pl_v${vertices}_e${edges}_k${k}.lp $vertices
                done
            fi
        done
    done
fi
echo

# generate shuffled data
vared -c -p 'Should shuffled data, too, be generated? [yn]: ' sh_create
if [[ $sh_create == y ]]; then
    if [[ $cw_create == y ]]; then
        for file in $dir/crossword/instances/*.lp; do
            shuf $file > ${file:r}.shuf-lp
        done
    fi
    if [[ $gr_create == y ]]; then
        for file in $dir/graphs/instances/*.lp; do
            shuf $file > ${file:r}.shuf-lp
        done
    fi
fi

