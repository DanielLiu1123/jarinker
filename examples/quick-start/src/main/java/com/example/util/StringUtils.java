package com.example.util;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;

public class StringUtils {

    public static List<String> split(String string, String separator) {
        return Lists.newArrayList(Strings.nullToEmpty(string).split(separator));
    }
}
