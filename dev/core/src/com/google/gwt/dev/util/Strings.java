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
package com.google.gwt.dev.util;

import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.ArrayList;

/**
 * String manipulation utilities.
 */
public class Strings {

  /**
   * Join strings inserting separator between them.
   */
  public static String join(String[] strings, String separator) {
    StringBuffer result = new StringBuffer();

    for (String s : strings) {
      if (result.length() != 0) {
        result.append(separator);
      }
      result.append(s);
    }

    return result.toString();
  }

  /**
   * @return the path components, result of splitting by "/".
   */
  public static String[] splitPath(String path) {
    ArrayList<String> result = Lists.newArrayList();
    int length = path.length();
    int begin = 0;
    for (int i = 0; i < length; i++) {
      if (path.charAt(i) == '/') {
        result.add(path.substring(begin, i));
        begin = i + 1;
      }
    }
    if (begin < length) {
      result.add(path.substring(begin));
    }
    return result.toArray(new String[result.size()]);
  }
}
