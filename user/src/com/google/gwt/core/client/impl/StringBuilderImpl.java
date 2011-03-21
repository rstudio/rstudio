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
 * An implementation for a {@link StringBuilder} or {@link StringBuffer}. This
 * class holds a default implementation based on an array of strings and the
 * JavaScript join function. Deferred bindings can substitute a subclass
 * optimized for a particular browser.
 * </p>
 * 
 * <p>
 * The main implementations are static classes nested within this one. All of
 * the implementations have been carefully tweaked to get the most inlining
 * possible, so be sure to check with
 * {@link com.google.gwt.emultest.java.lang.StringBuilderBenchmark StringBuilderBenchmark}
 * whenever these classes are modified.
 * </p>
 */
public class StringBuilderImpl {

  /**
   * A {@link StringBuilderImpl} that uses an array and an explicit length for
   * appending strings. Note that the length of the array is stored as a
   * property of the underlying JavaScriptObject. Making it a field of
   * {@link ImplArray} causes difficulty with inlining.
   */

  public static class ImplArray extends StringBuilderImpl {
    private  static native void setArrayLength(String[] array, int length) /*-{
      array.explicitLength = length;
    }-*/;

    private String[] array = new String[0];

    public ImplArray() {
      setArrayLength(array, 0);
    }

    @Override
    public native void append(String s) /*-{
      var a = this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplArray::array;
      a[a.explicitLength++] = s==null ? "null" : s;
    }-*/;

    @Override
    public int length() {
      return toString().length();
    }

    @Override
    public void replace(int start, int end, String toInsert) {
      String s = toString();
      array = new String[] {s.substring(0, start), toInsert, s.substring(end)};
    }

    @Override
    public native String toString() /*-{
      this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplArray::array = 
        [ this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplArray::array.join('') ];
      this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplArray::array.explicitLength = 1;
      return this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplArray::array[0];
    }-*/;
  }

  /**
   * A {@link StringBuilderImpl} that uses <code>push</code> for appending
   * strings.
   */
  public static class ImplPush extends StringBuilderImpl {
    private String[] array = new String[0];

    @Override
    public native void append(String s) /*-{
      this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplPush::array.push(s == null ? "null" : s);
    }-*/;

    @Override
    public int length() {
      return toString().length();
    }

    @Override
    public void replace(int start, int end, String toInsert) {
      String s = toString();
      array = new String[] {s.substring(0, start), toInsert, s.substring(end)};
    }

    @Override
    public native String toString() /*-{
      this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplPush::array = 
        [ this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplPush::array.join('') ];
      return this.@com.google.gwt.core.client.impl.StringBuilderImpl.ImplPush::array[0];
    }-*/;
  }

  /**
   * A {@link StringBuilderImpl} that uses += for appending strings.
   */
  public static class ImplStringAppend extends StringBuilderImpl {
    private String string = "";

    @Override
    public void append(String s) {
      string += s;
    }

    @Override
    public int length() {
      return string.length();
    }

    @Override
    public void replace(int start, int end, String toInsert) {
      string = string.substring(0, start) + toInsert + string.substring(end);
    }
    
    @Override
    public String toString() {
      return string;
    }
  }

  private static native String join(String[] stringArray) /*-{
    return stringArray.join('');
  }-*/;

  private static native String setLength(String[] stringArray, int length) /*-{
    stringArray.length = length;
  }-*/;

  private int arrayLen = 0;
  private String[] stringArray = new String[0];
  private int stringLength = 0;

  public void append(String toAppend) {
    // Coerce to "null" if null.
    if (toAppend == null) {
      toAppend = "null";
    }
    int appendLength = toAppend.length();
    if (appendLength > 0) {
      stringArray[arrayLen++] = toAppend;
      stringLength += appendLength;
      /*
       * If we hit 1k elements, let's do a join to reduce the array size. This
       * number was arrived at experimentally through benchmarking.
       */
      if (arrayLen > 1024) {
        toString();
        // Preallocate the next 1024 (faster on FF).
        setLength(stringArray, 1024);
      }
    }
  }

  public int length() {
    return stringLength;
  }

  public void replace(int start, int end, String toInsert) {
    // Get the joined string.
    String s = toString();

    // Build a new buffer in pieces (will throw exceptions).
    stringArray = new String[] {
        s.substring(0, start), toInsert, s.substring(end)};
    arrayLen = 3;

    // Calculate the new string length.
    stringLength += toInsert.length() - (end - start);
  }

  @Override
  public String toString() {
    /*
     * Normalize the array to exactly one element (even if it's completely
     * empty), so we can unconditionally grab the first element.
     */
    if (arrayLen != 1) {
      setLength(stringArray, arrayLen);
      String s = join(stringArray);
      // Create a new array to allow everything to get GC'd.
      stringArray = new String[] {s};
      arrayLen = 1;
    }
    return stringArray[0];
  }
}
