// Generated by CIRCT firtool-1.62.0
// external module blk_mem_gen_1

module ICache(
  input          clock,
                 reset,
                 io_inst_rreq,
  input  [31:0]  io_inst_addr,
  output         io_inst_valid,
  output [31:0]  io_inst_out,
  output [3:0]   io_mem_ren,
  output [31:0]  io_mem_raddr,
  input  [127:0] io_mem_rdata,
  input          io_mem_rrdy,
                 io_mem_rvalid
);

  wire [127:0]     _blk_mem_gen_1_7_douta;
  wire [127:0]     _blk_mem_gen_1_6_douta;
  wire [127:0]     _blk_mem_gen_1_5_douta;
  wire [127:0]     _blk_mem_gen_1_4_douta;
  wire [127:0]     _blk_mem_gen_1_3_douta;
  wire [127:0]     _blk_mem_gen_1_2_douta;
  wire [127:0]     _blk_mem_gen_1_1_douta;
  wire [127:0]     _blk_mem_gen_1_douta;
  wire [3:0][31:0] _GEN = '{32'h0, 32'h0, 32'h0, 32'h0};
  wire [5:0]       tagSrams_3_addra = {1'h0, io_inst_addr[8:4]};
  wire [23:0]      _tagSrams_dina_T = {1'h1, io_inst_addr[31:9]};
  wire             hitVec_0 = _blk_mem_gen_1_douta[23:0] == _tagSrams_dina_T;
  wire             hitVec_1 = _blk_mem_gen_1_1_douta[23:0] == _tagSrams_dina_T;
  wire             hitVec_2 = _blk_mem_gen_1_2_douta[23:0] == _tagSrams_dina_T;
  wire             hitVec_3 = _blk_mem_gen_1_3_douta[23:0] == _tagSrams_dina_T;
  reg  [1:0]       cnt_value;
  reg  [1:0]       state;
  wire             _GEN_0 = state == 2'h0;
  wire             _GEN_1 = state == 2'h1;
  wire [3:0]       _GEN_2 = {hitVec_3, hitVec_2, hitVec_1, hitVec_0};
  wire             hit = ~_GEN_0 & _GEN_1 & (|_GEN_2);
  wire [3:0][31:0] _GEN_3 =
    hitVec_3
      ? {{_blk_mem_gen_1_7_douta[127:96]},
         {_blk_mem_gen_1_7_douta[95:64]},
         {_blk_mem_gen_1_7_douta[63:32]},
         {_blk_mem_gen_1_7_douta[31:0]}}
      : hitVec_2
          ? {{_blk_mem_gen_1_6_douta[127:96]},
             {_blk_mem_gen_1_6_douta[95:64]},
             {_blk_mem_gen_1_6_douta[63:32]},
             {_blk_mem_gen_1_6_douta[31:0]}}
          : hitVec_1
              ? {{_blk_mem_gen_1_5_douta[127:96]},
                 {_blk_mem_gen_1_5_douta[95:64]},
                 {_blk_mem_gen_1_5_douta[63:32]},
                 {_blk_mem_gen_1_5_douta[31:0]}}
              : hitVec_0
                  ? {{_blk_mem_gen_1_4_douta[127:96]},
                     {_blk_mem_gen_1_4_douta[95:64]},
                     {_blk_mem_gen_1_4_douta[63:32]},
                     {_blk_mem_gen_1_4_douta[31:0]}}
                  : _GEN;
  wire             _GEN_4 = state == 2'h2;
  wire             _GEN_5 = (&state) & io_mem_rvalid;
  wire             _GEN_6 = _GEN_0 | _GEN_1 | _GEN_4;
  wire             tagSrams_0_wea = ~_GEN_6 & _GEN_5 & cnt_value == 2'h0;
  wire             tagSrams_1_wea = ~_GEN_6 & _GEN_5 & cnt_value == 2'h1;
  wire             tagSrams_2_wea = ~_GEN_6 & _GEN_5 & cnt_value == 2'h2;
  wire             tagSrams_3_wea = ~_GEN_6 & _GEN_5 & (&cnt_value);
  wire [127:0]     tagSrams_3_dina = {105'h1, io_inst_addr[31:9]};
  always @(posedge clock) begin
    if (reset) begin
      cnt_value <= 2'h0;
      state <= 2'h0;
    end
    else begin
      automatic logic [3:0][1:0] _GEN_7 =
        {{_GEN_5 ? 2'h1 : state},
         {io_mem_rrdy ? 2'h3 : state},
         {{~(|_GEN_2), 1'h0}},
         {io_inst_rreq ? 2'h1 : state}};
      cnt_value <= cnt_value + 2'h1;
      state <= _GEN_7[state];
    end
  end // always @(posedge)
  blk_mem_gen_1 blk_mem_gen_1 (
    .clka  (clock),
    .wea   (tagSrams_0_wea),
    .addra (tagSrams_3_addra),
    .dina  (tagSrams_3_dina),
    .douta (_blk_mem_gen_1_douta)
  );
  blk_mem_gen_1 blk_mem_gen_1_1 (
    .clka  (clock),
    .wea   (tagSrams_1_wea),
    .addra (tagSrams_3_addra),
    .dina  (tagSrams_3_dina),
    .douta (_blk_mem_gen_1_1_douta)
  );
  blk_mem_gen_1 blk_mem_gen_1_2 (
    .clka  (clock),
    .wea   (tagSrams_2_wea),
    .addra (tagSrams_3_addra),
    .dina  (tagSrams_3_dina),
    .douta (_blk_mem_gen_1_2_douta)
  );
  blk_mem_gen_1 blk_mem_gen_1_3 (
    .clka  (clock),
    .wea   (tagSrams_3_wea),
    .addra (tagSrams_3_addra),
    .dina  (tagSrams_3_dina),
    .douta (_blk_mem_gen_1_3_douta)
  );
  blk_mem_gen_1 blk_mem_gen_1_4 (
    .clka  (clock),
    .wea   (tagSrams_0_wea),
    .addra (tagSrams_3_addra),
    .dina  (io_mem_rdata),
    .douta (_blk_mem_gen_1_4_douta)
  );
  blk_mem_gen_1 blk_mem_gen_1_5 (
    .clka  (clock),
    .wea   (tagSrams_1_wea),
    .addra (tagSrams_3_addra),
    .dina  (io_mem_rdata),
    .douta (_blk_mem_gen_1_5_douta)
  );
  blk_mem_gen_1 blk_mem_gen_1_6 (
    .clka  (clock),
    .wea   (tagSrams_2_wea),
    .addra (tagSrams_3_addra),
    .dina  (io_mem_rdata),
    .douta (_blk_mem_gen_1_6_douta)
  );
  blk_mem_gen_1 blk_mem_gen_1_7 (
    .clka  (clock),
    .wea   (tagSrams_3_wea),
    .addra (tagSrams_3_addra),
    .dina  (io_mem_rdata),
    .douta (_blk_mem_gen_1_7_douta)
  );
  assign io_inst_valid = hit;
  assign io_inst_out = _GEN_3[io_inst_addr[3:2]];
  assign io_mem_ren = _GEN_0 | _GEN_1 ? 4'h0 : {4{_GEN_4 & io_mem_rrdy}};
  assign io_mem_raddr = {io_inst_addr[31:4], 4'h0};
endmodule

