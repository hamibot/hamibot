package com.stardust.autojs.ocr;

import android.graphics.Rect;

import java.util.List;

/**
 * Created by linke on 2021/12/12
 */
public class OcrResult {
    public boolean success;
    public String text;
    public List<OCrWord> words;

    public static class OCrWord {

        public String text;
        public Rect bounds;
        public float confidences;

        public OCrWord(String text, Rect rect, float confidences) {
            this.text = text;
            this.bounds = rect;
            this.confidences = confidences;
        }
    }
}
