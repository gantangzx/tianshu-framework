package com.gantang.tianshu.utils;

import cn.hutool.core.collection.CollectionUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName CollectionUtils
 * @Description TODO
 * @Author yaxi.hu
 * @Date 2020/8/26 16:06
 **/
public class CollectionUtils extends CollectionUtil {

    
    /**
     * @Author yaxi.hu
     * @Description 
     * @Date 16:18 2020/8/26
     * @param  list 原始列表
     * @param  fieldName 模糊查询的字段名称
     * @param  like 模糊查询条件
     * @return java.util.List<T>
     **/
    public static <T> List<T> like(List<T> list,String fieldName,String like) {
        List<T> results = new ArrayList<>();
        list.forEach(obj -> {
            StringBuilder builder = new StringBuilder();
            try {
                Field declaredField = obj.getClass().getDeclaredField(fieldName);
                boolean accessible = declaredField.isAccessible();
                declaredField.setAccessible(true);
                builder.append(declaredField.get(obj).toString());
                declaredField.setAccessible(accessible);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            // 设置大小写不敏感
            Pattern pattern = Pattern.compile(like,Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(builder);
            if(matcher.find()){
                results.add(obj);
            }
        });
        return results;
    }
}
