# This Makefile is designed to be run inside an activated Ruyi virtual environment.
CC ?= $(CROSS_PREFIX)-gcc
OBJCOPY ?= $(CROSS_PREFIX)-objcopy
# User-specific flags from the wizard are still used.
CFLAGS += __CFLAGS_OPTIONS__

# Define the output directory for intermediate files
OBJ_DIR = obj
# Define the target base name
TARGET_NAME = hello

# Final binary in the root directory
TARGET_BIN = $(TARGET_NAME).bin
# Intermediate ELF file in the obj directory
TARGET_ELF = $(OBJ_DIR)/$(TARGET_NAME).elf

# List of object files (will be placed in OBJ_DIR)
OBJS = $(OBJ_DIR)/main.o

.PHONY: all clean

# Default target: create the output directory and the final binary
all: $(OBJ_DIR) $(TARGET_BIN)

# Create the output directory if it doesn't exist
$(OBJ_DIR):
	mkdir -p $(OBJ_DIR)

# Create the final binary in the root directory from the ELF in obj
# We use the standard variables $(CC) and $(OBJCOPY) which Ruyi will set.
$(TARGET_BIN): $(TARGET_ELF)
	$(OBJCOPY) -O binary $< $@

# Link the ELF file in the obj directory
$(TARGET_ELF): $(OBJS)
	$(CC) $(LDFLAGS) -o $@ $^ $(LDLIBS)

# Compile C files into object files inside OBJ_DIR
$(OBJ_DIR)/%.o: %.c
	$(CC) $(CFLAGS) -c -o $@ $<

# Clean up all generated files from both root and obj directories
clean:
	rm -rf $(OBJ_DIR) $(TARGET_BIN)

