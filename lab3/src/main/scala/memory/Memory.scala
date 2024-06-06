package memory

import chisel3._
import chisel3.util._

import common.Constants

/** @brief
  *   黑盒，ip 核
  */
class blk_mem_gen_1 extends BlackBox {
  val io = IO(new Bundle {
    val clka  = Input(Clock())
    val wea   = Input(Bool())
    val addra = Input(UInt(Constants.Index_Width.W)) // 6
    val dina  = Input(UInt(Constants.CacheLine_Width.W)) // 132
    val douta = Output(UInt(Constants.CacheLine_Width.W)) //
  })
}
