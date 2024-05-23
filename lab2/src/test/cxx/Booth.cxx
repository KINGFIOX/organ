#include "VBooth.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

#include <iostream>

int main(int argc, char** argv)
{
    Verilated::commandArgs(argc, argv);

    size_t fail_cnt = 0;

    size_t success_cnt = 0;

#if 1

    for (int x = 0; x <= 127; x++) {
        for (int y = 1; y <= 127; y++) {

            auto top = new VBooth;

            {
                top->reset = 1;
                top->clock = 0;
                // 重置设备
                for (int i = 0; i < 5; i++) {
                    top->clock = !top->clock;
                    top->eval();
                }
                top->reset = 0;
            }

            // 主仿真循环
            for (int cycle = 0; cycle < 40; cycle++) {

                top->io_start = (cycle == 5);

                top->io_x = x;
                top->io_y = y;

                top->clock = 1;
                top->eval();

                top->clock = 0;
                top->eval();

                top->io_start = 0;
            }

            int top_z = top->io_z;

            top->final();

            delete top;

            if (int z = x * y; z == top_z) {
                success_cnt++;
            } else {
                fail_cnt++;
                std::cout << "---------- ----------" << std::endl;
                std::cout << "x: " << x << std::endl;
                std::cout << "y: " << y << std::endl;
                std::cout << "x/y: " << z << std::endl;
                std::cout << "mul: " << (int)top_z << std::endl;
            }
        }
    }

    std::cout << "---------- ---------- final ---------- ----------" << std::endl;

    std::cout << "success number: " << success_cnt << std::endl;
    std::cout << "failed number: " << fail_cnt << std::endl;

#else

    auto top = new VBooth;

    {
        top->reset = 1;
        top->clock = 0;
        // 重置设备
        for (int i = 0; i < 5; i++) {
            top->clock = !top->clock;
            top->eval();
        }
        top->reset = 0;
    }

    // int8_t x = 0b0110;
    // int8_t y = 0b0101;
    int8_t x = 6;
    int8_t y = 0b0101;

    // 主仿真循环
    for (int cycle = 0; cycle < 40; cycle++) {

        top->io_start = (cycle == 5);

        top->io_x = x;
        top->io_y = y;

        top->clock = 1;
        top->eval();

        top->clock = 0;
        top->eval();

        top->io_start = 0;
    }

    int top_z = top->io_z;

    top->final();

    delete top;

    if (int z = x * y; z == top_z) {
        success_cnt++;
    } else {
        fail_cnt++;
        std::cout << "---------- ----------" << std::endl;
        std::cout << "x: " << (int)x << std::endl;
        std::cout << "y: " << (int)y << std::endl;
        std::cout << "x * y: " << z << std::endl;
        std::cout << "mul: " << (int)top_z << std::endl;
    }

#endif

    return 0;
}
