# Playdate App

### Build KenginePlaydate.pdx

```shell
make clean && make
make run
```

Current errors:

```shell
Event handler invoked.
Event handler invoked.
Handling kEventInit...
Event handler invoked.
wrong file type: no header
```

The Kotlin Native gradle produced `libkengine_playdate.a` is for `Tag_CPU_arch: v4` instead of `Tag_CPU_arch: v7E-M`.


## Dev Notes

### Confirm CPU Architecture is ARM v7E-M (Cortex-M7)


```shell
$ file build/pdex.elf
```

```shell
build/pdex.elf: ELF 32-bit LSB executable, ARM, EABI5 version 1 (SYSV), statically linked, not stripped
```

```shell
$ arm-none-eabi-readelf -A build/pdex.elf
```

```shell
Attribute Section: aeabi
File Attributes
  Tag_CPU_name: "7E-M"
  Tag_CPU_arch: v7E-M
  Tag_CPU_arch_profile: Microcontroller
  Tag_THUMB_ISA_use: Thumb-2
  Tag_FP_arch: FPv5/FP-D16 for ARMv8
  Tag_ABI_PCS_wchar_t: 4
  Tag_ABI_FP_denormal: Needed
  Tag_ABI_FP_exceptions: Needed
  Tag_ABI_FP_number_model: IEEE 754
  Tag_ABI_align_needed: 8-byte
  Tag_ABI_align_preserved: 8-byte, except leaf SP
  Tag_ABI_enum_size: small
  Tag_ABI_HardFP_use: SP only
  Tag_ABI_VFP_args: VFP registers
  Tag_ABI_optimization_goals: Aggressive Speed
  Tag_CPU_unaligned_access: v6
```

CPU Arch not matching for `libkengine-playdate.a`.

```shell
$ file Source/libkengine_playdate.a
```

```shell
Source/libkengine_playdate.a: current ar archive
```

```shell
$ arm-none-eabi-readelf -A Source/libkengine_playdate.a
```

```shell
File: Source/libkengine_playdate.a(/private/var/folders/27/lg4ftdq17gd9g5738xm0txj40000gn/T/konan_temp5760948829765032764/libkengine_playdate.a.o)
Attribute Section: aeabi
File Attributes
  Tag_conformance: "2.09"
  Tag_CPU_arch: v4
  Tag_ARM_ISA_use: Yes
  Tag_ABI_PCS_R9_use: V6
  Tag_ABI_PCS_RW_data: PC-relative
  Tag_ABI_PCS_RO_data: PC-relative
  Tag_ABI_PCS_GOT_use: GOT-indirect
  Tag_ABI_PCS_wchar_t: 4
  Tag_ABI_FP_denormal: Needed
  Tag_ABI_FP_exceptions: Unused
  Tag_ABI_FP_number_model: IEEE 754
  Tag_ABI_align_needed: 8-byte
  Tag_ABI_align_preserved: 8-byte, except leaf SP
  Tag_ABI_enum_size: int
  Tag_ABI_VFP_args: VFP registers
  Tag_CPU_unaligned_access: v6
  Tag_ABI_FP_16bit_format: IEEE 754
```


### Generate Stubs (Not used)

`python3 generate_stubs.py linker_errors.log`