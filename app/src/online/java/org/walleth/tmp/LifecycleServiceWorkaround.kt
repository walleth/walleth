package org.walleth.tmp

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import org.jetbrains.annotations.Nullable

/**
 * A Service that is also a [LifecycleOwner].
 * Copy as a workaround for https://issuetracker.google.com/issues/144442247
 */
@SuppressLint("Registered")
open class LifecycleServiceWorkaround : Service(), LifecycleOwner {
    private val mDispatcher by lazy { ServiceLifecycleDispatcher(this) }
    @CallSuper
    override fun onCreate() {
        mDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    @CallSuper
    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        mDispatcher.onServicePreSuperOnBind()
        return null
    }

    @CallSuper
    override fun onStart(intent: Intent, startId: Int) {
        mDispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    // this method is added only to annotate it with @CallSuper.
    // In usual service super.onStartCommand is no-op, but in LifecycleService
    // it results in mDispatcher.onServicePreSuperOnStart() call, because
    // super.onStartCommand calls onStart().
    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = super.onStartCommand(intent, flags, startId)

    @CallSuper
    override fun onDestroy() {
        mDispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    override fun getLifecycle() = mDispatcher.lifecycle

}