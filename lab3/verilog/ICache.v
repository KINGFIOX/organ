`timescale 1ns / 1ps

// `define BLK_LEN  4
// `define BLK_SIZE (`BLK_LEN*32)

module ICache(
    input  wire         cpu_clk,
    input  wire         cpu_rst,        // high active
    // Interface to CPU
    input  wire         inst_rreq,      // 来自CPU的取指请求
    input  wire [31:0]  inst_addr,      // 来自CPU的取指地址
    output reg          inst_valid,     // 输出给CPU的指令有效信号（读指令命中）
    output reg  [31:0]  inst_out,       // 输出给CPU的指令
    // Interface to Read Bus
    output reg  [ 3:0]  mem_ren,        // 输出给主存的读使能信号
    output reg  [31:0]  mem_raddr,      // 输出给主存的读地址
    input  wire [`BLK_SIZE-1:0] mem_rdata   // 来自主存的读数据
    // 握手
    input  wire         mem_rrdy,       // 主存就绪信号（高电平表示主存可接收ICache的读请求）
    input  wire         mem_rvalid,     // 来自主存的数据有效信号
);

`ifdef ENABLE_ICACHE    /******** 不要修改此行代码 ********/

    wire [?:0] tag_from_cpu   = /* TODO */;    // 主存地址的TAG
    wire [?:0] offset         = /* TODO */;    // 32位字偏移量
    wire       valid_bit      = /* TODO */;    // Cache行的有效位
    wire [?:0] tag_from_cache = /* TODO */;    // Cache行的TAG

    // TODO: 定义ICache状态机的状态变量


    wire hit = /* TODO */;

    always @(*) begin
        inst_valid = hit;
        inst_out   = /* TODO: 根据字偏移，选择Cache行中的某个32位字输出指令 */;
    end

    wire       cache_we     = /* TODO */;     // ICache存储体的写使能信号
    wire [?:0] cache_index  = /* TODO */;     // 主存地址的Cache索引 / ICache存储体的地址
    wire [?:0] cache_line_w = /* TODO */;     // 待写入ICache的Cache行
    wire [?:0] cache_line_r;                  // 从ICache读出的Cache行

    // ICache存储体：Block MEM IP核
    blk_mem_gen_1 U_isram (
        .clka   (cpu_clk),
        .wea    (cache_we),
        .addra  (cache_index),
        .dina   (cache_line_w),
        .douta  (cache_line_r)
    );

    // TODO: 编写状态机现态的更新逻辑


    // TODO: 编写状态机的状态转移逻辑


    // TODO: 生成状态机的输出信号


    /******** 不要修改以下代码 ********/
`else

    localparam IDLE  = 2'b00;
    localparam STAT0 = 2'b01;
    localparam STAT1 = 2'b11;
    reg [1:0] state, nstat;

    always @(posedge cpu_clk or posedge cpu_rst) begin
        state <= cpu_rst ? IDLE : nstat;
    end

    always @(*) begin
        case (state)
            IDLE:    nstat = inst_rreq ? (mem_rrdy ? STAT1 : STAT0) : IDLE;
            STAT0:   nstat = mem_rrdy ? STAT1 : STAT0;
            STAT1:   nstat = mem_rvalid ? IDLE : STAT1;
            default: nstat = IDLE;
        endcase
    end

    always @(posedge cpu_clk or posedge cpu_rst) begin
        if (cpu_rst) begin
            inst_valid <= 1'b0;
            mem_ren    <= 4'h0;
        end else begin
            case (state)
                IDLE: begin
                    inst_valid <= 1'b0;
                    mem_ren    <= (inst_rreq & mem_rrdy) ? 4'hF : 4'h0;
                    mem_raddr  <= inst_rreq ? inst_addr : 32'h0;
                end
                STAT0: begin
                    mem_ren    <= mem_rrdy ? 4'hF : 4'h0;
                end
                STAT1: begin
                    mem_ren    <= 4'h0;
                    inst_valid <= mem_rvalid ? 1'b1 : 1'b0;
                    inst_out   <= mem_rvalid ? mem_rdata[31:0] : 32'h0;
                end
                default: begin
                    inst_valid <= 1'b0;
                    mem_ren    <= 4'h0;
                end
            endcase
        end
    end

`endif

endmodule
