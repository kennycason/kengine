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
UINCDIR = -I./Source

# Libraries to link
ULIBDIR = ./Source

# Compiler and linker flags
LDFLAGS += $(ULIBDIR) -lkengine_playdate

# Include common rules from the SDK
include $(SDK)/C_API/buildsupport/common.mk

# Ensure `main.c` is linked into pdex.elf
device_bin: $(OBJDIR)/pdex.elf
	@echo "Creating pdex.elf with main.c"
	cp $(OBJDIR)/pdex.elf Source/main.pdz

# Package `.pdx`
build/KenginePlaydate.pdx: device_bin
	@echo "Packaging KenginePlaydate.pdx..."
	mkdir -p build/KenginePlaydate.pdx
	cp Source/main.pdz build/KenginePlaydate.pdx/pdex.bin
	cp pdxinfo build/KenginePlaydate.pdx/pdxinfo
	$(SDK)/bin/pdc -v build/KenginePlaydate.pdx build/KenginePlaydate.pdx

# Run in Simulator
run: build/KenginePlaydate.pdx
	open -a "Playdate Simulator" $(PRODUCT)

## Clean build directory
#clean:
#	rm -rf build
#	rm -f Source/pdex.elf

.PHONY: all clean run