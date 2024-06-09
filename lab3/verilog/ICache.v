`timescale 1ns / 1ps

// `define BLK_LEN  4
// `define BLK_SIZE (`BLK_LEN*32)

module ICache (
    input wire cpu_clk,
    input wire cpu_rst,  // high active
    // Interface to CPU
    input wire inst_rreq,  // 来自CPU的取指请求
    input wire [31:0] inst_addr,  // 来自CPU的取指地址
    output reg inst_valid,  // 输出给CPU的指令有效信号（读指令命中）
    output reg [31:0] inst_out,  // 输出给CPU的指令
    // Interface to Read Bus
    input  wire         mem_rrdy,       // 主存就绪信号（高电平表示主存可接收ICache的读请求）
    output reg [3:0] mem_ren,  // 输出给主存的读使能信号
    output reg [31:0] mem_raddr,  // 输出给主存的读地址
    input wire mem_rvalid,  // 来自主存的数据有效信号
    input wire [`BLK_SIZE-1:0] mem_rdata  // 来自主存的读数据
);


  localparam IDLE = 2'b00;
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
          mem_ren <= mem_rrdy ? 4'hF : 4'h0;
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


endmodule
