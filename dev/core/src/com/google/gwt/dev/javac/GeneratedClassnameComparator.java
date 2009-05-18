/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.javac;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator to sort the compiler-generated classNames so that they can be
 * correlated. Examples of sorting orders by the comparator:
 * 
 * <pre>
 * <ul>
 * <li> Foo$1 < Foo$2 < Foo$3 < ... < Foo$10
 * <li> Foo$1 < Foo$2 < Foo$1$1 < Foo$1$2 < Foo$2$1 < Foo$2$2 < Foo$2$Baz
 * </pre>
 */
class GeneratedClassnameComparator implements Comparator<String>, Serializable {

  public int compare(String arg0, String arg1) {
    String pattern = "\\$";
    String splits0[] = arg0.split(pattern);
    String splits1[] = arg1.split(pattern);
    if (splits0.length != splits1.length) {
      return splits0.length - splits1.length;
    }
    for (int i = 0; i < splits0.length; i++) {
      int answer = compareWithoutDollars(splits0[i], splits1[i]);
      if (answer != 0) {
        return answer;
      }
    }
    return 0;
  }

  /*
   * 3 cases: (i) both can be converted to integer: compare the integral value.
   * (ii) only one can be converted to integer: the one with integral value is
   * lower. (iii) none can be converted to integer: compare the strings.
   */
  private int compareWithoutDollars(String arg0, String arg1) {
    boolean arg0IsInt = false;
    boolean arg1IsInt = false;
    int int0 = 0, int1 = 0;
    if ((arg0 == null) != (arg1 == null)) {
      return (arg0 == null) ? -1 : 1;
    }
    if (arg0 == null) {
      return 0;
    }

    if (arg0.charAt(0) != '-') {
      try {
        int0 = Integer.parseInt(arg0);
        arg0IsInt = true;
      } catch (NumberFormatException ex) {
        // ignored
      }
    }

    if (arg1.charAt(0) != '-') {
      try {
        int1 = Integer.parseInt(arg1);
        arg1IsInt = true;
      } catch (NumberFormatException ex) {
        // ignored
      }
    }

    if (arg0IsInt != arg1IsInt) {
      return arg0IsInt ? -1 : 1;
    }

    // now either both are int or both are Strings
    if (arg0IsInt) {
      return int0 - int1;
    }
    return arg0.compareTo(arg1);
  }
}
