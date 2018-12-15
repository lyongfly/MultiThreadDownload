package com.steven.download.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

/**
 * Description:
 * Data：11/30/2017-10:40 AM
 *
 * @author: yanzhiwen
 */
public class Utils {

    /**
     * 保留两位小数
     * @param value
     * @return
     */
    public static float keepTwoBit(float value) {
        DecimalFormat df = new DecimalFormat("0.00");
        return Float.parseFloat(df.format(value));
    }


}
