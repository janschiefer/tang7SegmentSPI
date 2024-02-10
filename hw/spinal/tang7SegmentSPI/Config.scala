package tang7SegmentSPI

import spinal.core._
import spinal.core.sim._

object Config {
  def spinal = SpinalConfig(
    targetDirectory = "hw/gen",
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH, // Tang nano initalizes regs only to zero, so positve reset on boot
      resetKind = BOOT
    ),
    defaultClockDomainFrequency = FixedFrequency(27 MHz),
    onlyStdLogicVectorAtTopLevelIo = true
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
