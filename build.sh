#!/bin/bash
set -e
source ~/.sdkman/bin/sdkman-init.sh
sdk use java 21.0.2-tem
source ~/oss-cad-suite/environment
rm -f *.json *.fs *.log hw/gen/*.v hw/gen/*.vhd
sbt "runMain tang7SegmentSPI.TopLevelVerilog"
# -retime fails to work, -noalu sometimes needed
yosys -DSYNTHESIS -l yosys.log -p "read_verilog -sv hw/gen/*.v; synth_gowin -top TopLevel  -abc9 -json build.yosys.json" 
nextpnr-gowin --placer-heap-timingweight 30 --placer-heap-cell-placement-timeout 0 --enable-globals --enable-auto-longwires --parallel-refine --threads 16 --json build.yosys.json --write build.nextpnr.json --device GW1NR-LV9QN88PC6/I5 --family GW1N-9C --freq 6.75 --timing-allow-fail --cst tangnano9k.cst -l nextpnr.log
gowin_pack -d GW1N-9C -o bitstream.fs build.nextpnr.json
openFPGALoader --verbose --board tangnano9k --bitstream bitstream.fs
