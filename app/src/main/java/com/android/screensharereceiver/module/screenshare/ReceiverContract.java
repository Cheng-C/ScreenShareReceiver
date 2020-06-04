package com.android.screensharereceiver.module.screenshare;

import android.content.Context;
import android.view.Surface;

import com.android.screensharereceiver.common.base.IBasePresenter;
import com.android.screensharereceiver.common.base.IBaseView;

public class ReceiverContract {
    public interface IPresenter extends IBasePresenter<ReceiverContract.IView> {
        void startUdpService(Context context, String ssCode);
        void prepareTcpConnect();
        void disconnect();
        void prepareScreenShare(Surface surface);
        void stopShareShare();
    }

    public interface IView extends IBaseView {
        void onConnectSuccess();
        void onDisconnectSuccess();
        void onStartScreenShare();
        void onStopScreenShare();
    }
}
