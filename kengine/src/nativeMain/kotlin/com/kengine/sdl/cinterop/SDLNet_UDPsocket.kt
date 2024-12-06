package com.kengine.sdl.cinterop

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
typealias SDLNet_UDPsocket = CPointer<cnames.structs._UDPsocket>