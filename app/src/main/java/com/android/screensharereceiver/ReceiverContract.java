package com.android.screensharereceiver;

import android.view.Surface;

import com.android.screensharereceiver.common.base.IBasePresenter;
import com.android.screensharereceiver.common.base.IBaseView;

import java.util.concurrent.ArrayBlockingQueue;

public class ReceiverContract {
    public interface IPresenter extends IBasePresenter<ReceiverContract.IView> {
        void prepareConnect();
        void prepareScreenShare(Surface surface);
        void stopShareShare();
        void disconnect();
    }

    public interface IView extends IBaseView {
        void onConnectSuccess();
        void onDisconnectSuccess();
    }
}
