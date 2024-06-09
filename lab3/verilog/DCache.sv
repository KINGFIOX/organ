`ifdef ENABLE_DCACHE

module DCache (
    input          cpu_clk,
    cpu_rst,
    input  [  3:0] data_ren,
    input  [ 31:0] data_addr,
    output         data_valid,
    output [ 31:0] data_rdata,
    input  [  3:0] data_wen,
    input  [ 31:0] data_wdata,
    output         data_wresp,
    input          dev_wrdy,
    output [  3:0] dev_wen,
    output [ 31:0] dev_waddr,
    dev_wdata,
    input          dev_rrdy,
    output [  3:0] dev_ren,
    output [ 31:0] dev_raddr,
    input          dev_rvalid,
    input  [127:0] dev_rdata,
    output         hit_r,
    hit_w,
    uncached
);

  wire [127:0] _U_dsram_douta;
  wire [127:0] _tagSram_douta;
  wire uncached_0 = (&(data_addr[31:16])) & ((|data_ren) | (|data_wen));
  wire hit = _tagSram_douta[23:0] == {1'h1, data_addr[31:9]};
  reg [2:0] r_state;
  reg [3:0] ren_r;
  reg [31:0] raddr;
  wire _GEN = r_state == 3'h0;
  wire _GEN_0 = r_state == 3'h1;
  wire _GEN_1 = r_state == 3'h2;
  wire _GEN_2 = r_state == 3'h3;
  wire [3:0][31:0] _GEN_3 = {
    {_U_dsram_douta[127:96]},
    {_U_dsram_douta[95:64]},
    {_U_dsram_douta[63:32]},
    {_U_dsram_douta[31:0]}
  };
  wire _GEN_4 = r_state == 3'h4;
  wire _GEN_5 = _GEN_1 | _GEN_2 | ~(_GEN_4 & dev_rrdy);
  wire _GEN_6 = r_state == 3'h5;
  wire _GEN_7 = _GEN_6 & dev_rvalid;
  wire _GEN_8 = _GEN | _GEN_0;
  wire _GEN_9 = _GEN_6 & dev_rvalid;
  wire _GEN_10 = _GEN | _GEN_0 | _GEN_1 | _GEN_2 | _GEN_4;
  wire _GEN_11 = ~_GEN_10 & _GEN_9;
  wire [5:0] _GEN_12 = {1'h0, data_addr[8:4]};
  wire _GEN_13 = _GEN_10 | ~_GEN_7;
  reg [1:0] w_state;
  reg [3:0] wen_r;
  reg [31:0] waddr;
  reg [31:0] wdata;
  reg wr_resp_REG;
  wire wr_resp = ~wr_resp_REG & dev_wrdy;
  wire _GEN_14 = w_state == 2'h0;
  wire _GEN_15 = _GEN_14 & (|data_wen);
  wire _GEN_16 = ~uncached_0 & ~(|(data_addr[1:0])) & hit;
  wire _GEN_17 = _GEN_14 & (|data_wen) & _GEN_16;
  wire _GEN_18 = w_state == 2'h1;
  wire _GEN_19 = _GEN_14 | ~(_GEN_18 & dev_wrdy);
  wire _GEN_20 = w_state == 2'h2;
  always @(posedge cpu_clk) begin
    if (cpu_rst) begin
      r_state <= 3'h0;
      ren_r   <= 4'h0;
      raddr   <= 32'h0;
      w_state <= 2'h0;
      wen_r   <= 4'h0;
      waddr   <= 32'h0;
      wdata   <= 32'h0;
    end else begin
      automatic logic [2:0] _GEN_21 = _GEN_7 ? 3'h0 : r_state;
      automatic
      logic [7:0][2:0]
      _GEN_22 = {
        {_GEN_21},
        {_GEN_21},
        {_GEN_21},
        {dev_rrdy ? 3'h5 : r_state},
        {{~hit, 2'h0}},
        {dev_rvalid ? 3'h0 : r_state},
        {dev_rrdy ? 3'h2 : r_state},
        {(|data_ren) ? {1'h0, ~(uncached_0 | (|(data_addr[1:0]))), 1'h1} : r_state}
      };
      r_state <= _GEN_22[r_state];
      if (_GEN & (|data_ren)) begin
        ren_r <= data_ren;
        raddr <= data_addr;
      end
      if (_GEN_14) begin
        if (|data_wen) w_state <= 2'h1;
      end else if (_GEN_18) begin
        if (dev_wrdy) w_state <= 2'h2;
      end else if (_GEN_20 & wr_resp) w_state <= 2'h0;
      if (_GEN_15) begin
        wen_r <= data_wen;
        waddr <= data_addr;
        wdata <= data_wdata;
      end
    end
    wr_resp_REG <= dev_wrdy;
  end  // always @(posedge)
  blk_mem_gen_1 tagSram (
      .clka(cpu_clk),
      .wea(_GEN_17 | _GEN_11),
      .addra(_GEN_17 | ~_GEN_13 ? _GEN_12 : 6'h0),
      .dina((_GEN_15 ? _GEN_16 | _GEN_10 | ~_GEN_7 : _GEN_13) ? 128'h0 : {105'h1, data_addr[31:9]}),
      .douta(_tagSram_douta)
  );
  blk_mem_gen_1 U_dsram (
      .clka (cpu_clk),
      .wea  (_GEN_11),
      .addra(_GEN_13 ? 6'h0 : _GEN_12),
      .dina (_GEN_13 ? 128'h0 : dev_rdata),
      .douta(_U_dsram_douta)
  );
  assign data_valid = ~_GEN_8 & (_GEN_1 ? dev_rvalid : _GEN_2 ? hit : ~_GEN_4 & _GEN_9);
  assign data_rdata =
    _GEN_8
      ? 32'h0
      : _GEN_1
          ? (dev_rvalid ? dev_rdata[31:0] : 32'h0)
          : _GEN_2
              ? (hit ? _GEN_3[data_addr[3:2]] : 32'h0)
              : _GEN_4 | ~_GEN_7 ? 32'h0 : dev_rdata[31:0];
  assign data_wresp = ~(_GEN_14 | _GEN_18) & _GEN_20 & wr_resp;
  assign dev_wen = _GEN_19 ? 4'h0 : wen_r;
  assign dev_waddr = _GEN_19 ? 32'h0 : waddr;
  assign dev_wdata = _GEN_19 ? 32'h0 : wdata;
  assign dev_ren = _GEN ? 4'h0 : _GEN_0 ? (dev_rrdy ? ren_r : 4'h0) : _GEN_5 ? 4'h0 : ren_r;
  assign dev_raddr =
    _GEN
      ? 32'h0
      : _GEN_0
          ? (dev_rrdy ? raddr : 32'h0)
          : _GEN_5 ? 32'h0 : {data_addr[31:4], 4'h0};
  assign hit_r = ~(_GEN | _GEN_0 | _GEN_1) & _GEN_2 & hit;
  assign hit_w = _GEN_15 & _GEN_16;
  assign uncached = uncached_0;
endmodule

`else

