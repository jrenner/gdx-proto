#!/bin/bash

# copy the finished products of the gradle build (desktop jar, android apk, etc) to the bin directory
cp desktop/build/libs/desktop*.jar bin
cp android/build/apk/android-debug*.apk bin
