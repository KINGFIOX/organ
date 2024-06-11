#include <stdint.h>

#define ADDRESS_WIDTH (24)
#define ALIGN_WIDTH (2)
#define OFFSET_WIDTH (3)
#define INDEX_WIDTH (8) // 512 个 Cacheline

/**
 * @brief 直接映射 + 组相连
 *
 */
typedef union {
    uint64_t address : ADDRESS_WIDTH;
    struct {
        uint64_t : ALIGN_WIDTH; // 对齐
        uint64_t offset : OFFSET_WIDTH;
        uint64_t index : INDEX_WIDTH;
        uint64_t tag : ADDRESS_WIDTH - OFFSET_WIDTH - INDEX_WIDTH - ALIGN_WIDTH;
    };
} addr;
