package com.kengine.sdl

//import kotlinx.cinterop.ExperimentalForeignApi
//import kotlinx.cinterop.alloc
//import kotlinx.cinterop.memScoped
//import sdl2.SDL_Rect
//
//@OptIn(ExperimentalForeignApi::class)
//fun createCopy(rect: SDL_Rect): SDL_Rect {
//    return memScoped {
//        alloc<SDL_Rect>().apply {
//            this.x = rect.x
//            this.y = rect.y
//            this.w = rect.w
//            this.h = rect.h
//        }
//    }
//}
//
//@OptIn(ExperimentalForeignApi::class)
//fun SDL_Rect.copy() = createCopy(this)
