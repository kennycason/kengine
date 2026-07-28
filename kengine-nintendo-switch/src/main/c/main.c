#include <stdio.h>
#include <switch.h>

#ifndef KENGINE_SWITCH_C_ONLY
#include "kengine_switch_kotlin_api.h"
#endif

static int kotlin_add_probe(void) {
#ifdef KENGINE_SWITCH_C_ONLY
    return 42;
#else
    kengine_switch_kotlin_ExportedSymbols* symbols = kengine_switch_kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchAdd(20, 22);
#endif
}

#ifndef KENGINE_SWITCH_C_ONLY
static int kotlin_message_code_probe(void) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kengine_switch_kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchMessageCode();
}
#endif

int main(int argc, char* argv[]) {
    (void)argc;
    (void)argv;

    consoleInit(NULL);

    PadState pad;
    padConfigureInput(1, HidNpadStyleSet_NpadStandard);
    padInitializeDefault(&pad);

    printf("Kengine Nintendo Switch\n");
    printf("Hello from libnx.\n");
    printf("Kotlin add probe: %d\n", kotlin_add_probe());

#ifndef KENGINE_SWITCH_C_ONLY
    printf("Kotlin message code: %d\n", kotlin_message_code_probe());
#else
    printf("Kotlin linkage disabled for this build.\n");
#endif

    printf("\nPress + to exit.\n");

    while (appletMainLoop()) {
        padUpdate(&pad);

        u64 buttons_down = padGetButtonsDown(&pad);
        if (buttons_down & HidNpadButton_Plus) {
            break;
        }

        consoleUpdate(NULL);
    }

    consoleExit(NULL);
    return 0;
}
