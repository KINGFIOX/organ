// Generated by CIRCT firtool-1.62.0
module Divider(
  input        clock,
               reset,
  input  [7:0] io_x,
               io_y,
  input        io_start,
  output [7:0] io_z,
               io_r,
  output       io_busy
);

  reg [6:0]  quotient;
  reg        quotient_sign;
  reg [13:0] remain;
  reg [2:0]  cnt_value;
  reg [13:0] extend_y;
  reg        busy;
  reg        state;
  always @(posedge clock) begin
    automatic logic _GEN;
    _GEN = ~state & io_start;
    if (reset) begin
      quotient <= 7'h0;
      remain <= 14'h0;
      cnt_value <= 3'h0;
      extend_y <= 14'h0;
      busy <= 1'h0;
      state <= 1'h0;
    end
    else begin
      automatic logic wrap;
      wrap = cnt_value == 3'h6;
      if (state) begin
        automatic logic [13:0] _GEN_0 = {remain[12:0], 1'h0};
        automatic logic [13:0] _wire_cal_T_2;
        automatic logic [13:0] _wire_cal_T_4;
        _wire_cal_T_2 = _GEN_0 + extend_y;
        _wire_cal_T_4 = _GEN_0 - extend_y;
        if (~state | wrap) begin
        end
        else
          quotient <=
            {quotient[5:0], ~(remain[13] ? _wire_cal_T_2[13] : _wire_cal_T_4[13])};
        if (wrap) begin
          if (remain[13])
            remain <= remain + extend_y;
          cnt_value <= 3'h0;
        end
        else begin
          if (remain[13])
            remain <= _wire_cal_T_2;
          else
            remain <= _wire_cal_T_4;
          cnt_value <= cnt_value + 3'h1;
        end
        state <= ~wrap;
      end
      else begin
        if (io_start) begin
          automatic logic [13:0] _wire_cal_T =
            {7'h0, io_x[6:0]} - {1'h0, io_y[6:0], 6'h0};
          quotient <= {6'h0, ~(_wire_cal_T[13])};
          remain <= _wire_cal_T;
        end
        state <= io_start | state;
      end
      if (_GEN)
        extend_y <= {1'h0, io_y[6:0], 6'h0};
      busy <= state & (state ? ~wrap : busy);
    end
    if (_GEN)
      quotient_sign <= io_x[7] ^ io_y[7];
  end // always @(posedge)
  assign io_z = {quotient_sign, quotient};
  assign io_r = {1'h0, remain[12:6]};
  assign io_busy = busy;
endmodule
