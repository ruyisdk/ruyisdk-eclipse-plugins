TOOLCHAIN_BIN = __TOOLCHAIN_BIN_PATH__
CROSS = __TOOLCHAIN_PREFIX_NAME__

export PATH := $(TOOLCHAIN_BIN):$(PATH)

CFLAGS = -Wall -O2 -nostdlib -nostartfiles -march=rv64imac -mabi=lp64

all: hello.bin

hello.elf: start.o main.o linker.ld
	$(CROSS)-ld -o $@ -T linker.ld start.o main.o

%.o: %.c
	$(CROSS)-gcc $(CFLAGS) -c -o $@ $<

%.o: %.S
	$(CROSS)-gcc $(CFLAGS) -c -o $@ $<

hello.bin: hello.elf
	$(CROSS)-objcopy -O binary $< $@

clean:
	rm -f *.o *.elf *.bin
