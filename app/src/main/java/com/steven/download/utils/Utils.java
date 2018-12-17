package com.steven.download.utils;

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
