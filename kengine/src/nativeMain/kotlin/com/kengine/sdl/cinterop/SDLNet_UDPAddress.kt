package com.kengine.sdl.cinterop

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
typealias SDLNet_UDPAddress = CPointer<cnames.structs.structs_IPaddress>