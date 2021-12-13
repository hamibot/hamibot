module.exports = function (runtime, global) {
    const rtOcr = runtime.ocr;
    const ocr = {}

    ocr.init = function () {
        return rtOcr.init();
    }

    //OCR识别结果结构层级级别[默认分割块]
    // 可选值
    // 0 -- 分割块
    // 1-- 块内段落
    // 2 -- 行
    // 3 -- 单词
    // 4 -- 字符
    ocr.ocrImage = function (image, level) {
       return rtOcr.ocrImage(image, level || 0);
    }

    ocr.end = function () {
        return rtOcr.end();
    }
    return ocr;
}
