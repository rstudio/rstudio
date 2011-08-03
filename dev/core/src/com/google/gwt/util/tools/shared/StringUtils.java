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
package com.google.gwt.util.tools.shared;

/**
 * String utility methods.
 */
public class StringUtils {

  public static char[] HEX_CHARS = new char[] {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
    'E', 'F'};

  /**
   * A 4-digit hex result.
   */
  public static void hex4(char c, StringBuffer sb) {
    sb.append(HEX_CHARS[(c & 0xF000) >> 12]);
    sb.append(HEX_CHARS[(c & 0x0F00) >> 8]);
    sb.append(HEX_CHARS[(c & 0x00F0) >> 4]);
    sb.append(HEX_CHARS[c & 0x000F]);
  }

  /**
   * Returns a string representation of the byte array as a series of
   * hexadecimal characters.
   *
   * @param bytes byte array to convert
   * @return a string representation of the byte array as a series of
   *         hexadecimal characters
   */
  public static String toHexString(byte[] bytes) {
    char[] hexString = new char[2 * bytes.length];
    int j = 0;
    for (int i = 0; i < bytes.length; i++) {
      hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
      hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
    }
    return new String(hexString);
  }
}
