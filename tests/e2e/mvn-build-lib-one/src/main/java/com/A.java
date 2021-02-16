package com;

import com.google.common.base.Strings;

public class A {

    public void foo() {
        System.out.println(Strings.padStart("xxx", 10, '0'));
    }
}