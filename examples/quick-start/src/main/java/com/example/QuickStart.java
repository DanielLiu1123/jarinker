package com.example;

import com.example.util.StringUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class QuickStart {

    public static void main(String[] args) {
        // Actually use Strings methods to create real dependencies
        String result = Strings.nullToEmpty(null);
        System.out.println("Strings result: " + result);

        // Actually use Lists methods to create real dependencies
        var list = Lists.newArrayList("a", "b", "c");
        System.out.println("Lists result: " + list);

        // Use StringUtils from different package
        var split = StringUtils.split("1,2,3", ",");
        System.out.println(split);
    }
}
