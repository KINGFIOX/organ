#include <stdint.h>

#define ADDRESS_WIDTH (18)
#define OFFSET_WIDTH (2)
#define INDEX_WIDTH (9)

/**
 * @brief 直接映射 + 组相连
 *
 */
typedef union {
    uint64_t address : ADDRESS_WIDTH;
    struct {
        uint64_t offset : OFFSET_WIDTH;
        uint64_t index : INDEX_WIDTH;
        uint64_t tag : ADDRESS_WIDTH - OFFSET_WIDTH - INDEX_WIDTH;
    };
} addr1;

/**
 * @brief 全相连
 *
 */
typedef union {
    uint64_t address : ADDRESS_WIDTH;
    struct {
        uint64_t offset : OFFSET_WIDTH;
        uint64_t tag : ADDRESS_WIDTH - OFFSET_WIDTH;
    };
} addr2;