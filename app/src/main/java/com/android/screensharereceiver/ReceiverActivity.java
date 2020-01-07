package com.android.screensharereceiver;

import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.TextView;

import com.android.screensharereceiver.common.base.BaseMvpActivity;

public class ReceiverActivity extends BaseMvpActivity<ReceiverContract.IPresenter> implements ReceiverContract.IView {

    private static final String TAG = "ReceiverActivity";

    private Surface surface;
    private SurfaceView surfaceView;

    private TextView recordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);

        recordTextView = findViewById(R.id.tv_record);
    }

    @Override
    protected ReceiverContract.IPresenter injectPresenter() {
        return new ReceiverPresenter();
    }

    @Override
    public void updateUI() {

    }

    @Override
    public void updateTextView(String message) {
        if (recordTextView != null) {
            recordTextView.append(message + "\n");
        }
    }
}
