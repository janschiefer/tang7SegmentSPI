package tang7SegmentSPI

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

// Hardware definition
case class DivisionFunction( num_bit_width : Int ) extends Component {

  val io = new Bundle {

    val start = in Bool()
    val divisor = in Bits( num_bit_width bits )
    val dividend = in Bits( num_bit_width bits )
    val quotient = out Bits( num_bit_width bits )
    val remainder = out Bits( num_bit_width bits )
    val busy = out Bool()

  }

    val dd  = RegInit(U(0, num_bit_width bits)) // dividend
    val dr  = RegInit(U(0, num_bit_width bits)) // divisor
    val q  = RegInit(U(0, num_bit_width bits)) // quotient
    val r  = RegInit(U(0, num_bit_width bits)) // remainder
    val bits  = RegInit(U(0, (log2Up(num_bit_width) + 1) bits)) // bits

    val busy_reg  = RegInit(False) // bits

    io.busy := busy_reg

    io.quotient  := q.asBits
    io.remainder := r.asBits

    val fsm = new StateMachine {

        val idle, prepare, shift, sub, done = State()

        setEntry(idle)

        idle.whenIsActive { 

            dd := io.dividend.asUInt
            dr := io.divisor.asUInt

            when(io.start) {
                busy_reg := True
                goto(prepare)
            }
        }

        prepare.whenIsActive { 

            q := 0 
            r := 0
            bits := U(num_bit_width)

            when( dr === U(0).resized ) {  //division by zero
               q := (default -> true)
               r := (default -> true)
               goto(done)
            }
            .elsewhen( dr > dd ) { //divisor > dididend
                r := dd
                goto(done)
            }
            .elsewhen( dr === dd ) { //divisor == dididend
               q := U(1).resized
               goto(done)
            }

            goto(shift)           
        }

        shift.whenIsActive { 
            val tmp = r( (num_bit_width - 2) downto 0 ) ## dd( num_bit_width - 1)
            when ( tmp.asUInt < dr ) {
               bits := bits - U(1).resized;
               r  := tmp.asUInt
               dd := ( dd( (num_bit_width-2) downto 0) ## False ).asUInt
            }
            .otherwise {
               goto(sub)
            }
        
        }

        sub.whenIsActive {

            when( bits > U(0).resized ) {
                val tmp_remainder = (r( (num_bit_width-2) downto 0 ) ## dd(num_bit_width-1)).asUInt
                r  := tmp_remainder
                dd := (dd( (num_bit_width-2) downto 0 ) ## False).asUInt
                //remainder minus divisor
                val diff = tmp_remainder - dr
                when( diff(num_bit_width - 1) === False ) { // No underflow
                  q := (q((num_bit_width - 2) downto 0) ## True).asUInt // slide 1 into result
                  r := diff
                }
                .otherwise { //Underflow
                  q := (q((num_bit_width - 2) downto 0) ## False).asUInt  // slide 0 in and continue to claculate with old value
                }
                bits := bits - U(1).resized;
            }
            .otherwise {
                goto(done)
            }

        }

        done.whenIsActive {

           busy_reg := False
           when ( io.start === False ) {
               goto(idle) 
           }

        }


    }


}

object DivisionFunction {}

/* 

architecture Behave_Unsigned of Divider is
signal dd : unsigned(b-1 downto 0); -- dividend
signal dr : unsigned(b-1 downto 0); -- divisor
signal q  : unsigned(b-1 downto 0); -- qoutient
signal r  : unsigned(b-1 downto 0); -- remainder
signal bits : integer range b downto 0;
type zustaende is (idle, prepare, shift, sub, done);
signal z : zustaende;

begin

   process 
   variable diff : unsigned(b-1 downto 0);
   begin
      wait until rising_edge(clk);
      case z is 
         when idle => 
            if (start='1') then 
               z <= prepare; 
               busy <= '1';
            end if;
            dd <= unsigned(dividend);
            dr <= unsigned(divisor);

         when prepare =>
            q    <= (others=>'0');
            r    <= (others=>'0');
            z    <= shift;            
            bits <= b;
            -- Sonderfall: Division durch Null
            if (dr=0) then  
               q <= (others=>'1');
               r <= (others=>'1');
               z <= done;
            -- Sonderfall: Divisor größer als Dividend
            elsif (dr>dd) then 
               r <= dd;
               z <= done;
            -- Sonderfall: Divisor gleich Dividend
            elsif (dr=dd) then
               q <= to_unsigned(1,b);
               z <= done;
            end if;

         when shift =>
            -- erst mal die beiden Operanden 
            -- für die Subtraktion zurechtrücken
            if ( (r(b-2 downto 0)&dd(b-1)) < dr ) then
               bits <= bits-1;
               r    <= r(b-2 downto 0)&dd(b-1);
               dd   <= dd(b-2 downto 0)&'0';
            else
               z    <= sub;
            end if;

         when sub =>
            if (bits>0) then
               r    <= r(b-2 downto 0)&dd(b-1);
               dd   <= dd(b-2 downto 0)&'0';
               -- Rest minus Divisor
               diff := (r(b-2 downto 0)&dd(b-1)) - dr;  
               if (diff(b-1)='0') then                 
                  -- wenn kein Unterlauf 
                  --> Divisor passt noch rein 
                  --> MSB=0 --> 1 in Ergebnis einschieben
                  q <= q(b-2 downto 0) & '1';
                  r <= diff;
               else
                  -- wenn Unterlauf 
                  --> 0 einschieben, mit altem Wert weiterrechnen
                  q <= q(b-2 downto 0) & '0';
               end if;
               bits <= bits-1;
            else
               z    <= done;
            end if;
            
         when done =>
            busy <= '0';
            -- Handshake: wenn nötig warten, bis start='0'
            if (start='0') then 
               z <= idle; 
            end if;
      end case;
   end process;
   
   quotient  <= std_logic_vector(q);
   remainder <= std_logic_vector(r);
   
end;

 */


