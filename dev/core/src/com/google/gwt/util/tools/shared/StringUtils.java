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
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

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
   * Generate JavaScript code that evaluates to the supplied string. Adapted
   * from {@link com.google.gwt.dev.js.rhino.ScriptRuntime#escapeString(String)}
   * . The difference is that we quote with either &quot; or &apos; depending on
   * which one is used less inside the string.
   */
  public static String javaScriptString(String value) {
    char[] chars = value.toCharArray();
    final int n = chars.length;
    int quoteCount = 0;
    int aposCount = 0;
    for (int i = 0; i < n; ++i) {
      switch (chars[i]) {
        case '"':
          ++quoteCount;
          break;
        case '\'':
          ++aposCount;
          break;
      }
    }

    StringBuffer result = new StringBuffer(value.length() + 16);

    char quoteChar = (quoteCount < aposCount) ? '"' : '\'';
    result.append(quoteChar);

    for (int i = 0; i < n; ++i) {
      char c = chars[i];

      if (' ' <= c && c <= '~' && c != quoteChar && c != '\\') {
        // an ordinary print character (like C isprint())
        result.append(c);
        continue;
      }

      int escape = -1;
      switch (c) {
        case '\b':
          escape = 'b';
          break;
        case '\f':
          escape = 'f';
          break;
        case '\n':
          escape = 'n';
          break;
        case '\r':
          escape = 'r';
          break;
        case '\t':
          escape = 't';
          break;
        case '"':
          escape = '"';
          break; // only reach here if == quoteChar
        case '\'':
          escape = '\'';
          break; // only reach here if == quoteChar
        case '\\':
          escape = '\\';
          break;
      }

      if (escape >= 0) {
        // an \escaped sort of character
        result.append('\\');
        result.append((char) escape);
      } else {
        /*
         * Emit characters from 0 to 31 that don't have a single character
         * escape sequence in octal where possible. This saves one or two
         * characters compared to the hexadecimal format '\xXX'.
         *
         * These short octal sequences may only be used at the end of the string
         * or where the following character is a non-digit. Otherwise, the
         * following character would be incorrectly interpreted as belonging to
         * the sequence.
         */
        if (c < ' ' && (i == n - 1 || chars[i + 1] < '0' || chars[i + 1] > '9')) {
          result.append('\\');
          if (c > 0x7) {
            result.append((char) ('0' + (0x7 & (c >> 3))));
          }
          result.append((char) ('0' + (0x7 & c)));
        } else {
          int hexSize;
          if (c < 256) {
            // 2-digit hex
            result.append("\\x");
            hexSize = 2;
          } else {
            // Unicode.
            result.append("\\u");
            hexSize = 4;
          }
          // append hexadecimal form of ch left-padded with 0
          for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
            int digit = 0xf & (c >> shift);
            result.append(HEX_CHARS[digit]);
          }
        }
      }
    }
    result.append(quoteChar);
    StringUtils.escapeClosingTags(result);
    String resultString = result.toString();
    return resultString;
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

  /**
   * Escapes any closing XML tags embedded in <code>str</code>, which could
   * potentially cause a parse failure in a browser, for example, embedding a
   * closing <code>&lt;script&gt;</code> tag.
   *
   * @param str an unescaped literal; May be null
   */
  private static void escapeClosingTags(StringBuffer str) {
    if (str == null) {
      return;
    }

    int index = 0;

    while ((index = str.indexOf("</", index)) != -1) {
      str.insert(index + 1, '\\');
    }
  }
}
