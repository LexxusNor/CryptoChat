package com.example.cryptochat;

import android.util.Log;
import android.util.Pair;

import androidx.core.util.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    // 35.214.3.133:8881/  - адрес сервера
    URI adress;
    WebSocketClient client;
    Map<Long, String> names = new ConcurrentHashMap<>();
    private Consumer<Pair<String, String>> onMessageReceived;
    private Consumer<Pair<String, String>> checkStatusUser;
    private Consumer<Integer> summOnlineUsers;

    private int sumUsers = 0;

    public Server(Consumer<Pair<String, String>> onMessageReceived, Consumer<Pair<String, String>> checkStatusUser, Consumer<Integer> summOnlineUsers) {
        this.onMessageReceived = onMessageReceived;
        this.checkStatusUser = checkStatusUser;
        this.summOnlineUsers = summOnlineUsers;
    }

    public void connect() throws URISyntaxException {
        adress = new URI("ws://35.214.3.133:8881");
        client = new WebSocketClient(adress) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                //при подключении к серверу
                Log.i("SERVER", "Connection to server is open");
                String myName = Protocol.packName(new Protocol.UserName("LexxusNor"));
                Log.i("SERVER", "Sending my name to server: " + myName);
                client.send(myName);


            }

            @Override
            public void onMessage(String message) {
                // при получении сообщения от сервера
                Log.i("SERVER", "Got message from server: " + message);
                int type = Protocol.getType(message);
                if (type == Protocol.USER_STATUS){
                    //обработать факт подключения или отключения пользователя
                    userStatusChanged(message);
                }
                if (type == Protocol.MESSAGE){
                    //показать сообщение на экране
                    displayIncomingMessage(message);
                }

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                // при закрытии соединения с сервером
                Log.i("SERVER", "Connection closed");
            }

            @Override
            public void onError(Exception ex) {
                // при возникновении ошибки
                Log.i("SERVER", "ERROR occuredd: " + ex);
            }
        };
        client.connect();
    }

    private void displayIncomingMessage(String json){
        Protocol.Message m = Protocol.unpackMessage(json);
        String name = names.get(m.getSender());
        if (name == null){
            name = "Unknown User";
        }
        onMessageReceived.accept(
                new Pair<String, String>(name, m.getEncodedText())
        );
    }

    private void userStatusChanged(String json){
        Protocol.UserStatus s = Protocol.unpackUserStatus(json);
        final Protocol.User user = s.getUser();
        if (s.isConnected()){
            names.put(user.getId(), user.getName());
            checkStatusUser.accept(
                    new Pair<String, String>(user.getName(), "on")
            );
            onlainUsers(1);
        } else {
            names.remove(user.getId());
            checkStatusUser.accept(
                    new Pair<String, String>(user.getName(), "off")
            );
            onlainUsers(-1);
        }
    }

    public void sendMessage(String message){
        if (client == null || !client.isOpen()){
            return;
        }
        Protocol.Message m = new Protocol.Message(message);
        m.setReceiver(Protocol.GROUP_CHAT);
        String packedMessage = Protocol.packMessage(m);
        Log.i("SERVER", "Send packed message: " + packedMessage);
        client.send(packedMessage);
    }

    public void onlainUsers(int count){
        sumUsers += count;
        summOnlineUsers.accept(sumUsers);
    }
}
