package org.jraf.android.simplewatchface2

import android.os.Handler
import android.os.Message
import java.lang.ref.WeakReference

class EngineHandler(reference: SimpleWatchFaceService.SimpleWatchFaceEngine) : Handler() {
    private val mWeakReference = WeakReference(reference)

    override fun handleMessage(msg: Message) {
        mWeakReference.get()?.handleUpdateTimeMessage()
    }
}