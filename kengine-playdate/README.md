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
make post_pdx       # maybe optional, same error with or without
make run 
```

Current errors from `make run`:
```
Kengine Playdate game started!
wrong file type: no header
```
or
```
Event handler invoked.
pdxinfo file not found.
pdxinfo file not found.
...
```

pdxinfo file not found.