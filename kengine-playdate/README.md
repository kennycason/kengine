# Playdate <> Kengine Demo

Playdate SDK setup: https://play.date/dev/

### Export SDK Path

```shell
export PLAYDATE_SDK_PATH=/Users/kennycason/Developer/PlaydateSDK
export PATH=$PATH:$PLAYDATE_SDK_PATH/bin
```

```shell
./gradlew build     # in project root
cd kengine-playdate/KenginePlaydate
make clean && make
# normally you'd just run make run, however there are some issues, so do the below
cp pdxinfo build/KenginePlaydate.pdx/ 
open -a "Playdate Simulator" build/KenginePlaydate.pdx
```

Simulator errors:
```
Event handler invoked.
Event handler invoked.
Handling kEventInit...
Kengine Playdate game started!
Game initialization complete.
<game_incompatible>
```