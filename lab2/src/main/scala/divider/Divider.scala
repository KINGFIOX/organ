package divider

/* ---------- ---------- divider ---------- ---------- */

import chisel3._
import chisel3.util._

/** @param width
  *   不包含符号位，要求 width 是 2 的次幂
  */
class Divider(_width: Int) extends Module {
  // require(isPow2(_width))
  val width = _width - 1
  val io = IO(new Bundle {
    val x     = Input(UInt((width + 1).W)) // 原码
    val y     = Input(UInt((width + 1).W))
    val start = Input(Bool())
    val z     = Output(UInt((width + 1).W)) // 商
    val r     = Output(UInt((width + 1).W)) // 余数，不包含符号位
    val busy  = Output(Bool()) // 忙信号
  })

  val quotient      = RegInit(0.U(width.W))
  val quotient_sign = Reg(Bool()) // 商的符号位
  io.z := Cat(quotient_sign, quotient)

  val remain_sign = RegInit(0.U(1.W))
  val remain      = RegInit(0.U((2 * width).W))
  io.r := Cat(remain_sign, remain(2 * width - 2, width - 1))

  val cnt      = Counter(width)
  val extend_y = RegInit(0.U((2 * width).W))

  val busy = RegInit(false.B)
  io.busy := busy

  val sIDLE :: sCompute :: Nil = Enum(2)
  val state                    = RegInit(sIDLE)

  switch(state) {
    is(sIDLE) {
      busy := false.B
      when(io.start) {

        /* ---------- 初始化 ---------- */

        // 1101 0
        val abs_y         = io.y(width - 1, 0)
        val wire_extend_y = Cat(0.U(1.W), abs_y, 0.U((width - 1).W)) // 2 * width
        extend_y    := wire_extend_y
        remain_sign := io.x(width)

        // 保存符号
        quotient_sign := io.x(width) ^ io.y(width)

        // 0000 1101
        val abs_x    = io.x(width - 1, 0)
        val extend_x = Cat(0.U(width.W), abs_x) // 2 * width
        busy := true.B

        /* ---------- 计算 ---------- */

        // 计算余数
        val wire_cal = extend_x - wire_extend_y
        remain := wire_cal
        // 上商
        quotient := 0.U(width.W) | ~wire_cal(2 * width - 1)

        state := sCompute
      }
    }
    is(sCompute) {
      when(cnt.inc()) {
        busy  := false.B
        state := sIDLE

        // 如果最后余数是 负数，那么就加回去
        remain := Mux(remain(2 * width - 1), remain + extend_y, remain)

      }.otherwise {
        /* ---------- 一些初始化 ---------- */
        busy  := true.B
        state := sCompute

        /* ---------- 计算余数 ---------- */
        val wire_shift_remain = (remain << 1)(2 * width - 1, 0)
        val wire_cal          = Mux(remain(2 * width - 1), wire_shift_remain + extend_y, wire_shift_remain - extend_y)
        remain := wire_cal

        /* ---------- 上商 ---------- */
        quotient := (quotient << 1)(width - 1, 0) | ~wire_cal(2 * width - 1)
      }
    }
  }

//   printf("---------- cnt=%d ----------\n", cnt.value)
//   printf("quotient: %b\n", quotient)
//   printf("remain: %b\n", remain)
}

import _root_.circt.stage.ChiselStage

/** Generate Verilog sources and save it in file GCD.v
  */
object Divider extends App {
  val width = if (args.length > 0) args(0).toInt else 8 // 默认值为8
  ChiselStage.emitSystemVerilogFile(
    new Divider(width),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
