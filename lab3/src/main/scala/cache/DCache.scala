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

  /* ---------- read ---------- */

  val r_IDLE :: r_STAT0 :: r_STAT1 :: Nil = Enum(3)
  val r_state                             = RegInit(r_IDLE)

  val ren_r = RegInit(0.U(Constants.CacheLine_Len.W))

  switch(r_state) {
    is(r_IDLE) {
      io.data_valid := false.B
      when(io.data_ren.orR) {
        when(io.dev_rrdy) {
          io.dev_ren := io.data_ren
          r_state    := r_STAT1
        }.otherwise {
          ren_r   := io.data_ren
          r_state := r_STAT0
        }
        io.dev_raddr := io.data_addr
      }.otherwise {
        io.dev_ren := 0.U
      }
    }
    is(r_STAT0) {
      when(io.dev_rrdy) {
        io.dev_ren := ren_r
        r_state    := r_STAT1
      }.otherwise {
        io.dev_ren := 0.U
        r_state    := r_STAT0
      }
    }
    is(r_STAT1) {
      io.dev_ren := 0.U
      when(io.dev_rvalid) {
        io.dev_ren   := ren_r
        io.dev_raddr := io.data_addr
        r_state      := r_IDLE
      }.otherwise {
        io.data_valid := false.B
        io.data_rdata := 0.U
        r_state       := r_STAT1
      }
    }
  }

  /* ---------- read ---------- */

  val wr_resp = Mux(io.dev_wrdy & (io.dev_wen === 0.U), true.B, false.B)

  val w_IDLE :: w_STAT0 :: w_STAT1 :: Nil = Enum(3)
  val w_state                             = RegInit(w_IDLE)

  val wen_r = RegInit(0.U(Constants.CacheLine_Len.W))

  switch(w_state) {
    is(w_IDLE) {
      io.data_wresp := false.B
      when(io.data_wen.orR) {
        when(io.dev_wrdy) {
          io.dev_wen := io.data_wen
          w_state    := w_STAT1
        }.otherwise {
          wen_r   := io.data_wen
          w_state := w_STAT0
        }
        io.dev_waddr := io.data_addr
        io.dev_wdata := io.data_wdata
      }.otherwise {
        io.dev_wen := 0.U
        w_state    := w_IDLE
      }
    }
    is(w_STAT0) {
      when(io.dev_wrdy) {
        w_state    := w_STAT1
        io.dev_wen := wen_r
      }.otherwise {
        io.dev_wen := 0.U
        w_state    := w_STAT0
      }
    }
    is(w_STAT1) {
      io.dev_wen := 0.U
      when(wr_resp) {
        io.data_wresp := true.B
        w_state       := w_IDLE
      }.otherwise {
        io.data_wresp := false.B
        w_state       := w_STAT1
      }
    }
  }

}

import _root_.circt.stage.ChiselStage
import chisel3.stage.ChiselGeneratorAnnotation

object DCache extends App {
  ChiselStage.emitSystemVerilogFile(
    new DCache,
    args        = Array("--target", "verilog"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
