# Chat Demo

Console TCP chat demo for `kengine-network`.

The server binds the first available port in `25000..25020`. Clients discover the first reachable server in that same range unless a port is supplied explicitly.

## Build

```bash
./gradlew :games:chat-demo:linkChatServerDebugExecutableMacosArm64
./gradlew :games:chat-demo:linkChatClientDebugExecutableMacosArm64
```

## Run

Start the server:

```bash
./games/chat-demo/build/bin/macosArm64/chatServerDebugExecutable/chatServer.kexe
```

Start clients in separate terminals:

```bash
./games/chat-demo/build/bin/macosArm64/chatClientDebugExecutable/chatClient.kexe kentroid
./games/chat-demo/build/bin/macosArm64/chatClientDebugExecutable/chatClient.kexe synthwizard
```

Send a public message by typing normally.

Send a private message with `@name message`:

```text
@kentroid secret ping
```

Use `/quit` to disconnect a client.
