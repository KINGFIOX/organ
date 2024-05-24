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
