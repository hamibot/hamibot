package com.hamibot.hamibot.autojs;

import com.hamibot.hamibot.services.CommandService;
import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.engine.JavaScriptEngine;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.autojs.execution.ScriptExecutionListener;
import com.hamibot.hamibot.App;
import com.hamibot.hamibot.R;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Stardust on 2017/5/3.
 */

public class ScriptExecutionGlobalListener implements ScriptExecutionListener {
    private static final String ENGINE_TAG_START_TIME = "ENGINE_TAG_START_TIME";

    @Override
    public void onStart(ScriptExecution execution) {
        execution.getEngine().setTag(ENGINE_TAG_START_TIME, System.currentTimeMillis());
    }

    @Override
    public void onSuccess(ScriptExecution execution, Object result) {
        onFinish(execution);
    }

    private void onFinish(ScriptExecution execution) {
        Long millis = (Long) execution.getEngine().getTag(ENGINE_TAG_START_TIME);
        if (millis == null)
            return;
        double seconds = (System.currentTimeMillis() - millis) / 1000.0;
        String scriptName = execution.getSource().toString();
        AutoJs.getInstance().getScriptEngineService().getGlobalConsole()
                .verbose(GlobalAppContext.getString(R.string.text_execution_finished), scriptName, seconds);
        JSONObject json = new JSONObject();
        try {
            json.put("name", scriptName);
            json.put("seconds", seconds);
            EventBus.getDefault().post(new CommandService.MessageEvent("a:script:stopped", json));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onException(ScriptExecution execution, Throwable e) {
        onFinish(execution);
    }
}
