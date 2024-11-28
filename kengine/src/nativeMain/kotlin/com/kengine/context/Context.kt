package com.kengine.context

abstract class Context {
    open fun create() {}
    open fun cleanup() {}
}