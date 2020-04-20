package com.android.screensharereceiver;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.android.screensharereceiver.common.base.BasePresenter;
import com.android.screensharereceiver.connection.TcpConnection;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReceiverPresenter extends BasePresenter<ReceiverContract.IView> implements ReceiverContract.IPresenter {

    private static final String TAG = "ReceiverPresenter";
    private static final int MESSAGE_UPDATE_UI = 1;

    private ExecutorService executorService = null;
    private TcpConnection tcpConnection = TcpConnection.getInstance();
    private ScreenDecoder screenDecoder;
    // fair:如果true，则按FIFO顺序处理插入或删除时阻塞的线程的队列访问；如果false，则未指定访问顺序。
    private ArrayBlockingQueue<byte[]> playQueue = new ArrayBlockingQueue<>(800, true);

    public ReceiverPresenter() {
        initThreadPool();
    }

    @SuppressLint("HandlerLeak")
    private Handler updateUiHandler = new Handler();

    @Override
    public void prepareConnect() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                tcpConnection.startServer(9988, new TcpConnection.TcpConnectListener() {
                    @Override
                    public void onSocketConnectSuccess() {

                    }

                    @Override
                    public void onSocketConnectFail(String message) {

                    }

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

                    @Override
                    public void onSocketDisconnect(String message) {
                        updateUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onDisconnectSuccess();
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void prepareScreenShare(Surface surface) {
        screenDecoder = new ScreenDecoder(surface, new ScreenDecoder.CmdListener() {
            @Override
            public void onReceiveDisconnectCmd() {
                disconnect();
            }
        });
        executorService.execute(screenDecoder);
    }

    @Override
    public void stopShareShare() {
        if (screenDecoder != null) {
            screenDecoder.stop();
        }
    }

    @Override
    public void disconnect() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                tcpConnection.disconnect(new TcpConnection.TcpDisconnectListener() {
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

    private void initThreadPool() {
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES * 2,
                        KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, taskQueue);
    }

}
