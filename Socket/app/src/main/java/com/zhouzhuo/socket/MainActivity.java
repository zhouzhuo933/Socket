package com.zhouzhuo.socket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int MESSAGE_RECEIVER_NEW_MSG = 1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;
    private Button btn_one ;
    private TextView tv_message;
    private EditText et_message;
    private PrintWriter mPrintWriter;
    private Socket mClientSocket;

    private Handler handler  = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MESSAGE_RECEIVER_NEW_MSG:
                    tv_message.setText(tv_message.getText()+(String)msg.obj);
                    break;
                case MESSAGE_SOCKET_CONNECTED:
                    btn_one.setEnabled(true);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initData() {
        Intent intent = new Intent(this,TCPServiceService.class);
        startService(intent);
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectTCPServer();
            }
        }).start();
    }

    private void connectTCPServer() {
        Socket socket = null;
        while (socket == null){
            try {
                socket = new Socket("localhost",8688);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream()
                )),true);
                handler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
                Log.d("zhouzhuo","connect server success");
            } catch (IOException e) {
                SystemClock.sleep(1000);
                Log.e("zhouzhuo","connect tcp server failed,retry....");
                e.printStackTrace();
            }
        }
        //接收服务器的消息
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            while (!MainActivity.this.isFinishing()){
                String msg = br.readLine();
                Log.d("zhouzhuo","receive:"+msg);
                if(msg!=null){
                    String time = formatDateTime(System.currentTimeMillis());
                    final String showedMsg = "server"+time+":"+msg+"\n";
                    handler.obtainMessage(MESSAGE_RECEIVER_NEW_MSG,showedMsg).sendToTarget();
                }
                Log.d("zhouzhuo","quit.....");
                MyUtils.close(mPrintWriter);
                MyUtils.close(br);
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        btn_one = (Button) findViewById(R.id.btn_one);
        btn_one.setOnClickListener(this);
        tv_message = (TextView) findViewById(R.id.tv_message);
        et_message = (EditText) findViewById(R.id.et_message);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_one:
                String msg = et_message.getText().toString().trim();
                if(!TextUtils.isEmpty(msg) && mPrintWriter!=null){
                    mPrintWriter.println(msg);
                    et_message.setText("");
                    String time = formatDateTime(System.currentTimeMillis());
                    String showedMsg = "self "+time +":"+msg+"\n";
                    tv_message.setText(tv_message.getText()+showedMsg);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectTCPServer();
                    }
                }).start();

                break;
        }
    }

    @Override
    protected void onDestroy() {
        if(mClientSocket !=null){
            try {
                mClientSocket.shutdownOutput();
                mClientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private String formatDateTime(long time){
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }
}
