// MAX7219 7 segment display driver
package tang7SegmentSPI

import spinal.core._
import spinal.lib.fsm._
import spinal.lib.com.spi.SpiMaster
import spinal.lib._


// Hardware definition
case class TopLevel() extends Component {
  val io = new Bundle {
    val spi_out = master(SpiMaster())

    val rst = in Bool ()
  }
  io.spi_out.mosi.setName("mosi")
  io.spi_out.ss.setName("ss")
  io.spi_out.sclk.setName("sclk")
  io.rst.setName("rst")
  
  val buffered_ext_resetn = BufferCC(input = io.rst, init = True, bufferDepth = 2)

  val timed_reset_reg_n = Reg(Bool) init (False)
  val reset_timeout = Timeout(10 ms)

  val slowDownFactor : Int = 4 

  when(reset_timeout) {
    timed_reset_reg_n := True
  }

  io.spi_out.mosi := False
 	io.spi_out.ss(0) := True
 	io.spi_out.sclk := False

  val main_clock_domain = ClockDomain(
    clock = ClockDomain.current.readClockWire,
    reset = timed_reset_reg_n,
    softReset = buffered_ext_resetn,
    config = ClockDomainConfig(
      clockEdge = RISING,
      resetKind = SYNC,
      resetActiveLevel = LOW,
      softResetActiveLevel = LOW
    ),
    frequency = FixedFrequency(ClockDomain.current.frequency.getValue)
  )

  main_clock_domain.setSynchronousWith(ClockDomain.current)

  val main_clock_area = new ClockingArea(main_clock_domain) {

 	//put code here
 	


    val fsm = new StateMachine{ 
    
    val INIT, SCAN_LIMIT, SET_DECODE, SET_INTENSITY, SET_NOTEST, SET_NOSHUTDOWN, SET_DIGIT_ZERO, IDLE1,IDLE2,IDLE3,IDLE4,IDLE5,IDLE6 = State()
    
    setEntry(INIT)

    val counter = Reg(UInt(7 + slowDownFactor bits))
    counter := counter + 1

    //Scan all digits
    INIT.whenIsActive{
        goto(SCAN_LIMIT)
    }

    //Display digit 0 only
    SCAN_LIMIT.onEntry(counter := 0)
    SCAN_LIMIT.whenIsActive{
      io.spi_out.ss(0) := False
      io.spi_out.sclk := counter(slowDownFactor)
      val bitstream = B"0000" ## B"1011" ## B"00000000"
      io.spi_out.mosi := bitstream.asBools.reverse((counter >> 1+slowDownFactor).resized)
      when(counter === (widthOf(bitstream)*2 << slowDownFactor)-1){
        goto(IDLE1)
      }
    }

    IDLE1.onEntry(counter := 0)
    IDLE1.whenIsActive{
      when(counter === 300){
        goto(SET_DECODE)
      }   
    }

    //Decode all digits
    SET_DECODE.onEntry(counter := 0)
    SET_DECODE.whenIsActive{
      io.spi_out.ss(0) := False
      io.spi_out.sclk := counter(slowDownFactor)
      val bitstream = B"0000" ## B"1001" ## B"11111111"
      io.spi_out.mosi := bitstream.asBools.reverse((counter >> 1+slowDownFactor).resized)
      when(counter === (widthOf(bitstream)*2 << slowDownFactor)-1){
        goto(IDLE2)
      }
    }

    IDLE2.onEntry(counter := 0)
    IDLE2.whenIsActive{
      when(counter === 300){
        goto(SET_INTENSITY)
      }   
    }

    //Full brightness
    SET_INTENSITY.onEntry(counter := 0)
    SET_INTENSITY.whenIsActive{
      io.spi_out.ss(0) := False
      io.spi_out.sclk := counter(slowDownFactor)
      val bitstream = B"0000" ## B"1010" ## B"00001111"
      io.spi_out.mosi := bitstream.asBools.reverse((counter >> 1+slowDownFactor).resized)
      when(counter === (widthOf(bitstream)*2 << slowDownFactor)-1){
        goto(IDLE3)
      }
    }

    IDLE3.onEntry(counter := 0)
    IDLE3.whenIsActive{
      when(counter === 300){
        goto(SET_NOTEST)
      }   
    }

    //NOT test mode
    SET_NOTEST.onEntry(counter := 0)
    SET_NOTEST.whenIsActive{
      io.spi_out.ss(0) := False
      io.spi_out.sclk := counter(slowDownFactor)
      val bitstream = B"0000" ## B"1111" ## B"00000000"
      io.spi_out.mosi := bitstream.asBools.reverse((counter >> 1+slowDownFactor).resized)
      when(counter === (widthOf(bitstream)*2 << slowDownFactor)-1){
        goto(IDLE4)
      }
    }

    IDLE4.onEntry(counter := 0)
    IDLE4.whenIsActive{
      when(counter === 300){
        goto(SET_NOSHUTDOWN)
      }   
    }

    //NOT shutdown mode
    SET_NOSHUTDOWN.onEntry(counter := 0)
    SET_NOSHUTDOWN.whenIsActive{
      io.spi_out.ss(0) := False
      io.spi_out.sclk := counter(slowDownFactor)
      val bitstream = B"0000" ## B"1100" ## B"00000001"
      io.spi_out.mosi := bitstream.asBools.reverse((counter >> 1+slowDownFactor).resized)
      when(counter === (widthOf(bitstream)*2 << slowDownFactor)-1){
        goto(IDLE5)
      }
    }

    IDLE5.onEntry(counter := 0)
    IDLE5.whenIsActive{
      when(counter === 300){
        goto(SET_DIGIT_ZERO)
      }   
    }

    //Dispay number 4 on digit 0
    SET_DIGIT_ZERO.onEntry(counter := 0)
    SET_DIGIT_ZERO.whenIsActive{
      io.spi_out.ss(0) := False
      io.spi_out.sclk := counter(slowDownFactor)
      val bitstream = B"0000" ## B"0001" ## B"00000100"
      io.spi_out.mosi := bitstream.asBools.reverse((counter >> 1+slowDownFactor).resized)
      when(counter === (widthOf(bitstream)*2 << slowDownFactor)-1){
        goto(IDLE6)
      }
    }

    IDLE6.onEntry(counter := 0)
    IDLE6.whenIsActive{
      when(counter === 300){
        goto(SCAN_LIMIT)
      }   
    }
    
   

  }

  }

}

object TopLevelVerilog extends App {
  Config.spinal.generateVerilog(TopLevel())
}

object TopLevelVhdl extends App {
  Config.spinal.generateVhdl(TopLevel())
}
