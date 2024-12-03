#!/bin/sh

cp build/bin/native/releaseShared/libkengine_playdate.dylib KenginePlaydate/Source/
cp build/bin/native/releaseStatic/libkengine_playdate.a KenginePlaydate/Source/

cd KenginePlaydate
make