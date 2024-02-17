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

  def generateMAX7219Bitstream(address: Bits, register_data: Bits ) : Bits = {
   return B"0000" ## address ## register_data
  }

  def sendMAX7219DataBit(data: Bits, counter: UInt, slowDownFactor : Int ) : Unit = {
    this.io.spi_out.ss(0) := False
    this.io.spi_out.sclk := counter(slowDownFactor)
    this.io.spi_out.mosi := data.asBools.reverse((counter >> 1+slowDownFactor).resized)
  }

  io.spi_out.mosi.setName("seven_segm_mosi")
  io.spi_out.ss.setName("seven_segm_ss")
  io.spi_out.sclk.setName("seven_segm_sclk")
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
    
    val INITIAL, CONFIGURATION, WAIT, SET_DIGIT = State()
    
    setEntry(INITIAL)

    val counter = Reg(UInt(7 + slowDownFactor bits))
    val configuration_stage = RegInit(U(0, 3 bits))
    val run_stage = RegInit(False)

    counter := counter + 1

    //Scan all digits
    INITIAL.whenIsActive{
        configuration_stage := 0
        run_stage := False
        goto(CONFIGURATION)
    }

    //Display digit 0 only
    CONFIGURATION.onEntry(counter := 0)
    CONFIGURATION.whenIsActive {

      val configuration_address= configuration_stage.mux(
        0 -> B"1011", //Scan limit 
        1 -> B"1001", //Decode mode 
        2 -> B"1010", //Intensity register
        3 -> B"1111", //Display test register 
        4 -> B"1100", //Shutdown register 
        default -> B"0000", //No operation
      )

        val configuration_reg_data = configuration_stage.mux(
        0 -> B"00000111", //Scan limit - display all digits
        1 -> B"11111111", //Decode mode - all digits
        2 -> B"00001111", //Intensity register - full brightness
        3 -> B"00000000", //Display test register - no, normal operation
        4 -> B"00000001", //Shutdown register - no, normal operation
        default -> B"00000000", //Empty
      )

      val configuration_bitstream = generateMAX7219Bitstream(configuration_address, configuration_reg_data) 

      sendMAX7219DataBit(configuration_bitstream, counter, slowDownFactor )

      when(counter === (widthOf(configuration_bitstream)*2 << slowDownFactor)-1) {
        configuration_stage := configuration_stage + U(1).resized
        goto(WAIT)
      }
    }

    WAIT.onEntry(counter := 0)
    WAIT.whenIsActive{
      when(counter === 300) {
        
        when (run_stage === False) {

        when (configuration_stage > 4) {
          run_stage := True
          goto(SET_DIGIT)
        }
        .otherwise {
          goto(CONFIGURATION)
        }

      }
      .otherwise {
        goto(SET_DIGIT)
      }

      }   
    }

    //Dispay number 4 on digit 0
    SET_DIGIT.onEntry(counter := 0)
    SET_DIGIT.whenIsActive {
 
      val bitstream = generateMAX7219Bitstream(B"0001", B"00000100") // Digit 0 - set to 4

      sendMAX7219DataBit(bitstream, counter, slowDownFactor )

      when(counter === (widthOf(bitstream)*2 << slowDownFactor)-1){
        goto(WAIT)
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
