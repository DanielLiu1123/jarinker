package com.example.quickstart;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple example application for testing Jarinker.
 * Uses some external dependencies that can be analyzed and potentially shrunk.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== Jarinker Quickstart Example ===");
        
        // Use Guava
        String padded = Strings.padEnd("Hello", 10, '!');
        System.out.println("Guava result: " + padded);
        
        // Use Apache Commons Lang
        String reversed = StringUtils.reverse("World");
        System.out.println("Commons Lang result: " + reversed);
        
        // Use some internal methods
        var helper = new Helper();
        helper.doSomething();
        
        System.out.println("Application completed successfully!");
    }
}
