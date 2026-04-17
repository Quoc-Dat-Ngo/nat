#!/bin/bash

cd ../
javac -d bin natty/**/*.java natty/*.java
java -cp bin natty.Nat 192.0.2.1 5 30 576 57713 60893