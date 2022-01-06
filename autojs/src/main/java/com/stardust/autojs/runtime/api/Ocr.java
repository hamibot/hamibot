package com.stardust.autojs.runtime.api;

import android.graphics.Bitmap;

import com.stardust.autojs.annotation.ScriptInterface;
import com.stardust.autojs.core.image.ImageWrapper;
import com.stardust.autojs.ocr.OcrResult;
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
    public OcrResult ocrImage(ImageWrapper image) {
        if (image == null) {
            return OcrResult.buildFailResult();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return OcrResult.buildFailResult();
        }
        return OcrHelper.getInstance().getOcrInstance().recognize(bitmap);
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

