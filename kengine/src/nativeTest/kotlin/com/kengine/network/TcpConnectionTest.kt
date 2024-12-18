//package com.kengine.network
//
//import com.kengine.log.Logging
//import kotlinx.coroutines.CompletableDeferred
//import kotlinx.coroutines.cancelAndJoin
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import kotlinx.coroutines.withTimeoutOrNull
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
//class TcpConnectionTest : Logging {
//
//    @Test
//    fun `basic tcp send and receive message`() = runBlocking {
//        val serverPort: UShort = 12345u
//        val serverAddress = IPAddress("127.0.0.1", serverPort)
//
//        val server = TcpServer(serverAddress)
//        server.start()
//        delay(100) // give server time to start
//
//        val client = TcpConnection(serverAddress)
//        client.connect()
//        val receivedMessage = CompletableDeferred<String>()
//
//        // start receiving on server
//        val serverJob = launch {
//            server.acceptConnection { connection ->
//                connection.subscribe { message: String ->
//                    logger.info { "Server got message: $message" }
//                    if (!receivedMessage.isCompleted) {
//                        receivedMessage.complete(message)
//                    }
//                }
//            }
//        }
//        delay(100) // wait for connection
//
//        // send message from client
//        val messageToSend = "Hello Server, sincerely client!"
//        logger.info {"Client sending message: $messageToSend" }
//        client.send(messageToSend)
//
//        val received = withTimeoutOrNull(2000L) { receivedMessage.await() }
//
//        try {
//            assertTrue(received != null, "Server did not receive any message")
//            assertEquals(messageToSend, received)
//        } finally {
//            client.close()
//            server.stop()
//            serverJob.cancelAndJoin()
//        }
//    }
//}
//
