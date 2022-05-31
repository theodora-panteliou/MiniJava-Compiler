#!/bin/bash

# check main folder java files
# make
FAIL="\e[31m"
SUCCESS="\e[32m"
RESET="\e[0m"

make
# search_dir="./examples/minijava-extra/"
search_dir="./examples/"
# for search_dir in ["./examples/" "./examples/minijava-extra/"]
# do
    for i in "$search_dir"*
    do  
    # i="LinearSearch.java"
        if [[ "$i" == *".java" && "$i" != *"error"* ]]; then
            # echo "$i"
            name=${i//${search_dir}/outputs/} 
            name=${name//\.java/\.ll}
            # echo "$name"
            java Main "$i" > "$name"
            clang -o out1 "$name"
            ./out1 > llvmout.txt
            out="$(javac "$i" 2>&1 > javaerror)"
            if [ "$(javac "$i" 2>&1 > javaerror)" ]; then
                echo -e "${FAIL}java compiler not compiling with error $out${RESET}"
                # continue
            fi
            java "$i" > javaout.txt 
            diff javaout.txt llvmout.txt

            if [[ "$(diff javaout.txt llvmout.txt)" && "$i" != *"OutOfBounds"* ]]; then
                rm -f ${search_dir}*.class
                echo -e "${FAIL}failed in $i ${RESET}"
                exit
            fi
            if [[ "$(diff javaout.txt llvmout.txt)" != "" && "$i" != *"OutOfBounds"* ]]; then
                rm -f ${search_dir}*.class
                echo -e "${FAIL}failed in $i ${RESET}"
                exit
            fi
            # output=$(java Main "$i" 2>&1 > $(echo $i.ll))
        fi

    done

    rm -f ${search_dir}*.class
# done