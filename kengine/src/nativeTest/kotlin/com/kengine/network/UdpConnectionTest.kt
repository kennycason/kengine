package com.kengine.network

import com.kengine.log.Logging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UdpConnectionTest : Logging {

    @Ignore
    fun `basic udp send and receive test with assertion`() = runBlocking {
        val receiverLocalPort: UShort = 12345u
        val senderLocalPort: UShort = 12346u
        val receiverAddress = IPAddress("localhost", receiverLocalPort)
        val senderAddress = IPAddress("localhost", senderLocalPort)

        val receiver = UdpConnection(receiverAddress)
        receiver.connect()

        val sender = UdpConnection(senderAddress)
        sender.connect()

        val receivedMessage = CompletableDeferred<String>()
        val receiveJob = launch {
            receiver.subscribe { message: String ->
                logger.info { "Receiver got message: $message" }
                if (!receivedMessage.isCompleted) {
                    receivedMessage.complete(message)
                }
            }
        }

        delay(100)

        val messageToSend = "Hello from sender!"
        logger.info {"Sender sending message: $messageToSend" }
        sender.send(messageToSend.encodeToByteArray(), receiverAddress)

        val received = withTimeoutOrNull(2000L) { receivedMessage.await() }

        try {
            assertTrue(received != null, "Receiver did not get any message")
            assertEquals(messageToSend, received)
        } finally {
            receiver.close()
            sender.close()
            receiveJob.cancelAndJoin()
        }
    }
}
