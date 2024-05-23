#include "VDivider.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

int main(int argc, char** argv)
{
    Verilated::commandArgs(argc, argv);
    VDivider* top = new VDivider;
    Verilated::traceEverOn(true); // 启用跟踪

    VerilatedVcdC* vcd = new VerilatedVcdC;
    top->trace(vcd, 99); // 跟踪深度
    vcd->open("divider.vcd"); // 打开VCD文件

    top->reset = 1;
    top->clock = 0;

    // 重置设备
    for (int i = 0; i < 5; i++) {
        top->clock = !top->clock;
        top->eval();
        vcd->dump(i); // 记录每个时钟周期
    }

    top->reset = 0;

    // 主仿真循环
    for (int cycle = 0; cycle < 40; cycle++) {

        top->io_start = (cycle == 5);

        top->io_x = 97;
        top->io_y = 14;

        top->clock = 1;
        top->eval();
        vcd->dump(2 * cycle + 1);

        top->clock = 0;
        top->eval();
        vcd->dump(2 * cycle + 2);

        top->io_start = 0;
    }

    printf("quot: %d\n", top->io_z);
    printf("remain: %d\n", top->io_r);

    vcd->close(); // 关闭VCD文件

    top->final();
    delete top;
    delete vcd;
    return 0;
}
