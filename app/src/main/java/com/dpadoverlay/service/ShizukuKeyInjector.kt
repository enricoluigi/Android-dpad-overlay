package com.dpadoverlay.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.dpadoverlay.shizuku.IKeyInjectorService
import com.dpadoverlay.shizuku.KeyInjectorUserService
import rikka.shizuku.Shizuku

object ShizukuKeyInjector {

    private var service: IKeyInjectorService? = null
    private var binding = false
    private var userServiceArgs: Shizuku.UserServiceArgs? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            binding = false
            service = IKeyInjectorService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            binding = false
        }
    }

    private fun buildArgs(context: Context): Shizuku.UserServiceArgs {
        return Shizuku.UserServiceArgs(
            ComponentName(context.applicationContext, KeyInjectorUserService::class.java)
        )
            .daemon(false)
            .processNameSuffix("key_injector")
            .version(1)
    }

    fun bind(context: Context) {
        if (!ShizukuHelper.isReady() || service != null || binding) return
        binding = true
        try {
            val args = buildArgs(context)
            userServiceArgs = args
            Shizuku.bindUserService(args, connection)
        } catch (_: Exception) {
            binding = false
        }
    }

    fun unbind() {
        val args = userServiceArgs ?: return
        if (service == null && !binding) return
        try {
            Shizuku.unbindUserService(args, connection, false)
        } catch (_: Exception) {
            // ignore
        }
        service = null
        binding = false
        userServiceArgs = null
    }

    fun injectKey(context: Context, keyCode: Int): Boolean {
        if (!ShizukuHelper.isReady()) return false
        if (service == null) {
            bind(context)
            return false
        }
        return try {
            service?.injectKey(keyCode) == true
        } catch (_: Exception) {
            service = null
            false
        }
    }
}
