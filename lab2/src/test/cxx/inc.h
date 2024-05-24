/**
 * @brief 补码 -> 原码
 *
 * @param num
 * @param width
 * @return int
 */
int two2ori(int num, size_t width = 8)
{
    int mask = 1 << (width - 1);
    assert(num != mask); // 不能是 -128, 不然没有对应
    if (num & mask) { // 负数
        return -num | mask;
    } else {
        return num;
    }
}

/**
 * @brief 原码 -> 补码
 *
 * @param num
 * @param width
 * @return int
 */
int ori2two(int num, size_t width = 8)
{
    int mask = 1 << (width - 1);
    if (num == mask) {
        return 0;
    } // -0 问题
    if (num & mask) { // 负数
        return -num | mask;
    } else {
        return num;
    }
}

/**
 * @brief 符号拓展
 *
 * @param num
 * @param width
 * @return int
 */
int sext(int num, size_t width = 8)
{
    int mask = 1 << (width - 1);
    if (num & mask) {
        num |= ~((1 << width) - 1);
    }
    return num;
}

/**
 * @brief 封装一个 4 位的整数
 *
 */
union int4_t {
    uint8_t full_byte;
    struct {
        uint8_t value : 4; // 只使用低4位
        uint8_t unused : 4; // 未使用的高4位
    } bits;

    /**
     * @brief 有参构造
     *
     * @param value
     */
    int4_t(int8_t value)
    {
        this->bits.value = value & 0x0f;
        if (this->bits.value & 0x08) {
            this->bits.unused = 0xf;
        } else {
            this->bits.unused = 0x0;
        }
    }

    /**
     * @brief 左值
     *
     * @param value
     * @return int4_t&
     */
    int4_t& operator=(int8_t value)
    {
        this->bits.value = value & 0x0f;
        if (this->bits.value & 0x08) {
            this->bits.unused = 0xf;
        } else {
            this->bits.unused = 0x0;
        }
        return *this;
    }

    /**
     * @brief 右值
     *
     * @return int8_t
     */
    operator int8_t() const
    {
        return this->full_byte;
    }

    friend std::ostream& operator<<(std::ostream& os, const int4_t& num)
    {
        return os << (int)(int8_t)num.full_byte;
    }

    int8_t get()
    {
        return this->full_byte;
    }
};
