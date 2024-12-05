#include <pd_api.h>

// forward declarations
extern int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg);
extern int _startKengineGame(PlaydateAPI* playdate);

//int _startKengineGame(PlaydateAPI* playdate) {
//    playdate->system->logToConsole("Kengine Playdate game started!");
//    // ...
//    playdate->system->logToConsole("Game initialization complete.");
//    return 0;
//}


int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
    playdate->system->logToConsole("Event handler invoked.");
    if (event == kEventInit) {
        playdate->system->logToConsole("Handling kEventInit...");
        //return _startKengineGame(playdate);
    }
    return 0;
}

// Define the entry point
__attribute__((used)) void* entryPoint = eventHandler;