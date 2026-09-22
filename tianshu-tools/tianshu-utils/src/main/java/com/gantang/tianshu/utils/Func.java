package com.gantang.tianshu.utils;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @ClassName Func
 * @Author huyaxi
 * @DATE 2019/6/21 13:56
 */
public class Func {

    /**
     * 设置默认字符串
     * @param val
     * @param defaultVal
     * @return
     */
    public static String defaultStr(Object val, String defaultVal) {
        return Objects.nonNull(val)? String.valueOf(val):defaultVal;
    }

    /**
     * 默认以","分割
     * 对象数组转字符串
     * @param list
     * @param list
     * @return
     */
    public static <T> String join(List<T> list) {
        return join(",",list);
    }

    /**
     * 对象数组转字符串
     * @param character
     * @param list
     * @return
     */
    public static String join(CharSequence character, List<Object> list) {
        return list.stream().map(String::valueOf).collect(Collectors.joining(character));
    }


    /**
     * 字符串数组转字符串
     * @param character
     * @param objects
     * @return
     */
    public static String join(CharSequence character,Object... objects){
        return Arrays.stream(objects).map(Object::toString).collect(Collectors.joining(character));
    }


    /**
     * 分割 字符串 删除常见 空白符
     * @param str
     * @param delimiter
     * @return
     */
    public static String[] splitTrim(@Nullable String str, @Nullable String delimiter) {
        return StringUtils.delimitedListToStringArray(str, delimiter, " \t\n\n\f");
    }


    /**
     * 对象组中是否存在 Empty Object
     *
     * @param os 对象组
     * @return boolean
     */
    public static boolean hasEmpty(Object... os) {
        for (Object o : os) {
            if (isEmpty(o)) {
                return true;
            }
        }
        return false;
    }



    /**
     * 判断空对象 object、map、list、set、字符串、数组
     *
     * @param obj the object to check
     * @return 数组是否为空
     */
    public static boolean isEmpty(@Nullable Object obj) {
        return Objects.isNull(obj);
    }


}
