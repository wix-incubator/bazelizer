package com.foo;

import java.util.*;

public class C {
    static final String ID;
    static {
        // com.google.common.base.Strings.lenientFormat
        // since 25.1
        ID = com.google.common.base.Strings.lenientFormat("C-id-%s", 1000);
    }
}