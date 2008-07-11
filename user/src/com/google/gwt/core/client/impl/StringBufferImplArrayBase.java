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
 * Superclass for all array-based string builder implementations.
 */
public abstract class StringBufferImplArrayBase extends StringBufferImpl {

  @Override
  public native void append(Object a, boolean x) /*-{
    a[a.explicitLength++] = x;
  }-*/;

  @Override
  public native void append(Object a, double x) /*-{
    a[a.explicitLength++] = x;
  }-*/;

  @Override
  public native void append(Object a, float x) /*-{
    a[a.explicitLength++] = x;
  }-*/;

  @Override
  public native void append(Object a, int x) /*-{
    a[a.explicitLength++] = x;
  }-*/;

  @Override
  public final void append(Object a, Object x) {
    appendNonNull(a, "" + x);
  }

  @Override
  public void append(Object a, String x) {
    appendNonNull(a, (x == null) ? "null" : x);
  }

  @Override
  public native void appendNonNull(Object a, String x) /*-{
    a[a.explicitLength++] = x;
  }-*/;

  @Override
  public final native Object createData() /*-{
    var array = [];
    array.explicitLength = 0;
    return array;
  }-*/;

  @Override
  public int length(Object a) {
    return toString(a).length();
  }

  @Override
  public final void replace(Object a, int start, int end, String toInsert) {
    String s = takeString(a);
    appendNonNull(a, s.substring(0, start));
    append(a, toInsert);
    appendNonNull(a, s.substring(end));
  }

  @Override
  public final String toString(Object a) {
    String s = takeString(a);
    appendNonNull(a, s);
    return s;
  }

  protected native String takeString(Object a) /*-{
    var s = a.join('');
    a.length = a.explicitLength = 0;
    return s;
  }-*/;
}
