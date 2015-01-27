package com.chigix.bio.proxy;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Richard Lea
 */
public class FormatDateTime {

    private static SimpleDateFormat timeStringFormat;

    static {
        timeStringFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public static String toTimeString(Date date) {
        return timeStringFormat.format(date);
    }
}
