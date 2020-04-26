package com.android.screensharereceiver.module.screenshare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.android.screensharereceiver.common.base.BasePresenter;
import com.android.screensharereceiver.module.connection.tcp.TcpConnectListener;
import com.android.screensharereceiver.module.connection.tcp.TcpConnection;
import com.android.screensharereceiver.module.connection.tcp.TcpDisconnectListener;
import com.android.screensharereceiver.module.connection.udp.UdpService;
import com.android.screensharereceiver.module.receiver.ScreenReceiver;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReceiverPresenter extends BasePresenter<ReceiverContract.IView> implements ReceiverContract.IPresenter {

    private static final String TAG = "ReceiverPresenter";

    private static final String SS_CODE = "ssCode";

    private ExecutorService executorService = null;
    private TcpConnection tcpConnection = TcpConnection.getInstance();
    private ScreenReceiver screenReceiver;
//    // fair:如果true，则按FIFO顺序处理插入或删除时阻塞的线程的队列访问；如果false，则未指定访问顺序。
//    private ArrayBlockingQueue<byte[]> playQueue = new ArrayBlockingQueue<>(800, true);

    @SuppressLint("HandlerLeak")
    private Handler updateUiHandler = new Handler();

    public ReceiverPresenter() {
        Log.i(TAG, "new ReceiverPresenter");
        initThreadPool();
    }

    @Override
    public void startUdpService(Context context, String ssCode) {
        //开启udp连接服务
        Intent serverIntent = new Intent(context, UdpService.class);
        serverIntent.putExtra(SS_CODE, ssCode);
        context.startService(serverIntent);
    }

    @Override
    public void prepareTcpConnect() {
        Log.i(TAG, "prepareConnect");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                tcpConnection.startServer(new TcpConnectListener() {
                    @Override
                    public void onTcpConnectSuccess() {
                        updateUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onConnectSuccess();
                            }
                        });
                    }

                    @Override
                    public void onTcpConnectFail(String message) {

                    }
                });
            }
        });
    }

    @Override
    public void disconnect() {
        Log.i(TAG, "disconnect");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                tcpConnection.disconnect(new TcpDisconnectListener() {
                    @Override
                    public void onTcpDisconnectSuccess() {
                        updateUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onDisconnectSuccess();
                            }
                        });
                    }

                    @Override
                    public void onTcpDisconnectFail(String message) {

                    }
                });
            }
        });
    }

    @Override
    public void prepareScreenShare(Surface surface) {
        Log.i(TAG, "prepareScreenShare");
        screenReceiver = new ScreenReceiver(surface, new ScreenReceiver.CmdListener() {
            @Override
            public void onReceiveDisconnectCmd() {
                disconnect();
            }

            @Override
            public void onReceiveStartScreenShare() {
                updateUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        view.onStartScreenShare();
                    }
                });
            }

            @Override
            public void onReceiveStopScreenShare() {
                updateUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        view.onStopScreenShare();
                    }
                });
            }
        });
        executorService.execute(screenReceiver);
    }

    @Override
    public void stopShareShare() {
        Log.i(TAG, "stopShareShare");
        if (screenReceiver != null) {
            screenReceiver.stop();
        }
    }

    @Override
    public void reConfigureDecoder(Surface surface) {
        screenReceiver.reConfigure(surface);
    }

    private void initThreadPool() {
        Log.i(TAG, "initThreadPool");
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES * 2,
                        KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, taskQueue);
    }

}
