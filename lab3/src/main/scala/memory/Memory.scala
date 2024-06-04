package memory

import chisel3._
import chisel3.util._

import common.Constants

class SRAM(val len: Int, val width: Int) extends Module {
  val io = IO(new Bundle {
    val wea   = Input(Bool())
    val addra = Input(UInt(log2Ceil(len).W))
    val dina  = Input(UInt(width.W))
    val douta = Output(UInt(width.W))
  })

  // 创建一个同步读取的内存
  val mem = SyncReadMem(len, UInt(width.W))

  // 写操作
  when(io.wea) {
    mem.write(io.addra, io.dina)
  }

  // 读操作，注意这里的读取是同步的
  io.douta := mem.read(io.addra)
}

import _root_.circt.stage.ChiselStage

object SRAM extends App {
  ChiselStage.emitSystemVerilogFile(
    new SRAM(10, 20),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
