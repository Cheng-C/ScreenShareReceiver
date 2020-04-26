package com.android.screensharereceiver.module.connection.udp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.screensharereceiver.common.utils.AboutNetUtils;
import com.android.screensharereceiver.common.utils.WeakHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;


/**
 *
 */
public class UdpService extends Service implements OnUdpConnectListener {
    
    private static final String TAG = "UdpService";
    private static final String SS_CODE = "ssCode";

    private String ssCode;
    private Handler mHandler;
    //服务端的ip
    private String ip = null;
    private static int BROADCAST_PORT = 15000;
    private static String BROADCAST_IP = "224.0.0.1";
    InetAddress inetAddress = null;
    //发送广播端的socket
    MulticastSocket multicastSocket = null;
    private Context context;
    private WeakHandler weakHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        Log.i(TAG, "onCreate");
        super.onCreate();
        context = this;
        mHandler = new Handler();
        weakHandler = new WeakHandler();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        ssCode = intent.getStringExtra(SS_CODE);
        initData();
        return super.onStartCommand(intent, flags, startId);
    }
    private void initData() {
        Log.i(TAG, "initData");
        ip = null;
        try {
            while (ip == null) {
                Log.i(TAG, "initData: getAddressIP");
                ip = getAddressIP();
            }
            inetAddress = InetAddress.getByName(BROADCAST_IP);//多点广播地址组
            multicastSocket = new MulticastSocket(BROADCAST_PORT);//多点广播套接字
            multicastSocket.setTimeToLive(1);
            multicastSocket.joinGroup(inetAddress);
            Log.i(TAG, "initData: start multicastsocket");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ip != null) {
            //开始广播
            new UdpBroadcastThread(context, ip, ssCode, inetAddress, multicastSocket,
                    BROADCAST_PORT, weakHandler, this);
        }
    }

    @Override
    public void udpConnectSuccess() {

    }

    @Override
    public void udpDisConnect() {
        // TODO: 2018/7/11 连接失败
        Log.e(TAG, "udpDisConnect: 连接失败");
        initData();
    }
    /**
     * 1.获取本机正在使用网络IP地址（wifi、有线）
     */
    public String getAddressIP() {
        //检查网络是否连接
        while (!AboutNetUtils.isNetWorkConnected(context)) {
        }
        ip = AboutNetUtils.getLocalIpAddress();
        return ip;
    }

//    // TODO: 2018/7/12 获取本地所有ip地址
//    public String getLocalIpAddress() {
//        String address = null;
//        try {
//            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
//                NetworkInterface intf = en.nextElement();
//                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
//                    InetAddress inetAddress = enumIpAddr.nextElement();
//                    if (!inetAddress.isLoopbackAddress()) {
//                        address = inetAddress.getHostAddress().toString();
//                        //ipV6
//                        if (!address.contains("::")) {
//                            return address;
//                        }
//                    }
//                }
//            }
//        } catch (SocketException ex) {
//            Log.e("getIpAddress Exception", ex.toString());
//        }
//        return null;
//    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        mHandler.post(new ToastRunnable("UDP Service is unavailable."));
        super.onDestroy();
    }


    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            this.mText = text;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }

    }
}
