#!/bin/bash
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 17.0.10-tem
source ~/oss-cad-suite/environment
sbt run
