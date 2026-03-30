package com.bagdatahouse.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合工具类
 */
public class CollUtils {

    private CollUtils() {}

    /**
     * 判断集合是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断Map是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断Map是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 获取集合的第一个元素
     */
    public static <T> T getFirst(List<T> list) {
        return isNotEmpty(list) ? list.get(0) : null;
    }

    /**
     * 获取集合的最后一个元素
     */
    public static <T> T getLast(List<T> list) {
        return isNotEmpty(list) ? list.get(list.size() - 1) : null;
    }

    /**
     * 将集合转换为Map
     */
    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> keyMapper) {
        return isEmpty(collection) ? Map.of() : 
               collection.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (v1, v2) -> v2));
    }

    /**
     * 提取集合中某个字段
     */
    public static <T, R> List<R> extractField(Collection<T> collection, Function<T, R> fieldExtractor) {
        return isEmpty(collection) ? List.of() : 
               collection.stream().map(fieldExtractor).collect(Collectors.toList());
    }

    /**
     * 过滤集合
     */
    public static <T> List<T> filter(Collection<T> collection, java.util.function.Predicate<T> predicate) {
        return isEmpty(collection) ? List.of() : 
               collection.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * 集合交集
     */
    public static <T> List<T> intersection(Collection<T> c1, Collection<T> c2) {
        if (isEmpty(c1) || isEmpty(c2)) {
            return List.of();
        }
        return c1.stream().filter(c2::contains).collect(Collectors.toList());
    }

    /**
     * 集合差集
     */
    public static <T> List<T> difference(Collection<T> c1, Collection<T> c2) {
        if (isEmpty(c1)) {
            return List.of();
        }
        if (isEmpty(c2)) {
            return new java.util.ArrayList<>(c1);
        }
        return c1.stream().filter(item -> !c2.contains(item)).collect(Collectors.toList());
    }

    /**
     * 将逗号分隔的字符串转换为List
     */
    public static List<String> splitToList(String str) {
        return splitToList(str, ",");
    }

    /**
     * 将指定分隔符的字符串转换为List
     */
    public static List<String> splitToList(String str, String separator) {
        if (StrUtil.isBlank(str)) {
            return List.of();
        }
        return cn.hutool.core.collection.ListUtil.toList(str.split(separator));
    }

    /**
     * 将List转换为逗号分隔的字符串
     */
    public static String listToStr(Collection<?> collection) {
        return listToStr(collection, ",");
    }

    /**
     * 将List转换为指定分隔符的字符串
     */
    public static String listToStr(Collection<?> collection, String separator) {
        if (isEmpty(collection)) {
            return "";
        }
        return collection.stream().map(Object::toString).collect(Collectors.joining(separator));
    }
}
