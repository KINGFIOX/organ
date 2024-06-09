package cache;

import chisel3._
import chisel3.util._

class LRU(n: Int) extends Module {
  require(isPow2(n), "Size must be a power of 2")

  val io = IO(new Bundle {
    val hitVec = Input(Vec(n, Bool()))
    val victim = Output(UInt(log2Ceil(n).W))
  })

  val lruMatrix = Seq.fill(n)(Counter(n))

  val wrap       = RegInit(false.B)
  val wrap_index = RegInit(0.U(log2Ceil(n).W))

  for (i <- 0 until n) {
    when(io.hitVec(i)) {
      lruMatrix(i).reset()
    }.otherwise {
      when(lruMatrix(i).inc()) {
        wrap       := true.B
        wrap_index := i.asUInt
      }
    }
  }

  val values   = VecInit(lruMatrix.map(_.value))
  val maxIndex = RegInit(0.U(log2Floor(n).W))
  when(wrap) {
    maxIndex := wrap_index
  }.otherwise {
    for (i <- 1 until n) {
      when(values(i) > values(maxIndex)) {
        maxIndex := i.U
      }
    }
  }

  io.victim := maxIndex
}
