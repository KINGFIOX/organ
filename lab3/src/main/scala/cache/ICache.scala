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
  })
  /* ---------- ---------- 初始化 output ---------- ---------- */
  io.inst_valid := false.B
  io.inst_out   := DontCare
  io.mem_ren    := 0.U
  io.mem_raddr  := DontCare

  /* ---------- ---------- 初始化 sram ---------- ---------- */
  val tagSram = Module(new SRAM(Constants.Cache_Len, 1 + Constants.Tag_Width))
  tagSram.io.addra := DontCare
  tagSram.io.dina  := DontCare
  tagSram.io.wea   := false.B
  val dataSram = Module(new SRAM(Constants.Cache_Len, Constants.CacheLine_Width))
  dataSram.io.addra := DontCare
  dataSram.io.dina  := DontCare
  dataSram.io.wea   := false.B

  /* ---------- ---------- addr 划分 ---------- ---------- */
  // [32-1, 32-20] = [31, 12]
  val tag = io.inst_addr(Constants.Addr_Width - 1, Constants.Addr_Width - Constants.Tag_Width)
  // [32-20-1, 32-20-10] = [11, 2]
  val index = io.inst_addr(Constants.Addr_Width - Constants.Tag_Width - 1, Constants.Offset_Width)
  // [1, 0]
  val offset = io.inst_addr(Constants.Offset_Width - 1, 0)

  val sramOutVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  sramOutVec := dataSram.io.douta.asTypeOf(sramOutVec)

  /* ---------- ---------- 状态机 ---------- ---------- */
  val sIdle :: sTAG_CHECK :: sREFILL :: Nil = Enum(3)
  val state                                 = RegInit(sIdle)

  when(io.inst_rreq) {
    /* ---------- 读取 ---------- */
    tagSram.io.addra  := index
    dataSram.io.addra := index
    /* ---------- 状态 ---------- */
    state := sTAG_CHECK
  }

  switch(state) {
    is(sIdle) { /* 啥也不干 */ }
    is(sTAG_CHECK) {
      // tag 命中，并且 valid 位为 1
      when(tagSram.io.douta === tag && tagSram.io.douta(Constants.Tag_Width) === 1.U) {
        /* ---------- hit ---------- */
        io.inst_out   := sramOutVec(offset)
        io.inst_valid := true.B
        /* ---------- 状态 ---------- */
        state := sIdle
      }.otherwise {
        /* ---------- 访存 ---------- */
        io.mem_ren   := "b1111".U(4.W) // 0b1111
        io.mem_raddr := io.inst_addr
        /* ---------- 状态 ---------- */
        state := sREFILL
      }
    }
    is(sREFILL) {
      /* ---------- miss ---------- */
      when(io.mem_rvalid && io.mem_rrdy) {
        /* ---------- 写入 ---------- */
        tagSram.io.wea    := true.B
        tagSram.io.addra  := index
        tagSram.io.dina   := Cat(1.U, tag)
        dataSram.io.wea   := true.B
        dataSram.io.addra := index
        dataSram.io.dina  := io.mem_rdata
        /* ---------- 状态 ---------- */
        state := sTAG_CHECK
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
