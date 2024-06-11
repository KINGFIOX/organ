#include "addr2.h"
#include <stdio.h>

int main(void) {
	addr a ;
	a.address = 0xabcdef;
	printf("offset=\t\t%03b\t\t(%d)\n", a.offset, a.offset);
	printf("index=\t\t%08b\t\t(%d)\n", a.index, a.index);
	printf("tag=\t\t%09b\t\t(%d)\n", a.tag, a.tag);
}