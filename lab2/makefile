# 需要修改的配置
TOP_MODULE = Booth
MODULE_DIR = booth
WIDTH = 8

# 这些不建议修改
VERILOG_SRC = $(TOP_MODULE).sv
CHISEL_SRC = src/main/scala/$(MODULE_DIR)/$(TOP_MODULE).scala
# TESTBENCH_SRC = src/test/cxx/main.cxx
TESTBENCH_SRC = src/test/cxx/$(TOP_MODULE).cxx
SIM_EXEC = build/$(TOP_MODULE)
CMAKE = cmake

all: $(SIM_EXEC)

$(VERILOG_SRC): $(CHISEL_SRC)
	@echo "Compiling chisel source..."
	sbt "runMain $(MODULE_DIR).$(TOP_MODULE) $(WIDTH)"

$(SIM_EXEC): $(VERILOG_SRC) $(TESTBENCH_SRC) makefile
	TOP_MODULE=$(TOP_MODULE) MODULE_DIR=$(MODULE_DIR) $(CMAKE) -GNinja -S . -B build
	$(CMAKE) --build build

run: $(SIM_EXEC)
	./$(SIM_EXEC)

clean:
	rm -rf build *.vcd *.sv

.PHONY: all clean
