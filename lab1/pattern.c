// 朴素字符串匹配算法的函数
// haystack - 目标字符串
// needle - 模式字符串
// haystack_len - 目标字符串长度
// needle_len - 模式字符串长度
int naive_match(const char* haystack, int haystack_len, const char* needle, int needle_len)
{
    if (needle_len == 0) {
        return 0; // 如果模式串为空，认为它总是匹配
    }

    // 遍历目标字符串的每个起始位置
    for (int i = 0; i <= haystack_len - needle_len; i++) {
        int j;
        // 检查从 i 位置开始的子串是否匹配
        for (j = 0; j < needle_len; j++) {
            if (haystack[i + j] != needle[j]) {
                break; // 如果字符不同，退出内层循环
            }
        }
        if (j == needle_len) {
            return i; // 找到匹配，返回起始位置
        }
    }

    return -1; // 没有找到匹配，返回 -1
}

char* hello = "hello";
int hello_len = 5;
char* hello_world = "abcd hello world";
int hello_world_len = 16;

int main(void)
{
    return naive_match(hello_world, hello_world_len, hello, hello_len);
}