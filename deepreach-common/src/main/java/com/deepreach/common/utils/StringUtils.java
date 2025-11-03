package com.deepreach.common.utils;

/**
 * 字符串工具类
 *
 * @author DeepReach Team
 */
public class StringUtils {

    public static final String EMPTY = "";

    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * 检查字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 检查对象是否为null
     */
    public static boolean isNull(Object object) {
        return object == null;
    }

    /**
     * 检查对象是否不为null
     */
    public static boolean isNotNull(Object object) {
        return !isNull(object);
    }

    /**
     * 将驼峰命名转换为下划线命名
     */
    public static String toUnderScoreCase(String str) {
        if (str == null) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        // 首字母小写
        if (str.length() > 0) {
            sb.append(Character.toLowerCase(str.charAt(0)));
        }
        // 处理后续字符
        for (int i = 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 检查字符串是否在指定字符串数组中（忽略大小写）
     */
    public static boolean inStringIgnoreCase(String str, String... strs) {
        if (str != null && strs != null) {
            for (String s : strs) {
                if (str.equalsIgnoreCase(trimToNull(s))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 去除字符串首尾的空格，如果结果为空字符串则返回null
     */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trim = str.trim();
        return trim.isEmpty() ? null : trim;
    }

    /**
     * 连接字符串数组
     */
    public static String join(String[] array, String separator) {
        if (array == null || array.length == 0) {
            return EMPTY;
        }
        if (separator == null) {
            separator = EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }
}