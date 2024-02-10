package tang7SegmentSPI

import spinal.core._
import spinal.core.sim._

object TopLevelSim extends App {

  val spinalSimConfig = SpinalConfig(defaultClockDomainFrequency = FixedFrequency(270 Hz))

  SimConfig.withConfig(spinalSimConfig).withVcdWave.allOptimisation.compile(TopLevel()).doSim { dut =>
    // Fork a process to generate the reset and the clock on the dut

    dut.io.rst #= true

    dut.clockDomain.forkStimulus(period = 5)

    var modelState = 0
    for (idx <- 0 to 270000) {
      // Wait a rising edge on the clock
      dut.clockDomain.waitRisingEdge()

    }

  }
}
