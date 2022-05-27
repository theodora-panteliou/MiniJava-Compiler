#!/bin/bash

# check main folder java files
# make
FAIL="\e[31m"
SUCCESS="\e[32m"
RESET="\e[0m"

for i in "$@"
do

    make
    java Main ./examples/"$i".java > outputs/"$i".ll
    clang -o out1 outputs/$i.ll
    ./out1 > llvmout.txt
    javac ./examples/"$i".java
    java ./examples/"$i".java > javaout.txt

    diff javaout.txt llvmout.txt
    # output=$(java Main "$i" 2>&1 > $(echo $i.ll))

done