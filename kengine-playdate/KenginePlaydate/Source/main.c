#include "pd_api.h"

// Forward declaration of eventHandler
extern int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg);

__attribute__((used)) void* entryPoint = eventHandler;

int _startKengineGame(PlaydateAPI* playdate) {
    playdate->system->logToConsole("Kengine Playdate game started!");
    return 0;
}

int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
    if (event == kEventInit) {
        return _startKengineGame(playdate);
    }
    return 0;
}