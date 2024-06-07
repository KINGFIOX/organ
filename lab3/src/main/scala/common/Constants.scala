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

  def Word_Bytes   = Word_Width / 8 // 4
  def Word_Align   = log2Floor(Word_Bytes) // 2
  def Offset_up    = Word_Align + Offset_Width - 1 // 3
  def Offset_down  = Word_Align // 2
  def Offset_Width = log2Floor(CacheLine_Len) // 偏移量 2

  def Index_up   = Offset_up + Index_Width - 1 // 9
  def Index_down = Offset_up + 1 // 4

  def Tag_up    = Addr_Width - 1 // 31
  def Tag_down  = Index_up + 1 // 10
  def Tag_Width = Tag_up - Tag_down + 1 // 22

}
