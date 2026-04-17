#!/bin/bash

# Compile all Java source files into the 'bin' directory.
# -d bin: specifies the output directory for compiled .class files
# natty/**/*.java: compiles all Java files in subdirectories (e.g., components/)
# natty/*.java: compiles top-level Java files (e.g., Nat.java)
javac -d bin natty/**/*.java natty/*.java

# Run the NAT program
# -cp bin: sets the classpath to the compiled classes in 'bin', natty.Nat: main class
# Arguments:
# Use 192.0.2.1 as the logical source IP address for outbound packets.
# Use ports 1–5 as the pool of logical external source ports.
# Expire translation entries that have been idle for 30 seconds.
# Fragment outbound packets larger than 576 bytes (including all logical headers).
# Listen on UDP port 57715 for internal traffic on 127.0.0.1.
# Forward outbound packets to UDP port 60893 on 127.0.0.1.
java -cp bin natty.Nat 192.0.2.1 5 30 576 57714 60893