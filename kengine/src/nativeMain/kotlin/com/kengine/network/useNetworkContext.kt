//package com.kengine.network
//
//import com.kengine.hooks.context.useContextWithReturn
//
//inline fun <R> useNetworkContext(cleanup: Boolean = false, block: NetworkContext.() -> R): R {
//    return useContextWithReturn<NetworkContext, R>(cleanup, block)
//}
