#include "VBooth.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

#include <cstdlib>
#include <iostream>

#include "inc.h"

#if 0

int main(int argc, char* argv[])
{
    Verilated::commandArgs(argc, argv);

    std::cout << WIDTH << std::endl;

    size_t fail_cnt = 0;

    size_t success_cnt = 0;

    for (int _x = LIMIT_MIN; _x <= LIMIT_MAX; _x++) {
        for (int _y = LIMIT_MIN; _y <= LIMIT_MAX; _y++) {

            int8_t x = (int8_t)x;
            int8_t y = (int8_t)y;

            auto dut = std::make_unique<VBooth>();

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

                dut->io_x = x;
                dut->io_y = y;

                dut->clock = 1;
                dut->eval();

                dut->clock = 0;
                dut->eval();

                dut->io_start = 0;
            }

            int16_t top_z = dut->io_z;

            dut->final();

            if (int16_t z = x * y; z == top_z) {
                success_cnt++;
                // std::cout << success_cnt << std::endl;
            } else {
                fail_cnt++;
                std::cout << "---------- ----------" << std::endl;
                std::cout << "x: " << (int)x << std::endl;
                std::cout << "y: " << (int)y << std::endl;
                std::cout << "x*y: " << z << std::endl;
                std::cout << "mul: " << (int)top_z << std::endl;
                // 补码
                std::cout << "x*y:\t\t" << std::bitset<WIDTH * 2>(z).to_string() << std::endl;
                std::cout << "mul:\t\t" << std::bitset<WIDTH * 2>(dut->io_z).to_string() << std::endl;
            }
        }
    }

    std::cout << "---------- ---------- final ---------- ----------" << std::endl;

    std::cout << "success number: " << success_cnt << std::endl;
    std::cout << "failed number: " << fail_cnt << std::endl;

    return 0;
}

#else

int main(int argc, char** argv)
{
    Verilated::commandArgs(argc, argv);
    Verilated::traceEverOn(true); // 启用波形跟踪

    size_t fail_cnt = 0;
    size_t success_cnt = 0;

    auto dut = std::make_unique<VBooth>();
    VerilatedVcdC* vcd = new VerilatedVcdC();
    dut->trace(vcd, 99); // 设定跟踪级别
    vcd->open("Booth.vcd"); // 打开VCD文件

    // 重置设备
    dut->reset = 1;
    dut->clock = 0;
    for (int i = 0; i < 5; i++) {
        dut->clock = !dut->clock;
        dut->eval();
        vcd->dump(10 * i); // 记录时间点
    }
    dut->reset = 0;

    /* ---------- ---------- 第一次实验 ---------- ---------- */

    srand(time(NULL));
    int8_t x = rand();
    int8_t y = rand();

    // int4_t x { 0b1000 }, y { 0b0101 };
    // int8_t x = 0b1000; // -8
    // int8_t y = 0b1010; // -6

    // BoothTx tx = { .x = 0b1000, .y = 0b1010 };

    // 主仿真循环
    for (int cycle = 0; cycle < 40; cycle++) {
        dut->io_start = (cycle == 5);
        dut->io_x = x;
        dut->io_y = y;

        dut->clock = 1;
        dut->eval();
        vcd->dump(10 * cycle + 5);

        dut->clock = 0;
        dut->eval();
        vcd->dump(10 * cycle + 10);

        dut->io_start = 0;
    }

    // 收集结果和清理
    int16_t top_z = dut->io_z;

    if (int16_t z = x * y; z == top_z) {
        success_cnt++;
        std::cout << success_cnt << std::endl;
    } else {
        fail_cnt++;
        std::cout << "---------- ----------" << std::endl;
        std::cout << "x: " << (int)x << std::endl;
        std::cout << "y: " << (int)y << std::endl;
        std::cout << "x*y: " << (int)z << std::endl;
        std::cout << "mul: " << (int)top_z << std::endl;
        std::cout << "x*y:\t\t" << std::bitset<WIDTH * 2>(z).to_string() << std::endl;
        std::cout << "mul:\t\t" << std::bitset<WIDTH * 2>(dut->io_z).to_string() << std::endl;
    }

    /* ---------- ---------- 第二次实验 ---------- ---------- */

    x = rand();
    y = rand();

    // int4_t x { 0b1000 }, y { 0b0101 };
    // int8_t x = 0b1000; // -8
    // int8_t y = 0b1010; // -6

    // BoothTx tx = { .x = 0b1000, .y = 0b1010 };

    // 主仿真循环
    for (int cycle = 0; cycle < 40; cycle++) {
        dut->io_start = (cycle == 5);
        dut->io_x = x;
        dut->io_y = y;

        dut->clock = 1;
        dut->eval();
        vcd->dump(10 * cycle + 5);

        dut->clock = 0;
        dut->eval();
        vcd->dump(10 * cycle + 10);

        dut->io_start = 0;
    }

    // 收集结果和清理
    top_z = dut->io_z;

    if (int16_t z = x * y; z == top_z) {
        success_cnt++;
        std::cout << success_cnt << std::endl;
    } else {
        fail_cnt++;
        std::cout << "---------- ----------" << std::endl;
        std::cout << "x: " << (int)x << std::endl;
        std::cout << "y: " << (int)y << std::endl;
        std::cout << "x*y: " << (int)z << std::endl;
        std::cout << "mul: " << (int)top_z << std::endl;
        std::cout << "x*y:\t\t" << std::bitset<WIDTH * 2>(z).to_string() << std::endl;
        std::cout << "mul:\t\t" << std::bitset<WIDTH * 2>(dut->io_z).to_string() << std::endl;
    }

    dut->final();
    vcd->close(); // 关闭VCD文件

    return 0;
}

#endif
