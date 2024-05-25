add_requires("verilator")

local TOP_MODULE = "Divider"
local MOUDLE_DIR = "divider"
local WIDTH = "32"

target("V" .. TOP_MODULE)
    add_rules("verilator.binary")
    set_toolchains("@verilator")
    add_files(TOP_MODULE .. ".sv")
    add_files("src/test/cxx/" .. TOP_MODULE .. ".cxx")
    add_values("verilator.flags", "--cc", "-Wall", "-Wno-UNUSEDSIGNAL", "--trace", "--timing", "-CFLAGS", "-DWIDTH=" .. WIDTH)
