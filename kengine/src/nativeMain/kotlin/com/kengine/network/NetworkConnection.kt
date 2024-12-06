package com.kengine.network

import kotlinx.serialization.KSerializer

interface NetworkConnection {
    val id: String
    fun connect()
    fun close()
    fun publish(data: UByteArray)
    fun publish(data: ByteArray)
    fun publish(data: String)
    fun <T> publish(data: T, serializer: KSerializer<T>)
    fun subscribe(onReceive: (UByteArray) -> Unit)
    fun subscribe(onReceive: (ByteArray) -> Unit)
    fun subscribe(onReceive: (String) -> Unit)
    fun <T> subscribe(onReceive: (T) -> Unit, serializer: KSerializer<T>)
}