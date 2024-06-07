import chisel3._
import chisel3.util._

import common.Constants

class DCache extends Module {
  val io = IO(new Bundle {
    // Interface to CPU
    val data_ren   = Input(UInt(Constants.CacheLine_Len.W))
    val data_addr  = Input(UInt(Constants.Addr_Width.W))
    val data_valid = Output(Bool())
    val data_rdata = Output(UInt(Constants.Word_Width.W))
    val data_wen   = Input(UInt(Constants.CacheLine_Len.W))
    val data_wdata = Input(UInt(Constants.Word_Width.W))
    val data_wresp = Output(Bool())
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
    val dev_rdata  = Input(UInt(Constants.CacheLine_Width.W)) // Assuming `BLK_SIZE = 4 * 32`
  })

  /* ---------- ---------- init ---------- ---------- */

  io.data_rdata := 0.U
  io.data_valid := 0.U
  io.data_wresp := 0.U

  io.dev_wen   := 0.U
  io.dev_waddr := 0.U
  io.dev_wdata := 0.U

  io.dev_ren   := 0.U
  io.dev_raddr := 0.U

  val uncached = Mux((io.data_addr(31, 16) === "hffff".U(16.W)) & (io.data_ren =/= 0.U | io.data_wen =/= 0.U), true.B, false.B)

  /* ---------- ---------- read ---------- ---------- */

  // Read path state definitions
  val rIdle :: rStat0 :: rStat1 :: Nil = Enum(3)
  val r_state                          = RegInit(rIdle)

  // Registers for storing state between cycles
  val ren_r = Reg(UInt(4.W))

  // Read path logic
  switch(r_state) {
    is(rIdle) {
      io.data_valid := 0.U
      when(io.data_ren.orR) {
        io.dev_raddr := io.data_addr
        when(io.dev_rrdy) {
          r_state    := rStat1
          io.dev_ren := io.data_ren
        }.otherwise {
          r_state := rStat0
          ren_r   := io.data_ren
        }
      }.otherwise {
        io.dev_ren := 0.U
        r_state    := rIdle
      }
    }
    is(rStat0) {
      when(io.dev_rrdy) {
        io.dev_ren := ren_r
        r_state    := rStat1
      }.otherwise {
        io.dev_ren := 0.U
        r_state    := rStat0
      }
    }
    is(rStat1) {
      io.dev_ren := 0.U
      when(io.dev_rvalid) {
        io.data_valid := 1.U
        io.data_rdata := io.dev_rdata
        r_state       := rIdle
      }.otherwise {
        io.data_valid := 0.U
        io.data_rdata := 0.U
        r_state       := rStat1
      }
    }
  }

  /* ---------- ---------- write ---------- ---------- */

  // Write path state definitions
  val wIdle :: wStat0 :: wStat1 :: Nil = Enum(3)
  val w_state                          = RegInit(wIdle)

  val wen_r   = Reg(UInt(4.W))
  val wr_resp = Mux(io.dev_wrdy & (io.dev_wen === 0.U), true.B, false.B)

  // Write path logic
  switch(w_state) {
    is(wIdle) {
      io.data_wresp := 0.U
      when(io.data_wen.orR) {
        when(io.dev_wrdy) {
          io.dev_wen := io.data_wen
          w_state    := wStat1
        }.otherwise {
          wen_r   := io.data_wen
          w_state := wStat0
        }
        io.dev_waddr := io.data_addr
        io.dev_wdata := io.data_wdata
      }.otherwise {
        w_state := wIdle
      }
    }
    is(wStat0) {
      when(io.dev_wrdy) {
        io.dev_wen := wen_r
        w_state    := wStat1
      }.otherwise {
        io.dev_wen := 0.U
        w_state    := wStat0
      }
    }
    is(wStat1) {
      io.dev_wen := 0.U;
      io.dev_wen := 0.U
      when(wr_resp) {
        io.data_wresp := true.B
        w_state       := wIdle
      }.otherwise {
        io.data_wresp := false.B
        w_state       := wStat1
      }
    }
  }

}

import _root_.circt.stage.ChiselStage

object ICache extends App {
  ChiselStage.emitSystemVerilogFile(
    new DCache,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
