	.data
hello:
	.string "hello"
hello_len:
	.word   5                  # 0x5
hello_world:
	.string "abcd hello world"
hello_world_len:
	.word   16                 # 0x10

	.text
main: # @main
	addi    sp, sp, -16
	sw      ra, 12(sp)         # 4-byte Folded Spill
	sw      s0, 8(sp)          # 4-byte Folded Spill
	addi    s0, sp, 16
	li      a0, 0
	sw      a0, -12(s0)
# FIXME
	lui     a0, 0x10010        # hello_world
	addi    a0, a0, 12         # a0 = a0 + c
	li      a1, 16
	lui     a2, 0x10010        # hello
	li      a3, 5
	call    naive_match
	li      a7, 1
	ecall                      # print int
	li      a0, 0
	lw      ra, 12(sp)         # 4-byte Folded Reload
	lw      s0, 8(sp)          # 4-byte Folded Reload
	addi    sp, sp, 16
	ret
naive_match: # @naive_match
	addi    sp, sp, -48
	sw      ra, 44(sp)         # 4-byte Folded Spill
	sw      s0, 40(sp)         # 4-byte Folded Spill
	addi    s0, sp, 48
	sw      a0, -16(s0)
	sw      a1, -20(s0)
	sw      a2, -24(s0)
	sw      a3, -28(s0)
	lw      a0, -28(s0)
	bnez    a0, .LBB0_2
	j       .LBB0_1
.LBB0_1:
	li      a0, 0
	sw      a0, -12(s0)
	j       .LBB0_15
.LBB0_2:
	li      a0, 0
	sw      a0, -32(s0)
	j       .LBB0_3
.LBB0_3: # =>This Loop Header: Depth=1
	lw      a1, -32(s0)
	lw      a0, -20(s0)
	lw      a2, -28(s0)
	sub     a0, a0, a2
	blt     a0, a1, .LBB0_14
	j       .LBB0_4
.LBB0_4: # in Loop: Header=BB0_3 Depth=1
	li      a0, 0
	sw      a0, -36(s0)
	j       .LBB0_5
.LBB0_5: # Parent Loop BB0_3 Depth=1
	lw      a0, -36(s0)
	lw      a1, -28(s0)
	bge     a0, a1, .LBB0_10
	j       .LBB0_6
.LBB0_6: # in Loop: Header=BB0_5 Depth=2
	lw      a0, -16(s0)
	lw      a1, -32(s0)
	lw      a2, -36(s0)
	add     a1, a1, a2
	add     a0, a0, a1
	lbu     a0, 0(a0)
	lw      a1, -24(s0)
	add     a1, a1, a2
	lbu     a1, 0(a1)
	beq     a0, a1, .LBB0_8
	j       .LBB0_7
.LBB0_7: # in Loop: Header=BB0_3 Depth=1
	j       .LBB0_10
.LBB0_8: # in Loop: Header=BB0_5 Depth=2
	j       .LBB0_9
.LBB0_9: # in Loop: Header=BB0_5 Depth=2
	lw      a0, -36(s0)
	addi    a0, a0, 1
	sw      a0, -36(s0)
	j       .LBB0_5
.LBB0_10: # in Loop: Header=BB0_3 Depth=1
	lw      a0, -36(s0)
	lw      a1, -28(s0)
	bne     a0, a1, .LBB0_12
	j       .LBB0_11
.LBB0_11:
	lw      a0, -32(s0)
	sw      a0, -12(s0)
	j       .LBB0_15
.LBB0_12: # in Loop: Header=BB0_3 Depth=1
	j       .LBB0_13
.LBB0_13: # in Loop: Header=BB0_3 Depth=1
	lw      a0, -32(s0)
	addi    a0, a0, 1
	sw      a0, -32(s0)
	j       .LBB0_3
.LBB0_14:
	li      a0, -1
	sw      a0, -12(s0)
	j       .LBB0_15
.LBB0_15:
	lw      a0, -12(s0)
	lw      ra, 44(sp)         # 4-byte Folded Reload
	lw      s0, 40(sp)         # 4-byte Folded Reload
	addi    sp, sp, 48
	ret
