// Generated by CIRCT firtool-1.62.0
// VCS coverage exclude_file
module mem_1024x22(
  input  [9:0]  R0_addr,
  input         R0_en,
                R0_clk,
  output [21:0] R0_data,
  input  [9:0]  W0_addr,
  input         W0_en,
                W0_clk,
  input  [21:0] W0_data
);

  reg [21:0] Memory[0:1023];
  reg        _R0_en_d0;
  reg [9:0]  _R0_addr_d0;
  always @(posedge R0_clk) begin
    _R0_en_d0 <= R0_en;
    _R0_addr_d0 <= R0_addr;
  end // always @(posedge)
  always @(posedge W0_clk) begin
    if (W0_en & 1'h1)
      Memory[W0_addr] <= W0_data;
  end // always @(posedge)
  assign R0_data = _R0_en_d0 ? Memory[_R0_addr_d0] : 22'bx;
endmodule

module SRAM(
  input         clock,
                io_wea,
  input  [9:0]  io_addra,
  input  [21:0] io_dina,
  output [21:0] io_douta
);

  mem_1024x22 mem_ext (
    .R0_addr (io_addra),
    .R0_en   (1'h1),
    .R0_clk  (clock),
    .R0_data (io_douta),
    .W0_addr (io_addra),
    .W0_en   (io_wea),
    .W0_clk  (clock),
    .W0_data (io_dina)
  );
endmodule

// VCS coverage exclude_file
module mem_1024x128(
  input  [9:0]   R0_addr,
  input          R0_en,
                 R0_clk,
  output [127:0] R0_data,
  input  [9:0]   W0_addr,
  input          W0_en,
                 W0_clk,
  input  [127:0] W0_data
);

  reg [127:0] Memory[0:1023];
  reg         _R0_en_d0;
  reg [9:0]   _R0_addr_d0;
  always @(posedge R0_clk) begin
    _R0_en_d0 <= R0_en;
    _R0_addr_d0 <= R0_addr;
  end // always @(posedge)
  always @(posedge W0_clk) begin
    if (W0_en & 1'h1)
      Memory[W0_addr] <= W0_data;
  end // always @(posedge)
  assign R0_data = _R0_en_d0 ? Memory[_R0_addr_d0] : 128'bx;
endmodule

module SRAM_1(
  input          clock,
                 io_wea,
  input  [9:0]   io_addra,
  input  [127:0] io_dina,
  output [127:0] io_douta
);

  mem_1024x128 mem_ext (
    .R0_addr (io_addra),
    .R0_en   (1'h1),
    .R0_clk  (clock),
    .R0_data (io_douta),
    .W0_addr (io_addra),
    .W0_en   (io_wea),
    .W0_clk  (clock),
    .W0_data (io_dina)
  );
endmodule

module DCache(
  input          clock,
                 reset,
  input  [31:0]  io_data_addr,
  input  [3:0]   io_data_ren,
  output [31:0]  io_data_rdata,
  output         io_data_valid,
  input  [3:0]   io_data_wen,
  input  [31:0]  io_data_wdata,
  output         io_data_wresp,
  input          io_dev_wrdy,
  output [3:0]   io_dev_wen,
  output [31:0]  io_dev_waddr,
                 io_dev_wdata,
  input          io_dev_rrdy,
  output [3:0]   io_dev_ren,
  output [31:0]  io_dev_raddr,
  input          io_dev_rvalid,
  input  [127:0] io_dev_rdata
);

  wire [127:0]     _dataSram_io_douta;
  wire [21:0]      _tagSram_io_douta;
  wire             _peripheral_T = io_data_addr[31:16] != 16'hFFFF;
  wire             _GEN = ~_peripheral_T & (|io_data_wen);
  wire [3:0]       _GEN_0 = _GEN ? io_data_wen : 4'h0;
  reg  [1:0]       state_r;
  wire             _GEN_1 = state_r == 2'h0;
  wire             _GEN_2 = state_r == 2'h1;
  wire             _GEN_3 = _tagSram_io_douta == {2'h0, io_data_addr[31:12]};
  wire             _GEN_4 = _GEN_3 & _tagSram_io_douta[20];
  wire [3:0][31:0] _GEN_5 =
    {{_dataSram_io_douta[127:96]},
     {_dataSram_io_douta[95:64]},
     {_dataSram_io_douta[63:32]},
     {_dataSram_io_douta[31:0]}};
  wire [31:0]      _GEN_6 = _GEN_5[io_data_addr[1:0]];
  wire             _GEN_7 = _GEN_2 & _GEN_4;
  wire             _GEN_8 = state_r == 2'h2;
  wire             _GEN_9 = _GEN_1 | _GEN_2;
  wire             _GEN_10 = _GEN_9 | ~(_GEN_8 & _tagSram_io_douta[21] & io_dev_wrdy);
  wire             _GEN_11 = _GEN_9 | ~_GEN_8;
  wire             _GEN_12 = ~(_GEN_1 | _GEN_2 | _GEN_8) & (&state_r);
  reg  [1:0]       state_w;
  wire             _GEN_13 = state_w == 2'h0;
  wire             _GEN_14 = state_w == 2'h1;
  wire             _GEN_15 = _GEN_3 & _tagSram_io_douta[20];
  wire             _GEN_16 = _GEN_14 & _GEN_15;
  wire             _GEN_17 = state_w == 2'h2;
  wire             _GEN_18 = _GEN_17 & _tagSram_io_douta[21] & io_dev_wrdy;
  wire             _GEN_19 = _GEN_18 | ~_GEN_10;
  wire             _GEN_20 = _GEN_13 | _GEN_14;
  wire             _GEN_21 =
    _GEN_13 ? _GEN_12 : _GEN_14 ? _GEN_15 | _GEN_12 : ~_GEN_17 & (&state_w) | _GEN_12;
  always @(posedge clock) begin
    if (reset) begin
      state_r <= 2'h0;
      state_w <= 2'h0;
    end
    else begin
      automatic logic [3:0][1:0] _GEN_22 =
        {{2'h1},
         {2'h3},
         {{~_GEN_4, 1'h0}},
         {_peripheral_T & (|io_data_ren) ? 2'h1 : state_r}};
      automatic logic [1:0]      _GEN_23;
      _GEN_23 = _GEN_22[state_r];
      if (_GEN_20)
        state_r <= _GEN_23;
      else if (_GEN_17)
        state_r <= 2'h3;
      else if (&state_w)
        state_r <= 2'h1;
      else
        state_r <= _GEN_23;
      if (_GEN_13 | ~_GEN_14) begin
        if (_peripheral_T & (|io_data_wen))
          state_w <= 2'h1;
      end
      else
        state_w <= {~_GEN_15, 1'h0};
    end
  end // always @(posedge)
  SRAM tagSram (
    .clock    (clock),
    .io_wea   (_GEN_21),
    .io_addra (io_data_addr[11:2]),
    .io_dina  ({_GEN_13 ? 2'h1 : {_GEN_16, 1'h1}, io_data_addr[31:12]}),
    .io_douta (_tagSram_io_douta)
  );
  SRAM_1 dataSram (
    .clock    (clock),
    .io_wea   (_GEN_21),
    .io_addra (io_data_addr[11:2]),
    .io_dina  (io_dev_rdata),
    .io_douta (_dataSram_io_douta)
  );
  assign io_data_rdata = _GEN_1 | ~_GEN_7 ? io_dev_rdata[31:0] : _GEN_6;
  assign io_data_valid =
    ~_GEN_1 & _GEN_7 | ~_peripheral_T & (|io_data_ren) & io_dev_rrdy & io_dev_rvalid;
  assign io_data_wresp = ~_GEN_13 & _GEN_16 | _GEN & io_dev_wrdy;
  assign io_dev_wen = _GEN_20 ? (_GEN_10 ? _GEN_0 : 4'hF) : _GEN_19 ? 4'hF : _GEN_0;
  assign io_dev_waddr =
    _GEN_20 | ~_GEN_18
      ? (_GEN_10 ? io_data_addr : {_tagSram_io_douta[19:0], io_data_addr[11:0]})
      : {_tagSram_io_douta[19:0], io_data_addr[11:0]};
  assign io_dev_wdata =
    _GEN_20 ? (_GEN_10 ? io_data_wdata : _GEN_6) : _GEN_19 ? _GEN_6 : io_data_wdata;
  assign io_dev_ren =
    _GEN_20 ? (_GEN_11 ? io_data_ren : 4'hF) : _GEN_17 | ~_GEN_11 ? 4'hF : io_data_ren;
  assign io_dev_raddr = io_data_addr;
endmodule

