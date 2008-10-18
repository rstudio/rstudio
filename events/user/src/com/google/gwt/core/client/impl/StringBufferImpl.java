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
package com.google.gwt.core.client.impl;

/**
 * <p>
 * The interface to defer bound implementations of {@link StringBuilder} and
 * {@link StringBuffer}.
 * </p>
 * 
 * <p>
 * All of the implementations have been carefully tweaked to get the most
 * inlining possible, so be sure to check with
 * {@link com.google.gwt.emultest.java.lang.StringBuilderBenchmark StringBuilderBenchmark}
 * whenever these classes are modified.
 * </p>
 */
public abstract class StringBufferImpl {

  /**
   * Append for primitive; the value can be stored and only later converted to a
   * string.
   */
  public abstract void append(Object data, boolean x);

  /**
   * Append for primitive; the value can be stored and only later converted to a
   * string.
   */
  public abstract void append(Object data, double x);

  /**
   * Append for primitive; the value can be stored and only later converted to a
   * string.
   */
  public abstract void append(Object data, float x);

  /**
   * Append for primitive; the value can be stored and only later converted to a
   * string.
   */
  public abstract void append(Object data, int x);

  /**
   * Append for object. It is important to immediately convert the object to a
   * string, because the conversion can give different results if it is
   * deferred.
   */
  public abstract void append(Object data, Object x);

  /**
   * Append for a possibly null string object.
   */
  public abstract void append(Object data, String x);

  /**
   * Append for a string that is definitely not null.
   */
  public abstract void appendNonNull(Object data, String x);

  /**
   * Returns a data holder object for use with subsequent calls.
   */
  public abstract Object createData();

  /**
   * Returns the current length of the string buffer.
   */
  public abstract int length(Object data);

  /**
   * Replaces a segment of the string buffer.
   */
  public abstract void replace(Object data, int start, int end, String toInsert);

  /**
   * Returns the string buffer as a String.
   */
  public abstract String toString(Object data);
}
