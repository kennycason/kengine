package com.kengine.network

import com.kengine.log.Logging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TcpConnectionTest : Logging {

    @Serializable
    private data class TestPayload(val id: Int, val body: String)

    @Test
    fun `basic tcp send and receive message`() = runBlocking {
        NetworkContext.get()

        val serverAddress = IPAddress("127.0.0.1", TcpTestPorts.next())
        val server = TcpServer(serverAddress)
        server.start()

        val serverConnection = CompletableDeferred<TcpConnection>()
        val receivedMessage = CompletableDeferred<String>()

        val serverJob = launch {
            server.acceptConnection { connection ->
                serverConnection.complete(connection)
                connection.subscribe { message: String ->
                    if (!receivedMessage.isCompleted) {
                        receivedMessage.complete(message)
                    }
                }
            }
        }

        var client: TcpConnection? = null
        var accepted: TcpConnection? = null
        try {
            delay(200)
            client = TcpConnection(serverAddress)
            client.connect()
            accepted = withTimeout(2_000) { serverConnection.await() }

            val messageToSend = "Hello Server, sincerely client!"
            client.send(messageToSend)

            val received = withTimeout(2_000) { receivedMessage.await() }
            assertEquals(messageToSend, received)
        } finally {
            client?.close()
            accepted?.close()
            server.stop()
            serverJob.cancelAndJoin()
            NetworkContext.get().cleanup()
        }
    }

    @Test
    fun `tcp string subscriber stays alive while idle before data arrives`() = runBlocking {
        NetworkContext.get()

        val serverAddress = IPAddress("127.0.0.1", TcpTestPorts.next())
        val server = TcpServer(serverAddress)
        server.start()

        val serverConnection = CompletableDeferred<TcpConnection>()
        val receivedMessage = CompletableDeferred<String>()

        val serverJob = launch {
            server.acceptConnection { connection ->
                serverConnection.complete(connection)
                connection.subscribe { message: String ->
                    receivedMessage.complete(message)
                }
            }
        }

        var client: TcpConnection? = null
        var accepted: TcpConnection? = null
        try {
            delay(200)
            client = TcpConnection(serverAddress)
            client.connect()
            accepted = withTimeout(2_000) { serverConnection.await() }

            delay(300)
            client.send("sent after idle")

            assertEquals("sent after idle", withTimeout(2_000) { receivedMessage.await() })
        } finally {
            client?.close()
            accepted?.close()
            server.stop()
            serverJob.cancelAndJoin()
            NetworkContext.get().cleanup()
        }
    }

    @Test
    fun `tcp string framing preserves back to back messages`() = runBlocking {
        NetworkContext.get()

        val serverAddress = IPAddress("127.0.0.1", TcpTestPorts.next())
        val server = TcpServer(serverAddress)
        server.start()

        val serverConnection = CompletableDeferred<TcpConnection>()
        val receivedMessages = Channel<String>(Channel.UNLIMITED)

        val serverJob = launch {
            server.acceptConnection { connection ->
                serverConnection.complete(connection)
                connection.subscribe { message: String ->
                    receivedMessages.trySend(message)
                }
            }
        }

        var client: TcpConnection? = null
        var accepted: TcpConnection? = null
        try {
            delay(200)
            client = TcpConnection(serverAddress)
            client.connect()
            accepted = withTimeout(2_000) { serverConnection.await() }

            client.send("first")
            client.send("second")

            assertEquals("first", withTimeout(2_000) { receivedMessages.receive() })
            assertEquals("second", withTimeout(2_000) { receivedMessages.receive() })
        } finally {
            client?.close()
            accepted?.close()
            server.stop()
            serverJob.cancelAndJoin()
            NetworkContext.get().cleanup()
        }
    }

    @Test
    fun `tcp serialized messages survive partial frame reads`() = runBlocking {
        NetworkContext.get()

        val serverAddress = IPAddress("127.0.0.1", TcpTestPorts.next())
        val server = TcpServer(serverAddress)
        server.start()

        val acceptedConnections = Channel<TcpConnection>(Channel.UNLIMITED)
        val receivedPayload = CompletableDeferred<TestPayload>()
        val payload = TestPayload(7, "x".repeat(5_000))

        val serverJob = launch {
            server.acceptConnection { connection ->
                acceptedConnections.trySend(connection)
                delay(100)
                connection.send(payload, TestPayload.serializer())
            }
        }

        var client: TcpConnection? = null
        val accepted = mutableListOf<TcpConnection>()
        try {
            delay(200)
            client = TcpConnection(serverAddress, bufferSize = 13)
            client.connect()
            client.subscribe(
                onReceive = { message: TestPayload ->
                    receivedPayload.complete(message)
                },
                serializer = TestPayload.serializer()
            )
            accepted += withTimeout(2_000) { acceptedConnections.receive() }

            assertEquals(payload, withTimeout(2_000) { receivedPayload.await() })
        } finally {
            client?.close()
            accepted.forEach { it.close() }
            server.stop()
            serverJob.cancelAndJoin()
            NetworkContext.get().cleanup()
        }
    }

    @Test
    fun `server accepted connections have unique non-placeholder ids`() = runBlocking {
        NetworkContext.get()

        val serverAddress = IPAddress("127.0.0.1", TcpTestPorts.next())
        val server = TcpServer(serverAddress)
        server.start()

        val acceptedConnections = Channel<TcpConnection>(Channel.UNLIMITED)
        val serverJob = launch {
            server.acceptConnection { connection ->
                acceptedConnections.trySend(connection)
            }
        }

        val clients = mutableListOf<TcpConnection>()
        val accepted = mutableListOf<TcpConnection>()
        try {
            delay(200)
            repeat(3) {
                TcpConnection(serverAddress).also { client ->
                    client.connect()
                    clients += client
                }
            }

            repeat(3) {
                accepted += withTimeout(2_000) { acceptedConnections.receive() }
            }

            val ids = accepted.map { it.id }
            assertEquals(3, ids.toSet().size)
            ids.forEach { id ->
                assertNotEquals("0.0.0.0:0", id)
                assertTrue(id.isNotBlank(), "Accepted connection id should not be blank")
            }
        } finally {
            clients.forEach { it.close() }
            accepted.forEach { it.close() }
            server.stop()
            serverJob.cancelAndJoin()
            NetworkContext.get().cleanup()
        }
    }

    @Test
    fun `closed tcp connection cannot be reused`() = runBlocking {
        NetworkContext.get()

        val serverAddress = IPAddress("127.0.0.1", TcpTestPorts.next())
        val server = TcpServer(serverAddress)
        server.start()

        val acceptedConnections = Channel<TcpConnection>(Channel.UNLIMITED)
        val serverJob = launch {
            server.acceptConnection { connection ->
                acceptedConnections.trySend(connection)
            }
        }

        var client: TcpConnection? = null
        var accepted: TcpConnection? = null
        try {
            delay(200)
            client = TcpConnection(serverAddress)
            client.connect()
            accepted = withTimeout(2_000) { acceptedConnections.receive() }
            client.close()

            assertFailsWith<IllegalStateException> {
                client.connect()
            }
            Unit
        } finally {
            client?.close()
            accepted?.close()
            server.stop()
            serverJob.cancelAndJoin()
            NetworkContext.get().cleanup()
        }
    }

    private object TcpTestPorts {
        private var nextPort = 23_000

        fun next(): UShort {
            val port = nextPort
            nextPort += 1
            return port.toUShort()
        }
    }
}
