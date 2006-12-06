/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util;

import java.util.Comparator;

/**
 * Performs a case-sensitive comparision of char arrays.
 */
public class CharArrayComparator implements Comparator {
    
    public static final CharArrayComparator INSTANCE = new CharArrayComparator();

    public int compare(Object o1, Object o2) {
        char[] a = (char[])o1;
        char[] b = (char[])o2;
        
        int ai = 0;
        int bi = 0;

        for (; ai < a.length && bi < b.length; ++ai, ++bi) {
            int c = a[ai] - b[bi];
            if (c != 0) {
                return c;
            }
        }

        if (ai == a.length && bi < b.length) {
            // a is shorter
            return -1;
        }

        if (ai < a.length && bi == b.length) {
            // b is shorter
            return 1;
        }
        
        // they are equal
        //
        return 0;
    }

}
