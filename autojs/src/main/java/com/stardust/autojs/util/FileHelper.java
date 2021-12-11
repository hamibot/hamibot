package com.stardust.autojs.util;

import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by linke on 2021/12/08
 */
public class FileHelper {

    /**
     * 复制文件
     * @param srcPath
     * @param destPath
     * @return
     */
    public static boolean copy(String srcPath, String destPath) {
        if (TextUtils.isEmpty(srcPath) || TextUtils.isEmpty(destPath)) {
            return false;
        }
        File srcFile = new File(srcPath);
        if (!srcFile.exists()) {
            return false;
        }
        try {
            return copy(new FileInputStream(srcFile), destPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 复制文件
     * @param is
     * @param destPath
     * @return
     */
    public static boolean copy(InputStream is, String destPath) {
        if (is == null || TextUtils.isEmpty(destPath)) {
            return false;
        }

        File destFile = new File(destPath);
        if (!destFile.getParentFile().exists()) {
            if (destFile.getParentFile().mkdirs()) {
                try {
                    if (!destFile.createNewFile()) {
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        FileOutputStream fos = null;
        try {
            byte[] data = new byte[1024];
            //输出流
            fos = new FileOutputStream(destPath);
            //开始处理流
            while (is.read(data) != -1) {
                fos.write(data);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
}
