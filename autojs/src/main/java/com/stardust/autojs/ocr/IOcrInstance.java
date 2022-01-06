package com.stardust.autojs.ocr;

import android.graphics.Bitmap;

/**
 * Created by linke on 2022/01/01
 */
public interface IOcrInstance<T> {
    T getInstance();

    void init();

    OcrResult recognize(Bitmap bitmap);

    void end();
}
