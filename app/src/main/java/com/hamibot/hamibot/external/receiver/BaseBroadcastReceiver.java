package com.hamibot.hamibot.external.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.hamibot.hamibot.ui.main.MainActivity_;
import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.execution.ExecutionConfig;

import com.hamibot.hamibot.autojs.AutoJs;
import com.hamibot.hamibot.model.script.ScriptFile;
import com.hamibot.hamibot.timing.IntentTask;
import com.hamibot.hamibot.timing.TimedTaskManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BaseBroadcastReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "[h4m1]EL.RR.BBR";

    @SuppressLint("CheckResult")
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "onReceive: intent = " + intent + ", this = " + this);
        try {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent i = new Intent(context, MainActivity_.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 这里必须为 FLAG_ACTIVITY_NEW_TASK
                context.startActivity(i);
            }
            /*TimedTaskManager.getInstance().getIntentTaskOfAction(intent.getAction())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(intentTask -> runTask(context, intent, intentTask), Throwable::printStackTrace);*/
        } catch (Exception e) {
            GlobalAppContext.toast(e.getMessage());
        }
    }

    static void runTask(Context context, Intent intent, IntentTask task) {
        Log.d(LOG_TAG, "runTask: action = " + intent.getAction() + ", script = " + task.getScriptPath());
        ScriptFile file = new ScriptFile(task.getScriptPath());
        ExecutionConfig config = new ExecutionConfig();
        config.setArgument("intent", intent.clone());
        config.setWorkingDirectory(file.getParent());
        try {
            AutoJs.getInstance().getScriptEngineService().execute(file.toSource(), config);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
