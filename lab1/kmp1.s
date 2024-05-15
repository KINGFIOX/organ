########## ########## data ########## ##########

	.data
	.align 3
g_pattern:
        .space	256
g_text:
        .space	256

########## ########## main ########## ##########

	.text
main:
        addi    sp,sp,-32
        sw      ra,28(sp)
        sw      s0,24(sp)
        addi    s0,sp,32
        lui     a5,%hi(.LC0)
        addi    a0,a5,%lo(.LC0)
        jal ra, printString
        # call    printString
        lui     a5, 0x10010 # pattern
        addi    a0,a5, 0
        jal ra, readString
        # call    readString
        sw      a0,-20(s0)
        lui     a5,%hi(.LC1)
        addi    a0,a5,%lo(.LC1)
        jal ra, printString
        # call    printString
        lui     a5, 0x10010 # text
        addi    a0,a5, 0x100
        jal ra, readString
        # call    readString
        sw      a0,-24(s0)
        lw      a3,-24(s0)
        lui     a5, 0x10010 # text
        addi    a2,a5, 0x100
        lw      a1,-20(s0)
        lui     a5, 0x10010 # pattern
        addi    a0,a5, 0
        jal ra, KMPSearch
        # call    KMPSearch
        sw      a0,-28(s0)
        lw      a5,-28(s0)
        bne     a5,zero,.L22
        lui     t5,%hi(.LC2)
        addi    a0,t5,%lo(.LC2)
        jal ra, printString
        # call    printIntLn
.L22:
        addi      a5, zero,0
        # li      a5,0
        addi      a0,a5, 0
        # mv      a0,a5
        lw      ra,28(sp)
        lw      s0,24(sp)
        addi    sp,sp,32
		addi a7, zero, 10 # exit 退出
		ecall
        # jr      ra

strLen:
        addi    sp,sp,-48
        sw      ra,44(sp)
        sw      s0,40(sp)
        addi    s0,sp,48
        sw      a0,-36(s0)
        sw      zero,-20(s0)
        j       .L2
.L3:
        lw      a5,-20(s0)
        addi    a5,a5,1
        sw      a5,-20(s0)
.L2:
        lw      a5,-20(s0)
        lw      a4,-36(s0)
        add     a5,a4,a5
        lbu     a5,0(a5)
        bne     a5,zero,.L3
        lw      a5,-20(s0)
        addi      a0,a5, 0
        # mv      a0,a5
        lw      ra,44(sp)
        lw      s0,40(sp)
        addi    sp,sp,48
        jr      ra
readString:
        addi    sp,sp,-48
        sw      ra,44(sp)
        sw      s0,40(sp)
        addi    s0,sp,48
        sw      a0,-36(s0)
        # li      a4,256
		addi a4, zero, 256
        lw      a5,-36(s0)
        addi a0, a5, 0
        # mv a0, a5
addi a1, a4, 0
# mv a1, a4
addi a7, zero, 8
# li a7, 8
ecall

        lw      a0,-36(s0)
        jal 	ra, strLen
        # call    strLen
        addi      a5,a0, 0
        # mv      a5,a0
        addi    a5,a5,-1
        sw      a5,-20(s0)
        lw      a5,-20(s0)
        lw      a4,-36(s0)
        add     a5,a4,a5
        sb      zero,0(a5)
        lw      a5,-20(s0)
        addi      a0,a5, 0
        # mv      a0,a5
        lw      ra,44(sp)
        lw      s0,40(sp)
        addi    sp,sp,48
        jr      ra
printIntLn:
        addi    sp,sp,-32
        sw      ra,28(sp)
        sw      s0,24(sp)
        addi    s0,sp,32
        sw      a0,-20(s0)
        lw      a5,-20(s0)
        addi a0, a5, 0
        # mv a0, a5
# li a7, 1
addi a7, zero, 1
ecall
        addi      a5, zero,10
        # li      a5,10
        addi a0, a5, 0
        # mv a0, a5
addi a7, zero, 11
# li a7, 11
ecall
        lw      ra,28(sp)
        lw      s0,24(sp)
        addi    sp,sp,32
        jr      ra
printString:
        addi    sp,sp,-32
        sw      ra,28(sp)
        sw      s0,24(sp)
        addi    s0,sp,32
        sw      a0,-20(s0)
        lw      a5,-20(s0)
        addi a0, a5, 0
        # mv a0, a5
addi a7, zero, 4
# li a7, 4
ecall   
        lw      ra,28(sp)
        lw      s0,24(sp)
        addi    sp,sp,32
        jr      ra
computeLPSArray:
        addi    sp,sp,-48
        sw      ra,44(sp)
        sw      s0,40(sp)
        addi    s0,sp,48
        sw      a0,-36(s0)
        sw      a1,-40(s0)
        sw      a2,-44(s0)
        sw      zero,-20(s0)
        lw      a5,-44(s0)
        sw      zero,0(a5)
		addi a5, zero, 1
        # li      a5,1
        sw      a5,-24(s0)
        j       .L10
.L13:
        lw      a5,-24(s0)
        lw      a4,-36(s0)
        add     a5,a4,a5
        lbu     a4,0(a5)
        lw      a5,-20(s0)
        lw      a3,-36(s0)
        add     a5,a3,a5
        lbu     a5,0(a5)
        bne     a4,a5,.L11
        lw      a5,-20(s0)
        addi    a5,a5,1
        sw      a5,-20(s0)
        lw      a5,-24(s0)
        slli    a5,a5,2
        lw      a4,-44(s0)
        add     a5,a4,a5
        lw      a4,-20(s0)
        sw      a4,0(a5)
        lw      a5,-24(s0)
        addi    a5,a5,1
        sw      a5,-24(s0)
        j       .L10
