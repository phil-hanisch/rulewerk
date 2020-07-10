#!/usr/bin/python
# -*- coding: utf-8 -*-
# Print all solutions encoded in one file clingo output (with one answerset per line)

import sys

filename = sys.argv[1]
gridsize = int(sys.argv[2])

#print("This is the name of the script: ", sys.argv[0])
#print("Number of arguments: ", len(sys.argv))
#print("The arguments are: " , str(sys.argv))

count = 0

fr = open(filename)
for line in fr:
    dic = {}
    facts = line.replace('"', '').replace('^^<http://www.w3.org/2001/XMLSchema#integer>', '').replace('^^<http://www.w3.org/2001/XMLSchema#string>', '').split(' ')
    for fact in facts:
        if fact.startswith("cell("):
            args = fact.replace('cell(','').replace('blank','.').replace(')','').replace('"','').replace(' ','.')
            num, char = args.split(',')
            num = int(num)
            dic[num] = char.strip()
    n = gridsize+2
    if len(dic) == n*n :
        count = count + 1
        print "\nSolution " + str(count) + ":"
        for i in range(n):
            for j in range(n):
                sys.stdout.write(dic[(i*n)+j])
                sys.stdout.write(" ")
            print ""
