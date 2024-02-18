#!/bin/bash
source ~/.sdkman/bin/sdkman-init.sh
sdk use java java 21.0.2-tem
source ~/oss-cad-suite/environment
sbt "runMain tang7SegmentSPI.TopLevelSim"
gtkwave simulation.gtkw&
