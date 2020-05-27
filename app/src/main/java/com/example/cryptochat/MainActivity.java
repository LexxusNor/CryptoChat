package com.example.cryptochat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    TextView onlineUsers;
    Button sendButton;
    EditText userInput;
    RecyclerView chatWindow;
    MessageController controller;
    Server server;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendButton = findViewById(R.id.sendButton);
        userInput = findViewById(R.id.userInput);
        chatWindow = findViewById(R.id.chatView);
        onlineUsers = findViewById(R.id.onlineUsers);


        controller = new MessageController();
        controller.setIncomingLayout(R.layout.message);
        controller.setOutgoingLayout(R.layout.outgoing_message);
        controller.setMessageTextId(R.id.messageText);
        controller.setMessageTimeId(R.id.messageDate);
        controller.setUserNameId(R.id.userName);
        controller.appendTo(chatWindow, this);

        controller.addMessage( new MessageController.Message("Всем привет!", "SkillBox", true)
        );

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = userInput.getText().toString();
                controller.addMessage( new MessageController.Message(text, "LexxusNor", false)
                );
                server.sendMessage(text);
                userInput.setText("");
            }
        });

        server = new Server(new Consumer<Pair<String, String>>() {
            @Override
            public void accept(final Pair<String, String> p) { // имя, сообщение
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        controller.addMessage(new MessageController.Message(p.second, p.first, true)
                        );
                    }
                });
            }
        }, new Consumer<Pair<String, String>>() {
            @Override
            public void accept(final Pair<String, String> s) {
                Log.i("SERVER", "Запустился метод статуса!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String name = s.first;
                        String status = s.second;
                        Log.i("SERVER", "Получение статуса пользователей: Имя: " + name + ", статус: " + status);
                        if (status.contains("on")) {
                            Toast toast = Toast.makeText(getApplicationContext(), name + " подключился к чату", Toast.LENGTH_LONG);
                            toast.show();
                        } else if (status.contains("off")) {
                            Toast toast = Toast.makeText(getApplicationContext(), name + " отключился от чата", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                });
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(final Integer i) {
                Log.i("SERVER", "Запустился метод подсчета пользователей онлайн");
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        Log.i("SERVER", "Summ of online users: " + i);
                        onlineUsers.setText("Пользователей онлайн: " + i);
                    }
                });
            }
        });
        try {
            server.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

    }
}
