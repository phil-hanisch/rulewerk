#!/usr/local/bin/zsh

class=stable-marriage
format=gg-aspif

folder="../$class/groundings"

sum=0
min=-1
max=-1

for f in $folder/*.$format; do
    line=$(ls -l $f)
    echo $line
    count=${${(z)line}[5]}
    ((sum = sum + count))
    if [[ $min = -1 || $count -gt $min ]]; then
        ((min = count))
    fi
    if [[ $max = -1 || $count -lt $max ]]; then
        ((max = count))
    fi
    echo $count $min $max $sum
    echo
done

echo sum: $sum B
echo max: $min B
echo min: $max B

