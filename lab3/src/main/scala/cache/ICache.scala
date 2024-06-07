package cache

import chisel3._
import chisel3.util._

import common.Constants
import memory._

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
  // [32-1, 32-20] = [31, 12]
  val tag = io.inst_addr(Constants.Addr_Width - 1, Constants.Addr_Width - Constants.Tag_Width)
  // [32-20-1, 32-20-10] = [11, 2]
  val index = io.inst_addr(Constants.Addr_Width - Constants.Tag_Width - 1, Constants.Offset_Width)
  // [1, 0]
  val offset = io.inst_addr(Constants.Offset_Width - 1, 0)

  val dataOutVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dataOutVec := dataSram.io.douta.asTypeOf(dataOutVec)

  /* ---------- ---------- 状态机 ---------- ---------- */
  // 0 1 2
  val sIdle :: sTAG_CHECK :: sREFILL :: sI_S0_mem :: sI_S1_mem :: Nil = Enum(5)
  val state                                                           = RegInit(sIdle)

  io.hit := (state === sIdle) && (RegNext(state) === sTAG_CHECK) && (RegNext(RegNext(state)) === sIdle)

  switch(state) {
    is(sIdle) {
      /* ---------- 状态 ---------- */
      state := Mux(io.inst_rreq, sTAG_CHECK, sIdle)
    }
    is(sTAG_CHECK) {
      /* ---------- 状态 ---------- */
      when(tagSram.io.douta(Constants.Tag_Width, 0) === Cat(1.U, tag)) {
        io.inst_valid := true.B
        io.inst_out   := dataOutVec(offset)
        state         := sIdle
      }.otherwise {
        state := sREFILL
      }
    }
    is(sREFILL) { /* sIdle */
      io.mem_raddr := io.inst_addr
      when(io.mem_rrdy) {
        io.mem_ren := "b1111".U(4.W)
        state      := sI_S1_mem
      }.otherwise {
        io.mem_ren := 0.U
        state      := sI_S0_mem
      }
    }
    is(sI_S0_mem) {
      when(io.mem_rrdy) {
        io.mem_ren := "b1111".U(4.W)
        state      := sI_S1_mem
      }.otherwise {
        io.mem_ren := 0.U
        state      := sI_S0_mem
      }
      // io.mem_ren := Mux(io.mem_rrdy, "b1111".U(4.W), 0.U)
      // state      := Mux(io.mem_rrdy, sI_S1_mem, sI_S0_mem)
    }
    is(sI_S1_mem) {
      /* ---------- 状态 ---------- */
      when(io.mem_rvalid) {
        state             := sTAG_CHECK
        tagSram.io.wea    := true.B
        tagSram.io.addra  := index
        tagSram.io.dina   := Cat(1.U, tag)
        dataSram.io.wea   := true.B
        dataSram.io.addra := index
        dataSram.io.dina  := io.mem_rdata
      }.otherwise {
        state := sI_S1_mem
      }
    }
  }

}

import _root_.circt.stage.ChiselStage

object ICache extends App {
  ChiselStage.emitSystemVerilogFile(
    new ICache,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
