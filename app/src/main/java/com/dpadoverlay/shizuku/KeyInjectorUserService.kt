package com.dpadoverlay.shizuku

import android.util.Log
import com.dpadoverlay.shizuku.IKeyInjectorService

class KeyInjectorUserService : IKeyInjectorService.Stub() {

    override fun injectKey(keyCode: Int): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("input", "keyevent", keyCode.toString()))
            process.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "injectKey failed for $keyCode", e)
            false
        }
    }

    override fun destroy() = Unit

    override fun exit() = Unit

    companion object {
        private const val TAG = "KeyInjectorUserService"
    }
}
