package cache

import chisel3._
import chisel3.util._

import common.Constants
import memory._
import coursier.core.Done
import dataclass.data

/** @brief
  *   暂时假设一个周期内不会同时读写。当然这里只有一个地址线，应该没啥问题
  *
  * @data_addr
  *   这个应该是按字寻址的
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
  io.data_rdata := 0.U
  io.data_wresp := false.B

  io.dev_wen   := 0.U
  io.dev_waddr := 0.U
  io.dev_wdata := 0.U

  io.dev_ren   := 0.U
  io.dev_raddr := 0.U

  io.hit_w := 0.U
  io.hit_r := 0.U

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

  val sR_IDLE :: sR_S0 :: sR_S1 :: Nil = Enum(3)
  val r_state                          = RegInit(sR_IDLE)

  /* ---------- ---------- read ---------- ---------- */

  switch(r_state) {
    is(sR_IDLE) {
      r_state := Mux(io.data_ren.orR, Mux(io.dev_rrdy, sR_S0, sR_S1), sR_IDLE)
    }
    is(sR_S0) {
      r_state := Mux(io.dev_rrdy, sR_S1, sR_S0)
    }
    is(sR_S1) {
      r_state := Mux(io.dev_rvalid, sR_IDLE, sR_S1)
    }
  }

  // 这个 ren_r 是因为：io.data_ren 可能会撤下来
  val ren_r = RegInit(0.U(Constants.CacheLine_Len.W))
  switch(r_state) {
    is(sR_IDLE) {
      io.data_valid := 0.U
      when(io.data_ren.orR) {
        when(io.dev_rrdy) {
          io.dev_ren := io.data_ren
        }.otherwise {
          ren_r := io.data_ren
        }
        io.dev_raddr := io.data_addr
      }.otherwise {
        io.dev_ren := 0.U
      }
    }
    is(sR_S0) {
      io.dev_ren := Mux(io.dev_rrdy, ren_r, 0.U)
    }
    is(sR_S1) {
      io.dev_ren    := 0.U
      io.data_valid := Mux(io.dev_rvalid, true.B, false.B)
      io.data_rdata := Mux(io.dev_rvalid, io.dev_rdata, 0.U)
    }
  }

  /* ---------- ---------- write ---------- ---------- */

  val sW_IDLE :: sW_S0 :: sW_S1 :: Nil = Enum(3)
  val w_state                          = RegInit(sW_IDLE)

  // 这个 ren_r 是因为：io.data_ren 可能会撤下来
  val wen_r   = RegInit(0.U(Constants.CacheLine_Len.W))
  val wr_resp = Mux(io.dev_wrdy & (io.dev_wen === 0.U), true.B, false.B)

  switch(w_state) {
    is(sW_IDLE) {
      w_state := Mux(io.data_wen.orR, Mux(io.dev_wrdy, sW_S1, sW_S0), sW_IDLE)
    }
    is(sW_S0) {
      w_state := Mux(io.dev_wrdy, sW_S1, sW_S0)
    }
    is(sW_S1) {
      w_state := Mux(wr_resp, sW_IDLE, sW_S1)
    }
  }

  switch(w_state) {
    is(sW_IDLE) {
      io.data_wresp := 0.U
      when(io.data_wen.orR) {
        when(io.dev_rrdy) {
          io.dev_wen := io.data_wen
        }.otherwise {
          wen_r := io.data_wen
        }
        io.dev_waddr := io.data_addr
        io.dev_wdata := io.data_wdata
      }.otherwise {
        io.dev_wen := 0.U
      }
    }
    is(sW_S0) {
      io.dev_wen := Mux(io.dev_wrdy, wen_r, 0.U)
      when(io.dev_rrdy) {
        io.dev_wen := wen_r
      }
    }
    is(sW_S1) {
      io.dev_wen    := 0.U
      io.data_wresp := Mux(wr_resp, true.B, false.B)
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

}

import _root_.circt.stage.ChiselStage

object DCache extends App {
  ChiselStage.emitSystemVerilogFile(
    new DCache,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
