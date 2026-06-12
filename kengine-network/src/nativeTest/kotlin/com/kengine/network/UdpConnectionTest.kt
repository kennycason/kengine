package com.kengine.network

import com.kengine.log.Logging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UdpConnectionTest : Logging {

    @Test
    fun `send large data packets`() = runBlocking {
        NetworkContext.get()

        val receiverAddress = IPAddress("127.0.0.1", UdpTestPorts.next())
        val receiver = UdpConnection(receiverAddress)
        receiver.connect()

        val senderAddress = IPAddress("127.0.0.1", UdpTestPorts.next())
        val sender = UdpConnection(senderAddress)
        sender.connect()

        val largeData = ByteArray(5_000) { it.toByte() }
        val received = CompletableDeferred<ByteArray>()

        receiver.subscribe { bytes: ByteArray ->
            received.complete(bytes)
        }

        try {
            sender.send(largeData, receiverAddress)
            val result = withTimeout(2_000) { received.await() }
            assertContentEquals(largeData, result)
        } finally {
            receiver.close()
            sender.close()
            NetworkContext.get().cleanup()
            delay(100)
        }
    }

    @Test
    fun `basic udp send and receive test with assertion`() = runBlocking {
        NetworkContext.get()

        val receiverAddress = IPAddress("127.0.0.1", UdpTestPorts.next())
        val receiver = UdpConnection(receiverAddress)
        receiver.connect()

        val senderAddress = IPAddress("127.0.0.1", UdpTestPorts.next())
        val sender = UdpConnection(senderAddress)
        sender.connect()

        val receivedMessage = CompletableDeferred<ByteArray>()

        receiver.subscribe { bytes: ByteArray ->
            if (!receivedMessage.isCompleted) {
                receivedMessage.complete(bytes)
            }
        }

        try {
            val messageToSend = "Hello from sender!"
            val messageBytes = messageToSend.encodeToByteArray()
            sender.send(messageBytes, receiverAddress)

            val receivedBytes = withTimeout(5_000) { receivedMessage.await() }
            assertEquals(messageToSend, receivedBytes.decodeToString())
        } finally {
            receiver.close()
            sender.close()
            NetworkContext.get().cleanup()
            delay(100)
        }
    }

    @Test
    fun `udp callback exception does not stop receive loop or leak packet cleanup`() = runBlocking {
        NetworkContext.get()

        val receiverAddress = IPAddress("127.0.0.1", UdpTestPorts.next())
        val receiver = UdpConnection(receiverAddress)
        receiver.connect()

        val senderAddress = IPAddress("127.0.0.1", UdpTestPorts.next())
        val sender = UdpConnection(senderAddress)
        sender.connect()

        val firstAttempted = CompletableDeferred<Unit>()
        val secondReceived = CompletableDeferred<String>()

        receiver.subscribe { bytes: ByteArray ->
            val message = bytes.decodeToString()
            if (message == "first") {
                firstAttempted.complete(Unit)
                throw RuntimeException("intentional callback failure")
            }
            if (message == "second") {
                secondReceived.complete(message)
            }
        }

        try {
            sender.send("first", receiverAddress)
            withTimeout(2_000) { firstAttempted.await() }

            sender.send("second", receiverAddress)
            assertEquals("second", withTimeout(2_000) { secondReceived.await() })
        } finally {
            receiver.close()
            sender.close()
            NetworkContext.get().cleanup()
            delay(100)
        }
    }

    @Test
    fun `closed udp connection cannot be reused`() = runBlocking {
        NetworkContext.get()

        val receiverAddress = IPAddress("127.0.0.1", UdpTestPorts.next())
        val receiver = UdpConnection(receiverAddress)
        receiver.connect()

        try {
            receiver.close()
            assertFailsWith<IllegalStateException> {
                receiver.connect()
            }
            Unit
        } finally {
            receiver.close()
            NetworkContext.get().cleanup()
            delay(100)
        }
    }

    private object UdpTestPorts {
        private var nextPort = 24_000

        fun next(): UShort {
            val port = nextPort
            nextPort += 1
            return port.toUShort()
        }
    }
}
