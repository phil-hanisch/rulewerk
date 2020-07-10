import sys
import argparse

'''
transform a line of an aspif grounding in a more readable format

@param  line           String           a line in aspif grounding
@param  literalShowMap String -> String a dictionary that maps literal integers (as strings) to literal strings
@return                String           a more readable version of the given line
'''
def transform(line, literalShowMap):
    elements = line.strip().split(" ")
    if elements[0] == "1":
        return transformRuleStatement(elements, literalShowMap)
    elif elements[0] == "4":
        return transformShowStatement(elements)
    else:
        return line

'''
@param  elements       String[]         list of strings that represent a rule statement
@param  literalShowMap String -> String a dictionary that maps literal integers (as strings) to literal strings
@return                String           a more readable version of the rule statement
'''
def transformRuleStatement(elements, literalShowMap):
    ruleType = elements[1]
    headLiteralCount = int(elements[2])
    headLiterals = elements[3:3+headLiteralCount]
    body = elements[3+headLiteralCount:]

    headString = transformDisjunction(headLiterals, literalShowMap) if ruleType == '0' else transformChoice(headLiterals, literalShowMap) if ruleType == '1' else 'Invalid head'
    bodyString = transformBody(body, literalShowMap)

    return headString + (" :- " + bodyString + ' .' if len(bodyString) != 0 else ' .')

'''
@param  headElements   String[]         list of head literals
@param  literalShowMap String -> String a dictionary that maps literal integers (as strings) to literal strings
@return                String           a disjunction of the head literals
'''
def transformDisjunction(headLiterals, literalShowMap):
    return " | ".join([literalShowMap.get(literal, literal) for literal in headLiterals])

'''
@param  headElements   String[]         list of head literals
@param  literalShowMap String -> String a dictionary that maps literal integers (as strings) to literal strings
@return                String           a choice of the head literals
'''
def transformChoice(headLiterals, literalShowMap):
    return "{ " + "; ".join([literalShowMap.get(literal, literal) for literal in headLiterals]) + " }"

'''
@param  body           String[]         list of strings that represents an aspif body
@param  literalShowMap String -> String a dictionary that maps literal integers (as strings) to literal strings
@return                String           a more readable version of the body
'''
def transformBody(body, literalShowMap):
    bodyType = body[0]
    if bodyType == '0':
        # normal body
        return ', '.join([literalShowMap.get(literal, literal) for literal in body[2:]])
    elif bodyType == '1':
        # weigthed body
        lowerBound = body[1] 
        elements = body[3:]
        weightedLiterals = [elements[i+1] + ':' + literalShowMap.get(elements[i], elements[i]) for i in range(0,len(elements),2)]
        return lowerBound + ' <= #count { ' + '; '.join(weightedLiterals) + ' }'
    else:
        return 'Invalid body'

'''
@param  elements String[] list of strings that represents a show statement
@return          String   a more readable version of the show statement
'''
def transformShowStatement(elements):
    return elements[2] + ' :- ' + ', '.join(elements[4:]) + ' .'

'''
create a map that contains those integers of the aspif encoding that can be identified with a literal

@param  sourceFile String           the name of the file containing the aspif encoding
@return            String -> String a dictionary that maps integer (as strings) to literals (as strings)
'''
def createLiteralMap(sourceFile):
    literalShowMap = {}
    with open(sourceFile, 'r') as src:
        for line in src:
            elements = line.strip().split(" ")
            # only consider show statements with exactly one integer
            if elements[0] == '4' and len(elements) == 5 and elements[3] == '1':
                integerString = elements[4]
                literalString = elements[2]
                literalShowMap[integerString] = literalString
    return literalShowMap

'''
make an aspif grounding a little bit more human-friendly
- resolve the following statement types: rule statement and show statement
- reconstruct head and body with a 'typical' logical representation
- optionally, try to re-identify integers with the corresponding literal string
'''
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("source", help="the source file containing the aspif grounding")
    parser.add_argument("-e", "--extended", help="Try to use literals instead of integers in rule statements", action="store_true")
    parser.add_argument("-t", "--target", help="the target file")
    args = parser.parse_args()

    sourceFile = args.source
    literalShowMap = createLiteralMap(sourceFile) if args.extended else {}

    with open(args.source, 'r') as src:
        if args.target:
            with open(args.target, 'w') as target:
                for line in src:
                    print(transform(line, literalShowMap), file=target)
        else:
            for line in src:
                print(transform(line, literalShowMap))

