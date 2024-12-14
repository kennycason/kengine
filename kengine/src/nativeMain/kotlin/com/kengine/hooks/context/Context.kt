package com.kengine.hooks.context

abstract class Context {
    open fun create() {}
    open fun cleanup() {}
}