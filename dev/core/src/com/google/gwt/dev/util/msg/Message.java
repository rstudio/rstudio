// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.msg;

import com.google.gwt.core.ext.TreeLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Fast way to produce messages for the logger.
 * Use $N to specify the replacement argument.
 * Caveats:
 * - N must be a single digit (you shouldn't need more than 10 args, right?)
 * - '$' cannot be escaped
 * - each arg can only appear once
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

  /**
   * Creates a lazily-formatted message.
   */
  protected Message(TreeLogger.Type type, String fmt, int args) {
    assert (type != null);
    assert (fmt != null);
    assert (args >= 0);
    fType = type;

    fFmtParts = new char[args + 1][];
    fArgIndices = new int[args];
    int from = 0;
    for (int i = 0; i < args; ++i) {
      int to = fmt.indexOf('$', from);
      if (to != -1) {
        if (to < fmt.length() - 1) {
          char charDigit = fmt.charAt(to+1); 
          if (Character.isDigit(charDigit)) { 
            int digit = Character.digit(charDigit, 10);
            fFmtParts[i] = fmt.substring(from, to).toCharArray();
            fArgIndices[i] = digit;
            from = to + 2;
            continue;
          }
        }
      }
      throw new IllegalArgumentException("Expected arg $" + i);
    }
    fFmtParts[args] = fmt.substring(from).toCharArray();

    int minChars = 0;
    for (int i = 0, n = fFmtParts.length; i < n; ++i)
      minChars += fFmtParts[i].length;
    fMinChars = minChars;
  }

  protected final Formatter getFormatter(Class c) {
    return FMT_CLASS;
  }

  protected final Formatter getFormatter(File f) {
    return FMT_FILE;
  }

  protected final Formatter getFormatter(URL u) {
    return FMT_URL;
  }

  protected final Formatter getFormatter(Integer i) {
    return FMT_INTEGER;
  }

  protected Formatter getFormatter(Long l) {
    return FMT_LONG;
  }

  protected final Formatter getFormatter(Method m) {
    return FMT_METHOD;
  }

  protected final Formatter getFormatter(String s) {
    return FMT_STRING;
  }

  protected final Formatter getToStringFormatter() {
    return FMT_TOSTRING;
  }

  protected final Formatter getFormatter(String[] sa) {
    return FMT_STRING_ARRAY;
  }

  protected final TreeLogger.Type fType;
  protected final char[][] fFmtParts;
  protected final int[] fArgIndices;
  protected final int fMinChars;
}
