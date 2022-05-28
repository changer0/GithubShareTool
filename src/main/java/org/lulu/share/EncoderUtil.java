package org.lulu.share;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class EncoderUtil {
    public static String encode(String target) {
        try {
            return URLEncoder.encode(target, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
