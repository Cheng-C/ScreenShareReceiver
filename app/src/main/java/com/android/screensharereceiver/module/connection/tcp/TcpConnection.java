package com.android.screensharereceiver.module.connection.tcp;

import android.util.Log;

import com.android.screensharereceiver.common.constant.Constants;
import com.android.screensharereceiver.common.utils.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpConnection {
    private static final String TAG = "TcpConnection";
    private static int TCP_PORT = 9988;
    private ServerSocket server;
    private Socket socket;

    private OutputStream outputStream;
    private InputStream inputStream;

    private TcpConnection() { }

    private static class SingletonHolder {
        private static TcpConnection instance = new TcpConnection();
    }

    public static TcpConnection getInstance() {
        return SingletonHolder.instance;
    }


    public void startServer(TcpConnectListener tcpConnectListener) {
        Log.i(TAG, "startServer");
        try {
            if (server == null) {
                server = new ServerSocket();
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(TCP_PORT));
            }

            socket = server.accept();

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            if (tcpConnectListener != null) {
                Log.i(TAG, "startServer: TCP连接成功");
                tcpConnectListener.onTcpConnectSuccess();
            }
        } catch (IOException e) {
            if (tcpConnectListener != null) {
                Log.i(TAG, "startServer: TCP连接失败");
                tcpConnectListener.onTcpConnectFail(e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public void disconnect(TcpDisconnectListener tcpDisconnectListener) {
        Log.i(TAG, "disconnect");
        try {
            if (socket == null) {
                return;
            }
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
            socket = null;

            if (tcpDisconnectListener != null) {
                Log.i(TAG, "disconnect: TCP断开连接成功");
                tcpDisconnectListener.onTcpDisconnectSuccess();
            }
        } catch (IOException e) {
            if (tcpDisconnectListener != null) {
                Log.i(TAG, "disconnect: TCP断开连接失败");
                tcpDisconnectListener.onTcpDisconnectFail(e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public byte[] receiveData() {
        Log.i(TAG, "receiveData");
        byte[] data = null;
        try {
            byte[] cmd = readByte(inputStream, 4);
            Log.i(TAG, "receiveData: cmd: " + ByteUtils.bytesToInt(cmd));
            byte[] size = readByte(inputStream, 4);
            if (size == null || size.length == 0){
                return null;
            }
            int bufferSize = ByteUtils.bytesToInt(size);

            Log.i(TAG, "receiveData: bufferSize:" + bufferSize);
            if (bufferSize != 0) {
                data = readByte(inputStream, bufferSize);
            }
            // 如果收到开始传屏、停止传屏或断开连接消息，将消息返回给调用处进行处理
            if (ByteUtils.bytesToInt(cmd) == Constants.START_SCREEN_SHARE ||
                    ByteUtils.bytesToInt(cmd) == Constants.STOP_SCREEN_SHARE ||
                    ByteUtils.bytesToInt(cmd) == Constants.DISCONNECT) {
                return cmd;
            }
        } catch (IOException e) {
            Log.i(TAG, "receiveData: 异常");
            e.printStackTrace();
        }

        return data;
    }

    private byte[] readByte(InputStream inputStream, int readSize) throws IOException {
        Log.i(TAG, "readByte");
        byte[] buff = new byte[readSize];
        int len = 0;
        int eachLen;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (len < readSize) {
            eachLen = inputStream.read(buff);
            if (eachLen != -1) {
                len += eachLen;
                baos.write(buff, 0, eachLen);
            } else {
                // 连接断开则返回DISCONNECT消息
                return ByteUtils.int2Bytes(Constants.DISCONNECT);
            }
            if (len < readSize) {
                buff = new byte[readSize - len];
            }
        }
        byte[] b = baos.toByteArray();
        baos.close();
        return b;
    }

}
