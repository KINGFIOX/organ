`timescale 1ns / 1ps

// `define BLK_LEN  4
// `define BLK_SIZE (`BLK_LEN*32)

module DCache(
    input  wire         cpu_clk,
    input  wire         cpu_rst,        // high active
    // Interface to CPU
    input  wire [ 3:0]  data_ren,       // 来自CPU的读使能信号
    input  wire [31:0]  data_addr,      // 来自CPU的地址（读、写共用）
    output reg          data_valid,     // 输出给CPU的数据有效信号
    output reg  [31:0]  data_rdata,     // 输出给CPU的读数据
    input  wire [ 3:0]  data_wen,       // 来自CPU的写使能信号
    input  wire [31:0]  data_wdata,     // 来自CPU的写数据
    output reg          data_wresp,     // 输出给CPU的写响应（高电平表示DCache已完成写操作）
    // Interface to Write Bus
    input  wire         dev_wrdy,       // 主存的写就绪信号（高电平表示主存可接收DCache的写请求）
    output reg  [ 3:0]  dev_wen,        // 输出给主存的写使能信号
    output reg  [31:0]  dev_waddr,      // 输出给主存的写地址
    output reg  [31:0]  dev_wdata,      // 输出给主存的写数据
    // Interface to Read Bus
    input  wire         dev_rrdy,       // 主存的读就绪信号（高电平表示主存可接收DCache的读请求）
    output reg  [ 3:0]  dev_ren,        // 输出给主存的读使能信号
    output reg  [31:0]  dev_raddr,      // 输出给主存的读地址
    input  wire         dev_rvalid,     // 来自主存的数据有效信号
    input  wire [`BLK_SIZE-1:0] dev_rdata   // 来自主存的读数据
);

    // Peripherals access should be uncached.
    wire uncached = (data_addr[31:16] == 16'hFFFF) & (data_ren != 4'h0 | data_wen != 4'h0) ? 1'b1 : 1'b0;

`ifdef ENABLE_DCACHE    /******** 不要修改此行代码 ********/

    wire [?:0] tag_from_cpu   = /* TODO */;    // 主存地址的TAG
    wire [?:0] offset         = /* TODO */;    // 32位字偏移量
    wire       valid_bit      = /* TODO */;    // Cache行的有效位
    wire [?:0] tag_from_cache = /* TODO */;    // Cache行的TAG

    // TODO: 定义DCache读状态机的状态变量


    wire hit_r = /* TODO */;        // 读命中
    wire hit_w = /* TODO */;        // 写命中

    always @(*) begin
        data_valid = hit_r;
        data_rdata = /* TODO: 根据字偏移，选择Cache行中的某个32位字输出数据 */; 
    end

    wire       cache_we     = /* TODO */;     // DCache存储体的写使能信号
    wire [?:0] cache_index  = /* TODO */;     // 主存地址的Cache索引 / DCache存储体的地址
    wire [?:0] cache_line_w = /* TODO */;     // 待写入DCache的Cache行
    wire [?:0] cache_line_r;                  // 从DCache读出的Cache行

    // DCache存储体：Block RAM IP核
    blk_mem_gen_1 U_dsram (
        .clka   (cpu_clk),
        .wea    (cache_we),
        .addra  (cache_index),
        .dina   (cache_line_w),
        .douta  (cache_line_r)
    );

    // TODO: 编写DCache读状态机现态的更新逻辑


    // TODO: 编写DCache读状态机的状态转移逻辑（注意处理uncached访问）


    // TODO: 生成DCache读状态机的输出信号





    ///////////////////////////////////////////////////////////
    // TODO: 定义DCache写状态机的状态变量


    // TODO: 编写DCache写状态机的现态更新逻辑


    // TODO: 编写DCache写状态机的状态转移逻辑（注意处理uncached访问）


    // TODO: 生成DCache写状态机的输出信号


    // TODO: 写命中时，只需修改Cache行中的其中一个字。请在此实现之。


    /******** 不要修改以下代码 ********/
`else

    localparam R_IDLE  = 2'b00;
    localparam R_STAT0 = 2'b01;
    localparam R_STAT1 = 2'b11;
    reg [1:0] r_state, r_nstat;
    reg [3:0] ren_r;

    always @(posedge cpu_clk or posedge cpu_rst) begin
        r_state <= cpu_rst ? R_IDLE : r_nstat;
    end

    always @(*) begin
        case (r_state)
            R_IDLE:  r_nstat = (|data_ren) ? (dev_rrdy ? R_STAT1 : R_STAT0) : R_IDLE;
            R_STAT0: r_nstat = dev_rrdy ? R_STAT1 : R_STAT0;
            R_STAT1: r_nstat = dev_rvalid ? R_IDLE : R_STAT1;
            default: r_nstat = R_IDLE;
        endcase
    end

    always @(posedge cpu_clk or posedge cpu_rst) begin
        if (cpu_rst) begin
            data_valid <= 1'b0;
            dev_ren    <= 4'h0;
        end else begin
            case (r_state)
                R_IDLE: begin
                    data_valid <= 1'b0;

                    if (|data_ren) begin
                        if (dev_rrdy)
                            dev_ren <= data_ren;
                        else
                            ren_r   <= data_ren;

                        dev_raddr <= data_addr;
                    end else
                        dev_ren   <= 4'h0;
                end
                R_STAT0: begin
                    dev_ren    <= dev_rrdy ? ren_r : 4'h0;
                end   
                R_STAT1: begin
                    dev_ren    <= 4'h0;
                    data_valid <= dev_rvalid ? 1'b1 : 1'b0;
                    data_rdata <= dev_rvalid ? dev_rdata : 32'h0;
                end
                default: begin
                    data_valid <= 1'b0;
                    dev_ren    <= 4'h0;
                end 
            endcase
        end
    end

    localparam W_IDLE  = 2'b00;
    localparam W_STAT0 = 2'b01;
    localparam W_STAT1 = 2'b11;
    reg  [1:0] w_state, w_nstat;
    reg  [3:0] wen_r;
    wire       wr_resp = dev_wrdy & (dev_wen == 4'h0) ? 1'b1 : 1'b0;

    always @(posedge cpu_clk or posedge cpu_rst) begin
        w_state <= cpu_rst ? W_IDLE : w_nstat;
    end

    always @(*) begin
        case (w_state)
            W_IDLE:  w_nstat = (|data_wen) ? (dev_wrdy ? W_STAT1 : W_STAT0) : W_IDLE;
            W_STAT0: w_nstat = dev_wrdy ? W_STAT1 : W_STAT0;
            W_STAT1: w_nstat = wr_resp ? W_IDLE : W_STAT1;
            default: w_nstat = W_IDLE;
        endcase
    end

    always @(posedge cpu_clk or posedge cpu_rst) begin
        if (cpu_rst) begin
            data_wresp <= 1'b0;
            dev_wen    <= 4'h0;
        end else begin
            case (w_state)
                W_IDLE: begin
                    data_wresp <= 1'b0;

                    if (|data_wen) begin
                        if (dev_wrdy)
                            dev_wen <= data_wen;
                        else
                            wen_r   <= data_wen;

                        dev_waddr  <= data_addr;
                        dev_wdata  <= data_wdata;
                    end else
                        dev_wen    <= 4'h0;
                end
                W_STAT0: begin
                    dev_wen    <= dev_wrdy ? wen_r : 4'h0;
                end
                W_STAT1: begin
                    dev_wen    <= 4'h0;
                    data_wresp <= wr_resp ? 1'b1 : 1'b0;
                end
                default: begin
                    data_wresp <= 1'b0;
                    dev_wen    <= 4'h0;
                end
            endcase
        end
    end

`endif

endmodule
