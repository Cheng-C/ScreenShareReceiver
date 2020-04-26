package com.android.screensharereceiver.module.connection.udp;

import android.content.Context;
import android.util.Log;

import com.android.screensharereceiver.common.utils.AboutNetUtils;
import com.android.screensharereceiver.common.utils.ByteUtils;
import com.android.screensharereceiver.common.utils.WeakHandler;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;

/**
 * Created by wt on 2018/7/11.
 * udp广播，建立连接使用
 */
public class UdpBroadcastThread extends Thread {
    private String ip;
    private String ssCode;
    private InetAddress inetAddress;
    private int broadcastPort;
    private Context context;
    //发送广播端的socket
    private WeakHandler weakHandler;
    private MulticastSocket multicastSocket;
    private OnUdpConnectListener listener;
    public UdpBroadcastThread(Context context, String ip, String ssCode, InetAddress inetAddress,
                              MulticastSocket multicastSocket, int broadcastPort, WeakHandler weakHandler, OnUdpConnectListener listener) {
        Log.e("123", "UDPBoardcastThread: zzz");
        this.context = context;
        this.ip = ip;
        this.ssCode = ssCode;
        this.inetAddress = inetAddress;
        this.broadcastPort = broadcastPort;
        this.weakHandler = weakHandler;
        this.multicastSocket = multicastSocket;
        this.listener = listener;
        this.start();
    }
    @Override
    public void run() {
        DatagramPacket dataPacket = null;
        //将本机的IP地址放到数据包里
        byte[] ipData = ip.getBytes();
        byte[] ssCodeData = ssCode.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(8 + ipData.length + ssCodeData.length);
        byteBuffer.put(ByteUtils.int2Bytes(ipData.length));
        byteBuffer.put(ByteUtils.int2Bytes(ssCodeData.length));
        byteBuffer.put(ipData);
        byteBuffer.put(ssCodeData);
        byte[] data = byteBuffer.array();

        dataPacket = new DatagramPacket(data, data.length, inetAddress, broadcastPort);
        //判断是否中断连接了
        while (AboutNetUtils.isNetWorkConnected(context)) {
            try {
                multicastSocket.send(dataPacket);
                Thread.sleep(5000);
//                Log.e("123:","再次发送ip地址广播");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        weakHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.udpDisConnect();
            }
        });
    }
}
