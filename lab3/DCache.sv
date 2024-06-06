// Generated by CIRCT firtool-1.62.0
// external module blk_mem_gen_1

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
  input  [127:0] io_dev_rdata,
  output         io_hit_r,
                 io_hit_w
);

  wire [127:0]     _dataSram_douta;
  wire [127:0]     _tagSram_douta;
  wire             _peripheral_T = io_data_addr[31:16] != 16'hFFFF;
  wire             _GEN = ~_peripheral_T & (|io_data_wen);
  wire [3:0]       _GEN_0 = _GEN ? io_data_wen : 4'h0;
  reg  [2:0]       state_r;
  wire             _GEN_1 = state_r == 3'h1;
  wire             _GEN_2 =
    _tagSram_douta[23:0] == io_data_addr[31:8] & _tagSram_douta[24];
  wire [3:0][31:0] _GEN_3 =
    {{_dataSram_douta[127:96]},
     {_dataSram_douta[95:64]},
     {_dataSram_douta[63:32]},
     {_dataSram_douta[31:0]}};
  wire             _GEN_4 = (|state_r) & _GEN_1 & _GEN_2;
  wire             _GEN_5 = state_r == 3'h2;
  wire             _GEN_6 = _tagSram_douta[25] & io_dev_wrdy;
  wire [31:0]      _GEN_7 = {_tagSram_douta[1:0], io_data_addr[7:2], 24'h0};
  wire [31:0]      _GEN_8 = _GEN_6 ? _GEN_7 : io_data_addr;
  wire [31:0]      _GEN_9 = _GEN_6 ? _dataSram_douta[31:0] : io_data_wdata;
  wire             _GEN_10 = state_r == 3'h3;
  wire [31:0]      _GEN_11 = {_tagSram_douta[1:0], io_data_addr[7:2], 24'h1};
  wire             _GEN_12 = state_r == 3'h4;
  wire [31:0]      _GEN_13 = {_tagSram_douta[1:0], io_data_addr[7:2], 24'h2};
  wire             _GEN_14 = state_r == 3'h5;
  wire             _GEN_15 = _GEN_14 & io_dev_wrdy;
  wire             _GEN_16 = _GEN_10 | _GEN_12;
  wire             _GEN_17 = _GEN_16 | ~_GEN_15;
  wire             _GEN_18 = ~(|state_r) | _GEN_1;
  wire [31:0]      _GEN_19 = {_tagSram_douta[1:0], io_data_addr[7:2], 24'h3};
  wire [31:0]      _GEN_20 =
    _GEN_18
      ? io_data_addr
      : _GEN_5
          ? _GEN_8
          : _GEN_10
              ? (io_dev_wrdy ? _GEN_11 : io_data_addr)
              : _GEN_12
                  ? (io_dev_wrdy ? _GEN_13 : io_data_addr)
                  : _GEN_15 ? _GEN_19 : io_data_addr;
  wire [31:0]      _GEN_21 =
    _GEN_18 ? io_data_addr : _GEN_5 ? _GEN_8 : _GEN_17 ? io_data_addr : _GEN_19;
  wire [31:0]      _GEN_22 =
    _GEN_18
      ? io_data_wdata
      : _GEN_5
          ? _GEN_9
          : _GEN_10
              ? (io_dev_wrdy ? _dataSram_douta[63:32] : io_data_wdata)
              : _GEN_12
                  ? (io_dev_wrdy ? _dataSram_douta[95:64] : io_data_wdata)
                  : _GEN_15 ? _dataSram_douta[127:96] : io_data_wdata;
  wire [31:0]      _GEN_23 =
    _GEN_18
      ? io_data_wdata
      : _GEN_5 ? _GEN_9 : _GEN_17 ? io_data_wdata : _dataSram_douta[127:96];
  wire             _GEN_24 = ~(|state_r) | _GEN_1 | _GEN_5 | _GEN_16 | ~_GEN_14;
  wire             _GEN_25 = state_r == 3'h6;
  wire             _GEN_26 =
    ~(~(|state_r) | _GEN_1 | _GEN_5 | _GEN_10 | _GEN_12 | _GEN_14) & _GEN_25;
  reg  [2:0]       state_w;
  wire             _GEN_27 = state_w == 3'h1;
  wire             _GEN_28 =
    _tagSram_douta == {104'h0, io_data_addr[31:8]} & _tagSram_douta[24];
  wire             _GEN_29 = state_w == 3'h2;
  wire             _GEN_30 = state_w == 3'h3;
  wire             _GEN_31 = state_w == 3'h4;
  wire             _GEN_32 = state_w == 3'h5;
  wire             _GEN_33 = ~(|state_w) | _GEN_27;
  wire             _GEN_34 = ~(|state_w) | _GEN_27 | _GEN_29 | _GEN_30 | _GEN_31;
  wire             _GEN_35 = state_w == 3'h6;
  wire             _GEN_36 =
    (|state_w)
      ? (_GEN_27
           ? _GEN_28 | _GEN_26
           : ~(_GEN_29 | _GEN_30 | _GEN_31 | _GEN_32) & _GEN_35 | _GEN_26)
      : _GEN_26;
  reg  [2:0]       io_hit_r_REG;
  reg  [2:0]       io_hit_r_REG_1;
  reg  [2:0]       io_hit_r_REG_2;
  reg  [2:0]       io_hit_w_REG;
  reg  [2:0]       io_hit_w_REG_1;
  reg  [2:0]       io_hit_w_REG_2;
  always @(posedge clock) begin
    if (reset) begin
      state_r <= 3'h0;
      state_w <= 3'h0;
    end
    else begin
      automatic logic            _GEN_37;
      automatic logic [2:0]      _GEN_38;
      automatic logic [7:0][2:0] _GEN_39;
      automatic logic [2:0]      _GEN_40;
      automatic logic            _GEN_41;
      _GEN_37 = _peripheral_T & (|io_data_ren);
      _GEN_38 = _GEN_25 | _GEN_37 ? 3'h1 : state_r;
      _GEN_39 =
        {{_GEN_38},
         {3'h1},
         {3'h6},
         {3'h5},
         {3'h4},
         {_tagSram_douta[25] ? 3'h3 : _GEN_37 ? 3'h1 : state_r},
         {{1'h0, ~_GEN_2, 1'h0}},
         {_GEN_38}};
      _GEN_40 = _GEN_39[state_r];
      _GEN_41 = _peripheral_T & (|io_data_wen);
      if (_GEN_34) begin
        if (|state_r)
          state_r <= _GEN_40;
        else if (_GEN_37)
          state_r <= 3'h1;
      end
      else if (_GEN_32)
        state_r <= 3'h6;
      else if (_GEN_35)
        state_r <= 3'h1;
      else if (|state_r)
        state_r <= _GEN_40;
      else if (_GEN_37)
        state_r <= 3'h1;
      if (|state_w) begin
        if (_GEN_27)
          state_w <= {1'h0, ~_GEN_28, 1'h0};
        else if (_GEN_29)
          state_w <= 3'h3;
        else if (_GEN_30)
          state_w <= 3'h4;
        else if (_GEN_31)
          state_w <= 3'h5;
        else if (_GEN_41)
          state_w <= 3'h1;
      end
      else if (_GEN_41)
        state_w <= 3'h1;
    end
    io_hit_r_REG <= state_r;
    io_hit_r_REG_1 <= state_r;
    io_hit_r_REG_2 <= io_hit_r_REG_1;
    io_hit_w_REG <= state_w;
    io_hit_w_REG_1 <= state_w;
    io_hit_w_REG_2 <= io_hit_w_REG_1;
  end // always @(posedge)
  blk_mem_gen_1 tagSram (
    .clka  (clock),
    .wea   (_GEN_36),
    .addra (io_data_addr[7:2]),
    .dina
      ({(|state_w) & _GEN_27 ? {102'h0, _GEN_28, 1'h1} : 104'h1, io_data_addr[31:8]}),
    .douta (_tagSram_douta)
  );
  blk_mem_gen_1 dataSram (
    .clka  (clock),
    .wea   (_GEN_36),
    .addra (io_data_addr[7:2]),
    .dina  (io_dev_rdata),
    .douta (_dataSram_douta)
  );
  assign io_data_rdata = _GEN_4 ? _GEN_3[io_data_addr[1:0]] : io_dev_rdata[31:0];
  assign io_data_valid =
    _GEN_4 | ~_peripheral_T & (|io_data_ren) & io_dev_rrdy & io_dev_rvalid;
  assign io_data_wresp = (|state_w) & _GEN_27 & _GEN_28 | _GEN & io_dev_wrdy;
  assign io_dev_wen =
    _GEN_33 | ~(_GEN_29 ? _tagSram_douta[25] : _GEN_30 | _GEN_31 | _GEN_32)
      ? (_GEN_18 | ~(_GEN_5 ? _GEN_6 : (_GEN_10 | _GEN_12 | _GEN_14) & io_dev_wrdy)
           ? _GEN_0
           : 4'hF)
      : io_dev_wrdy
          ? 4'hF
          : _GEN_18
              ? _GEN_0
              : _GEN_5 ? (_GEN_6 ? 4'hF : _GEN_0) : _GEN_17 ? _GEN_0 : 4'hF;
  assign io_dev_waddr =
    _GEN_33
      ? _GEN_20
      : _GEN_29
          ? (_tagSram_douta[25] ? (io_dev_wrdy ? _GEN_7 : _GEN_21) : _GEN_20)
          : _GEN_30
              ? (io_dev_wrdy ? _GEN_11 : _GEN_21)
              : _GEN_31
                  ? (io_dev_wrdy ? _GEN_13 : _GEN_21)
                  : _GEN_32 ? (io_dev_wrdy ? _GEN_19 : _GEN_21) : _GEN_20;
  assign io_dev_wdata =
    _GEN_33
      ? _GEN_22
      : _GEN_29
          ? (_tagSram_douta[25]
               ? (io_dev_wrdy ? _dataSram_douta[31:0] : _GEN_23)
               : _GEN_22)
          : _GEN_30
              ? (io_dev_wrdy ? _dataSram_douta[63:32] : _GEN_23)
              : _GEN_31
                  ? (io_dev_wrdy ? _dataSram_douta[95:64] : _GEN_23)
                  : _GEN_32 ? (io_dev_wrdy ? _dataSram_douta[127:96] : _GEN_23) : _GEN_22;
  assign io_dev_ren =
    _GEN_34 ? (_GEN_24 ? io_data_ren : 4'hF) : _GEN_32 | ~_GEN_24 ? 4'hF : io_data_ren;
  assign io_dev_raddr = io_data_addr;
  assign io_hit_r = ~(|state_r) & io_hit_r_REG == 3'h1 & io_hit_r_REG_2 != 3'h6;
  assign io_hit_w = ~(|state_w) & io_hit_w_REG == 3'h1 & io_hit_w_REG_2 != 3'h6;
endmodule

