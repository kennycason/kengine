#include <stdlib.h>
#include "pd_api.h"

extern int _startKengineGame(PlaydateAPI* playdate);

int eventHandler(PlaydateAPI* playdate, PDSystemEvent event, uint32_t arg) {
    if (event == kEventInit) {
        return _startKengineGame(playdate);
    }
    return 0;
}
