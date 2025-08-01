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

class TcpConnectionTest : Logging {

    @Test
    fun `basic tcp send and receive message`() = runBlocking {
        NetworkContext.get() // Ensure network is initialized

        val serverPort: UShort = 12345u
        val serverAddress = IPAddress("127.0.0.1", serverPort)

        val server = TcpServer(serverAddress)
        server.start()

        val serverConnection = CompletableDeferred<TcpConnection>()
        val receivedMessage = CompletableDeferred<String>()

        // Start server accepting connections
        val serverJob = launch {
            server.acceptConnection { connection ->
                logger.info { "Server handling new connection" }
                serverConnection.complete(connection)

                connection.subscribe { message: String ->
                    logger.info { "Server got message: $message" }
                    if (!receivedMessage.isCompleted) {
                        receivedMessage.complete(message)
                    }
                }

                // Keep connection alive until test is done
                while (connection.isRunning) {
                    delay(100)
                }
            }
        }

        delay(500) // Give server time to start

        // Connect client
        val client = TcpConnection(serverAddress)
        client.connect()

        // Wait for server to accept connection
        val conn = withTimeoutOrNull(2000L) { serverConnection.await() }
        assertTrue(conn != null, "Server did not accept connection")

        // Send message from client
        val messageToSend = "Hello Server, sincerely client!"
        logger.info { "Client sending message: $messageToSend" }
        client.send(messageToSend)

        // Wait for server to receive message
        val received = withTimeoutOrNull(2000L) { receivedMessage.await() }

        try {
            assertTrue(received != null, "Server did not receive any message")
            assertEquals(messageToSend, received)
        } finally {
            client.close()
            server.stop()
            serverJob.cancelAndJoin()
        }
    }
}
