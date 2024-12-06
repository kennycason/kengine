package com.kengine.sdl.cinterop

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
typealias SDLNet_TCPsocket = CPointer<cnames.structs._TCPsocket>