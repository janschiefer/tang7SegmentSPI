// MAX7219 7 segment display driver
package tang7SegmentSPI

import spinal.core._
import spinal.lib.com.spi.SpiMaster
import spinal.lib._

// Hardware definition
case class TopLevel() extends Component {

  val io = new Bundle {
    val spi_out = master(SpiMaster())

    val rst = in Bool ()
  }

  io.spi_out.mosi.setName("seven_segm_mosi")
  io.spi_out.ss.setName("seven_segm_ss")
  io.spi_out.sclk.setName("seven_segm_sclk")
  io.rst.setName("rst")

  val buffered_ext_resetn =
    BufferCC(input = io.rst, init = True, bufferDepth = 2)

  val timed_reset_reg_n = Reg(Bool) init (False)
  val reset_timeout = Timeout(10 ms)

  when(reset_timeout) {
    timed_reset_reg_n := True
  }

  /*
  val clk_div_counter = Counter(3)
  val divided_clock = RegInit(False)

  clk_div_counter.increment()

  when(clk_div_counter.willOverflow) {
    divided_clock := !divided_clock
  }
  */

  val divided_clock = ClockDomain.current.clock

  val main_clock_domain = ClockDomain(
    clock = divided_clock,
    reset = timed_reset_reg_n,
    softReset = buffered_ext_resetn,
    config = ClockDomainConfig(
      clockEdge = RISING,
      resetKind = SYNC,
      resetActiveLevel = LOW,
      softResetActiveLevel = LOW
    ),
    //frequency = FixedFrequency(ClockDomain.current.frequency.getValue / 4)
    frequency = FixedFrequency(ClockDomain.current.frequency.getValue)
  )

  main_clock_domain.setSynchronousWith(ClockDomain.current)

  val main_clock_area = new ClockingArea(main_clock_domain) {

    val driver = MAX7219Driver()

    driver.io.spi_out <> io.spi_out

  }

}

object TopLevelVerilog extends App {
  Config.spinal.generateVerilog(TopLevel())
}

object TopLevelVhdl extends App {
  Config.spinal.generateVhdl(TopLevel())
}
