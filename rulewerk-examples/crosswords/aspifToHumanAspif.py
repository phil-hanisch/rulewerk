import sys
import argparse

# @param line String a line in aspif grounding
# @return String a more readable version of the given line
def transform(line):
    elements = line.strip().split(" ")
    if elements[0] == "1":
        return transformRuleStatement(elements)
    elif elements[0] == "4":
        return transformShowStatement(elements)
    else:
        return line

# @param elements String[] list of strings that represent a rule statement
# @return String a more readable version of the line
def transformRuleStatement(elements):
    ruleType = elements[1]
    headLiteralCount = int(elements[2])
    headLiterals = elements[3:3+headLiteralCount]
    body = elements[3+headLiteralCount:]

    headString = transformDisjunction(headLiterals) if ruleType == '0' else transformChoice(headLiterals) if ruleType == '1' else 'Invalid head'
    bodyString = transformBody(body)

    return headString + (" :- " + bodyString + ' .' if len(bodyString) != 0 else ' .')

# @param headElements String[] list of head literals
# @return String a disjunction of the head literals
def transformDisjunction(headLiterals):
    return " | ".join(headLiterals)

# @param headElements String[] list of head literals
# @return String a choice of the head literals
def transformChoice(headLiterals):
    return "{ " + "; ".join(headLiterals) + " }"

# @param body String[] list of strings that represents an aspif body
# @return String a more readable version of the body
def transformBody(body):
    bodyType = body[0]
    if bodyType == '0':
        # normal body
        return ', '.join(body[2:])
    elif bodyType == '1':
        # weigthed body
        lowerBound = body[1] 
        elements = body[3:]
        weightedLiterals = [elements[i+1] + ':' + elements[i] for i in range(0,len(elements),2)]
        return lowerBound + ' <= #count { ' + '; '.join(weightedLiterals) + ' }'
    else:
        return 'Invalid body'

# @param elements String[] list of strings that represents a show statement
# @return String a more readable version of the show statement
def transformShowStatement(elements):
    return elements[2] + ' :- ' + ', '.join(elements[4:]) + ' .'

# make an aspif grounding a little bit more human-friendly
if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("source", help="the source file containing the aspif grounding")
    # parser.add_argument("-e", "--extended", help="Try to use literals instead of integers in rule statements", action="store_true")
    parser.add_argument("-t", "--target", help="the target file")
    args = parser.parse_args()

    with open(args.source, 'r') as src:
        if args.target:
            with open(args.target, 'w') as target:
                for line in src:
                    print(transform(line), file=target)
        else:
            for line in src:
                print(transform(line))

