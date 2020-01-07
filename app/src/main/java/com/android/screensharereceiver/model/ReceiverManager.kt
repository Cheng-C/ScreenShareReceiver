package com.android.screensharereceiver.model

class ReceiverManager private constructor() {
    companion object {
        val instance: ReceiverManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ReceiverManager()
        }
    }

    fun getData(): String {
        return ReceiverHelper.getData()
    }
}