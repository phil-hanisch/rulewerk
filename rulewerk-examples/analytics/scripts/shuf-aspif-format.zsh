#!/usr/local/bin/zsh

echo 'asp 1 0 0' > $2

lines=${$(wc -l $1)[1]}
tail -n +2 ${1} | head -n $((lines-2)) | shuf >> $2

echo '0' >> $2
