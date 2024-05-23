package booth

import chisel3._
import chisel3.util._

class Booth(val width: Int) extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val x     = Input(SInt(width.W))
    val y     = Input(SInt(width.W))
    val z     = Output(SInt((2 * width).W))
    val busy  = Output(Bool())
  })

  // State definitions
  val sIdle :: sCompute :: Nil = Enum(2)
  val state                    = RegInit(sIdle)

  val busy = RegInit(false.B)
  io.busy := busy

  val _x = RegInit(0.S(width.W))
  val _y = RegInit(0.S(width.W))
  val _z = RegInit(0.S((2 * width).W))
  // Output connections
  io.z := _z.asSInt

  // Counter for shifts
  val cnt = Counter(width)

  // printf("---------- cnt=%d ----------\n", cnt.value)
  // printf("_z: %b\n", _z)

  // State transition logic
  switch(state) {
    is(sIdle) {
      when(io.start) {
        val _q = Cat(io.y(0), 0.U(1.W))
        switch(_q) {
          is("b00".U, "b11".U) {
            _z := _z.asSInt >> 1
          }
          is("b01".U) {
            _z := Cat(io.x.asUInt, io.y.asUInt).asSInt >> 1
          }
          is("b10".U) {
            _z := Cat((-io.x).asUInt, io.y.asUInt).asSInt >> 1
          }
        }

        /* ---------- 状态 ---------- */
        busy  := true.B
        state := sCompute

        /* ---------- init ---------- */
        _x := io.x
        _y := io.y
      }
    }
    is(sCompute) {
      when(cnt.inc()) { // Increment the counter and check if it has reached its maximum

        /* ---------- 状态 ---------- */
        state := sIdle
        busy  := false.B
      }.otherwise {
        val _q = Cat(_y(cnt.value + 1.U), _y(cnt.value))
        switch(_q) {
          is("b00".U, "b11".U) {
            _z := _z.asSInt >> 1
          }
          is("b01".U) {
            _z := (_z + (_x << width)).asSInt >> 1
          }
          is("b10".U) {
            _z := (_z - (_x << width)).asSInt >> 1
          }
        }

        /* ---------- 状态 ---------- */
        busy  := true.B
        state := sCompute
      }
    }
  }

}

// The module can be instantiated and tested in a Chisel tester or used as part of a larger design.

import _root_.circt.stage.ChiselStage

/** Generate Verilog sources and save it in file GCD.v
  */
object Booth extends App {
  ChiselStage.emitSystemVerilogFile(
    new Booth(8),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
