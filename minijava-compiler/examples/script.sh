#!/bin/bash

# check main folder java files
# make
FAIL="\e[31m"
SUCCESS="\e[32m"
RESET="\e[0m"

for i in ./examples/*.java
do
    # echo "$i"
    output=$(java Main "$i" 2>&1 > tempoffsets.txt)
    # echo "$output"
    if [[ "$i" == *"-error"* && "$output" == *"semantically correct"* ]]; then
        echo -e "${FAIL}$i failed${RESET}"
    
    elif [[ "$i" != *"-error"* && "$output" != *"semantically correct"* ]]; then
        echo -e "${FAIL}$i failed${RESET}"
    
    elif [[ "$i" != *"-error"* && "$output" == *"semantically correct"* ]]; then # also check offsets
        # find offset file
        name=${i//\.\/examples/\.\/examples\/offset-examples} 
        name=${name//\.java/\.txt}
        if [[ $(diff -wB $name tempoffsets.txt) != "" ]]; then
            echo -e "${FAIL}$i failed pn offsets${RESET}"
        else
            echo -e "${SUCCESS}$i passed${RESET}"
        fi
    
    else
        echo -e "${SUCCESS}$i passed${RESET}"
    fi
done

for i in ./examples/minijava-error-extra/*.java
do
    # echo "$i"
    output=$(java Main "$i" 2>&1 > /dev/null)
    # echo "$output"
    if [[ "$output" == *"semantically correct"* ]]; then
        echo -e "${FAIL}$i failed${RESET}"
    else 
        echo -e "${SUCCESS}$i passed${RESET}"
    fi
done

for i in ./examples/minijava-extra/*.java
do
    # echo -e "$i"
    output=$(java Main "$i" 2>&1 > /dev/null)
    # echo -e "$output"
    if [[ "$output" != *"semantically correct"* ]]; then
        echo -e "${FAIL} $i failed${RESET}"
    else 
        echo -e "${SUCCESS} $i passed${RESET}"
    fi
done
