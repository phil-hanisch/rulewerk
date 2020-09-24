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

aspFileToRulewerkFile $1 $2
