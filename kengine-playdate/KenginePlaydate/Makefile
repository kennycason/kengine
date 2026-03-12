HEAP_SIZE      = 8388208
STACK_SIZE     = 61800

PRODUCT = build/KenginePlaydate.pdx

# Locate the SDK
SDK = ${PLAYDATE_SDK_PATH}
ifeq ($(SDK),)
SDK = $(shell egrep '^\s*SDKRoot' ~/.Playdate/config | head -n 1 | cut -c9-)
endif

ifeq ($(SDK),)
$(error SDK path not found; set ENV value PLAYDATE_SDK_PATH)
endif

# Source files
SRC = Source/main.c

# Include paths
UINCDIR = -I./Source -I$(SDK)/C_API

# Libraries to link
ULIBDIR = ./Source ../build/bin/playdate/kenginePlaydateReleaseStatic
ULIBS = -lkengine_playdate

# Compiler and linker flags
LDFLAGS += $(addprefix -L, $(ULIBDIR)) $(ULIBS)

# Include common rules from the SDK
include $(SDK)/C_API/buildsupport/common.mk

copy_libkengine:
	@echo "Copying libkengine_playdate.a to ./Source..."
	cp ../build/bin/playdate/libkengine_playdate.a ./Source

device_bin: copy_libkengine $(OBJDIR)/pdex.elf
	@echo "Converting pdex.elf to pdex.bin..."
	arm-none-eabi-objcopy -O binary $(OBJDIR)/pdex.elf $(OBJDIR)/pdex.bin

build/KenginePlaydate.pdx: device_bin
	@echo "Packaging KenginePlaydate.pdx..."
	mkdir -p build/KenginePlaydate.pdx
	cp $(OBJDIR)/pdex.bin build/KenginePlaydate.pdx/pdex.bin
	cp pdxinfo build/KenginePlaydate.pdx/

# Run in Simulator
run: build/KenginePlaydate.pdx
	open -a "Playdate Simulator" $(PRODUCT)

.PHONY: all clean run copy_libkengine
