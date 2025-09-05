package com.example.quickstart;

/**
 * Helper class to demonstrate internal dependencies.
 */
public class Helper {
    
    public void doSomething() {
        System.out.println("Helper is doing something...");
        
        var util = new Util();
        util.performTask();
    }
}
