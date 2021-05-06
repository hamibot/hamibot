package com.hamibot.hamibot.ui.log;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;

import com.stardust.autojs.core.console.ConsoleView;
import com.stardust.autojs.core.console.ConsoleImpl;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import com.hamibot.hamibot.R;
import com.hamibot.hamibot.autojs.AutoJs;
import com.hamibot.hamibot.ui.BaseActivity;

@EActivity(R.layout.activity_log)
public class LogActivity extends BaseActivity {

    @ViewById(R.id.console)
    ConsoleView mConsoleView;

    private ConsoleImpl mConsoleImpl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @AfterViews
    void setupViews() {
        setToolbarAsBack(getString(R.string.text_log));
        mConsoleImpl = AutoJs.getInstance().getGlobalConsole();
        mConsoleView.setConsole(mConsoleImpl);
        mConsoleView.findViewById(R.id.input_container).setVisibility(View.GONE);
    }

    // 清空日志
    @Click(R.id.fab)
    void clearConsole() {
        mConsoleImpl.clear();
    }
}
