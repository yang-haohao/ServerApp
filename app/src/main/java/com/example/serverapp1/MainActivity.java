package com.example.serverapp1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    TextView tv_main_content;
    Handler handler=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_main_content=(TextView)findViewById(R.id.tv_main_content);

        handler=new Handler(){
            public void handleMessage(Message msg){
                switch (msg.what){
                    case 0x1234:
                        tv_main_content.setText(tv_main_content.getText().toString()+"\n"+msg.getData().getString("new_content"));
                }
            }
        };
        new Thread(new ServerListener(handler)).start();
    }
}

class ServerListener implements Runnable{
    public static ArrayList<Socket> socketList=null;
    Handler handler;
    public ServerListener(Handler handler){
        this.handler=handler;
        socketList=new ArrayList<>();
    }
    @Override
    public void run() {
        try {
            ServerSocket ss=new ServerSocket(40000);
            while (true){
                Socket sk=ss.accept();
                socketList.add(sk);
                new Thread(new ServerProcessThread(sk,handler)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ServerProcessThread implements Runnable{
    Socket sk=null;
    BufferedReader br=null;
    Handler handler=null;
    public ServerProcessThread(Socket sk,Handler handler) throws IOException {
        this.sk=sk;
        this.handler=handler;
        br=new BufferedReader(new InputStreamReader(sk.getInputStream(),"utf-8"));
    }
    @Override
    public void run() {
        String content=null;
        while ((content=readFromClient())!=null){
            Message msg=new Message();
            msg.what=0x1234;
            Bundle bundle=new Bundle();
            bundle.putString("new_content",content);
            msg.setData(bundle);
            handler.sendMessage(msg);
            for(Iterator<Socket>it = ServerListener.socketList.iterator();it.hasNext();){
                Socket outSk=it.next();
                try {
                    OutputStream outputStream=outSk.getOutputStream();
                    outputStream.write((content+"\n").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    it.remove();
                }
            }
        }
    }

    private String readFromClient() {
        try {
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            ServerListener.socketList.remove(sk);
        }
        return null;
    }
}

