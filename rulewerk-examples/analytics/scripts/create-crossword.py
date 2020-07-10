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
parser.add_argument("--rulewerk", help="use rulewerk syntax for the output", action="store_true")
parser.add_argument("--words", help="file with words to use", default="words1.txt")
parser.add_argument("--no-show", help="hide the show statement", action="store_true")
args = parser.parse_args()

sizeH = args.size
sizeV = args.size
rulewerk = args.rulewerk

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

# Fixed rules for guessing words
print("")
program = """
% Helper predicate for equality
equals(L,L) :- word(Var1, Var2, L) .
equals(C,C) :- border(C, Var) .
equals(C,C) :- vinner(C) .

cell(C,L) :- border(C,L) .

% Transcribe words horizontally into the grid:
cell(Cr,L) :- hstart(C,W), word(W,P,L), right(P,C,Cr) .

% Require horizontal words
% -- 1{ hstart(C,W) : word(W,0,_) }1 :- cell(C,blank), hinner(C) .
1 { hstart(C,W) : word(W,0,Anonym) } 1 :- cell(C,blank), hinner(C) .

% Transcribe words vertically into the grid:
cell(Cr,L) :- vstart(C,W), word(W,P,L), down(P,C,Cr) .

% Require vertical words
% -- 1{ vstart(C,W) : word(W,0,_) }1 :- cell(C,blank), vinner(C) .
1 { vstart(C,W) : word(W,0,Anonym) } 1 :- cell(C,blank), vinner(C) .

% Record some word usage metrics for constraints:
used(W,C) :- vstart(C,W), realWord(W) .
used(W,C) :- hstart(C,W), realWord(W) .
vunit(Cd) :- vstart(C,W), unitWord(W), down(1,C,Cd) .
hunit(Cr) :- hstart(C,W), unitWord(W), right(1,C,Cr) .

% Ensure reachability:
reachable(C) :- topleft(C) .
reachable(Cr) :- reachable(C), right(1,C,Cr), cell(Cr,L), not equals(L,blank) .
reachable(Cl) :- reachable(C), right(1,Cl,C), cell(Cl,L), not equals(L,blank) .
reachable(Cd) :- reachable(C), down(1,C,Cd), cell(Cd,L), not equals(L,blank) .
reachable(Cu) :- reachable(C), down(1,Cu,C), cell(Cu,L), not equals(L,blank) .

% Constraints
 % no conflicting letters in inner cells and on margin:
 :- cell(C,L), cell(C,M), not equals(L,M), vinner(C), hinner(C) .
 :- cell(C,L), cell(C,blank), not equals(L,blank) .
 % no blank right after the empty word:
 :- hstart(C,w), right(2,C,Cr), cell(Cr,blank) .
 :- vstart(C,w), down(2,C,Cd), cell(Cd,blank) .
 % no crossing unit words:
 :- vunit(C), hunit(C) .
 % no duplicated words:
 :- used(W,C1), used(W,C2), not equals(C1,C2) .
 :- vstart(C,W), hstart(C,W), realWord(W) .
 % all letters reachable from start:
 :- cell(C,L), not equals(L,blank), not reachable(C) .
"""

rulewerkProgram = """
% Helper predicate for equality
equals(?L,?L) :- word(?Var1, ?Var2, ?L) .
equals(?C,?C) :- border(?C, ?Var) .
equals(?C,?C) :- vinner(?C) .

cell(?C,?L) :- border(?C,?L) .

% Transcribe words horizontally into the grid:
cell(?Cr,?L) :- hstart(?C,?W), word(?W,?P,?L), right(?P,?C,?Cr) .

% Require horizontal words
1 { hstart(?C,?W) : word(?W,0,?Anonym) } 1 :- cell(?C,blank), hinner(?C) .

% Transcribe words vertically into the grid:
cell(?Cr,?L) :- vstart(?C,?W), word(?W,?P,?L), down(?P,?C,?Cr) .

% Require vertical words
1 { vstart(?C,?W) : word(?W,0,?Anonym) } 1 :- cell(?C,blank), vinner(?C) .

% Record some word usage metrics for constraints:
used(?W,?C) :- vstart(?C,?W), realWord(?W) .
used(?W,?C) :- hstart(?C,?W), realWord(?W) .
vunit(?Cd) :- vstart(?C,?W), unitWord(?W), down(1,?C,?Cd) .
hunit(?Cr) :- hstart(?C,?W), unitWord(?W), right(1,?C,?Cr) .

% Ensure reachability:
reachable(?C) :- topleft(?C) .
reachable(?Cr) :- reachable(?C), right(1,?C,?Cr), cell(?Cr,?L), not equals(?L,blank) .
reachable(?Cl) :- reachable(?C), right(1,?Cl,?C), cell(?Cl,?L), not equals(?L,blank) .
reachable(?Cd) :- reachable(?C), down(1,?C,?Cd), cell(?Cd,?L), not equals(?L,blank) .
reachable(?Cu) :- reachable(?C), down(1,?Cu,?C), cell(?Cu,?L), not equals(?L,blank) .

% ?Constraints
 % no conflicting letters in inner cells and on margin:
 :- cell(?C,?L), cell(?C,?M), not equals(?L,?M), vinner(?C), hinner(?C) .
 :- cell(?C,?L), cell(?C,blank), not equals(?L,blank) .
 % no blank right after the empty word:
 :- hstart(?C,w), right(2,?C,?Cr), cell(?Cr,blank) .
 :- vstart(?C,w), down(2,?C,?Cd), cell(?Cd,blank) .
 % no crossing unit words:
 :- vunit(?C), hunit(?C) .
 % no duplicated words:
 :- used(?W,?C1), used(?W,?C2), not equals(?C1,?C2) .
 :- vstart(?C,?W), hstart(?C,?W), realWord(?W) .
 % all letters reachable from start:
 :- cell(?C,?L), not equals(?L,blank), not reachable(?C) .
"""

if rulewerk:
    print(rulewerkProgram)
else:
    print(program)

if not args.no_show:
    print("#show cell/2 .")
