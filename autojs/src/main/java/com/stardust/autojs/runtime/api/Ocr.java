package com.stardust.autojs.runtime.api;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.stardust.autojs.annotation.ScriptInterface;
import com.stardust.autojs.core.image.ImageWrapper;
import com.stardust.autojs.util.OcrHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by linke on 2021/12/08
 */
public class Ocr {

    //初始化超时时间5s
    private static final long INIT_TIMEOUT = 5000;

    @ScriptInterface
    public boolean init() {
        Ref<Boolean> isSuccess = new Ref<>(false);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        OcrHelper.getInstance().initIfNeeded(() -> {
            countDownLatch.countDown();
            isSuccess.value = true;
        });
        try {
            countDownLatch.await(INIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        return isSuccess.value;
    }

    @ScriptInterface
    public String ocrImage(ImageWrapper image) {
        if (image == null) {
            return "";
        }
        try {
            TessBaseAPI tessBaseAPI = OcrHelper.getInstance().getTessBaseAPI();
            if (tessBaseAPI != null) {
                tessBaseAPI.setImage(image.getBitmap());
                return tessBaseAPI.getUTF8Text();
            }
        } catch (Throwable th) {
            // ignore
        }
        return "";
    }

    @ScriptInterface
    public boolean end() {
        OcrHelper.getInstance().end();
        return true;
    }

    static class Ref<T> {
        public T value;

        public Ref(T value) {
            this.value = value;
        }
    }
}

