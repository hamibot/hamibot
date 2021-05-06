package com.hamibot.hamibot.services


import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.hamibot.hamibot.BuildConfig
import com.hamibot.hamibot.Constants
import com.hamibot.hamibot.Pref
import com.hamibot.hamibot.R
import com.hamibot.hamibot.autojs.AutoJs
import com.hamibot.hamibot.model.script.Scripts.run
import com.hamibot.hamibot.ui.main.MainActivity
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.script.StringScriptSource
import com.stardust.pio.PFiles
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.File
import java.util.*


@SuppressLint("InvalidWakeLockTag")
class CommandService : Service() {

    private val LOG_TAG = "[h4m1][Command]";

    private val mScriptExecutions = HashMap<String, ScriptExecution>()

    //private val prefs by lazy { this.getSharedPreferences("com.hamibot.hamibot", Context.MODE_PRIVATE) }
    private val powerManager by lazy { applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val wakeLock by lazy { powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, "commandService") }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate()")
        EventBus.getDefault().register(this)
        if (Constants.socket == null) {
            createSocket()
        }
        connectSocket()
        setRepeatingAlarm() // 作用未知
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //Log.d(LOG_TAG, "onStartCommand()")

        /*if (Constants.socket?.connected() == false) {
            Log.d(LOG_TAG, "Socket is connecting...")
            Constants.socket?.connect()
        }*/

