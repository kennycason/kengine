package com.kengine.network

import com.kengine.context.ContextRegistry

fun getNetworkContext(): NetworkContext {
    return ContextRegistry.get<NetworkContext>()
}
