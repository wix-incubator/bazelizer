package com.foo;

import com.google.common.base.Strings;

public class B {
    static final String ID = "B" + Strings.repeat("0", 20);

    static void run() {
        System.out.println(ID + "" + com.foo.A.ID);
    }
}