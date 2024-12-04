#!/bin/sh

cp build/bin/playdate/kenginePlaydateReleaseStatic/libkengine_playdate.a KenginePlaydate/Source/

cd KenginePlaydate

make clean
make