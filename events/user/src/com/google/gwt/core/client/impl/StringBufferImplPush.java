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
 * A {@link StringBufferImplArrayBase} that uses <code>push</code> for
 * appending strings. Some external benchmarks suggest this implementation, but
 * in practice our measurements indication that {@link StringBufferImplArray}
 * has a slight edge on every browser; the performance is often very close to
 * {@link StringBufferImplConcat}.
 */
public class StringBufferImplPush extends StringBufferImplArrayBase {

  @Override
  public native void append(Object a, boolean x) /*-{
    a.push(x);
  }-*/;

  @Override
  public native void append(Object a, double x) /*-{
    a.push(x);
  }-*/;

  @Override
  public native void append(Object a, float x) /*-{
    a.push(x);
  }-*/;

  @Override
  public native void append(Object a, int x) /*-{
    a.push(x);
  }-*/;

  @Override
  public native void appendNonNull(Object a, String x) /*-{
    a.push(x);
  }-*/;
}
