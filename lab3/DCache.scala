package cache

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
    val hit_r      = Output(Bool())
    val hit_w      = Output(Bool())
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
  // val tagSram = Module(new SRAM(Constants.Cache_Len, 1 + 1 + Constants.Tag_Width))
  val tagSram = Module(new blk_mem_gen_1)
  tagSram.io.addra := DontCare
  tagSram.io.dina  := DontCare
  tagSram.io.wea   := false.B
  tagSram.io.clka  := clock
  // val dataSram = Module(new SRAM(Constants.Cache_Len, Constants.CacheLine_Width))
  val U_dsram = Module(new blk_mem_gen_1)
  U_dsram.io.addra := DontCare
  U_dsram.io.dina  := DontCare
  U_dsram.io.wea   := false.B
  U_dsram.io.clka  := clock

  /* ---------- ---------- 对于外设，不进行 cache ---------- ---------- */
  // val peripheral = !(io.data_addr.asUInt <= "hFFFEFFFF".U(Constants.Addr_Width.W))

  // Peripherals access should be uncached.
  val uncached = Mux((io.data_addr(31, 16) === "hFFFF".U(16.W)) & (io.data_ren =/= 0.U(4.W) | io.data_wen =/= 0.U(4.W)), true.B, false.B)

  when(uncached && io.data_ren =/= 0.U) {
    io.dev_ren := io.data_ren
    when(io.dev_rrdy && io.dev_rvalid) {
      io.data_valid := true.B
      io.data_rdata := io.dev_rdata
    }
  }

  when(uncached && io.data_wen =/= 0.U) {
    io.dev_wen := io.data_wen
    when(io.dev_wrdy) {
      io.dev_waddr := io.data_addr
      io.dev_wdata := io.data_wdata
      // 我觉得这里应该有 io.dev_wresp
      io.data_wresp := true.B
    }
  }

  /* ---------- ---------- addr 划分 ---------- ---------- */
  // [32-1, 32-24] = [31, 8]
  val tag = io.data_addr(Constants.Addr_Width - 1, Constants.Addr_Width - Constants.Tag_Width)
  // [32-20-1, 32-24-6] = [7, 2]
  val index = io.data_addr(Constants.Addr_Width - Constants.Tag_Width - 1, Constants.Offset_Width)
  // [1, 0]
  val offset = io.data_addr(Constants.Offset_Width - 1, 0)

  /* ---------- 把 data 转换成向量，方便修改 ---------- */

  val dataOutVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dataOutVec := U_dsram.io.douta.asTypeOf(dataOutVec)

  val dataInVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dataInVec := U_dsram.io.dina.asTypeOf(dataInVec)

  /* ---------- ---------- write ---------- ---------- */
  val sIdle_w :: sTAG_CHECK_w :: sWRITE_BACKn_w :: sREFILL_w :: Nil = Enum(4)
  val state_w                                                       = RegInit(sIdle_w)

  val cnt_w = Counter(Constants.CacheLine_Len)

  io.hit_w := (state_w === sIdle_w) && (RegNext(state_w) === sTAG_CHECK_w) && (RegNext(RegNext(state_w)) =/= sREFILL_w)

  switch(state_w) {
    is(sIdle_w) {
      when(~uncached && io.data_wen =/= 0.U) {
        /* ---------- 读取 ---------- */
        tagSram.io.addra := index
        U_dsram.io.addra := index
        /* ---------- 状态 ---------- */
        state_w := sTAG_CHECK_w
      }
    }
    is(sTAG_CHECK_w) {
      when(tagSram.io.douta(Constants.Tag_Width, 0) === Cat(1.U, tag)) {
        /* ---------- hit ---------- */
        U_dsram.io.wea    := true.B
        dataInVec(offset) := io.data_wdata
        tagSram.io.wea    := true.B
        // dirty=1 + valid=1 + tag
        tagSram.io.dina := Cat(0.U((Constants.CacheLine_Width - Constants.Tag_Width - 1).W), 1.U, 1.U, tag)

        /* ---------- 响应 ---------- */
        io.data_wresp := true.B

        /* ---------- 状态 --------- */
        state_w := sIdle_w
      }.otherwise {
        /* ---------- 状态 ---------- */
        when(tagSram.io.douta(Constants.Tag_Width + 1)) {
          state_w := sWRITE_BACKn_w
        }.otherwise {
          state_w      := sREFILL_w
          io.dev_ren   := "b1111".U(4.W)
          io.dev_raddr := Cat(tag, index, 0.U(2.W))
        }
      }
    }
    is(sWRITE_BACKn_w) {
      when(cnt_w.inc()) {
        /* ---------- 访存 ---------- */
        io.dev_ren   := "b1111".U(4.W)
        io.dev_raddr := Cat(tag, index, 0.U(2.W))
        /* ---------- 状态 ---------- */
        state_w := sREFILL_w
      }.otherwise {
        val ofst      = cnt_w.value(Constants.Offset_Width - 1, 0)
        val tag_sram  = tagSram.io.douta(Constants.Tag_Width - 1, 0)
        val addr_sram = Cat(tag_sram, index, ofst)
        val data      = dataOutVec(ofst)
        when(io.dev_wrdy) {
          io.dev_wen   := "b1111".U(4.W)
          io.dev_waddr := addr_sram
          io.dev_wdata := data
        }
      }
    }
    is(sREFILL_w) {
      when(io.dev_rvalid /* && io.dev_wrdy */ ) {
        /* ---------- write cache ---------- */
        tagSram.io.wea   := true.B
        tagSram.io.addra := index
        // refill 阶段 dirty=0 + valid=0 + tag
        tagSram.io.dina  := Cat(0.U((Constants.CacheLine_Width - Constants.Tag_Width - 2).W), 0.U, 1.U, tag)
        U_dsram.io.wea   := true.B
        U_dsram.io.addra := index
        U_dsram.io.dina  := io.dev_rdata
        /* ---------- 状态 ---------- */
        state_w := sTAG_CHECK_w
      }
    }
  }

  /* ---------- ---------- read ---------- ---------- */
  val sIdle_r :: sTAG_CHECK_r :: sWRITE_BACKn_r :: sREFILL_r :: Nil = Enum(4)
  val state_r                                                       = RegInit(sIdle_r)

  val cnt_r = Counter(Constants.CacheLine_Len)

  io.hit_r := (state_r === sIdle_r) && (RegNext(state_r) === sTAG_CHECK_r) && (RegNext(RegNext(state_r)) =/= sREFILL_r)

  switch(state_r) {
    is(sIdle_r) {
      when(~uncached && io.data_ren =/= 0.U) {
        /* ---------- 读取 ---------- */
        tagSram.io.addra := index
        U_dsram.io.addra := index
        /* ---------- 状态 ---------- */
        state_r := sTAG_CHECK_r
      }
    }
    is(sTAG_CHECK_r) {
      when(tagSram.io.douta(Constants.Tag_Width, 0) === Cat(1.U, tag)) { // cache 命中，不用 write back
        /* ---------- hit ---------- */
        io.data_rdata := dataOutVec(offset)
        io.data_valid := true.B
        /* ---------- 状态 --------- */
        state_r := sIdle_r
      }.otherwise {
        /* ---------- 状态 ---------- */
        when(tagSram.io.douta(Constants.Tag_Width + 1)) { // 如果 dirty=1
          /* ---------- 状态 ---------- */
          state_r := sWRITE_BACKn_r
        }.otherwise {
          /* ---------- IO ---------- */
          io.dev_ren   := "b1111".U(4.W)
          io.dev_raddr := Cat(tag, index, 0.U(2.W))
          /* ---------- 状态 ---------- */
          state_r := sREFILL_r
        }
      }
    }
    is(sWRITE_BACKn_r) {
      when(cnt_r.inc()) {
        /* ---------- 访存 ---------- */
        io.dev_ren   := "b1111".U(4.W)
        io.dev_raddr := Cat(tag, index, 0.U(2.W))
        /* ---------- 状态 ---------- */
        state_r := sREFILL_r
      }.otherwise { // 连续写
        val ofst      = cnt_r.value(Constants.Offset_Width - 1, 0)
        val tag_sram  = tagSram.io.douta(Constants.Tag_Width - 1, 0)
        val addr_sram = Cat(tag_sram, index, ofst)
        val data      = dataOutVec(ofst)
        when(io.dev_wrdy) {
          io.dev_wen   := "b1111".U(4.W)
          io.dev_waddr := addr_sram
          io.dev_wdata := data
        }
      }
    }
    is(sREFILL_r) {
      when(io.dev_rvalid /* && io.dev_wrdy */ ) {
        /* ---------- refill cache ---------- */
        tagSram.io.wea   := true.B
        tagSram.io.addra := index
        // dirty=0 + valid=1 + tag
        tagSram.io.dina  := Cat(0.U((Constants.CacheLine_Width - Constants.Tag_Width - 2).W), 0.U, 1.U, tag)
        U_dsram.io.wea   := true.B
        U_dsram.io.addra := index
        U_dsram.io.dina  := io.dev_rdata
        /* ---------- 状态 ---------- */
        state_r := sTAG_CHECK_r
      }
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
