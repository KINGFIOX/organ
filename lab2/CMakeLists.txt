cmake_minimum_required(VERSION 3.12)
cmake_policy(SET CMP0074 NEW)
cmake_policy(SET CMP0144 NEW)
project(Booth)

# 当然啦，这里要修改成你的 VERILATOR_ROOT, 通常是 /usr/local/share/verilator
set(VERILATOR_ROOT "/usr/local/share/verilator" CACHE STRING "Verilator root directory")

set(TOP_MODULE $ENV{TOP_MODULE})
set(MODULE_DIR $ENV{MODULE_DIR})
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

set(CMAKE_C_COMPILER "/usr/bin/gcc")
set(CMAKE_CXX_COMPILER "/usr/bin/g++")
set(CMAKE_CXX_STANDARD 20)

find_package(verilator HINTS ${VERILATOR_ROOT})
if (NOT verilator_FOUND)
  message(FATAL_ERROR "Verilator was not found. Either install it, or set the VERILATOR_ROOT environment variable")
endif()

add_executable(${TOP_MODULE} "src/test/cxx/${TOP_MODULE}.cxx")
target_compile_features(${TOP_MODULE} PUBLIC cxx_std_20)

# Add the Verilated circuit to the target
# COVERAGE 覆盖率统计
verilate(${TOP_MODULE} COVERAGE TRACE
  INCLUDE_DIRS "./src/test/cxx"
  VERILATOR_ARGS -Wall -Wno-UNUSEDSIGNAL
  SOURCES "$ENV{TOP_MODULE}.sv")