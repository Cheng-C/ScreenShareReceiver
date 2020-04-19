package com.android.screensharereceiver.connection;

import android.util.Log;

import com.android.screensharereceiver.Constants;
import com.android.screensharereceiver.ReceiveData;
import com.android.screensharereceiver.utils.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;


public class TcpConnection {
    private static final String TAG = "TcpConnection";
    private ServerSocket server;
    private Socket socket;
    private boolean canReceive = false;

    private OutputStream outputStream;
    private InputStream inputStream;

    private TcpConnectListener tcpConnectListener;
    private TcpDisconnectListener tcpDisconnectListener;

    private TcpConnection() {

    }

    private static class SingletonHolder {
        private static TcpConnection instance = new TcpConnection();
    }

    public static TcpConnection getInstance() {
        return SingletonHolder.instance;
    }

    public interface TcpConnectListener {
        //socket连接成功
        void onSocketConnectSuccess();

        //socket连接失败
        void onSocketConnectFail(String message);

        //tcp连接成功
        void onTcpConnectSuccess();

        //tcp连接失败
        void onTcpConnectFail(String message);

        //socket断开连接
        void onSocketDisconnect(String message);

    }

    public interface TcpDisconnectListener {
        void onTcpDisconnectSuccess();
        void onTcpDisconnectFail(String message);
    }

    public void setTcpConnectListener(TcpConnectListener tcpConnectListener) {
        this.tcpConnectListener = tcpConnectListener;
    }

    public void setTcpDisconnectListener(TcpDisconnectListener tcpDisconnectListener) {
        this.tcpDisconnectListener = tcpDisconnectListener;
    }

    public void startServer(int port, TcpConnectListener tcpConnectListener) {
        try {
            server = new ServerSocket(port);
            socket = server.accept();
            Log.i(TAG, "startServer: socket已连接");
            if (tcpConnectListener != null) {
                // Socket连接成功
                tcpConnectListener.onSocketConnectSuccess();
            }
        } catch (IOException e) {
            if (tcpConnectListener != null) {
                // Socket连接错误
                tcpConnectListener.onSocketConnectFail(e.getMessage());
            }
            e.printStackTrace();
        }
        try {
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            if (tcpConnectListener != null) {
                // TCP连接成功
                tcpConnectListener.onTcpConnectSuccess();
            }
        } catch (IOException e) {
            if (tcpConnectListener != null) {
                // TCP连接成功
                tcpConnectListener.onTcpConnectFail(e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public void disconnect(TcpDisconnectListener tcpDisconnectListener) {
        try {
            if (socket == null) {
                return;
            }
            socket.close();
            socket = null;
            if (tcpDisconnectListener != null) {
                tcpDisconnectListener.onTcpDisconnectSuccess();
            }
        } catch (IOException e) {
            if (tcpDisconnectListener != null) {
                tcpDisconnectListener.onTcpDisconnectFail(e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public void startReceiving(ArrayBlockingQueue<byte[]> playQueue) {
        canReceive = true;
        while (canReceive) {
            try {
                byte[] size = readByte(inputStream, 4);
                if (size == null || size.length == 0){
                    continue;
                }
                int bufferSize = ByteUtils.bytesToInt(size);
                if (bufferSize != 0) {
                    Log.i(TAG, "startReceiving: bufferSize:" + bufferSize);
                    byte[] encodeData = readByte(inputStream, bufferSize);
                    // checkQueue(playQueue); // 检查队列空间是否充足
                    playQueue.put(encodeData);
                }

            } catch (IOException e) {
                // 与发送端连接断开
                canReceive = false;
                if (tcpConnectListener != null) {
                    tcpConnectListener.onSocketDisconnect(e.getMessage());
                }
                Log.i(TAG, "startReceiving: 与发送端连接断开");
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] receiveData() {
        byte[] data = null;
        try {
            byte[] cmd = readByte(inputStream, 4);
            byte[] size = readByte(inputStream, 4);
            if (size == null || size.length == 0){
                return null;
            }
            int bufferSize = ByteUtils.bytesToInt(size);

            if (bufferSize != 0) {
                Log.i(TAG, "startReceiving: bufferSize:" + bufferSize);
                data = readByte(inputStream, bufferSize);
            }
            // 如果收到停止传屏消息
            if (ByteUtils.bytesToInt(cmd) == Constants.STOP_SCREEN_SHARE) {
                return cmd;
            }
        } catch (IOException e) {
            // 与发送端连接断开
            if (tcpConnectListener != null) {
                tcpConnectListener.onSocketDisconnect(e.getMessage());
            }
            Log.i(TAG, "startReceiving: 与发送端连接断开");
            e.printStackTrace();
        }

        return data;
    }

    private void checkQueue(ArrayBlockingQueue<byte[]> playQueue) throws InterruptedException {
        if (playQueue.size() > 30) {
            for (int i = 0; i < 25; i++) {
                playQueue.take();
            }
        }
    }

    private byte[] readByte(InputStream inputStream, int readSize) throws IOException {
        byte[] buff = new byte[readSize];
        int len = 0;
        int eachLen = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (len < readSize) {
            eachLen = inputStream.read(buff);
            if (eachLen != -1) {
                len += eachLen;
                baos.write(buff, 0, eachLen);
            }
            if (len < readSize) {
                buff = new byte[readSize - len];
            }
        }
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }


    public void release() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            canReceive = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
