package com.android.screensharereceiver.module.screenshare;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.screensharereceiver.R;
import com.android.screensharereceiver.common.base.BaseMvpActivity;
import com.android.screensharereceiver.common.utils.AboutNetUtils;

public class ReceiverActivity extends BaseMvpActivity<ReceiverContract.IPresenter> implements ReceiverContract.IView {

    private static final String TAG = "ReceiverActivity";

    private SurfaceView surfaceView;
    private LinearLayout llMessage;
    private TextView tvDeviceName;
    private TextView tvIpAddress;

    private Surface surface;

    private String currentIP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            presenter.stopShareShare();
            presenter.disconnect();
        }
    }

    @Override
    protected ReceiverContract.IPresenter injectPresenter() {
        return new ReceiverPresenter();
    }

    @Override
    protected void initView() {
        // 没有工具栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_receiver);

        llMessage = findViewById(R.id.llMessage);
        tvDeviceName = findViewById(R.id.tvDeviceName);
        tvIpAddress = findViewById(R.id.tvIpAddress);

        surfaceView = findViewById(R.id.surface);
        //surfaceView.setVisibility(View.GONE);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        // 使背景色不为默认黑色
        surfaceView.setZOrderOnTop(true);
        surfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    protected void initData() {
        currentIP = AboutNetUtils.getLocalIpAddress();
        if (!TextUtils.isEmpty(AboutNetUtils.getDeviceModel())) {
            tvDeviceName.setText(AboutNetUtils.getDeviceModel());
        } else {
            tvDeviceName.setText("未知设备");
        }
        if (!TextUtils.isEmpty(currentIP)) {
            tvIpAddress.setText("IP：" + currentIP);
        } else {
            tvIpAddress.setText("未知IP");
        }

        Toast.makeText(this, "连接准备", Toast.LENGTH_SHORT).show();
        presenter.startUdpService(this);
        presenter.prepareTcpConnect();
    }

    @Override
    public void onConnectSuccess() {
        Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
        llMessage.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        presenter.prepareScreenShare(surface);

    }

    @Override
    public void onDisconnectSuccess() {
        Toast.makeText(this, "连接断开", Toast.LENGTH_SHORT).show();
        llMessage.setVisibility(View.VISIBLE);
        surfaceView.setVisibility(View.GONE);
        presenter.prepareTcpConnect();
    }

}
