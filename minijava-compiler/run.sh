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
        if [[ "$i" == *".java" && "$i" != *"error"* ]]; then
            # echo "$i"
            name=${i//${search_dir}/outputs/} 
            name=${name//\.java/\.ll}
            echo "$name"
            java Main "$i" > "$name"
            clang -o out1 "$name"
            ./out1 > llvmout.txt
            javac "$i"
            java "$i" > javaout.txt 
            diff javaout.txt llvmout.txt

            # if [ "$(diff javaout.txt llvmout.txt)" ]; then
            #     rm -f ${search_dir}*.class
            #     echo  "${FAIL} failed in $i ${RESET}"
            #     exit
            # fi
            # output=$(java Main "$i" 2>&1 > $(echo $i.ll))
        fi

    done

    rm -f ${search_dir}*.class
# done