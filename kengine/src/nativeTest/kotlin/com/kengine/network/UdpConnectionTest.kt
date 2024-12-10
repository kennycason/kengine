package com.kengine.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UdpConnectionTest {

    @Test
    fun `basic udp send and receive test with assertion`() = runBlocking {
        useNetworkContext {
            val receiverLocalPort: UShort = 12345u
            val senderLocalPort: UShort = 12346u
            val receiverAddress = IPAddress("127.0.0.1", receiverLocalPort)
            val senderAddress = IPAddress("127.0.0.1", senderLocalPort)

            val receiver = UdpConnection(receiverAddress)
            receiver.connect()

            val sender = UdpConnection(senderAddress)
            sender.connect()

            val receivedMessage = CompletableDeferred<String>()
            val receiveJob = launch {
                receiver.subscribe { message: String ->
                    println("Receiver got message: $message")
                    if (!receivedMessage.isCompleted) {
                        receivedMessage.complete(message)
                    }
                }
            }

            delay(1000)

            val messageToSend = "Hello from sender!"
            println("Sender sending message: $messageToSend")
            sender.send(data = messageToSend.encodeToByteArray(), receiverAddress)

            val received = withTimeoutOrNull(1000L) { receivedMessage.await() }


            receiver.close()
            sender.close()
            receiveJob.cancelAndJoin()

            assertTrue(received != null, "Receiver did not get any message")
            assertEquals(messageToSend, received, "Received message does not match sent message")
        }
    }
}