        return Service.START_STICKY
    }

    private fun createSocket() {
        Log.d(LOG_TAG, "createSocket()")
        val opts = IO.Options()
        opts.transports = arrayOf(WebSocket.NAME)
        opts.forceNew = true
        opts.reconnection = true
        opts.query = "token=${Constants.WS_TOKEN}&bot"
        val socket = IO.socket(Constants.WS_SERVER, opts)
        Constants.socket = socket

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(LOG_TAG, "Socket connected")
            // 打印日志到 autojs
            AutoJs.getInstance().scriptEngineService.globalConsole.info(GlobalAppContext.getString(R.string.connected))
            wakeLock.acquire(60 * 60 * 1000)

            join()
        }
        socket?.on(Socket.EVENT_DISCONNECT) { args ->
            Log.d(LOG_TAG, "Socket disconnected")
            val reason = args[0]
            // 打印日志到 autojs
            AutoJs.getInstance().scriptEngineService.globalConsole.warn(GlobalAppContext.getString(R.string.disconnected))
            // 如果是客户端主动断开连接，则重新连接
            // TODO 已知问题，连接后会同时有 2 个连接，不知道是属于正常还是异常。与 forceNew 无关，reconnection 为 false 时即便主动连接也会无法连接，因此必须为 true
            if ("io client disconnect".equals(reason)) {
                connectSocket()
                // 连接后会马上有一个 forced close 的 EVENT_DISCONNECT，接着又马上重新连接
            }
        }
        socket?.on(Socket.EVENT_RECONNECTING) {
            Log.d(LOG_TAG, "Socket reconnecting...")
        }
        // 配对
        socket?.on("pairing:success") { args ->
            try {
                val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.pairing:success...data:" + objectToJson(data))
                val name = data.optString("name")
                val token = data.optString("token")
                Pref.setToken(token) // 保存 token
                Pref.setRobotName(name) // 保存 robot name
                // 返回结果给 MainActivity
                EventBus.getDefault().post(MainActivity.BindEvent("success", "配对成功"))
                join()
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.pairing:success Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.pairing:success Exception: $e")
            }
        }
        socket?.on("pairing:fail") { args ->
            try {
                val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.pairing:fail...data:" + objectToJson(data))
                val message = data.optString("message")
                // 返回结果给 MainActivity
                EventBus.getDefault().post(MainActivity.BindEvent("fail", message))
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.pairing:fail Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.pairing:fail Exception: $e")
            }
        }
        socket?.on("unpair:success") { args ->
            try {
                //val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.unpair:success")
                Pref.setToken("") // 清空 token
                Pref.setRobotName("") // 清空 robot name
                EventBus.getDefault().post(MainActivity.BindEvent("success", "解除配对成功"))
                disconnectSocket()
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.unpair:success Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.unpair:success Exception: $e")
            }
        }
        socket?.on("unpair:fail") { args ->
            try {
                //val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.unpair:fail")
                // 虽然系统认为解除配对失败，但仍然删除 token，以便能重新进行配对
                Pref.setToken("") // 清空 token
                Pref.setRobotName("") // 清空 robot name
                EventBus.getDefault().post(MainActivity.BindEvent("success", "删除配对成功"))
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.unpair:fail Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.unpair:fail Exception: $e")
            }
        }
        // 加入
        socket?.on("join:success") { args ->
            try {
                val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.join:success...data:" + objectToJson(data))
                val name = data.optString("name")
                Pref.setRobotName(name) // 保存 robot name
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.join:success Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.join:success Exception: $e")
            }
        }
        socket?.on("join:fail") { args ->
            try {
                Log.d(LOG_TAG, "on.join:fail")
                Pref.setToken("") // 清空 token
                Pref.setRobotName("") // 清空 robot name
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.join:fail Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.join:fail Exception: $e")
            }
        }
        // 脚本
        socket?.on("script:run") { args ->
            try {
                val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.script:run:" + objectToJson(data))
                val id = data.getString("id")
                val name = data.getString("name")
                val text = data.getString("text")
                stopScript(id)
                runScript(id, name, text)
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.script:run Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.script:run Exception: $e")
            }
        }
        socket?.on("script:stop") { args ->
            try {
                val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.script:stop:" + objectToJson(data))
                val id = data.getString("id")
                stopScript(id)
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.script:stop Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.script:stop Exception: $e")
            }
        }
        socket?.on("script:stopAll") { args ->
            try {
                Log.d(LOG_TAG, "on.script:stopAll")
                AutoJs.getInstance().scriptEngineService.stopAllAndToast()
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.script:stopAll Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.script:stopAll Exception: $e")
            }
        }
        // 其他
        socket?.on("robot:name") { args ->
            try {
                val data = JSONObject(args[0].toString())
                Log.d(LOG_TAG, "on.robot:name...data:" + objectToJson(data))
                val name = data.optString("name")
                Pref.setRobotName(name) // 保存 robot name
            } catch (e: Error) {
                Log.d(LOG_TAG, "on.robot:name Error: $e")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "on.robot:name Exception: $e")
            }
        }
    }

    private fun connectSocket() {
        if (Constants.socket?.connected() == false) {
            Log.d(LOG_TAG, "connectSocket()")
            Constants.socket?.connect()
            // 打印日志到 autojs
            AutoJs.getInstance().scriptEngineService.globalConsole.verbose(GlobalAppContext.getString(R.string.connecting))
        }
    }

    private fun disconnectSocket() {
        if (Constants.socket?.connected() == true) {
            Constants.socket?.disconnect() // 关闭连接
        }
    }

    private fun reconnectSocket() {
        if (Constants.socket?.connected() == true) {
            Log.d(LOG_TAG, "reconnectSocket()")
            Constants.socket?.disconnect() // 关闭连接
            connectSocket()
        } else {
            Constants.socket?.connect()
        }
    }

    private fun join() {
        val token = Pref.getToken()
        if (!"".equals(token)) {
            val data = JSONObject()
            val deviceInfo = getDeviceInfo()
            data.put("token", token)
            data.put("deviceInfo", deviceInfo)
            Log.d(LOG_TAG, "join...data:" + data)
            Constants.socket?.emit("a:join", data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock.release()
        Log.d(LOG_TAG, "onDestroy()")
        EventBus.getDefault().unregister(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        wakeLock.release()
        Log.d(LOG_TAG, "onTaskRemoved()")
    }

    data class MessageEvent(val name: String, val data: JSONObject)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent) {
        Log.d(LOG_TAG, "onMessageEvent:::" + event)
        val name = event.name
        val data = event.data
        Log.d(LOG_TAG, "onMessageEvent.emit:::" + objectToJson(data))
        if (Constants.socket?.connected() == true) {
            when (name) {
                "a:pair:pairing" -> {
                    val deviceInfo = getDeviceInfo()
                    data.put("deviceInfo", deviceInfo)
                }
                "a:pair:unpair" -> {
                    //reconnectSocket()
                }
            }
            Constants.socket?.emit(name, data)
        } else {
            // socket.io 没有连接时，如果是 pairing 事件则提示失败
            if ("a:pair:pairing".equals(name) || "a:pair:unpair".equals(name)) {
                EventBus.getDefault().post(MainActivity.BindEvent("fail", "网络连接失败"))
            }
        }
        // TEST - START -
        /*if (event.event == "bind") {
            Pref.setBindCode(event.message)
        } else {
            Pref.setBindCode("")
        }
        EventBus.getDefault().post(MainActivity.BindEvent(action, "success"))*/
        // TEST - END -
    }

    private fun runScript(viewId: String, name: String, script: String) {
        val scriptName = if (TextUtils.isEmpty(name)) {
            "[$viewId]"
        } else {
            PFiles.getNameWithoutExtension(name)
        }
        mScriptExecutions[viewId] = run(StringScriptSource(scriptName, script))!!
    }

    private fun stopScript(viewId: String) {
        val execution = mScriptExecutions[viewId]
        if (execution != null) {
            execution.engine.forceStop()
            mScriptExecutions.remove(viewId)
        }
    }

    private fun saveScript(name: String, script: String) {
        var scriptName = name
        if (TextUtils.isEmpty(scriptName)) {
            scriptName = "untitled"
        }
        scriptName = PFiles.getNameWithoutExtension(scriptName)
        if (!scriptName.endsWith(".js")) {
            scriptName = "$scriptName.js"
        }
        val file = File(Pref.getScriptDirPath(), scriptName)
        PFiles.ensureDir(file.path)
        PFiles.write(file, script)
    }

    private fun objectToJson(obj: Any?): String = Gson().toJson(obj)

    private fun setRepeatingAlarm() {
        val intent = Intent(this, Receiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60 * 1000, 10 * 60 * 1000, pendingIntent)
    }

    // @SuppressLint("HardwareIds") 不提示不推荐使用 ANDROID_ID
    // 推荐使用 Advertising ID，但这是 com.google.android.gms 的，反感
    @SuppressLint("HardwareIds")
    private fun getDeviceInfo(): JSONObject {
        val info = JSONObject()
        info.put("androidId", Settings.Secure.getString(this.applicationContext.contentResolver, Settings.Secure.ANDROID_ID)) // 恢复出厂会刷新此 id
        info.put("sdk", Integer.valueOf(Build.VERSION.SDK_INT).toString())
        info.put("version", Build.VERSION.RELEASE)
        info.put("serial", Build::class.java.getField("SERIAL").get(null) as String) // 并不一定能获取到
        info.put("brand", Build::class.java.getField("BRAND").get(null) as String)
        info.put("model", Build::class.java.getField("MODEL").get(null) as String)
        val telephonyManager =
                this.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val provider = telephonyManager.networkOperatorName
        info.put("provider", provider)
        // 应用自身的信息
        info.put("appVersion", BuildConfig.VERSION_NAME)
        info.put("appVersionCode", BuildConfig.VERSION_CODE)
        // 配置信息
        //info.put("email", prefs.getString("email", ""))
        return info
    }
}