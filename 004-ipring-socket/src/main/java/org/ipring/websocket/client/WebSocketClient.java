package org.ipring.websocket.client;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class WebSocketClient {

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to server");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received message: " + message);
    }

    @OnError
    public void onError(Throwable throwable) {
        System.err.println("Error: " + throwable.getMessage());
    }

    public static void main(String[] args) {
        String serverUri = "ws://localhost:58041/ws?ct4Token=1231231"; // WebSocket服务器地址

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        try {
            Session session = container.connectToServer(WebSocketClient.class, URI.create(serverUri));
            session.getBasicRemote().sendText("Hello, server!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
