package com.gantang.tianshu.utils;




import com.gantang.tianshu.constants.StringPool;

import java.util.Map;

/**
 * @ClassName ApiUtil
 * @Author huyaxi
 * @DATE 2020/4/3 10:32
 */
public class ApiUtil {


    /**
     * 组装URL
     * @param baseUrl
     * @param map
     * @return
     */
    public static String assembleGetUrl(String baseUrl, Map<String,String> map) {
        StringBuffer httpUrl = new StringBuffer();
        httpUrl.append(baseUrl).append(StringPool.GET_DELIMITER);
        map.forEach((key, value) -> httpUrl.append(key).append(StringPool.EQUAL_SIGN).append(value).append(StringPool.GET_PLACEHOLDER));
        httpUrl.deleteCharAt(httpUrl.lastIndexOf(StringPool.GET_PLACEHOLDER));
        return httpUrl.toString();
    }
}
