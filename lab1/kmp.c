const int max_len = 256;
const char ln = '\n';

/**
 * @brief 全局常量，C 语言应该是初始化为 全 0
 *
 */
char g_pattern[256];
char g_text[256];

/**
 * @brief 获取字符串的长度
 *
 * @param buffer
 * @return int
 */
int strLen(const char* buffer)
{
    int length = 0;
    // 遍历字符直到遇到终止符
    while (buffer[length] != '\0') {
        length++;
    }
    return length;
}

/**
 * @brief 读取 stdin 字符串到 buffer 中
 *
 * @param buffer
 */
int readString(char* buffer)
{
    asm volatile(
        "mv a0, %0\n" // 将缓冲区地址放入寄存器a0
        "mv a1, %1\n" // 将最大字符数放入寄存器a1
        "li a7, 8\n" // 系统调用号8 (ReadString)
        "ecall\n" // 执行系统调用
        : // 无输出操作数
        : "r"(buffer), "r"(max_len) // 输入缓冲区地址和最大字符数
        : "a0", "a1", "a7" // 声明ecall更改的寄存器
    );
    int pattern_len = strLen(buffer) - 1;
    buffer[pattern_len] = '\0';
    return pattern_len;
}

/**
 * @brief 打印一个整数
 *
 * @param num
 */
void printIntLn(int num)
{
    asm volatile(
        "mv a0, %0\n"
        "li a7, 1\n" // printInt
        "ecall\n"
        : // 丢掉输出
        : "r"(num) // 输入
        : "a0", "a7" // 破坏描述
    );
    asm volatile(
        "mv a0, %0\n"
        "li a7, 11\n" // printChar
        "ecall\n"
        : // 丢掉输出
        : "r"(ln) // 输入
        : "a0", "a7" // 破坏描述
    );
}

/**
 * @brief 打印一个 以 '\0' 结尾的字符串
 *
 * @param s
 */
void printString(char* s)
{
    asm volatile(
        "mv a0, %0\n"
        "li a7, 4\n" // printInt
        "ecall\n"
        : // 丢掉输出
        : "r"(s) // 输入
        : "a0", "a7" // 破坏描述
    );
}

// 准备KMP算法的部分匹配表
void computeLPSArray(char* pattern, int patternLen, int* lps)
{
    int length = 0; // 最长的前缀子串长度
    lps[0] = 0; // 第一个字符的LPS总是0
    int i = 1;

    while (i < patternLen) {
        if (pattern[i] == pattern[length]) {
            length++;
            lps[i] = length;
            i++;
        } else {
            if (length != 0) {
                length = lps[length - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
    }
}

// KMP字符串匹配算法
/**
 * @brief
 *
 * @param pattern
 * @param patternLen
 * @param text
 * @param textLen
 * @return int 返回找到了几个数
 */
int KMPSearch(char* pattern, int patternLen, char* text, int textLen)
{
    // 创建部分匹配表并计算
    int lps[patternLen];
    computeLPSArray(pattern, patternLen, lps);

    int i = 0; // text的索引
    int j = 0; // pattern的索引

    int cnt = 0;
    while (i < textLen) {
        if (pattern[j] == text[i]) {
            i++;
            j++;
        }

        if (j == patternLen) {
            // 打印匹配的开始位置
            printIntLn(i - j);
            cnt++;
            j = lps[j - 1];
        } else if (i < textLen && pattern[j] != text[i]) {
            if (j != 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
    }
    return cnt;
}

int main(void)
{
    printString("please input pattern: \0");
    int pattern_len = readString(g_pattern);
    printString("please input text: \0");
    int text_len = readString(g_text);
    int cnt = KMPSearch(g_pattern, pattern_len, g_text, text_len);
    if (cnt == 0) {
        printString("not found");
    }
    return 0;
}
