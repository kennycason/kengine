#include <stdint.h>
#include <pd_api.h>

// Forward declaration of eventHandler
extern int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg);

// Define the entryPoint symbol
__attribute__((used)) void* entryPoint = eventHandler;

// Shim for Playdate's event handler entry point
//int eventHandlerShim(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
//    return eventHandler(playdate, event, arg);
//}

// Define the entryPoint symbol
//__attribute__((used)) void* entryPoint = eventHandlerShim;
//__attribute__((used, section(".entry_point"))) void* entryPoint = eventHandlerShim;

int _startKengineGame(PlaydateAPI* playdate) {
    // replace with actual game initialization code
    return 0;
}

int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
    if (event == kEventInit) {
        return _startKengineGame(playdate);
    }
    return 0;
}

