#!/usr/local/bin/zsh

# This script shuffles the statement of a grounding in aspif.
# It ensures that the first and last line stay at that positions.

echo 'asp 1 0 0' > $2

lines=${$(wc -l $1)[1]}
tail -n +2 ${1} | head -n $((lines-2)) | shuf >> $2

echo '0' >> $2
