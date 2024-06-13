package cache;

import chisel3._
import chisel3.util._

import common.Constants

import memory.blk_mem_gen_1

class DCache(n: Int = 4) extends Module {
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

  val wr_resp = ~RegNext(io.dev_wrdy) && io.dev_wrdy

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

  val hitVec = WireInit(VecInit(Seq.fill(n)(false.B)))

  val is_dirty = WireInit(false.B)

  val hit_i = WireInit(0.U(log2Ceil(n).W))
  for (i <- 0 until n) {
    tagSrams(i).addra  := index
    dataSrams(i).addra := index
    when(tagSrams(i).douta(Constants.Tag_Width, 0) === Cat(1.U, tag)) { // 只有命中了，才会走下面这个流程
      hitVec(i) := true.B
      hit_i     := i.asUInt
      is_dirty  := tagSrams(i).douta(Constants.Tag_Width + 1)
    }
  }

  /* ---------- ---------- victim ---------- ---------- */

  val victim_cnt = Counter(n)
  victim_cnt.inc()

  /* ---------- read ---------- */

  val r_IDLE :: r_NOCACHE0 :: r_NOCACHE1 :: r_CHECK :: r_CLEAN_REFILL0 :: r_CLEAN_REFILL1 :: r_WRITEBACK0 :: r_WRITEBACK1 :: r_DIRTY_REFILL0 :: r_DIRTY_REFILL1 :: Nil = Enum(10)
  val r_state                                                                                                                                                          = RegInit(r_IDLE)
  dontTouch(r_state)

  val ren_r = RegInit(0.U(Constants.CacheLine_Len.W))

  /* write 计数器 */
  val r_cnt = Counter(Constants.CacheLine_Len)
  dontTouch(r_cnt.value)

  val r_victim = RegInit(0.U(log2Ceil(n).W))

