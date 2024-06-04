import chisel3._
import chisel3.util._

import common.Constants
import memory._
import coursier.core.Done
import dataclass.data

/** @brief
  *   暂时假设一个周期内不会同时读写。当然这里只有一个地址线，应该没啥问题
  */
class DCache extends Module {
  val io = IO(new Bundle {
    // Interface to CPU
    val data_addr  = Input(UInt(Constants.Addr_Width.W))
    val data_ren   = Input(UInt(Constants.CacheLine_Len.W))
    val data_rdata = Output(UInt(Constants.Word_Width.W))
    val data_valid = Output(Bool())
    val data_wen   = Input(UInt(Constants.CacheLine_Len.W))
    val data_wdata = Input(UInt(Constants.Word_Width.W))
    val data_wresp = Output(Bool()) // 写响应
    // Interface to Write Bus
    val dev_wrdy  = Input(Bool())
    val dev_wen   = Output(UInt(Constants.CacheLine_Len.W))
    val dev_waddr = Output(UInt(Constants.Addr_Width.W))
    val dev_wdata = Output(UInt(Constants.Word_Width.W))
    // Interface to Read Bus
    val dev_rrdy   = Input(Bool())
    val dev_ren    = Output(UInt(Constants.CacheLine_Len.W))
    val dev_raddr  = Output(UInt(Constants.Addr_Width.W))
    val dev_rvalid = Input(Bool())
    val dev_rdata  = Input(UInt(Constants.CacheLine_Width.W))
  })

  /* ---------- ---------- 初始化 output ---------- ---------- */
  io.data_valid := false.B
  io.data_rdata := DontCare
  io.data_wresp := false.B

  io.dev_wen   := 0.U
  io.dev_waddr := DontCare
  io.dev_wdata := DontCare

  io.dev_ren   := DontCare
  io.dev_raddr := DontCare

  /* ---------- ---------- 初始化 sram ---------- ---------- */
  // tagSram: dirty + valid + tag
  val tagSram = Module(new SRAM(Constants.Cache_Len, 1 + 1 + Constants.Tag_Width))
  tagSram.io.addra := DontCare
  tagSram.io.dina  := DontCare
  tagSram.io.wea   := false.B
  val dataSram = Module(new SRAM(Constants.Cache_Len, Constants.CacheLine_Width))
  dataSram.io.addra := DontCare
  dataSram.io.dina  := DontCare
  dataSram.io.wea   := false.B

  /* ---------- ---------- 对于外设，不进行 cache ---------- ---------- */
  val peripheral = !(io.data_addr.asUInt <= "hFFFEFFFF".U(Constants.Addr_Width.W))

  when(peripheral && io.data_ren =/= 0.U) {
    io.dev_ren := io.data_ren
    when(io.dev_rrdy && io.dev_rvalid) {
      io.data_valid := true.B
      io.data_rdata := io.dev_rdata
    }
  }

  when(peripheral && io.data_wen =/= 0.U) {
    io.dev_wen := io.data_wen
    when(io.dev_wrdy) {
      io.dev_waddr := io.data_addr
      io.dev_wdata := io.data_wdata
      // FIXME 我觉得这里很奇怪，应该还要有来自于 总线的 响应
      io.data_wresp := true.B
    }
  }

  /* ---------- ---------- addr 划分 ---------- ---------- */
  // [32-1, 32-20] = [31, 12]
  val tag = io.data_addr(Constants.Addr_Width - 1, Constants.Addr_Width - Constants.Tag_Width)
  // [32-20-1, 32-20-10] = [11, 2]
  val index = io.data_addr(Constants.Addr_Width - Constants.Tag_Width - 1, Constants.Offset_Width)
  // [1, 0]
  val offset = io.data_addr(Constants.Offset_Width - 1, 0)

  val dataOutVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dataOutVec := dataSram.io.douta.asTypeOf(dataOutVec)

  val dataInVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dataInVec := dataSram.io.dina.asTypeOf(dataInVec)

  /* ---------- ---------- 读 ---------- ---------- */
  val sIdle_r :: sTAG_CHECK_r :: sDIRTY_CHECK_r :: sREFILL_r :: Nil = Enum(4)
  val state_r                                                       = RegInit(sIdle_r)

