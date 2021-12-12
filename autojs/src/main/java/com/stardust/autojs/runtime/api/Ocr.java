package com.stardust.autojs.runtime.api;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.stardust.autojs.annotation.ScriptInterface;
import com.stardust.autojs.core.image.ImageWrapper;
import com.stardust.autojs.ocr.OcrResult;
import com.stardust.autojs.util.OcrHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by linke on 2021/12/08
 */
public class Ocr {

    // OCR识别结果结构层级级别[默认分割块]
    // 可选值
    // 0 -- 分割块
    // 1-- 块内段落
    // 2 -- 行
    // 3 -- 单词
    // 4 -- 字符
    public static final int DEFAULT_LEVEL = TessBaseAPI.PageIteratorLevel.RIL_BLOCK;

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
    public OcrResult ocrImage(ImageWrapper image, int level) {
        if (image == null) {
            return null;
        }
        if (level > TessBaseAPI.PageIteratorLevel.RIL_SYMBOL || level < TessBaseAPI.PageIteratorLevel.RIL_BLOCK) {
            level = DEFAULT_LEVEL;
        }
        OcrResult ocrResult = new OcrResult();
        try {
            TessBaseAPI tessBaseAPI = OcrHelper.getInstance().getTessBaseAPI();
            if (tessBaseAPI != null) {
                tessBaseAPI.setImage(image.getBitmap());
                ocrResult.success = true;
                ocrResult.text = tessBaseAPI.getUTF8Text();
                List<OcrResult.OCrWord> words = new ArrayList<>();
                ocrResult.words = words;
                ResultIterator resultIterator = tessBaseAPI.getResultIterator();
                resultIterator.begin();
                do {
                    words.add(new OcrResult.OCrWord(resultIterator.getUTF8Text(level), resultIterator.getBoundingRect(level), resultIterator.confidence(level) / 100));
                } while (resultIterator.next(level));
                resultIterator.delete();
                tessBaseAPI.clear();
            }
        } catch (Throwable th) {
            ocrResult.success = false;
            ocrResult.text = "";
            ocrResult.words = Collections.emptyList();
        }

        return ocrResult;
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