  switch(r_state) {
    is(r_IDLE) { // 0
      when(io.data_ren =/= 0.U) {
        ren_r := io.data_ren
        when(uncached || nonAlign) { /* 直接访问主存 */
          r_state := r_NOCACHE0
        }.otherwise { /* 访问 cache */
          r_state := r_CHECK
        }
      }
    }
    is(r_NOCACHE0) { // 1
      when(io.dev_rrdy) {
        io.dev_ren   := ren_r
        io.dev_raddr := io.data_addr
        r_state      := r_NOCACHE1
      }
    }
    is(r_NOCACHE1) { // 2
      io.dev_ren := 0.U
      when(io.dev_rvalid) {
        io.data_rdata := io.dev_rdata
        io.data_valid := 1.U
        r_state       := r_IDLE
      }
    }
    is(r_CHECK) { // 3
      when(hitVec.asUInt =/= 0.U) { /* hit */
        hit_r := true.B
        val line = dataSrams(hit_i).douta.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
        io.data_rdata := line(offset)
        io.data_valid := true.B
        r_state       := r_IDLE
      }.otherwise { /* miss */
        r_victim := victim_cnt.value
        when(is_dirty) { /* 脏数据，写回，然后再载入 */
          r_state := r_WRITEBACK0
        }.otherwise { /* 不是脏数据，那么直接载入 */
          r_state := r_CLEAN_REFILL0
        }
      }
    }
    is(r_CLEAN_REFILL0) { // 4
      when(io.dev_rrdy) {
        io.dev_ren   := "b1111".U(4.W)
        io.dev_raddr := Cat(tag, index, 0.U((Constants.Offset_Width + Constants.Word_Align).W))
        r_state      := r_CLEAN_REFILL1
      }
    }
    is(r_CLEAN_REFILL1) { // 5
      when(io.dev_rvalid) {
        /* 更新 cache */
        tagSrams(r_victim).wea   := true.B
        tagSrams(r_victim).dina  := Cat(0.U, 1.U, tag)
        dataSrams(r_victim).wea  := true.B
        dataSrams(r_victim).dina := io.dev_rdata

        /* 对外返回响应 */
        val line = io.dev_rdata.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
        io.data_rdata := line(offset)
        io.data_valid := true.B

        /* 状态 */
        r_state := r_IDLE
      }
    }
    is(r_WRITEBACK0) { // 6
      when(RegNext(r_cnt.value) === (Constants.CacheLine_Len - 1).U && r_cnt.value === 0.U /* 到点了 */ ) { /* 跳入 write allocate 状态 */
        r_state := r_DIRTY_REFILL0
      }.otherwise {
        when(io.dev_wrdy) { /* write back */
          /* 定义临时变量 */
          val waddr /* 写回原来的地址 */ = Cat(tagSrams(r_victim).douta(Constants.Tag_Width - 1, 0), index, r_cnt.value(Constants.Offset_Width - 1, 0), 0.U(Constants.Word_Align.W)) /* 合成地址 */
          val line                = dataSrams(r_victim).douta.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))

          /* trigger */
          io.dev_wen   := "b1111".U(4.W)
          io.dev_waddr := waddr
          io.dev_wdata := line(r_cnt.value)
          r_cnt.inc()

          /* 状态 */
          r_state := r_WRITEBACK1
        }
      }
    }
    is(r_WRITEBACK1) { // 7
      when(wr_resp) {
        r_state := r_WRITEBACK0
      }
    }
    is(r_DIRTY_REFILL0) { // 8
      when(io.dev_rrdy) {
        io.dev_ren   := "b1111".U(4.W)
        io.dev_raddr := Cat(tag, index, 0.U((Constants.Offset_Width + Constants.Word_Align).W))
        r_state      := r_DIRTY_REFILL1
      }
    }
    is(r_DIRTY_REFILL1) { // 9
      when(io.dev_rvalid) {
        tagSrams(r_victim).wea   := true.B
        tagSrams(r_victim).dina  := Cat(1.U, 1.U, tag)
        dataSrams(r_victim).wea  := true.B
        dataSrams(r_victim).dina := io.dev_rdata

        /* 响应, 状态 */
        io.data_rdata := io.dev_rdata
        io.data_valid := true.B
        r_state       := r_IDLE
      }
    }
  }

  /* ---------- write ---------- */

  val w_IDLE :: w_STAT0_uncached :: w_STAT1_uncached :: w_TAG_CHECK :: w_CLEAN_REFILL0 :: w_CLEAN_REFILL1 :: w_WRITEBACK0 :: w_WRITEBACK1 :: w_DIRTY_REFILL0 :: w_DIRTY_REFILL1 :: Nil = Enum(10)
  val w_state                                                                                                                                                                          = RegInit(w_IDLE)
  dontTouch(w_state)

  val wen_r = RegInit(0.U(Constants.CacheLine_Len.W))
  val wdata = RegInit(0.U(Constants.Word_Width.W)) /* 这个必须有，因为 wdata 会立即撤销 */

  /* write 计数器 */
  val w_cnt = Counter(Constants.CacheLine_Len)
  dontTouch(w_cnt.value)

  val w_victim = RegInit(0.U(log2Ceil(n).W))

  /* 状态机 */
  switch(w_state) {
    is(w_IDLE) { // 0
      when(io.data_wen =/= 0.U) {
        wen_r := io.data_wen
        wdata := io.data_wdata
        when(uncached || nonAlign) { /* 如果是不 cache */
          w_state := w_STAT0_uncached
        }.otherwise {
          w_state := w_TAG_CHECK
        }
      }
    }
    is(w_STAT0_uncached) { // 1
      when(io.dev_wrdy) {
        io.dev_wen   := wen_r
        io.dev_waddr := io.data_addr
        io.dev_wdata := wdata
        w_state      := w_STAT1_uncached
      }
    }
    is(w_STAT1_uncached) { // 2
      when(wr_resp) {
        io.data_wresp := true.B
        w_state       := w_IDLE
      }
    }
    is(w_TAG_CHECK) { // 3
      when(hitVec.asUInt =/= 0.U) { /* 命中了 */
        tagSrams(hit_i).wea  := true.B
        tagSrams(hit_i).dina := Cat(1.U, 1.U, tag)
        dataSrams(hit_i).wea := true.B
        val line = dataSrams(hit_i).douta.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
        line(offset)          := wdata
        dataSrams(hit_i).dina := line.asUInt
        hit_w                 := true.B

        /* 响应, 状态 */
        io.data_wresp := true.B
        w_state       := w_IDLE
      }.otherwise { /* 没命中 */
        w_victim := victim_cnt.value
        when(is_dirty) { /* 脏数据，写回，然后再载入 */
          w_state := w_WRITEBACK0
        }.otherwise { /* 不是脏数据，那么直接载入 */
          w_state := w_CLEAN_REFILL0
        }
      }
    }
    is(w_CLEAN_REFILL0) { // 4
      when(io.dev_rrdy) {
        io.dev_ren   := "b1111".U(4.W)
        io.dev_raddr := io.data_addr
        w_state      := w_CLEAN_REFILL1
      }
    }
    is(w_CLEAN_REFILL1) { // 5
      io.dev_ren := 0.U
      when(io.dev_rvalid) {
        tagSrams(w_victim).wea  := true.B
        tagSrams(w_victim).dina := Cat(1.U, 1.U, tag)
        dataSrams(w_victim).wea := true.B
        val line = io.dev_rdata.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
        line(offset)             := wdata
        dataSrams(w_victim).dina := line.asUInt

        /* 响应, 状态 */
        io.data_wresp := true.B
        w_state       := w_IDLE
      }
    }
    is(w_WRITEBACK0) { // 6
      when(RegNext(w_cnt.value) === (Constants.CacheLine_Len - 1).U && w_cnt.value === 0.U) { /* 跳入 write allocate 状态 */
        w_state := w_DIRTY_REFILL0
      }.otherwise {
        when(io.dev_wrdy) { /* write back */
          /* 定义临时变脸 */
          val waddr = Cat(tagSrams(w_victim).douta(Constants.Tag_Width - 1, 0), index, w_cnt.value(Constants.Offset_Width - 1, 0), 0.U(Constants.Word_Align.W)) /* 合成地址 */
          val line  = dataSrams(w_victim).douta.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))

          /* trigger */
          io.dev_wen   := "b1111".U(4.W)
          io.dev_waddr := waddr
          io.dev_wdata := line(w_cnt.value)
          w_cnt.inc()

          /* 状态 */
          w_state := w_WRITEBACK1
        }
      }
    }
    is(w_WRITEBACK1) { // 7
      when(wr_resp) {
        w_state := w_WRITEBACK0
      }
    }
    is(w_DIRTY_REFILL0) { // 8
      when(io.dev_rrdy) {
        io.dev_ren   := "b1111".U(4.W)
        io.dev_raddr := Cat(tag, index, 0.U((Constants.Offset_Width + Constants.Word_Align).W))
        w_state      := w_DIRTY_REFILL1
      }
    }
    is(w_DIRTY_REFILL1) { // 9
      when(io.dev_rvalid) {
        val line = io.dev_rdata.asTypeOf(Vec(Constants.CacheLine_Len, UInt(Constants.Word_Width.W)))
        line(offset)             := wdata
        tagSrams(w_victim).wea   := true.B
        tagSrams(w_victim).dina  := Cat(1.U, 1.U, tag)
        dataSrams(w_victim).wea  := true.B
        dataSrams(w_victim).dina := line.asUInt

        /* 响应, 状态 */
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