.L11:
        lw      a5,-20(s0)
        beq     a5,zero,.L12
        lw      a4,-20(s0)
		# FIXME
		lui a5, 0x40000 # 1073741824的高20位是0x40000
		addi a5, a5, 0 # 1073741824的低12位是0
        # li      a5,1073741824
        addi    a5,a5,-1
        add     a5,a4,a5
        slli    a5,a5,2
        lw      a4,-44(s0)
        add     a5,a4,a5
        lw      a5,0(a5)
        sw      a5,-20(s0)
        j       .L10
.L12:
        lw      a5,-24(s0)
        slli    a5,a5,2
        lw      a4,-44(s0)
        add     a5,a4,a5
        sw      zero,0(a5)
        lw      a5,-24(s0)
        addi    a5,a5,1
        sw      a5,-24(s0)
.L10:
        lw      a4,-24(s0)
        lw      a5,-40(s0)
        blt     a4,a5,.L13
        
        
        lw      ra,44(sp)
        lw      s0,40(sp)
        addi    sp,sp,48
        jr      ra
KMPSearch:
        addi    sp,sp,-64
        sw      ra,60(sp)
        sw      s0,56(sp)
        sw      s1,52(sp)
        addi    s0,sp,64
        sw      a0,-52(s0)
        sw      a1,-56(s0)
        sw      a2,-60(s0)
        sw      a3,-64(s0)
        addi      a3,sp, 0
        # mv      a3,sp
        addi      s1,a3, 0
        # mv      s1,a3
        lw      a3,-56(s0)
        addi    a2,a3,-1
        sw      a2,-32(s0)
        addi      a2,a3, 0
        # mv      a2,a3
        addi      t3,a2, 0
        # mv      t3,a2
        addi      t4, zero,0
        # li      t4,0
        srli    a2,t3,27
        slli    a7,t4,5
        or      a7,a2,a7
        slli    a6,t3,5
        addi      a2,a3, 0
        # mv      a2,a3
        addi      t1,a2, 0
        # mv      t1,a2
        addi      t2, zero,0
        # li      t2,0
        srli    a2,t1,27
        slli    a5,t2,5
        or      a5,a2,a5
        slli    a4,t1,5
        addi      a5,a3, 0
        # mv      a5,a3
        slli    a5,a5,2
        addi    a5,a5,15
        srli    a5,a5,4
        slli    a5,a5,4
        sub     sp,sp,a5
        addi      a5,sp, 0
        # mv      a5,sp
        addi    a5,a5,3
        srli    a5,a5,2
        slli    a5,a5,2
        sw      a5,-36(s0)
        lw      a2,-36(s0)
        lw      a1,-56(s0)
        lw      a0,-52(s0)
        jal ra, computeLPSArray
        # call    computeLPSArray
        sw      zero,-20(s0)
        sw      zero,-24(s0)
        sw      zero,-28(s0)
        j       .L15
.L19:
        lw      a5,-24(s0)
        lw      a4,-52(s0)
        add     a5,a4,a5
        lbu     a4,0(a5)
        lw      a5,-20(s0)
        lw      a3,-60(s0)
        add     a5,a3,a5
        lbu     a5,0(a5)
        bne     a4,a5,.L16
        lw      a5,-20(s0)
        addi    a5,a5,1
        sw      a5,-20(s0)
        lw      a5,-24(s0)
        addi    a5,a5,1
        sw      a5,-24(s0)
.L16:
        lw      a4,-24(s0)
        lw      a5,-56(s0)
        bne     a4,a5,.L17
        lw      a4,-20(s0)
        lw      a5,-24(s0)
        sub     a5,a4,a5
        addi      a0,a5, 0
        # mv      a0,a5
        jal ra, printIntLn
        # call    printIntLn
        lw      a5,-28(s0)
        addi    a5,a5,1
        sw      a5,-28(s0)
        lw      a5,-24(s0)
        addi    a5,a5,-1
        lw      a4,-36(s0)
        slli    a5,a5,2
        add     a5,a4,a5
        lw      a5,0(a5)
        sw      a5,-24(s0)
        j       .L15
.L17:
        lw      a4,-20(s0)
        lw      a5,-64(s0)
        bge     a4,a5,.L15
        lw      a5,-24(s0)
        lw      a4,-52(s0)
        add     a5,a4,a5
        lbu     a4,0(a5)
        lw      a5,-20(s0)
        lw      a3,-60(s0)
        add     a5,a3,a5
        lbu     a5,0(a5)
        beq     a4,a5,.L15
        lw      a5,-24(s0)
        beq     a5,zero,.L18
        lw      a5,-24(s0)
        addi    a5,a5,-1
        lw      a4,-36(s0)
        slli    a5,a5,2
        add     a5,a4,a5
        lw      a5,0(a5)
        sw      a5,-24(s0)
        j       .L15
.L18:
        lw      a5,-20(s0)
        addi    a5,a5,1
        sw      a5,-20(s0)
.L15:
        lw      a4,-20(s0)
        lw      a5,-64(s0)
        blt     a4,a5,.L19
        lw      a5,-28(s0)
        addi      sp,s1, 0
        # mv      sp,s1
        addi      a0,a5, 0
        # mv      a0,a5
        addi    sp,s0,-64
        lw      ra,60(sp)
        lw      s0,56(sp)
        lw      s1,52(sp)
        addi    sp,sp,64
        jr      ra

	.section .rodata
	.align 3
.LC0:
        .string "please input pattern: "
        .string ""
.LC1:
        .string "please input text: "
        .string ""
.LC2:
        .string "not found"
        .string ""
