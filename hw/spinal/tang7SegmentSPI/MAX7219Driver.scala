package tang7SegmentSPI

import spinal.core._
import spinal.lib.fsm._
import spinal.lib.com.spi.SpiMaster
import spinal.lib._

// Hardware definition
case class MAX7219Driver() extends Component {

  val io = new Bundle {
    val spi_out = master(SpiMaster())
  }

  def generateMAX7219Bitstream(address: Bits, register_data: Bits): Bits = {
    return B"0000" ## address ## register_data
  }

  def sendMAX7219DataBit(data: Bits, counter: UInt, slowDownFactor: Int): Unit = {
    this.io.spi_out.ss(0) := False
    this.io.spi_out.sclk := counter(slowDownFactor)
    this.io.spi_out.mosi := data.asBools.reverse((counter >> 1 + slowDownFactor).resized)
  }

  def generateMAX7219DigitNumberBitstream(digit: UInt, number: UInt, decimal_point: Bool): Bits = {

    val current_digit_address = digit.mux(
      0 -> B"0001", // Digit 0
      1 -> B"0010", // Digit 1
      2 -> B"0011", // Digit 2
      3 -> B"0100", // Digit 3
      4 -> B"0101", // Digit 4
      5 -> B"0110", // Digit 5
      6 -> B"0111", // Digit 6
      7 -> B"1000" // Digit 7
    )

    val number_bits = number.mux(
      0 -> B"0000", // Number 0
      1 -> B"0001", // Number 1
      2 -> B"0010", // Number 2
      3 -> B"0011", // Number 3
      4 -> B"0100", // Number 4
      5 -> B"0101", // Number 5
      6 -> B"0110", // Number 6
      7 -> B"0111", // Number 7
      8 -> B"1000", // Number 8
      9 -> B"1001", // Number 9
      default -> B"1111" // Blank
    )

    val register_data_number = decimal_point ## B"000" ## number_bits

    return generateMAX7219Bitstream(current_digit_address, register_data_number) // Digit 0 - set to 4

  }

  val slowDownFactor: Int = 4

  val test_number = U(581108);

  io.spi_out.mosi := False
  io.spi_out.ss(0) := True
  io.spi_out.sclk := False

  // put code here

  val fsm = new StateMachine {

    val INITIAL, CONFIGURATION, CALCULATE_DIGIT, SET_DIGIT, LOOP_FOREVER = State()
    val WAIT = new StateDelay(120)

    setEntry(INITIAL)

    val division_module = DivisionFunction( 16 )

    val counter = Reg(UInt(7 + slowDownFactor bits))
    val configuration_stage = RegInit(U(0, 3 bits))
    val run_stage = RegInit(False)
    val current_digit = RegInit(U(0, 3 bits))
    val current_number = RegInit(U(0, 4 bits))
    val tmp_number = RegInit(U(0,27 bits))

    counter := counter + 1

    // Scan all digits
    INITIAL.whenIsActive {
      counter := 0
      configuration_stage := 0
      run_stage := False
      current_digit := 0
      current_number := 0
      tmp_number := 0

      goto(CONFIGURATION)
    }

    // Display digit 0 only
    CONFIGURATION.onEntry(counter := 0)
    CONFIGURATION.whenIsActive {

      val configuration_address = configuration_stage.mux(
        0 -> B"1011", // Scan limit
        1 -> B"1001", // Decode mode
        2 -> B"1010", // Intensity register
        3 -> B"1111", // Display test register
        4 -> B"1100", // Shutdown register
        default -> B"0000" // No operation
      )

      val configuration_reg_data = configuration_stage.mux(
        0 -> B"00000111", // Scan limit - display all digits
        1 -> B"11111111", // Decode mode - all digits
        2 -> B"00001111", // Intensity register - full brightness
        3 -> B"00000000", // Display test register - no, normal operation
        4 -> B"00000001", // Shutdown register - no, normal operation
        default -> B"00000000" // Empty
      )

      val bitstream = generateMAX7219Bitstream(configuration_address, configuration_reg_data)

      sendMAX7219DataBit(bitstream, counter, slowDownFactor)

      when(counter === (widthOf(bitstream) * 2 << slowDownFactor) - 1) {
        configuration_stage := configuration_stage + U(1).resized
        goto(WAIT)
      }
    }

    WAIT.whenCompleted {

      when(run_stage === False) {

        when(configuration_stage > 4) {
          run_stage := True
          tmp_number := test_number.resized
          current_number := 0
          goto(CALCULATE_DIGIT)
        }
          .otherwise {
            goto(CONFIGURATION)
          }

      }
        .otherwise {  
          goto(CALCULATE_DIGIT)
        }

    }

    CALCULATE_DIGIT.whenIsActive {
      
      when( tmp_number =/= U(0).resized) {
        current_number := (tmp_number % U(10).resized).resized
        tmp_number := (tmp_number / U(10).resized).resized
      }
      .otherwise {
        current_number := U(10).resized // Number > 9 = Blank
      }
        goto(SET_DIGIT)
    }

    SET_DIGIT.onEntry(counter := 0)
    SET_DIGIT.whenIsActive {

      val bitstream = generateMAX7219DigitNumberBitstream(current_digit, current_number, False)

      sendMAX7219DataBit(bitstream, counter, slowDownFactor)

      when(counter === (widthOf(bitstream) * 2 << slowDownFactor) - 1) {

        when(current_digit <= U(6).resized) {
          current_digit := current_digit + U(1).resized
          goto(WAIT)

        }.otherwise {
          goto(LOOP_FOREVER)
        }

      }
    }

    LOOP_FOREVER.whenIsActive {
      goto(LOOP_FOREVER)
    }


  }

}

object MAX7219Driver {}
