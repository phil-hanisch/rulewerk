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

#show cell/2 .
