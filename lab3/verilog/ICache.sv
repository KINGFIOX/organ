// Generated by CIRCT firtool-1.62.0
// external module blk_mem_gen_1

module ICache(
  input          cpu_clk,
                 cpu_rst,
                 inst_rreq,
  input  [31:0]  inst_addr,
  output         inst_valid,
  output [31:0]  inst_out,
  output [3:0]   mem_ren,
  output [31:0]  mem_raddr,
  input  [127:0] mem_rdata,
  input          mem_rrdy,
                 mem_rvalid,
  output         hit
);

  wire [127:0]     _dataSram_douta;
  wire [127:0]     _tagSram_douta;
  reg  [1:0]       state;
  reg  [1:0]       hit_REG;
  reg  [1:0]       hit_REG_1;
  reg  [1:0]       hit_REG_2;
  wire             _GEN = state == 2'h1;
  wire             _GEN_0 = _tagSram_douta[24:0] == {1'h1, inst_addr[31:8]};
  wire [3:0][31:0] _GEN_1 =
    {{_dataSram_douta[127:96]},
     {_dataSram_douta[95:64]},
     {_dataSram_douta[63:32]},
     {_dataSram_douta[31:0]}};
  wire             _GEN_2 = state == 2'h2;
  wire             _GEN_3 = ~(~(|state) | _GEN) & _GEN_2 & mem_rvalid;
  always @(posedge cpu_clk) begin
    if (cpu_rst)
      state <= 2'h0;
    else if (|state) begin
      if (_GEN) begin
        if (_GEN_0)
          state <= 2'h0;
        else if (mem_rrdy)
          state <= 2'h2;
      end
      else if (_GEN_2 & mem_rvalid)
        state <= 2'h1;
    end
    else if (inst_rreq)
      state <= 2'h1;
    hit_REG <= state;
    hit_REG_1 <= state;
    hit_REG_2 <= hit_REG_1;
  end // always @(posedge)
  blk_mem_gen_1 tagSram (
    .clka  (cpu_clk),
    .wea   (_GEN_3),
    .addra (inst_addr[7:2]),
    .dina  ({104'h1, inst_addr[31:8]}),
    .douta (_tagSram_douta)
  );
  blk_mem_gen_1 dataSram (
    .clka  (cpu_clk),
    .wea   (_GEN_3),
    .addra (inst_addr[7:2]),
    .dina  (mem_rdata),
    .douta (_dataSram_douta)
  );
  assign inst_valid = (|state) & _GEN & _GEN_0;
  assign inst_out = _GEN_1[inst_addr[1:0]];
  assign mem_ren = ~(|state) | ~_GEN | _GEN_0 ? 4'h0 : {4{mem_rrdy}};
  assign mem_raddr = inst_addr;
  assign hit = ~(|state) & hit_REG == 2'h1 & hit_REG_2 == 2'h0;
endmodule
