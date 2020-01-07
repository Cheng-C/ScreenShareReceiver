package com.android.screensharereceiver;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.screensharereceiver.common.base.BasePresenter;
import com.android.screensharereceiver.model.ReceiverManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReceiverPresenter extends BasePresenter<ReceiverContract.IView> implements ReceiverContract.IPresenter {

    private static final String TAG = "ReceiverPresenter";
    private static final int MESSAGE_UPDATE_TEXT_VIEW = 0;
    private static final int PORT = 9988;
    private ServerSocket server = null;
    private Socket client = null;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private ExecutorService executorService = null;
    boolean canReceive = false;

    public ReceiverPresenter() {
        initThreadPool();
        startServer();
    }

    @SuppressLint("HandlerLeak")
    private Handler updateUIHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MESSAGE_UPDATE_TEXT_VIEW) {
                view.updateTextView((String)msg.obj);
            } else {
                view.updateUI();
            }
        }
    };

    @Override
    public String getData() {
        return ReceiverManager.Companion.getInstance().getData();
    }

    private void initThreadPool() {
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES * 2,
                        KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, taskQueue);
    }

    public void startServer() {

        //执行任务
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new ServerSocket(PORT);
                    prepareConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void startReceiving() {
        executorService.execute(new Runnable() {
            String message = null;
            Message handlerMessage;

            @Override
            public void run() {
                while (canReceive) {
                    try {
                        message = bufferedReader.readLine();
                        if (message != null) {
                            handlerMessage = updateUIHandler.obtainMessage();
                            handlerMessage.what = MESSAGE_UPDATE_TEXT_VIEW;
                            handlerMessage.obj = message;
                            handlerMessage.sendToTarget();
                            if (message.equals("客户端断开连接")) {
                                Log.i(TAG, "连接断开");
                                release();
                                prepareConnection();
                            }
                            message = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void prepareConnection() {
        try {
            client = server.accept();
            Log.i(TAG, "连接成功");
            String data = getData(); // 获取数据data = getData(); // 获取数据
            printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    client.getOutputStream(), "UTF-8")), true);
            bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            Message handlerMessage = updateUIHandler.obtainMessage();
            handlerMessage.what = MESSAGE_UPDATE_TEXT_VIEW;
            handlerMessage.obj = "连接成功";
            handlerMessage.sendToTarget();
            canReceive = true;
            startReceiving();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void release() {
        try {
            client.close();
            printWriter.close();
            bufferedReader.close();
            canReceive = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
