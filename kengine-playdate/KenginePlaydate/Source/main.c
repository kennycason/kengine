#include <pd_api.h>

extern int _startKengineGame(PlaydateAPI* playdate);
extern int _updateKengineGame(void);
extern void _cleanupKengineGame(void);

static int update(void* userdata) {
    return _updateKengineGame();
}

int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
    if (event == kEventInit) {
        playdate->system->logToConsole("Initializing Kengine Playdate...");
        playdate->system->setUpdateCallback(update, NULL);
        // NOTE: _startKengineGame is commented out until the ARM architecture
        // mismatch is resolved (Kotlin/Native linuxArm32Hfp produces ARMv4,
        // Playdate requires ARMv7E-M).
        // _startKengineGame(playdate);
    } else if (event == kEventTerminate) {
        playdate->system->logToConsole("Terminating Kengine Playdate...");
        // _cleanupKengineGame();
    }
    return 0;
}

__attribute__((used)) void* entryPoint = eventHandler;
