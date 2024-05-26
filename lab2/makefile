# 需要修改的配置
TOP_MODULE = Booth
MOUDLE_DIR = booth
WIDTH = 8

# 这些不建议修改
VERILOG_SRC = $(TOP_MODULE).sv
CHISEL_SRC = src/main/scala/$(MOUDLE_DIR)/$(TOP_MODULE).scala
# TESTBENCH_SRC = src/test/cxx/main.cxx
TESTBENCH_SRC = src/test/cxx/$(TOP_MODULE).cxx
SIM_EXEC = obj_dir/V$(TOP_MODULE)

# Makefile for Verilator simulation
VERILATOR = verilator
VERILATOR_FLAGS = -Wall --cc --trace -Iobj_dir -Wno-UNUSEDSIGNAL  -CFLAGS "-DWIDTH=$(WIDTH)"

all: $(SIM_EXEC)

$(VERILOG_SRC): $(CHISEL_SRC) makefile
	@echo "Compiling chisel source..."
	sbt "runMain $(MOUDLE_DIR).$(TOP_MODULE) $(WIDTH)"

$(SIM_EXEC): $(VERILOG_SRC) $(TESTBENCH_SRC) makefile
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
	rm -rf obj_dir $(TOP_MODULE).vcd $(VERILOG_SRC)

.PHONY: all run clean