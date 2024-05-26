add_requires("verilator")

local TOP_MODULE = "Booth"
local MOUDLE_DIR = "booth"
local WIDTH = "8"

target("V" .. TOP_MODULE)
    add_rules("verilator.binary")
    set_toolchains("@verilator")
    add_files(TOP_MODULE .. ".sv")
    add_files("src/test/cxx/" .. TOP_MODULE .. ".cxx")
    add_values("verilator.flags", "--cc", "-Wall", "-Wno-UNUSEDSIGNAL", "--trace", "--timing", "-CFLAGS", "-DWIDTH=" .. WIDTH)
