#!/usr/local/bin/zsh

class=stable-marriage
format=gg-aspif

folder="../$class/groundings"

sum=0
min=-1
max=-1

for f in $folder/*.$format; do
    line=$(wc -l $f)
    echo $line
    count=${${(z)line}[1]}
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

echo sum: $sum lines
echo max: $min lines
echo min: $max lines

