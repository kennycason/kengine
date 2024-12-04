#include "pd_api.h"

// Forward declaration of eventHandler
extern int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg);

// Define the entryPoint symbol used by Playdate
__attribute__((used)) void* entryPoint = eventHandler;

// Replace with actual game initialization code
int _startKengineGame(PlaydateAPI* playdate) {
    playdate->system->logToConsole("Kengine Playdate game started!");
    return 0;
}

// Event handler required by Playdate
int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
    if (event == kEventInit) {
        return _startKengineGame(playdate);
    }
    return 0;
}