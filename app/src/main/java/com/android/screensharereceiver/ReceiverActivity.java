package com.android.screensharereceiver;

import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.android.screensharereceiver.common.base.BaseMvpActivity;

public class ReceiverActivity extends BaseMvpActivity<ReceiverContract.IPresenter> implements ReceiverContract.IView {

    private static final String TAG = "ReceiverActivity";

    private SurfaceView surfaceView;
    private Surface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        Toast.makeText(ReceiverActivity.this, "准备连接", Toast.LENGTH_SHORT).show();
        presenter.prepareConnect();

        surfaceView = findViewById(R.id.surface);
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
    public void onConnectSuccess() {
        Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
        presenter.prepareScreenShare(surface);
    }

    @Override
    public void onDisconnectSuccess() {
        Toast.makeText(this, "断开连接成功", Toast.LENGTH_SHORT).show();
    }

}
