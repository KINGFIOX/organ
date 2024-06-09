package cache

import chisel3._
import chisel3.util._

import common.Constants
import memory._
import scala.collection.immutable.Stream.Cons

class ICache extends Module {
  val io = IO(new Bundle {
    val inst_rreq  = Input(Bool()) // Instruction fetch signal
    val inst_addr  = Input(UInt(Constants.Addr_Width.W))
    val inst_valid = Output(Bool()) // Whether ICache hit
    val inst_out   = Output(UInt(Constants.Word_Width.W)) // Instruction output

    // 这里是一个 cacheline 一个 cacheline 读入的
    val mem_ren   = Output(UInt(Constants.CacheLine_Len.W)) // 读使能
    val mem_raddr = Output(UInt(Constants.Addr_Width.W)) // 读地址
    val mem_rdata = Input(UInt(Constants.CacheLine_Width.W)) // 读数据
    // 握手
    val mem_rrdy   = Input(Bool()) // 主存就绪
    val mem_rvalid = Input(Bool()) // 来自主存的数据有效
    // hit
    val hit = Output(Bool())
  })
  /* ---------- ---------- 初始化 output ---------- ---------- */
  io.inst_valid := false.B
  io.inst_out   := DontCare
  io.mem_ren    := 0.U
  io.mem_raddr  := DontCare
  io.hit        := false.B

  /* ---------- ---------- 初始化 sram ---------- ---------- */
  val tagSram = Module(new blk_mem_gen_1)
  tagSram.io.addra := DontCare
  tagSram.io.dina  := DontCare
  tagSram.io.wea   := false.B
  tagSram.io.clka  := clock
  val dataSram = Module(new blk_mem_gen_1)
  dataSram.io.addra := DontCare
  dataSram.io.dina  := DontCare
  dataSram.io.wea   := false.B
  dataSram.io.clka  := clock

  /* ---------- ---------- addr 划分 ---------- ---------- */
  val tag    = io.inst_addr(Constants.Tag_up, Constants.Tag_down)
  val index  = io.inst_addr(Constants.Index_up, Constants.Index_down)
  val offset = io.inst_addr(Constants.Offset_up, Constants.Offset_down)

  tagSram.io.addra  := index
  dataSram.io.addra := index

  val dataOutVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dataOutVec := dataSram.io.douta.asTypeOf(dataOutVec)

  /* ---------- ---------- 状态机 ---------- ---------- */
  // 0 1 2
  val sIdle :: sTAG_CHECK :: sREFILL :: sI_S1_mem :: Nil = Enum(4)
  val state                                              = RegInit(sIdle)

  switch(state) {
    is(sIdle) {
      /* ---------- 状态 ---------- */
      when(io.inst_rreq) {
        state := sTAG_CHECK
      }
    }
    is(sTAG_CHECK) {
      when(tagSram.io.douta(Constants.Tag_Width, 0) === Cat(1.U, tag)) {
        io.inst_valid := true.B
        io.inst_out   := dataOutVec(offset)
        io.hit        := true.B
        state         := sIdle
      }.otherwise {
        state := sREFILL
      }
    }
    is(sREFILL) { /* sIdle */
      io.mem_raddr := Cat(tag, index, 0.U((Constants.Offset_Width + Constants.Word_Align).W))
      when(io.mem_rrdy) {
        io.mem_ren := "b1111".U(4.W)
        state      := sI_S1_mem
      }
    }
    is(sI_S1_mem) {
      /* ---------- 状态 ---------- */
      when(io.mem_rvalid) {
        state            := sTAG_CHECK
        tagSram.io.wea   := true.B
        tagSram.io.dina  := Cat(1.U, tag)
        dataSram.io.wea  := true.B
        dataSram.io.dina := io.mem_rdata
      }
    }
  }

}

import _root_.circt.stage.ChiselStage

object ICache extends App {
  ChiselStage.emitSystemVerilogFile(
    new ICache,
    // args        = Array("--target", "verilog"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