  when(!peripheral && io.data_ren =/= 0.U) {
    /* ---------- 读取 ---------- */
    tagSram.io.addra  := index
    dataSram.io.addra := index
    /* ---------- 状态 ---------- */
    state_r := sTAG_CHECK_r
  }
  switch(state_r) {
    is(sIdle_r) { /* 啥也不干 */ }
    is(sTAG_CHECK_r) {
      val valid = tagSram.io.douta(Constants.Tag_Width)
      when(tagSram.io.douta === tag && valid === 1.U) {
        /* ---------- hit ---------- */
        io.data_rdata := dataOutVec(offset)
        io.data_valid := true.B
        /* ---------- 状态 --------- */
        state_r := sIdle_r
      }.otherwise {
        /* ---------- 状态 ---------- */
        state_r := sDIRTY_CHECK_r
      }
    }
    is(sDIRTY_CHECK_r) {
      val dirty = tagSram.io.douta(Constants.Tag_Width + 1)
      when(dirty === 1.U) { // 脏数据 FIXME
        val tag_sram  = tagSram.io.douta(Constants.Tag_Width - 1, 0)
        val addr_sram = Cat(tag_sram, index, offset)
        val data      = dataOutVec(offset)
        when(io.dev_wrdy) {
          io.dev_wen   := "b1111".U(4.W)
          io.dev_waddr := addr_sram
          io.dev_wdata := data
        }
      }
      /* ---------- 访存 ---------- */
      io.dev_ren   := "b1111".U(4.W) // 0b1111
      io.dev_raddr := io.data_addr
      /* ---------- 状态 ---------- */
      state_r := sREFILL_r
    }
    is(sREFILL_r) {
      /* ---------- refill cache ---------- */
      tagSram.io.wea    := true.B
      tagSram.io.addra  := index
      tagSram.io.dina   := Cat(0.U, 1.U, tag)
      dataSram.io.wea   := true.B
      dataSram.io.addra := index
      dataSram.io.dina  := io.dev_rdata
      /* ---------- 状态 ---------- */
      state_r := sTAG_CHECK_r
    }
  }

  /* ---------- ---------- 写 ---------- ---------- */
  val sIdle_w :: sTAG_CHECK_w :: sDIRTY_CHECK_w :: sREFILL_w :: Nil = Enum(4)
  val state_w                                                       = RegInit(sIdle_r)

  when(!peripheral && io.data_wen =/= 0.U) {
    /* ---------- 读取 ---------- */
    tagSram.io.addra  := index
    dataSram.io.addra := index
    /* ---------- 状态 ---------- */
    state_w := sTAG_CHECK_w
  }

  switch(state_w) {
    is(sIdle_w) { /* 啥也不干 */ }
    is(sTAG_CHECK_w) {
      val valid = tagSram.io.douta(Constants.Tag_Width)
      when(tagSram.io.douta === tag && valid === 1.U) {
        /* ---------- hit ---------- */
        dataSram.io.wea   := true.B
        dataInVec(offset) := io.data_wdata
        tagSram.io.wea    := true.B
        tagSram.io.dina   := Cat(1.U, 1.U, tag) // 设置为脏数据

        /* ---------- 响应 ---------- */
        io.data_wresp := true.B

        /* ---------- 状态 --------- */
        state_w := sIdle_w
      }.otherwise {
        /* ---------- 状态 ---------- */
        state_w := sDIRTY_CHECK_w
      }
    }
    is(sDIRTY_CHECK_w) {
      val dirty = tagSram.io.douta(Constants.Tag_Width + 1)
      when(dirty === 1.U) { // 脏数据
        // FIXME 这里肯定是有问题的，应该一次写一个 cacheline
        val tag_sram  = tagSram.io.douta(Constants.Tag_Width - 1, 0)
        val addr_sram = Cat(tag_sram, index, offset)
        val data      = dataOutVec(offset)
        when(io.dev_wrdy) {
          io.dev_wen   := "b1111".U(4.W)
          io.dev_waddr := addr_sram
          io.dev_wdata := data
        }
      }
      /* ---------- 访存 ---------- */
      io.dev_ren   := "b1111".U(4.W) // 0b1111
      io.dev_raddr := io.data_addr
      /* ---------- 状态 ---------- */
      state_r := sREFILL_r
    }
    is(sREFILL_w) {
      /* ---------- write cache ---------- */
      tagSram.io.wea    := true.B
      tagSram.io.addra  := index
      tagSram.io.dina   := Cat(0.U, 1.U, tag)
      dataSram.io.wea   := true.B
      dataSram.io.addra := index
      dataSram.io.dina  := io.dev_rdata
      /* ---------- 状态 ---------- */
      state_r := sTAG_CHECK_w
    }
  }

}

import _root_.circt.stage.ChiselStage

object DCache extends App {
  ChiselStage.emitSystemVerilogFile(
    new DCache,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
