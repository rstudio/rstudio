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

/**
 * Fast way to produce messages for the logger. Use $N to specify the replacement argument.
 * <p>
 * Caveats:
 * <ul>
 * <li>N must be a single digit (you shouldn't need more than 10 args, right?)
 * <li>'$' cannot be escaped
 * <li>each arg can only appear once
 * </ul>
 */
public abstract class Message {

  private static final Formatter FMT_TOSTRING = new FormatterToString();
  private static final Formatter FMT_CLASS = new FormatterForClass();
  private static final Formatter FMT_INTEGER = new FormatterForInteger();
  private static final Formatter FMT_STRING = new FormatterForString();

  protected final TreeLogger.Type type;

  protected final char[][] fmtParts;

  protected final int[] argIndices;

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
      if (to != -1 && to < fmt.length() - 1) {
        char charDigit = fmt.charAt(to + 1);
        if (Character.isDigit(charDigit)) {
          int digit = Character.digit(charDigit, 10);
          fmtParts[i] = fmt.substring(from, to).toCharArray();
          argIndices[i] = digit;
          from = to + 2;
          continue;
        }
      }
      throw new IllegalArgumentException("Expected arg $" + i);
    }
    fmtParts[args] = fmt.substring(from).toCharArray();
  }

  /**
   * @param c a Class
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(Class<?> c) {
    return FMT_CLASS;
  }

  /**
   * @param i an Integer
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(Integer i) {
    return FMT_INTEGER;
  }

  /**
   * @param s a String
   * @return a suitable Formatter
   */
  protected final Formatter getFormatter(String s) {
    return FMT_STRING;
  }

  protected final Formatter getToStringFormatter() {
    return FMT_TOSTRING;
  }

  protected TreeLogger branch(TreeLogger logger, Throwable caught) {
    return branch(logger, toArray(), new Formatter[0], caught);
  }

  protected TreeLogger branch(TreeLogger logger, Object arg, Formatter fmt, Throwable caught) {
    return branch(logger, toArray(arg), toArray(fmt), caught);
  }

  protected TreeLogger branch(TreeLogger logger, Object[] args, Object[] fmts,
      Throwable caught) {
    return logger.branch(type, compose(args, fmts), caught);
  }

  protected String compose(Object[] args, Object[] fmts) {
    // Format the objects.

    assert args.length == fmts.length;
    String[] formattedArgs = new String[args.length];
    for (int i = 0; i < argIndices.length; i++) {
      int referredIndex = argIndices[i];
      Object referredArg = args[referredIndex];
      Formatter formatter = (Formatter) fmts[referredIndex];
      formattedArgs[i] = (referredArg != null ? formatter.format(referredArg) : "null");
    }

    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < formattedArgs.length; i++) {
      stringBuilder.append(fmtParts[i]);
      stringBuilder.append(formattedArgs[i]);
    }
    // final literal
    stringBuilder.append(fmtParts[fmtParts.length - 1]);

    return stringBuilder.toString();
  }

  protected void log(TreeLogger logger, Throwable caught) {
    log(logger, toArray(), toArray(), caught);
  }

  protected void log(TreeLogger logger, Object arg, Formatter fmt, Throwable caught) {
    log(logger, toArray(arg), toArray(fmt), caught);
  }

  protected void log(TreeLogger logger, Object arg1, Object arg2, Formatter fmt1, Formatter fmt2,
      Throwable caught) {
    log(logger, toArray(arg1, arg2), toArray(fmt1, fmt2), caught);
  }

  protected void log(TreeLogger logger, Object arg1, Object arg2, Object arg3, Formatter fmt1,
      Formatter fmt2, Formatter fmt3, Throwable caught) {
    log(logger, toArray(arg1, arg2, arg3), toArray(fmt1, fmt2, fmt3), caught);
  }

  protected void log(TreeLogger logger, Object arg1, Object arg2, Object arg3, Object arg4,
      Formatter fmt1, Formatter fmt2, Formatter fmt3, Formatter fmt4, Throwable caught) {
    log(logger, toArray(arg1, arg2, arg3, arg4), toArray(fmt1, fmt2, fmt3, fmt4), caught);
  }

  protected void log(TreeLogger logger, Object[] args, Object[] fmts, Throwable caught) {
    if (logger.isLoggable(type)) {
      logger.log(type, compose(args, fmts), caught);
    }
  }

  private Object[] toArray(Object... pars) {
    return pars;
  }
}
