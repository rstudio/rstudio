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
}
