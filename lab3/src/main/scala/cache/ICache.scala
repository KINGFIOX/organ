package cache

import chisel3._
import chisel3.util._

import common.Constants
import scala.collection.immutable.Stream.Cons

import memory.blk_mem_gen_1;

class ICache(n: Int = 2) extends Module {
  require(isPow2(n))
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
  val hit = RegInit(false.B)
  dontTouch(hit);

  /* ---------- ---------- 初始化 sram ---------- ---------- */
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
  val tag    = io.inst_addr(Constants.Tag_up, Constants.Tag_down)
  val index  = io.inst_addr(Constants.Index_up, Constants.Index_down)
  val offset = io.inst_addr(Constants.Offset_up, Constants.Offset_down)

  /* ---------- ---------- 判断命中，以及选取那一路的逻辑 ---------- ---------- */

  val dataOutVec = VecInit(Seq.fill(Constants.CacheLine_Len)(0.U(Constants.Word_Width.W)))

  /* ---------- cache ---------- */
  val hitVec = WireInit(VecInit(Seq.fill(n)(false.B)))

  for (i <- 0 until n) {
    tagSrams(i).addra  := index
    dataSrams(i).addra := index
    when(tagSrams(i).douta(Constants.Tag_Width, 0) === Cat(1.U, tag)) {
      hitVec(i)  := true.B
      dataOutVec := dataSrams(i).douta.asTypeOf(dataOutVec)
    }
  }

  /* ---------- ---------- victim ---------- ---------- */

  val cnt = Counter(n)
  cnt.inc()
  val victim = cnt.value

  /* ---------- ---------- 状态机 ---------- ---------- */
  // 0 1 2
  val sIdle :: sTAG_CHECK :: sREFILL :: sI_S1_mem :: Nil = Enum(4)
  val state                                              = RegInit(sIdle)

  switch(state) {
    is(sIdle) {
      /* ---------- 状态 ---------- */
      when(io.inst_rreq) {
        state := sTAG_CHECK
      }
    }
    is(sTAG_CHECK) {
      when(hitVec.asUInt =/= 0.U) {
        io.inst_valid := true.B
        io.inst_out   := dataOutVec(offset)
        state         := sIdle
        hit           := true.B
      }.otherwise {
        state := sREFILL
      }
    }
    is(sREFILL) { /* sIdle */
      io.mem_raddr := Cat(tag, index, 0.U((Constants.Offset_Width + Constants.Word_Align).W))
      when(io.mem_rrdy) {
        io.mem_ren := "b1111".U(4.W)
        state      := sI_S1_mem
      }
    }
    is(sI_S1_mem) {
      /* ---------- 状态 ---------- */
      when(io.mem_rvalid) {
        state                  := sTAG_CHECK
        tagSrams(victim).wea   := true.B
        tagSrams(victim).dina  := Cat(1.U, tag)
        dataSrams(victim).wea  := true.B
        dataSrams(victim).dina := io.mem_rdata
      }
    }
  }

}

import _root_.circt.stage.ChiselStage

object ICache extends App {
  ChiselStage.emitSystemVerilogFile(
    new ICache(4),
    // args        = Array("--target", "verilog"),
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
}
