package com.android.screensharereceiver;

import com.android.screensharereceiver.common.base.IBasePresenter;
import com.android.screensharereceiver.common.base.IBaseView;

public class ReceiverContract {
    public interface IPresenter extends IBasePresenter<ReceiverContract.IView> {
        String getData();
    }

    public interface IView extends IBaseView {
        void updateUI();
        void updateTextView(String message);
    }
}
