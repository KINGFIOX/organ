// #include <stdio.h>

#define STR_A "1qab9a0bcabcds13"
#define STR_B "bcds"

int find_substr(char* str, char* pattern, int len1, int len2);

int main()
{
    int pos = find_substr(STR_A, STR_B, 16, 4);
    /* printf("%d\n", pos); */

    return pos;
}

int find_substr(char* str, char* pattern, int len1, int len2)
{
    int pos = -1;
    for (int i = 0; i < len1; i++) {
        for (int j = 0; j < len2; j++)
            if (str[i + j] != pattern[j])
                break;
            else if (j == len2 - 1) {
                pos = i;
                break;
            }
        if (pos != -1)
            break;
    }

    return pos;
}
