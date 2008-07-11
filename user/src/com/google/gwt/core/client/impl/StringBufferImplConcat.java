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
 * A {@link StringBufferImplArrayBase} that does a concat for toString(). The
 * performance of this implementation is generally slightly worse than
 * {@link StringBufferImplArray} for everything except IE, where it's terrible.
 */
public class StringBufferImplConcat extends StringBufferImplArrayBase {
  /**
   * We don't need to do the null check because concat does it automagically.
   */
  @Override
  public native void append(Object a, String x) /*-{
    a[a.explicitLength++] = x;
  }-*/;

  @Override
  protected native String takeString(Object a) /*-{
    var s = String.prototype.concat.apply('', a);
    a.length = a.explicitLength = 0;
    return s;
  }-*/;
}
