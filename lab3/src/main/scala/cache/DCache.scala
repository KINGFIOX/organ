package cache;

import chisel3._
import chisel3.util._

import common.Constants

import memory.blk_mem_gen_1

class DCache(n: Int = 2) extends Module {
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

  val hit_r = RegInit(false.B)
  dontTouch(hit_r)
  val hit_w = RegInit(false.B)
  dontTouch(hit_w)

  /* ---------- ---------- addr ---------- ---------- */

  val uncached = Mux((io.data_addr(31, 16) === "hffff".U(16.W)) & (io.data_ren =/= 0.U | io.data_wen =/= 0.U), true.B, false.B)
  dontTouch(uncached)

  val nonAlign = (io.data_addr & "b011".U(Constants.Addr_Width.W)).orR

  /* ---------- ---------- sram ---------- ---------- */

  val tagSrams  = VecInit(Seq.fill(n)(Module(new blk_mem_gen_1).io))
  val dataSrams = VecInit(Seq.fill(n)(Module(new blk_mem_gen_1).io))

// Configure each SRAM module
  for (i <- 0 until n) {
    tagSrams(i).dina := DontCare
    tagSrams(i).wea  := false.B
    tagSrams(i).clka := clock

    dataSrams(i).dina := DontCare
    dataSrams(i).wea  := false.B
    dataSrams(i).clka := clock
  }

  /* ---------- ---------- addr 划分 ---------- ---------- */

  val tag    = io.data_addr(Constants.Tag_up, Constants.Tag_down)
  val index  = io.data_addr(Constants.Index_up, Constants.Index_down)
  val offset = io.data_addr(Constants.Offset_up, Constants.Offset_down)

  /* ---------- ---------- sram 数据通路 ---------- ---------- */
  val dcacheOutVec = VecInit(Seq.fill(Constants.CacheLine_Len)(0.U(Constants.Word_Width.W)))

  /* ---------- cache ---------- */
  val hitVec = WireInit(VecInit(Seq.fill(n)(false.B)))

  val hitIndex = RegInit(0.U(log2Ceil(n).W))
  for (i <- 0 until n) {
    tagSrams(i).addra  := index
    dataSrams(i).addra := index
    when(tagSrams(i).douta(Constants.Tag_Width, 0) === Cat(1.U, tag)) {
      hitVec(i)    := true.B
      dcacheOutVec := dataSrams(i).douta.asTypeOf(dcacheOutVec)
      hitIndex     := i.U
    }
  }

  /* ---------- ---------- victim ---------- ---------- */

  val cnt = Counter(n)
  cnt.inc()
  val victim = cnt.value

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
      when(hitVec.asUInt.orR) { /* hit */
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
        tagSrams(victim).wea   := true.B
        tagSrams(victim).dina  := Cat(1.U, tag)
        dataSrams(victim).wea  := true.B
        dataSrams(victim).dina := io.dev_rdata

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
        when(~uncached && ~nonAlign && hitVec.asUInt.orR) { /* cacheline 直接作废 */
          val line = dataSrams(hitIndex).douta.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
          line(offset)             := wdata
          dataSrams(hitIndex).wea  := true.B
          dataSrams(hitIndex).dina := line.asUInt
          hit_w                    := true.B
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
    new DCache(4),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
