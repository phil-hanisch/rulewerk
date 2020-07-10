#!/usr/bin/python
# -*- coding: utf-8 -*-

import argparse

def check_positive(value):
    ivalue = int(value)
    if ivalue <= 0:
        raise argparse.ArgumentTypeError("%s is an invalid positive int value" % value)
    return ivalue

parser = argparse.ArgumentParser()
parser.add_argument("size", help="sets the height and width of the crossword", type=check_positive)
parser.add_argument("words", help="file with words to use")
args = parser.parse_args()

sizeH = args.size
sizeV = args.size

words = []
#[""]
for char in range(ord('A'), ord('Z')+1) :
	words.append(chr(char))

f = open(args.words, "r")
wordcount = 0
for line in f:
	line = line.strip()
	if not (all(ord(c) < 128 for c in line)):
		continue
	if any(c.isdigit() for c in line):
		continue
	if len(line)>1 :
		words.append(line.upper())
	wordcount = wordcount + 1
print("% Loaded " + str(wordcount) + " words from file.")

# Prepare word facts
wordidx = 0
for word in words:
	if not (all(ord(c) < 128 for c in word)):
		 continue
	wordIdx = "w" + word
	print("word(" + wordIdx + ",0,blank) .")
	wordpos = 1
	for letter in word:
		print("word(" + wordIdx + "," + str(wordpos) + ",\"" + letter + "\") .")
		wordpos = wordpos + 1
	if wordpos>2:
		print("realWord(" + wordIdx + ") .")
	elif wordpos==2:
		print("unitWord(" + wordIdx + ") .")
	print("word(" + wordIdx + "," + str(wordpos) + ",blank) .")
	wordidx = wordidx + 1

# Prepare grid geometry:
print("")
print("topleft(" + str(sizeV+2) + ") .")
print("")
for i in range(sizeV):
	for j in range(sizeH + 1):
		print("hinner(" + str( (i+1)*(sizeH+2)+j ) + ") .")

print("")
for i in range(sizeV +1):
	for j in range(sizeH):
		print("vinner(" + str( (i)*(sizeH+2)+j+1 ) + ") .")

print("")
for i in range(sizeV):
	for j in range(sizeH + 1):
		for k in range(sizeH - j + 1):
			print("right(" + str(k+1) + "," + str( (i+1)*(sizeH+2)+j ) + "," + str( (i+1)*(sizeH+2)+j+k+1 ) + ") .")

print("")
for i in range(sizeV +1):
	for j in range(sizeH):
		for k in range(sizeV - i + 1):
			print("down(" + str(k+1) + "," + str( (i)*(sizeH+2)+j+1 ) + "," + str( (i+k+1)*(sizeH+2)+j+1 ) + ") .")

# Prepare border:
print("")
for i in range(sizeH + 2):
	print("border(" + str(i) + ",blank) .")
	print("border(" + str(i+ (sizeV+1)*(sizeH+2)) + ",blank) .")
	print("cell(" + str(i) + ",blank) .")
	print("cell(" + str(i+ (sizeV+1)*(sizeH+2)) + ",blank) .")

for i in range(sizeV):
	print("border(" + str((i+1)*(sizeH+2)) + ",blank) .")
	print("border(" + str((i+1)*(sizeH+2)+sizeH+1) + ",blank) .")
	print("cell(" + str(i) + ",blank) .")
	print("cell(" + str(i+ (sizeV+1)*(sizeH+2)) + ",blank) .")

