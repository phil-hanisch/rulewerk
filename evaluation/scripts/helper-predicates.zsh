#!/usr/local/bin/zsh

# This script can be used to write facts for helper predicates for ASP encodings,
# thereby encoding needed helper structures, e.g., a successor relation.

# Specify the file where the facts should be written to
vared -c -p "Output file: " file

# Write facts for encoding a successor relation between the integers min and max
vared -c -p "succ? [yn]: " succ
if [[ $succ = y ]]; then
    vared -c -p "min: " min
    vared -c -p "max: " max
    for i in {$min..$((max-1))}; do
        echo "succ($i,$((i+1))) ." >> $file
    done
fi
