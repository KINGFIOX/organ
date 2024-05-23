package booth

import chisel3._
import chisel3.util._
import scala.annotation.switch
import scala.util.control.Exception.Catch
import scala.util.matching.Regex

/**
  * 补码，booth 乘法器
  *
  * @param width
  */
class Booth(width: Int) extends Module {
  val io = IO(new Bundle {
    val x     = Input(UInt((width + 1).W)) // 原码
    val y     = Input(UInt((width + 1).W))
    val start = Input(Bool())
    val z     = Output(UInt((2 * width).W)) // 商
    val busy  = Output(Bool()) // 忙信号
  })

  val sIDLE :: sCompute :: Nil = Enum(2)
  val state                    = RegInit(sIDLE)

  val q_reg = RegInit(0.U(2.W)) // 用来存放最后两位

  val cnt = Counter(width) // 右移次数

  val _x = RegInit(0.U((width).W))
  val _y = RegInit(0.U((width).W))

  val _z = RegInit(0.U((2 * width).W))
  io.z := _z

  val busy = RegInit(false.B)
  io.busy := busy

  switch(state) {
    is(sIDLE) {
      when(io.start) {
        /* ---------- init ---------- */
        q_reg := Cat(io.y(0), 0.U(1.W))
        _x    := io.x(width - 1, 0)
        _y    := io.y(width - 1, 0)
        _z    := Cat(0.U(width.W), io.y)
        busy  := true.B

        /* ---------- 状态转移 ---------- */
        state := sCompute
      }
    }
    is(sCompute) {
      when(cnt.inc()) {
        q_reg := Cat(_y(cnt.value + 1.U), _y(cnt.value))
        switch(q_reg) {
          is("b11".U) {
            _z := (_z.asSInt >> 1).asUInt
          }
          is("b00".U) {
            _z := (_z.asSInt >> 1).asUInt
          }
          is("b01".U) {
            _z := (Cat(_z(width + width - 1, width) + _x.asUInt, _z(width - 1, 0)).asSInt >> 1).asUInt
          }
          is("b10".U) {
            _z := (Cat(_z(width + width - 1, width) - _x.asUInt, _z(width - 1, 0)).asSInt >> 1).asUInt
          }
        }

        state := sCompute
        busy  := true.B
      }.otherwise {

        state := sIDLE
        busy  := false.B
      }
    }
  }

}

import _root_.circt.stage.ChiselStage

/** Generate Verilog sources and save it in file GCD.v
  */
object Booth extends App {
  ChiselStage.emitSystemVerilogFile(
    new Booth(8),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
