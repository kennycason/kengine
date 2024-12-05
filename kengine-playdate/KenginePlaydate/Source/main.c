#include <pd_api.h>

// forward declarations
extern int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg);
extern int _startKengineGame(PlaydateAPI* playdate);

int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
    playdate->system->logToConsole("Event handler invoked.");
    if (event == kEventInit) {
        playdate->system->logToConsole("Handling kEventInit...");
        // return _startKengineGame(playdate);
        // ^ results in linker errors due to libkengine_playdate.a not being cortex-m7 arch
    }
    return 0;
}

// define the entry point
__attribute__((used)) void* entryPoint = eventHandler;