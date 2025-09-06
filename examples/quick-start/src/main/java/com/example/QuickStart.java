package com.example;

import com.example.util.StringUtils;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class QuickStart {

    public static void main(String[] args) {
        System.out.println(Strings.class);
        System.out.println(Lists.class);

        // Use StringUtils from different package
        System.out.println(StringUtils.split("1,2,3", ","));
    }
}