module DCache (
    input wire cpu_clk,
    input wire cpu_rst,  // high active
    // Interface to CPU
    input wire [3:0] data_ren,  // 来自CPU的读使能信号
    input wire [31:0] data_addr,  // 来自CPU的地址（读、写共用）
    output reg data_valid,  // 输出给CPU的数据有效信号
    output reg [31:0] data_rdata,  // 输出给CPU的读数据
    input wire [3:0] data_wen,  // 来自CPU的写使能信号
    input wire [31:0] data_wdata,  // 来自CPU的写数据
    output reg data_wresp,  // 输出给CPU的写响应（高电平表示DCache已完成写操作）
    // Interface to Write Bus
    input  wire         dev_wrdy,       // 主存的写就绪信号（高电平表示主存可接收DCache的写请求）
    output reg [3:0] dev_wen,  // 输出给主存的写使能信号
    output reg [31:0] dev_waddr,  // 输出给主存的写地址
    output reg [31:0] dev_wdata,  // 输出给主存的写数据
    // Interface to Read Bus
    input  wire         dev_rrdy,       // 主存的读就绪信号（高电平表示主存可接收DCache的读请求）
    output reg [3:0] dev_ren,  // 输出给主存的读使能信号
    output reg [31:0] dev_raddr,  // 输出给主存的读地址
    input wire dev_rvalid,  // 来自主存的数据有效信号
    input wire [`BLK_SIZE-1:0] dev_rdata  // 来自主存的读数据
);

  // Peripherals access should be uncached.
  wire uncached = (data_addr[31:16] == 16'hFFFF) & (data_ren != 4'h0 | data_wen != 4'h0) ? 1'b1 : 1'b0;

  /* ---------- ---------- read ---------- ---------- */

  localparam R_IDLE = 2'b00;
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
            if (dev_rrdy) dev_ren <= data_ren;
            else ren_r <= data_ren;

            dev_raddr <= data_addr;
          end else dev_ren <= 4'h0;
        end
        R_STAT0: begin
          dev_ren <= dev_rrdy ? ren_r : 4'h0;
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

  /* ---------- ---------- write ---------- ---------- */

  localparam W_IDLE = 2'b00;
  localparam W_STAT0 = 2'b01;
  localparam W_STAT1 = 2'b11;
  reg [1:0] w_state, w_nstat;
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
            if (dev_wrdy) dev_wen <= data_wen;
            else wen_r <= data_wen;

            dev_waddr <= data_addr;
            dev_wdata <= data_wdata;
          end else dev_wen <= 4'h0;
        end
        W_STAT0: begin
          dev_wen <= dev_wrdy ? wen_r : 4'h0;
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


endmodule

`endif
