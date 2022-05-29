package org.lulu.share;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地文件 kv 存储
 *
 *         <dependency>
 *             <groupId>org.json</groupId>
 *             <artifactId>json</artifactId>
 *             <version>20220320</version>
 *         </dependency>
 *
 *        <dependency>
 *             <groupId>commons-io</groupId>
 *             <artifactId>commons-io</artifactId>
 *             <version>2.11.0</version>
 *         </dependency>
 */
public class KVStorage {

    public static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    public static void init() {
        String s = "";
        try {
            s = FileUtils.readFileToString(getAndCreateConfigFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (StringUtils.isEmpty(s)) {
            return;
        }
        JSONObject jsonObject = new JSONObject(s);
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            CACHE.put(key, jsonObject.optString(key));
        }
    }

    public static void put(String key, String value) {

        try {
            CACHE.put(key, value);
            File file = getAndCreateConfigFile();

            String s = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            JSONObject jsonObj;
            if (s.isEmpty()) {
                jsonObj = new JSONObject();
            } else {
                jsonObj = new JSONObject(s);

            }
            jsonObj.put(key, value);
            FileUtils.writeStringToFile(file, jsonObj.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key, String defaultValue) {
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        String s = "";
        try {
            s = FileUtils.readFileToString(getAndCreateConfigFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObj;
        if (s.isEmpty()) {
            return defaultValue;
        } else {
            jsonObj = new JSONObject(s);
        }
        String v = jsonObj.optString(key, defaultValue);
        CACHE.put(key, v);
        return v;
    }

    private static File getAndCreateConfigFile() {
        String fileName = "config.json";
        File file = new File(fileName);
        if (file.exists()) {
            return file;
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

}
