#include "VDivider.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

#include <iostream>

#include "inc.h"

#if 1

int main(int argc, char** argv)
{
    Verilated::commandArgs(argc, argv);

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
                dut->io_x = two2ori(x);
                dut->io_y = two2ori(y);

                dut->clock = 1;
                dut->eval();

                dut->clock = 0;
                dut->eval();

                dut->io_start = 0;
            }

            // 输出原码，转为补码
            int top_z = ori2two(dut->io_z);
            int top_r = ori2two(dut->io_r);

            dut->final();

            if (int z = x / y, r = x % y; z == top_z && r == top_r) {
                success_cnt++;
            } else {
                fail_cnt++;
                std::cout << "---------- ----------" << std::endl;
                std::cout << "x: " << x << std::endl;
                std::cout << "y: " << y << std::endl;
                // 原码
                std::cout << "x/y:\t\t" << std::bitset<8>(two2ori(z)).to_string() << std::endl;
                std::cout << "quot:\t\t" << std::bitset<8>(dut->io_z).to_string() << std::endl;
                // 原码
                std::cout << "x%y:\t\t" << std::bitset<8>(two2ori(r)).to_string() << std::endl;
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

int main(int argc, char** argv)
{
    Verilated::commandArgs(argc, argv);
    Verilated::traceEverOn(true); // 启用波形跟踪

    size_t fail_cnt = 0;
    size_t success_cnt = 0;

    auto dut = std::make_unique<VDivider>();
    VerilatedVcdC* vcd = new VerilatedVcdC();
    dut->trace(vcd, 99); // 设定跟踪级别
    vcd->open("divider.vcd"); // 打开VCD文件

    // 重置设备
    dut->reset = 1;
    dut->clock = 0;
    for (int i = 0; i < 5; i++) {
        dut->clock = !dut->clock;
        dut->eval();
        vcd->dump(10 * i); // 记录时间点
    }
    dut->reset = 0;

    int x = -13;
    int y = 6;

    // 主仿真循环
    for (int cycle = 0; cycle < 40; cycle++) {
        dut->io_start = (cycle == 5);
        // 原码输入
        dut->io_x = switch_two_ori(x);
        dut->io_y = switch_two_ori(y);

        dut->clock = 1;
        dut->eval();
        vcd->dump(10 * cycle + 5);

        dut->clock = 0;
        dut->eval();
        vcd->dump(10 * cycle + 10);

        dut->io_start = 0;
    }

    // 原码转补码
    int top_z = switch_two_ori(dut->io_z);
    int top_r = switch_two_ori(dut->io_r);

    dut->final();
    vcd->close(); // 关闭VCD文件

    // 结果打印和统计略...
    if (int z = x / y, r = x % y; z == top_z && r == top_r) {
        success_cnt++;
    } else {
        fail_cnt++;
        std::cout << "---------- ----------" << std::endl;
        std::cout << "x: " << x << std::endl;
        std::cout << "y: " << y << std::endl;
        std::cout << "x/y: " << z << std::endl;
        std::cout << "quot: " << (int)top_z << std::endl;
        // std::cout << "x%y: \t\t" << std::bitset<8>(r).to_string() << std::endl;
        // std::cout << "remain: \t" << std::bitset<8>(top_r).to_string() << std::endl;
        // std::cout << r << std::endl;
        // std::cout << top_r << std::endl;
        std::cout << "x%y:\t\t" << std::bitset<8>(switch_two_ori(r)).to_string() << std::endl;
        std::cout << "rema:\t\t" << std::bitset<8>(dut->io_r).to_string() << std::endl;
    }

    return 0;
}

#endif