package com.android.screensharereceiver.common.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseMvpActivity<T extends IBasePresenter> extends AppCompatActivity implements IBaseView {

    protected T presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = injectPresenter();
        initView();
        initData();
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        presenter.attachView(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            presenter.detachView(this);
        }
    }

    protected abstract T injectPresenter();

    protected abstract void initView();

    protected abstract void initData();

}
