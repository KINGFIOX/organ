package common

import chisel3._
import chisel3.util._

object Constants {
  val Addr_Width    = 32 // Length of the address line
  val Word_Width    = 32 // Bit width of a word is 32 bits
  val CacheLine_Len = 4 // 一个 cacheline 只有 4 个 word, 那么就是 16Bytes 的 cacheline
  val Index_Width   = 6

  def CacheLine_Width = CacheLine_Len * Word_Width // 一个 cacheline 的 width 4 * 32 = 128
  def Cache_Len       = (1 << Index_Width) // 有 64 个 cacheline
  def Offset_Width    = log2Floor(CacheLine_Len) // 偏移量 2
  def Tag_Width       = Addr_Width - Offset_Width - Index_Width // tag 的长度 32 - 2 - 6 = 24
}
