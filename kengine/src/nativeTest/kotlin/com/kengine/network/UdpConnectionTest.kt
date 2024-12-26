package com.kengine.network

import com.kengine.log.Logging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UdpConnectionTest : Logging {

    @Test
    fun `send large data packets`() = runBlocking {
        NetworkContext.get()

        val receiverLocalPort: UShort = 12345u
        val receiverAddress = IPAddress("127.0.0.1", receiverLocalPort)
        val receiver = UdpConnection(receiverAddress)
        receiver.connect()

        val senderLocalPort: UShort = 12346u
        val senderAddress = IPAddress("127.0.0.1", senderLocalPort)
        val sender = UdpConnection(senderAddress)
        sender.connect()

        val largeData = ByteArray(5000) { it.toByte() }
        val received = CompletableDeferred<ByteArray>()

        val receiveJob = launch {
            receiver.subscribe { it: ByteArray ->
                received.complete(it)
            }
        }

        try {
            sender.send(largeData, receiverAddress)
            val result = withTimeoutOrNull(2000) { received.await() }
            assertEquals(largeData.size, result?.size)
        } finally {
            receiver.close()
            sender.close()
            receiveJob.cancelAndJoin()
            NetworkContext.get().cleanup()
            delay(100) // Give time for cleanup
        }
    }

    @Test
    fun `basic udp send and receive test with assertion`() = runBlocking {
        NetworkContext.get()

        // Use different ports for this test
        val receiverLocalPort: UShort = 12347u
        val receiverAddress = IPAddress("127.0.0.1", receiverLocalPort)
        val receiver = UdpConnection(receiverAddress)
        receiver.connect()

        delay(500)

        val senderLocalPort: UShort = 12348u
        val senderAddress = IPAddress("127.0.0.1", senderLocalPort)
        val sender = UdpConnection(senderAddress)
        sender.connect()

        delay(500)

        val receivedMessage = CompletableDeferred<ByteArray>()

        val receiveJob = launch {
            logger.info { "Setting up subscriber" }
            receiver.subscribe { bytes: ByteArray ->
                logger.info { "Subscriber received ${bytes.size} bytes" }
                if (!receivedMessage.isCompleted) {
                    receivedMessage.complete(bytes)
                }
            }
            logger.info { "Subscriber setup complete" }
        }

        delay(1000)

        try {
            val messageToSend = "Hello from sender!"
            val messageBytes = messageToSend.encodeToByteArray()
            logger.info { "Sending message of ${messageBytes.size} bytes" }
            sender.send(messageBytes, receiverAddress)
            logger.info { "Message sent" }

            val receivedBytes = withTimeoutOrNull(5000L) {
                receivedMessage.await()
            }

            assertTrue(receivedBytes != null, "Receiver did not get any message")
            assertEquals(messageToSend, receivedBytes!!.decodeToString())
        } finally {
            receiver.close()
            sender.close()
            receiveJob.cancelAndJoin()
            NetworkContext.get().cleanup()
            delay(100) // Give time for cleanup
        }
    }
}
