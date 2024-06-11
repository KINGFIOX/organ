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
    // val uncached   = Output(Bool())
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

  val hit_r = WireInit(false.B)
  dontTouch(hit_r)
  val hit_w = WireInit(false.B)
  dontTouch(hit_w)

  /* ---------- ---------- addr ---------- ---------- */

  val uncached = Mux((io.data_addr(31, 16) === "hffff".U(16.W)) & (io.data_ren =/= 0.U | io.data_wen =/= 0.U), true.B, false.B)
  dontTouch(uncached)
  val nonAlign = (io.data_addr & "b011".U(Constants.Addr_Width.W)).orR

  /* ---------- ---------- sram ---------- ---------- */
  import memory.blk_mem_gen_1

  val tagSram = Module(new blk_mem_gen_1)
  tagSram.io.addra := 0.U
  tagSram.io.dina  := 0.U
  tagSram.io.wea   := 0.U
  tagSram.io.clka  := clock
  val U_dsram = Module(new blk_mem_gen_1)
  U_dsram.io.addra := 0.U
  U_dsram.io.dina  := 0.U
  U_dsram.io.wea   := 0.U
  U_dsram.io.clka  := clock

  /* ---------- ---------- addr 划分 ---------- ---------- */

  val tag    = io.data_addr(Constants.Tag_up, Constants.Tag_down)
  val index  = io.data_addr(Constants.Index_up, Constants.Index_down)
  val offset = io.data_addr(Constants.Offset_up, Constants.Offset_down)

  /* ---------- ---------- sram 数据通路 ---------- ---------- */

  tagSram.io.addra := index
  U_dsram.io.addra := index

  val sram_valid_tag_out = tagSram.io.douta(Constants.Tag_Width, 0)

  val dcacheOutVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dcacheOutVec := U_dsram.io.douta.asTypeOf(dcacheOutVec)

  val dataInVec = Wire(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
  dataInVec := U_dsram.io.dina.asTypeOf(dataInVec)

  /* ---------- read ---------- */

  val r_IDLE :: r_NOCACHE0 /* 发送请求 */ :: r_NOCACHE1 /* 准备接受 */ :: r_CHECK :: r_REFILL1 :: Nil = Enum(5)
  val r_state                                                                                 = RegInit(r_IDLE)

  val ren_r = RegInit(0.U(Constants.CacheLine_Len.W))

  switch(r_state) {
    is(r_IDLE) {
      when(io.data_ren =/= 0.U) {
        ren_r := io.data_ren
        when(uncached || nonAlign) { /* 直接访问主存 */
          r_state := r_NOCACHE0
        }.otherwise { /* 访问 cache */
          r_state := r_CHECK
        }
      }
    }
    is(r_NOCACHE0) {
      when(io.dev_rrdy) {
        io.dev_ren   := ren_r
        io.dev_raddr := io.data_addr
        r_state      := r_NOCACHE1
      }
    }
    is(r_NOCACHE1) {
      io.dev_ren := 0.U
      when(io.dev_rvalid) {
        io.data_rdata := io.dev_rdata
        io.data_valid := 1.U
        r_state       := r_IDLE
      }
    }
    is(r_CHECK) {
      when(sram_valid_tag_out === Cat(1.U(1.W), tag)) { /* hit */
        io.data_valid := true.B
        io.data_rdata := dcacheOutVec(offset)
        r_state       := r_IDLE
        hit_r         := true.B
      }.otherwise { /* miss -> refill */
        when(io.dev_rrdy) {
          io.dev_ren   := "b1111".U(4.W)
          io.dev_raddr := Cat(tag, index, 0.U((Constants.Offset_Width + Constants.Word_Align).W))
          r_state      := r_REFILL1
        }
      }
    }
    is(r_REFILL1) {
      when(io.dev_rvalid) {
        /* 更新 cache */
        tagSram.io.wea  := true.B
        tagSram.io.dina := Cat(1.U, tag)
        U_dsram.io.wea  := true.B
        U_dsram.io.dina := io.dev_rdata

        /* 状态 */
        r_state := r_CHECK
      }
    }
  }

  /* ---------- write ---------- */

  val w_IDLE :: w_STAT0 :: w_STAT1 :: Nil = Enum(3)
  val w_state                             = RegInit(w_IDLE)

  val wen_r = RegInit(0.U(Constants.CacheLine_Len.W))
  val wdata = RegInit(0.U(Constants.Word_Width.W)) /* 这个必须有，因为 wdata 会立即撤销 */

  // /* io.dev_wrdy 再次拉高的时候，就行了 */
  // val wr_resp = io.dev_wrdy & (io.dev_wen === 0.U)
  val wr_resp = ~RegNext(io.dev_wrdy) && io.dev_wrdy

  switch(w_state) {
    is(w_IDLE) {
      when(io.data_wen =/= 0.U) {
        wen_r   := io.data_wen
        wdata   := io.data_wdata
        w_state := w_STAT0
      }
    }
    is(w_STAT0) {
      when(io.dev_wrdy) {
        io.dev_wen   := wen_r
        io.dev_waddr := io.data_addr
        io.dev_wdata := wdata
        w_state      := w_STAT1
        when(~uncached && ~nonAlign && sram_valid_tag_out === Cat(1.U(1.W), tag)) { /* cacheline 直接作废 */
          val line = U_dsram.io.douta.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
          line(offset)    := wdata
          U_dsram.io.wea  := true.B
          U_dsram.io.dina := line.asUInt
          hit_w           := true.B
        }
      }
    }
    is(w_STAT1) {
      when(wr_resp) {
        io.data_wresp := true.B
        w_state       := w_IDLE
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
