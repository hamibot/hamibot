package com.stardust.autojs.util;

import android.os.Looper;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.stardust.app.GlobalAppContext;

import java.io.File;
import java.io.IOException;

/**
 * Created by linke on 2021/12/10
 */
public class OcrHelper {
    private static final String TAG = "OCR Helper";

    private TessBaseAPI mTessBaseAPI;

    private boolean isInitialized;

    private OcrHelper() {
    }

    private static class Holder {
        private static final OcrHelper INSTANCE = new OcrHelper();
    }

    public static OcrHelper getInstance() {
        return Holder.INSTANCE;
    }

    private synchronized void init() {
        // 拷贝文件
        if (mTessBaseAPI == null) {
            mTessBaseAPI = new TessBaseAPI();
            try {
                String destPath = GlobalAppContext.get().getFilesDir().getAbsolutePath();
                String destFilePath = destPath + File.separator + "tessdata" + File.separator + "chi_sim.traineddata";
                boolean copyRes = true;
                if (!new File(destFilePath).exists()) {
                    copyRes = FileHelper.copy(GlobalAppContext.get().getAssets().open("ocr/chi_sim.traineddata"), destFilePath);
                }
                if (copyRes) {
                    boolean initResult = mTessBaseAPI.init(destPath, "chi_sim", TessBaseAPI.OEM_TESSERACT_ONLY);
                    Log.d(TAG, "init: == " + initResult);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void initIfNeeded(InitializeCallback callback) {
        if (isInitialized) {
            if (callback != null) {
                callback.onInitFinish();
                return;
            }
        }
        isInitialized = true;
        if (Looper.getMainLooper() == Looper.myLooper()) {
            new Thread(() -> {
                init();
                if (callback != null) {
                    callback.onInitFinish();
                }
            }).start();
        } else {
            init();
            if (callback != null) {
                callback.onInitFinish();
            }
        }
    }

    public synchronized TessBaseAPI getTessBaseAPI() {
        return mTessBaseAPI;
    }

    public synchronized void end() {
        if (mTessBaseAPI != null) {
            mTessBaseAPI.end();
            mTessBaseAPI = null;
            isInitialized = false;
        }
    }


    public interface InitializeCallback {
        void onInitFinish();
    }
}
