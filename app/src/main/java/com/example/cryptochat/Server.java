package com.example.cryptochat;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private WebSocketClient client;
    private Map<Long, String> names = new ConcurrentHashMap<>();
    private Consumer<Pair<String, String>> onMessageReceived;
    private Consumer<String> onUserConnected;   //Consumer для подключившегося пользователя
    private Consumer<Integer> onUpdateCount;    //Consumer для изменения количества пользователей

    public Server(Consumer<Pair<String, String>> onMessageReceived,
                  Consumer<String> onUserConnected,
                  Consumer<Integer> onUpdateCount) {
        this.onMessageReceived = onMessageReceived;
        this.onUserConnected = onUserConnected;
        this.onUpdateCount = onUpdateCount;
    }

    public void connect() {
        URI addr = null;
        try {
            addr = new URI("ws://35.214.3.133:8881");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;

        }
        client = new WebSocketClient(addr) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i("SERVER", "Connected to server");
            }

            @Override
            public void onMessage(String json) {
                int type = Protocol.getType(json);
                if (type == Protocol.MESSAGE) {
                    displayIncoming(Protocol.unpackMessage(json));
                }
                if (type == Protocol.USER_STATUS) {
                    updateStatus(Protocol.unpackStatus(json));
                }
                Log.i("SERVER", "Got message: " + json);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("SERVER", "Connection closed");
            }

            @Override
            public void onError(Exception ex) {
                Log.e("SERVER", "onError", ex);
            }
        };

        client.connect();
    }

    public void disconnect() {
        client.close();
    }

    public void sendMessage(String text) {
        Protocol.Message mess = new Protocol.Message(text);
        if (client != null && client.isOpen()) {
            client.send(Protocol.packMessage(mess));
        }
    }

    public void sendName(String name) {
        Protocol.UserName userName = new Protocol.UserName(name);
        if (client != null && client.isOpen()) {
            client.send(Protocol.packName(userName));
        }
    }

    private void updateStatus(Protocol.UserStatus status) {
        Protocol.User user = status.getUser();
        if (status.isConnected()) {
            String userName = user.getName();   // задаём имя пользователя
            names.put(user.getId(), userName);
            //Вызов Consumer при подключении нового пользователя
            onUserConnected.accept(userName);
        } else
            names.remove(user.getId());
        //Вызов Consumer для обновления количества пользователей on-line
        onUpdateCount.accept(names.size());
    }

    private void displayIncoming(Protocol.Message message) {
        String name = names.get(message.getSender());
        if (name == null) {
            name = "Unnamed";
        }

        onMessageReceived.accept(
            new Pair<>(name, message.getEncodedText())
        );
    }
}
