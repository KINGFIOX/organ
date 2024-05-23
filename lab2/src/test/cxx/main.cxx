#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <memory>
#include <stdlib.h>

#include <verilated.h>
#include <verilated_vcd_c.h>

#include "VDivider.h"

#define STRINGIFY(x) #x
#define TOSTRING(x) STRINGIFY(x)

#define MAX_SIM_TIME 300
#define VERIF_START_TIME 7
vluint64_t sim_time = 0;
vluint64_t posedge_cnt = 0;

/**
 * @brief 只有 driver 才包含有 DUT
 *
 */

class DividerInTx {
public:
    bool start;
    uint32_t x;
    uint32_t y;

    DividerInTx() = default;
    DividerInTx(bool _start, uint32_t _x, uint32_t _y)
        : start(_start)
        , x(_x)
        , y(_y)
    {
    }
};

/**
 * @brief 发生器
 *
 * @return DividerInTx*
 */
DividerInTx* rndDividerInTx()
{
    // 20% chance of generating a transaction
    if (rand() % 20 == 0) {
        DividerInTx* tx = new DividerInTx(rand() % 2, rand(), rand());
        return tx;
    } else {
        return nullptr;
    }
}

// ALU input interface driver
// 这个 driver 也就是做了一个事情：给 dut 的端口赋值
class DividerInDrv {
private:
    VDivider* dut;

public:
    DividerInDrv(VDivider* dut)
        : dut(dut)
    {
    }

    void drive(DividerInTx* tx)
    {
        // 初始化都是 0
        dut->io_start = 0;

        // Don't drive anything if a transaction item doesn't exist
        if (tx != nullptr) {
            dut->io_start = tx->start;
            dut->io_x = tx->x;
            dut->io_y = tx->y;
            delete tx;
        }
    }
};

// ALU output interface transaction item class
/**
 * @brief 因为我们只有一个输出
 *
 */
class DividerOutTx {
public:
    uint16_t z;
    uint16_t r;
    bool busy;
    uint32_t _x;
    uint32_t _y;
    DividerOutTx(uint16_t _z, uint16_t _r, bool _busy, uint32_t __x, uint32_t __y)
        : z(_z)
        , r(_r)
        , busy(_busy)
        , _x(__x)
        , _y(__y)
    {
    }
};

/**
 * @brief
 *
 */
class DividerScb {
private:
    std::deque<DividerInTx*> in_q;

public:
    // Input interface monitor port
    /**
     * @brief 向 DUT 写入 transaction
     *
     * @param tx
     */
    void writeIn(DividerInTx* tx)
    {
        // Push the received transaction item into a queue for later
        in_q.push_back(tx);
    }

    // Output interface monitor port
    // output
    void writeOut(DividerOutTx* tx)
    {
        // 如果 dut 不输出，那么肯定 writeOut 一定不会被调用
        if (in_q.empty()) {
            std::cout << "Fatal Error in AluScb: empty AluInTx queue" << std::endl;
            exit(1);
        }

        DividerInTx* in = in_q.front();
        in_q.pop_front();

        if (!tx->busy) {
            // 这里主要的一个问题就是: in.x != out.x
            // 我们这里其实相当于是锁上了，计算是需要时间的
            uint16_t quotient = tx->_x / tx->_y;

            uint16_t remain = tx->_x % tx->_y;

            // Compare the expected result with the actual result
            if (tx->z != quotient) {
                printf("failed: expected: %04X, got: %04X\n", quotient, tx->z);
            } else {
                printf("success: %04X\n", tx->z);
            }
        }

        // As the transaction items were allocated on the heap, it's important
        // to free the memory after they have been used
        delete in;
        delete tx;
    }
};

/**
 * @brief 暗中观察，input 接口上发生的变化，就是这个上面的变化生命周期要比
 * transaction gen 长
 *
 */
class DividerInMon {
private:
    VDivider* dut;
    DividerScb* scb;

public:
    DividerInMon(VDivider* dut, DividerScb* scb)
        : dut(dut)
        , scb(scb)
    {
    }

    void monitor()
    {
        DividerInTx* tx = new DividerInTx(dut->io_start, dut->io_x, dut->io_y);
        scb->writeIn(tx);
    }
};

/**
 * @brief
 *
 */
class DividerOutMon {
private:
    VDivider* dut;
    DividerScb* scb;

public:
    DividerOutMon(VDivider* dut, DividerScb* scb)
    {
        this->dut = dut;
        this->scb = scb;
    }

    void monitor()
    {
        DividerOutTx* tx = new DividerOutTx(dut->io_z, dut->io_r, dut->io_busy, dut->io_x, dut->io_y);

        // then pass the transaction item to the scoreboard
        scb->writeOut(tx);
    }
};

void dut_reset(VDivider* dut, vluint64_t& sim_time)
{
    dut->reset = 0;
    if (sim_time >= 3 && sim_time < 6) {
        dut->reset = 1;
        dut->io_x = 0;
        dut->io_y = 0;
        dut->io_start = 0;
    }
}

int main(int argc, char** argv, char** env)
{
    srand(time(0));
    Verilated::commandArgs(argc, argv);
    VDivider* dut = new VDivider;

    // 注册 vcd
    Verilated::traceEverOn(true);
    VerilatedVcdC* m_trace = new VerilatedVcdC;
    dut->trace(m_trace, 5);
    m_trace->open("main.vcd");

    DividerInTx* tx;

    // Here we create the driver, scoreboard, input and output monitor blocks
    // monitor 给 scb 写入，并不是传入一个 transaction 就写一次。而是 monitor
    // 一次就 注册
    DividerInDrv* drv = new DividerInDrv(dut);
    DividerScb* scb = new DividerScb();
    DividerInMon* inMon = new DividerInMon(dut, scb);
    DividerOutMon* outMon = new DividerOutMon(dut, scb);

    while (sim_time < MAX_SIM_TIME) {
        dut_reset(dut, sim_time);
        dut->clock ^= 1;
        dut->eval();

        // Do all the driving/monitoring on a positive edge
        if (dut->clock == 1) {

            if (sim_time >= VERIF_START_TIME) {
                // Generate a randomised transaction item of type AluInTx
                tx = rndDividerInTx();

                // Pass the transaction item to the ALU input interface driver,
                // which drives the input interface based on the info in the
                // transaction item
                drv->drive(tx);

                // Monitor the input interface
                inMon->monitor();

                // Monitor the output interface
                // out monitor 写到 scoreboard 里面
                outMon->monitor();
            }
        }
        // end of positive edge processing

        // 写入日志
        m_trace->dump(sim_time);
        sim_time++;
    }

    m_trace->close();
    delete dut;
    delete outMon;
    delete inMon;
    delete scb;
    delete drv;
    exit(EXIT_SUCCESS);
}