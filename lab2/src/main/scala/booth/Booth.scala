package booth

import chisel3._
import chisel3.util._

class Booth(val width: Int) extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val x = Input(UInt(width.W))
    val y = Input(UInt(width.W))
    val z = Output(UInt((2 * width).W))
    val busy = Output(Bool())
  })

  // State definitions
  val sIdle :: sCompute :: Nil = Enum(2)
  val state = RegInit(sIdle)

  val busy = RegInit(false.B)
  io.busy := busy

  val _x = RegInit(0.U(width.W))
  val _y = RegInit(0.U(width.W))
  val _z = RegInit(0.U((2 * width).W))
  // Output connections
  io.z := _z

  // Counter for shifts
  val cnt = Counter(width)

  // State transition logic
  switch(state) {
    is(sIdle) {
      when(io.start) {
        val _q = Cat(io.y(0), 0.U(1.W))
        switch(_q) {
          is("b00".U, "b11".U) {
            val __z = Cat(0.U(width.W), 0.U(width.W))
            val __z_sign = __z(2 * width - 1)
            _z := Cat(__z_sign, __z(2 * width - 1, 1))
          }
          is("b01".U) {
            val __z = Cat(io.x, 0.U(width.W))
            val __z_sign = __z(2 * width - 1)
            _z := Cat(__z_sign, __z(2 * width - 1, 1))
          }
          is("b10".U) {
            val __z = Cat(-io.x, 0.U(width.W))
            val __z_sign = __z(2 * width - 1)
            _z := Cat(__z_sign, __z(2 * width - 1, 1))
          }
        }

        /* ---------- 状态 ---------- */
        busy := true.B
        state := sCompute

        /* ---------- init ---------- */
        _x := io.x
        _y := io.y
      }
    }
    is(sCompute) {
      when(cnt.inc()) { // Increment the counter and check if it has reached its maximum

        // // 很奇怪
        // when(_x === Cat(1.U(1.W), 0.U((width - 1).W))) {
        //   _z := -_z
        // }

        /* ---------- 状态 ---------- */
        state := sIdle
        busy := false.B
      }.otherwise {
        val _q = Cat(_y(cnt.value + 1.U), _y(cnt.value))
        switch(_q) {
          is("b00".U, "b11".U) {
            val __z = _z
            val __z_sign = __z(2 * width - 1)
            _z := Cat(__z_sign, __z(2 * width - 1, 1))
          }
          is("b01".U) {
            val __z = _z + Cat(_x, 0.U(width.W))
            val __z_sign = __z(2 * width - 1)
            _z := Cat(__z_sign, __z(2 * width - 1, 1))
          }
          is("b10".U) {
            val __z = _z - Cat(_x, 0.U(width.W))
            val __z_sign = __z(2 * width - 1)
            _z := Cat(__z_sign, __z(2 * width - 1, 1))
          }
        }

        /* ---------- 状态 ---------- */
        busy := true.B
        state := sCompute
      }
    }
  }

}

// The module can be instantiated and tested in a Chisel tester or used as part of a larger design.

import _root_.circt.stage.ChiselStage

object Booth extends App {
  val width = if (args.length > 0) args(0).toInt else 8 // 默认值为8
  ChiselStage.emitSystemVerilogFile(
    new Booth(width),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
