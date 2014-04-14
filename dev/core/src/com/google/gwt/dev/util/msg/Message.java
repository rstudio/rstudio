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
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Fast way to produce messages for the logger. Use $N to specify the
 * replacement argument. Caveats: - N must be a single digit (you shouldn't need
 * more than 10 args, right?) - '$' cannot be escaped - each arg can only appear
 * once
 */
public abstract class Message {

  private static final Formatter FMT_TOSTRING = new FormatterToString();
  private static final Formatter FMT_CLASS = new FormatterForClass();
  private static final Formatter FMT_FILE = new FormatterForFile();
  private static final Formatter FMT_URL = new FormatterForURL();
  private static final Formatter FMT_INTEGER = new FormatterForInteger();
  private static final Formatter FMT_LONG = new FormatterForLong();
  private static final Formatter FMT_METHOD = new FormatterForMethod();
  private static final Formatter FMT_STRING = new FormatterForString();
  private static final Formatter FMT_STRING_ARRAY = new FormatterForStringArray();

  protected final TreeLogger.Type type;

  protected final char[][] fmtParts;

  protected final int[] argIndices;

  protected final int minChars;

  /**
   * Creates a lazily-formatted message.
   */
  protected Message(TreeLogger.Type type, String fmt, int args) {
    assert (type != null);
    assert (fmt != null);
    assert (args >= 0);
    this.type = type;

    fmtParts = new char[args + 1][];
    argIndices = new int[args];
    int from = 0;
    for (int i = 0; i < args; ++i) {
      int to = fmt.indexOf('$', from);
      if (to != -1) {
        if (to < fmt.length() - 1) {
          char charDigit = fmt.charAt(to + 1);
          if (Character.isDigit(charDigit)) {
            int digit = Character.digit(charDigit, 10);
            fmtParts[i] = fmt.substring(from, to).toCharArray();
            argIndices[i] = digit;
            from = to + 2;
            continue;
          }
        }
      }
      throw new IllegalArgumentException("Expected arg $" + i);
    }
    fmtParts[args] = fmt.substring(from).toCharArray();

    int minNumChars = 0;
    for (int i = 0, n = fmtParts.length; i < n; ++i) {
      minNumChars += fmtParts[i].length;
    }
    this.minChars = minNumChars;
  }

  /**
   * @param c a Class
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(Class<?> c) {
    return FMT_CLASS;
  }

  /**
   * @param f a File
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(File f) {
    return FMT_FILE;
  }

  /**
   * @param i an Integer
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(Integer i) {
    return FMT_INTEGER;
  }

  /**
   * @param l a Long
   * @return a suitable Formatter
   */
  protected Formatter getFormatter(Long l) {
    return FMT_LONG;
  }

  /**
   * @param m a Method
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(Method m) {
    return FMT_METHOD;
  }

  /**
   * @param s a String
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(String s) {
    return FMT_STRING;
  }

  /**
   * @param sa a String array
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(String[] sa) {
    return FMT_STRING_ARRAY;
  }

  /**
   * @param u a URL
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(URL u) {
    return FMT_URL;
  }

  protected final Formatter getToStringFormatter() {
    return FMT_TOSTRING;
  }
}
