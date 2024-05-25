# 需要修改的配置
TOP_MODULE = Booth
MOUDLE_DIR = booth

VERILOG_SRC = $(TOP_MODULE).sv
CHISEL_SRC = src/main/scala/booth/$(TOP_MODULE).scala
# TESTBENCH_SRC = src/test/cxx/main.cxx
TESTBENCH_SRC = src/test/cxx/$(TOP_MODULE).cxx
SIM_EXEC = obj_dir/V$(TOP_MODULE)

# Makefile for Verilator simulation
VERILATOR = verilator
VERILATOR_FLAGS = -Wall --cc --trace -Iobj_dir -Wno-UNUSEDSIGNAL

all: $(SIM_EXEC)

$(VERILOG_SRC): $(CHISEL_SRC)
	@echo "Compiling chisel source..."
	sbt "runMain $(MOUDLE_DIR).$(TOP_MODULE)"

$(SIM_EXEC): $(VERILOG_SRC) $(TESTBENCH_SRC)
	@echo "Compiling Verilog source..."
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILOG_SRC) --exe $(TESTBENCH_SRC) 
	@echo "Building C++ simulation model..."
	$(MAKE) -C obj_dir -f V$(TOP_MODULE).mk

# 生成 vcd 文件, 并用 dwfv 运行 cargo install dwfv
.PHONY:
run: $(SIM_EXEC)
	@./$(SIM_EXEC)

.PHONY:
sim: run
	dwfv $(TOP_MODULE).vcd
	
clean:
	rm -rf obj_dir $(TOP_MODULE).vcd

.PHONY: all run clean