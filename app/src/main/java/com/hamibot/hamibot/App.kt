package com.hamibot.hamibot

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.multidex.MultiDexApplication
import android.view.View
import android.widget.ImageView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.evernote.android.job.JobRequest
import com.flurry.android.FlurryAgent
import com.squareup.leakcanary.LeakCanary
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.ui.inflater.ImageLoader
import com.stardust.autojs.core.ui.inflater.util.Drawables
import com.stardust.theme.ThemeColor
import com.tencent.bugly.Bugly
import com.tencent.bugly.crashreport.CrashReport
import com.hamibot.hamibot.autojs.AutoJs
import com.hamibot.hamibot.autojs.key.GlobalKeyObserver
import com.hamibot.hamibot.external.receiver.DynamicBroadcastReceivers
import com.hamibot.hamibot.services.CommandService
import com.hamibot.hamibot.theme.ThemeColorManagerCompat
import com.hamibot.hamibot.timing.TimedTaskManager
import com.hamibot.hamibot.timing.TimedTaskScheduler
import com.hamibot.hamibot.tool.CrashHandler
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Stardust on 2017/1/27.
 */

class App : MultiDexApplication() {
    lateinit var dynamicBroadcastReceivers: DynamicBroadcastReceivers
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate()")
        GlobalAppContext.set(this)
        instance = WeakReference(this)
        setUpStaticsTool()
        //setUpDebugEnvironment()
        init()
    }

    private fun setUpStaticsTool() {
        if (BuildConfig.DEBUG)
            return
        FlurryAgent.Builder()
                .withLogEnabled(BuildConfig.DEBUG)
                .build(this, "NR7G6ZMY5SXS4H52FNP7")
    }

    private fun init() {
        AutoJs.initInstance(this)
        if (Pref.isRunningVolumeControlEnabled()) {
            GlobalKeyObserver.init()
        }
        setupDrawableImageLoader()
        TimedTaskScheduler.init(this)
        initDynamicBroadcastReceivers()
    }

    @SuppressLint("CheckResult")
    private fun initDynamicBroadcastReceivers() {
        dynamicBroadcastReceivers = DynamicBroadcastReceivers(this)
        val localActions = ArrayList<String>()
        val actions = ArrayList<String>()
        TimedTaskManager.getInstance().allIntentTasks
                .filter { task -> task.action != null }
                .doOnComplete {
                    if (localActions.isNotEmpty()) {
                        dynamicBroadcastReceivers.register(localActions, true)
                    }
                    if (actions.isNotEmpty()) {
                        dynamicBroadcastReceivers.register(actions, false)
                    }
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(
                            DynamicBroadcastReceivers.ACTION_STARTUP
                    ))
                }
                .subscribe({
                    if (it.isLocal) {
                        localActions.add(it.action)
                    } else {
                        actions.add(it.action)
                    }
                }, { it.printStackTrace() })
    }

    private fun setupDrawableImageLoader() {
        Drawables.setDefaultImageLoader(object : ImageLoader {
            override fun loadInto(imageView: ImageView, uri: Uri) {
                Glide.with(imageView)
                        .load(uri)
                        .into(imageView)
            }

            override fun loadIntoBackground(view: View, uri: Uri) {
                Glide.with(view)
                        .load(uri)
                        .into(object : SimpleTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                view.background = resource
                            }
                        })
            }

            override fun load(view: View, uri: Uri): Drawable {
                throw UnsupportedOperationException()
            }

            override fun load(view: View, uri: Uri, drawableCallback: ImageLoader.DrawableCallback) {
                Glide.with(view)
                        .load(uri)
                        .into(object : SimpleTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                drawableCallback.onLoaded(resource)
                            }
                        })
            }

            override fun load(view: View, uri: Uri, bitmapCallback: ImageLoader.BitmapCallback) {
                Glide.with(view)
                        .asBitmap()
                        .load(uri)
                        .into(object : SimpleTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                bitmapCallback.onLoaded(resource)
                            }
                        })
            }
        })
    }

    companion object {
        private val LOG_TAG = "[h4m1][App]"
        private val BUGLY_APP_ID = ""

        private lateinit var instance: WeakReference<App>

        val app: App
            get() = instance.get()!!
    }
}
