package com.zhouzhuo.socket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * Created by zhouzhuo on 2018/1/4.
 */

public class TCPServiceService extends Service{
    private boolean mIsServiceDestroyed;
    private String[] mDefinedMessages = new String[]{
         "你好啊,哈哈",
            "请问你叫什么名字呀?",
            "今天北京天气不错,shy",
            "你知道吗？我是可以和多个人同时聊天的哦",
            "给你讲个笑话吧:据说爱笑的运气不会太差,不知道真假。"
    };

    @Override
    public void onCreate() {
        new Thread(new TcpServer()).start();
        super.onCreate();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class TcpServer implements Runnable{
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(8688);
            } catch (IOException e) {
                Log.e("zhouzhuo","establish tcp server failed,port:8686");
                e.printStackTrace();
                return;
            }

            while (!mIsServiceDestroyed){
                try {
                    final Socket client = serverSocket.accept();
                    Log.d("zhouzhuo","accept");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                responseClient(client);
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }

        private void responseClient(Socket client) throws IOException{
            //用于接受客户端的信息
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream())
            );
            //用于向客户端发送消息消息
            PrintWriter out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(client.getOutputStream()))
            ,true);
            out.println("欢迎来到聊天室");
            while (!mIsServiceDestroyed){
                String str = in.readLine();
                Log.d("zhouzhuo","msg from client:"+str);
                if(str == null){
                    break;
                }
                int i = new Random().nextInt(mDefinedMessages.length);
                String msg = mDefinedMessages[i];
                out.println(msg);
                Log.d("zhouzhuo","send:"+msg);

            }
            Log.d("zhouzhuo","client quilt.");
            MyUtils.close(out);
            MyUtils.close(in);
            client.close();
        }
    }

    @Override
    public void onDestroy() {
        mIsServiceDestroyed =true;
        super.onDestroy();
    }
}
