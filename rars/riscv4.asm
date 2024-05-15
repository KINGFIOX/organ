	.text
main:
        addi    sp,sp,-32
        sw      ra,28(sp)
        sw      s0,24(sp)
        addi    s0,sp,32
        lui     a5,%hi(arr)
        addi    a5,a5,%lo(arr)
        lw      a4,0(a5)
        lw      a5,4(a5)
        sw      a4,-20(s0)
        li      a5,1
        sw      a5,-24(s0)
        j       .L2
.L5:
        lw      a5,-20(s0)
        mv      a0,a5
        srai    a5,a5,31
        mv      a1,a5
        lui     a5,%hi(arr)
        addi    a4,a5,%lo(arr)
        lw      a5,-24(s0)
        slli    a5,a5,3
        add     a5,a4,a5
        lw      a2,0(a5)
        lw      a3,4(a5)
        mv      a4,a0
        mv      a5,a1
        mv      a7,a3
        mv      a6,a5
        bgt     a7,a6,.L4
        mv      a7,a3
        mv      a6,a5
        bne     a7,a6,.L3
        mv      a7,a2
        mv      a6,a4
        bleu    a7,a6,.L3
.L4:
        mv      a4,a2
        mv      a5,a3
.L3:
        sw      a4,-20(s0)
        lw      a5,-24(s0)
        addi    a5,a5,1
        sw      a5,-24(s0)
.L2:
        lw      a4,-24(s0)
        li      a5,9
        ble     a4,a5,.L5
        lw      a5,-20(s0)
        mv      a0,a5
        lw      ra,28(sp)
        lw      s0,24(sp)
        addi    sp,sp,32
		li		a7, 10
		ecall

	.data
arr:
        .word   4
        .word   0
        .word   2
        .word   0
        .word   7
        .word   0
        .word   5
        .word   0
        .word   8
        .word   0
        .word   -9
        .word   -1
        .word   11
        .word   0
        .word   32
        .word   0
        .word   20
        .word   0
        .word   18
        .word   0

