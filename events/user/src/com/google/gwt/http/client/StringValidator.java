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
package com.google.gwt.http.client;

/**
 * Utility class for validating strings.
 * 
 * TODO(mmendez): Is there a better place for this?
 */
final class StringValidator {
  /**
   * Returns true if the string is empty or null.
   * 
   * @param string to test if null or empty
   * 
   * @return true if the string is empty or null
   */
  public static boolean isEmptyOrNullString(String string) {
    return (string == null) || (0 == string.trim().length());
  }

  /**
   * Throws if <code>value</code> is <code>null</code> or empty. This method
   * ignores leading and trailing whitespace.
   * 
   * @param name the name of the value, used in error messages
   * @param value the string value that needs to be validated
   * 
   * @throws IllegalArgumentException if the string is empty, or all whitespace
   * @throws NullPointerException if the string is <code>null</code>
   */
  public static void throwIfEmptyOrNull(String name, String value) {
    assert (name != null);
    assert (name.trim().length() != 0);

    throwIfNull(name, value);

    if (0 == value.trim().length()) {
      throw new IllegalArgumentException(name + " cannot be empty");
    }
  }

  /**
   * Throws a {@link NullPointerException} if the value is <code>null</code>.
   * 
   * @param name the name of the value, used in error messages
   * @param value the value that needs to be validated
   * 
   * @throws NullPointerException if the value is <code>null</code>
   */
  public static void throwIfNull(String name, Object value) {
    if (null == value) {
      throw new NullPointerException(name + " cannot be null");
    }
  }

  private StringValidator() {
  }
}
