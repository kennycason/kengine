#!/usr/bin/env python3

import re
import sys
import os

def extract_missing_symbols(log):
    """
    Extracts undefined symbols from the linker error log.
    """
    pattern = r"undefined reference to `([^']+)'"
    return re.findall(pattern, log)

def generate_stub(symbol):
    """
    Generates a C-compatible stub for the given symbol.
    """
    # Replace C++ namespaces with underscores to create C-compatible names
    symbol = re.sub(r'std::', '', symbol)
    symbol = re.sub(r'::', '_', symbol)

    # Handle constructor and destructor symbols
    symbol = re.sub(r'::~?', 'destroy_', symbol)

    # Handle operator overloading symbols
    symbol = re.sub(r'operator\s+(\S+)', r'operator_\1', symbol)

    # Remove any template parameters or extra characters
    symbol = re.sub(r'<.*?>', '', symbol)

    # Generate a simple stub based on the symbol type
    # This assumes the symbol is a function. Adjust as needed.
    return f'void {symbol}() {{ /* Stub implementation */ }}\n'

def main():
    if len(sys.argv) != 2:
        print("Usage: generate_stubs.py <linker_errors.log>")
        sys.exit(1)

    linker_log_file = sys.argv[1]

    if not os.path.isfile(linker_log_file):
        print(f"Error: {linker_log_file} not found.")
        sys.exit(1)

    # Read the linker error log
    with open(linker_log_file, 'r') as f:
        linker_log = f.read()

    # Extract missing symbols
    missing_symbols = extract_missing_symbols(linker_log)

    if not missing_symbols:
        print("No missing symbols found.")
        sys.exit(0)

    # Remove duplicates
    missing_symbols = list(set(missing_symbols))

    # Prepare the stubs file
    stubs_file = 'Source/stubs.c'

    # Create or clear the stubs.c file if it's the first run
    if not os.path.exists(stubs_file):
        with open(stubs_file, 'w') as f:
            f.write('// Auto-generated stubs for missing symbols\n\n')
            f.write('#include <sys/stat.h>\n#include <fcntl.h>\n\n')

    # Append new stubs to stubs.c
    with open(stubs_file, 'a') as f:
        for sym in missing_symbols:
            # Only generate stubs for functions, ignore variables
            if '(' in sym and ')' in sym:
                # Extract function name without parameters
                func_name = sym.split('(')[0]
                stub = generate_stub(func_name)
                f.write(stub)

    print(f"Generated stubs for {len(missing_symbols)} symbols. Please run 'make' again.")

if __name__ == "__main__":
    main()