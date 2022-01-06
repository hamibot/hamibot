package com.stardust.autojs.ocr;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import com.stardust.app.GlobalAppContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cn.codekong.paddle.ocr.OCRPredictor;
import cn.codekong.paddle.ocr.OCRResultModel;

/**
 * Created by linke on 2022/01/01
 */
public class PaddleOcrInstance implements IOcrInstance<OCRPredictor> {

    private OCRPredictor mOCRPredictor;

    public PaddleOcrInstance() {
        this.mOCRPredictor = new OCRPredictor();
    }

    @Override
    public OCRPredictor getInstance() {
        return this.mOCRPredictor;
    }

    @Override
    public void init() {
        if (mOCRPredictor != null) {
            mOCRPredictor.init(GlobalAppContext.get());
        }
    }

    @Override
    public OcrResult recognize(Bitmap bitmap) {
        OcrResult ocrResult;
        if (mOCRPredictor != null && bitmap != null && !bitmap.isRecycled()) {
            mOCRPredictor.setInputImage(bitmap);
            List<OCRResultModel> ocrResultModelList = mOCRPredictor.runOcr();
            ocrResult = transformData(ocrResultModelList);
            ocrResult.timeRequired = mOCRPredictor.inferenceTime();
        } else {
            ocrResult = OcrResult.buildFailResult();
        }
        return ocrResult;
    }

    @Override
    public void end() {
        if (mOCRPredictor != null) {
            mOCRPredictor.release();
        }
    }

    public OcrResult transformData(List<OCRResultModel> ocrResultModelList) {
        if (ocrResultModelList == null) {
            return OcrResult.buildFailResult();
        }
        OcrResult ocrResult = new OcrResult();
        List<OcrResult.OCrWord> wordList = new ArrayList<>();
        StringBuilder textSb = new StringBuilder();
        for (OCRResultModel model : ocrResultModelList) {
            List<Point> pointList = model.getPoints();
            // left为x最小值,top为y最小值,right为x最大值,bottom为y最大值
            if (pointList.isEmpty()) {
                continue;
            }
            Point firstPoint = pointList.get(0);
            int left = firstPoint.x;
            int top = firstPoint.y;
            int right = firstPoint.x;
            int bottom = firstPoint.y;
            for (Point p : pointList) {
                if (p.x < left) {
                    left = p.x;
                }
                if (p.x > right) {
                    right = p.x;
                }
                if (p.y < top) {
                    top = p.y;
                }
                if (p.y > bottom) {
                    bottom = p.y;
                }
            }
            textSb.append(model.getLabel());
            Rect rect = new Rect(left, top, right, bottom);
            OcrResult.OCrWord oCrWord = new OcrResult.OCrWord(model.getLabel(), rect, model.getConfidence());
            wordList.add(oCrWord);
        }
        ocrResult.success = true;
        ocrResult.words = wordList;
        ocrResult.text = textSb.toString();
        return ocrResult;
    }
}
