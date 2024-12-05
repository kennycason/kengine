# Makefile for KenginePlaydate

# ========================================
# Variables
# ========================================

# Heap and Stack Sizes
HEAP_SIZE      = 8388208
STACK_SIZE     = 61800

# Product Output
PRODUCT        = build/KenginePlaydate.pdx
PDX_DIR        = build/KenginePlaydate.pdx

# Makefile Flags
MAKEFLAGS      += V=1

# SDK Path Detection
SDK = $(PLAYDATE_SDK_PATH)

ifeq ($(SDK),)
    $(error SDK path not found; set ENV variable PLAYDATE_SDK_PATH)
endif

$(info SDK is "$(SDK)")

# Compiler and Tools
TRGT            = arm-none-eabi-
GCC_PATH        = $(shell which $(TRGT)gcc)
GCC             = $(if $(GCC_PATH),$(shell dirname $(GCC_PATH))/$(TRGT)gcc,/usr/local/bin/$(TRGT)gcc)

OBJCPY_PATH     = $(shell which $(TRGT)objcopy)
CP              = $(if $(OBJCPY_PATH),$(shell dirname $(OBJCPY_PATH))/$(TRGT)objcopy,/usr/local/bin/$(TRGT)objcopy)

PDC             = $(SDK)/bin/pdc

# Assembler
AS              = $(GCC) -x assembler-with-cpp

# Stripping
STRIP           = $(GCC)$(TRGT)strip

# Binary and Hex
BIN             = $(CP) -O binary
HEX             = $(CP) -O ihex

# MCU Type
MCU             = cortex-m7

# C Defines
DDEFS           = -DTARGET_PLAYDATE=1 -DTARGET_EXTENSION=1

# Include Directories
DINCDIR         = . $(SDK)/C_API

# ASM Defines
DADEFS          = 

# Library Directories and Libraries
DLIBDIR         = . $(SRC_DIR)
DLIBS           = -lkengine_playdate

# Optimization Flags
OPT             = -O2 -falign-functions=16 -fomit-frame-pointer

# Linker script
LDSCRIPT        = $(SDK)/C_API/buildsupport/link_map.ld

# FPU Settings
FPU             = -mfloat-abi=hard -mfpu=fpv5-sp-d16 -D__FPU_USED=1

# Include Flags
INCDIR          = $(patsubst %,-I%,$(DINCDIR))

# Library Flags
LIBDIR          = $(patsubst %,-L%,$(DLIBDIR))

# Object and Dependency Directories
OBJDIR          = build
DEPDIR          = $(OBJDIR)/dep

# Define C Defines
DEFS            = $(DDEFS) $(UDEFS)

# Define ASM Defines
ADEFS           = $(DADEFS) $(UADEFS) -D__HEAP_SIZE=$(HEAP_SIZE) -D__STACK_SIZE=$(STACK_SIZE)

# Source Directory
SRC_DIR         = Source

# VPATH to locate source files
VPATH += $(SRC_DIR) $(SDK)/C_API/buildsupport

# Source Files
SRC = main.c stubs.c setup.c

# Object Files
OBJS = $(addprefix $(OBJDIR)/, $(SRC:.c=.o))

# Compiler Flags for C
CFLAGS          = -mcpu=$(MCU) -mthumb $(FPU) $(OPT) -ffreestanding -nostdlib \
                  -specs=nano.specs -u _printf_float $(INCDIR) -Wall -Wextra \
                  $(ADEFS) $(DEFS)

# Linker Flags
LDFLAGS         = -nostartfiles -T$(LDSCRIPT) -Wl,-Map=$(OBJDIR)/pdex.map,--cref,--gc-sections,--no-warn-mismatch,--emit-relocs $(LIBDIR)

# Extended Linker Flags
LD_FLAGS        = $(LDFLAGS)

# Dependency flags
CFLAGS          += -MD -MP -MF $(DEPDIR)/$(@F).d

# ========================================
# Phony Targets
# ========================================
.PHONY: all clean run generate_stubs device

# ========================================
# Default Target
# ========================================
all: copy_libkengine device_bin $(PRODUCT)

# ========================================
# Debug Target
# ========================================
debug: CFLAGS += -O0
debug: all

# ========================================
# Print Variable
# ========================================
print-%  : ; @echo $* = $($*)

# ========================================
# Directory Creation Rules
# ========================================
MKOBJDIR:
	mkdir -p $(OBJDIR)

MKDEPDIR:
	mkdir -p $(DEPDIR)

# ========================================
# Copy Library to Source Directory
# ========================================
copy_libkengine:
	@echo "Copying libkengine_playdate.a to ./Source..."
	@cp ../build/bin/playdate/libkengine_playdate.a  $(SRC_DIR)/libkengine_playdate.a

# ========================================
# Device Target
# ========================================
device: device_bin

# ========================================
# Binary Packaging
# ========================================
device_bin: $(OBJDIR)/pdex.elf
	cp $(OBJDIR)/pdex.elf Source

$(PRODUCT): device_bin
	@echo "Generating .pdx bundle..."
	$(PDC) Source $(PDX_DIR)
	@cp pdxinfo $(PDX_DIR)/

# ========================================
# Compilation Rules
# ========================================
$(OBJDIR)/%.o : %.c | MKOBJDIR MKDEPDIR
	mkdir -p `dirname $@`
	$(GCC) -c $(CFLAGS) $< -o $@

$(OBJDIR)/%.o : %.s | MKOBJDIR MKDEPDIR
	mkdir -p `dirname $@`
	$(AS) -c $(ADEFS) $< -o $@

# ========================================
# Linking Rules
# ========================================
.PRECIOUS: $(OBJDIR)/pdex.elf
.PRECIOUS: $(OBJDIR)/pdex.bin
.PRECIOUS: $(OBJDIR)/pdex.hex

$(OBJDIR)/pdex.elf: $(OBJS) $(LDSCRIPT)
	$(GCC) $(OBJS) $(LD_FLAGS) $(DLIBS) -o $@ 2> linker_errors.log || $(MAKE) generate_stubs

# ========================================
# Hex and Binary Conversion
# ========================================
$(OBJDIR)/pdex.hex: $(OBJDIR)/pdex.elf
	$(HEX) $< $@

$(OBJDIR)/pdex.bin: $(OBJDIR)/pdex.elf
	$(BIN) $< $@


# ========================================
# Clean Target
# ========================================
clean:
	-rm -rf $(OBJDIR)
	-rm -fR $(PRODUCT)
	-rm -fR $(SRC_DIR)/pdex.bin
	-rm -fR $(SRC_DIR)/pdex.dylib
	-rm -fR $(SRC_DIR)/pdex.so
	-rm -fR $(SRC_DIR)/pdex.dll
	-rm -fR $(SRC_DIR)/pdex.elf
	-rm -fR $(SRC_DIR)/libkengine_playdate.a

# ========================================
# Dependency Inclusion
# ========================================
# Include the dependency files, should be the last of the makefile
-include $(wildcard $(DEPDIR)/*)

# ========================================
# Stub Generation
# ========================================
generate_stubs:
	@echo "Generating stubs disabled, do so manually"
#	@echo "Generating stubs for missing symbols..."
#	@python3 generate_stubs.py linker_errors.log
#	@rm linker_errors.log
#	@echo "Stubs generated. Rebuilding..."
	@#make

# *** EOF ***