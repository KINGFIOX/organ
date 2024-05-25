#include "VDivider.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

#include <iostream>

#include "inc.h"

#if 1

int main(int argc, char** argv)
{
    Verilated::commandArgs(argc, argv);

    size_t width = std::stoul(argv[1]);

    size_t fail_cnt = 0;
    size_t success_cnt = 0;

    for (int x = -127; x <= 127; x++) {
        for (int y = -127; y <= 127; y++) {

            if (y == 0) {
                continue;
            }

            auto dut = std::make_unique<VDivider>();

            {
                dut->reset = 1;
                dut->clock = 0;
                // 重置设备
                for (int i = 0; i < 5; i++) {
                    dut->clock = !dut->clock;
                    dut->eval();
                }
                dut->reset = 0;
            }

            // 主仿真循环
            for (int cycle = 0; cycle < 40; cycle++) {

                dut->io_start = (cycle == 5);

                // 输入原码
                dut->io_x = two2ori(x, width);
                dut->io_y = two2ori(y, width);

                dut->clock = 1;
                dut->eval();

                dut->clock = 0;
                dut->eval();

                dut->io_start = 0;
            }

            // 输出原码，转为补码
            int top_z = ori2two(dut->io_z, width);
            int top_r = ori2two(dut->io_r, width);

            dut->final();

            if (int z = x / y, r = x % y; z == top_z && r == top_r) {
                success_cnt++;
            } else {
                fail_cnt++;
                std::cout << "---------- ----------" << std::endl;
                std::cout << "x: " << x << std::endl;
                std::cout << "y: " << y << std::endl;
                // 原码
                std::cout << "x/y:\t\t" << std::bitset<8>(two2ori(z, width)).to_string() << std::endl;
                std::cout << "quot:\t\t" << std::bitset<8>(dut->io_z).to_string() << std::endl;
                // 原码
                std::cout << "x%y:\t\t" << std::bitset<8>(two2ori(r, width)).to_string() << std::endl;
                std::cout << "rema:\t\t" << std::bitset<8>(dut->io_r).to_string() << std::endl;
            }
        }
    }

    std::cout << "---------- ---------- final ---------- ----------" << std::endl;

    std::cout << "success number: " << success_cnt << std::endl;
    std::cout << "failed number: " << fail_cnt << std::endl;

    return 0;
}

#else

#endif