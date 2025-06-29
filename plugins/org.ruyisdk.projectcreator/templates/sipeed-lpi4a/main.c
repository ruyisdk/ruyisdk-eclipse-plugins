#include <stdint.h>
#include <stddef.h>
void main() {
    const char *msg = "Hello, RISC-V!\n";
    volatile char *uart = (char *)0x10000000;  
    while (*msg) {
        *uart = *msg++;
    }

    while (1); 
}
