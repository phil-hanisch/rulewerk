#!/usr/local/bin/zsh

vared -c -p "Output file: " file
vared -c -p "succ? [yn]: " succ
if [[ $succ = y ]]; then
    vared -c -p "min: " min
    vared -c -p "max: " max
    for i in {$min..$((max-1))}; do
        echo "succ($i,$((i+1))) ." >> $file
    done
fi
