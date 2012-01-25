/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.typedarrays.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.typedarrays.shared.ArrayBuffer;

/**
 * JS native implementation of {@link ArrayBuffer}.
 */
public final class ArrayBufferNative extends JavaScriptObject implements ArrayBuffer {

  /**
   * @param length
   * @return an {@link ArrayBuffer} instance
   */
  public static native ArrayBufferNative create(int length) /*-{
    return new ArrayBuffer(length);
  }-*/;

  protected ArrayBufferNative() {
  }

  @Override
  public native int byteLength() /*-{
    return this.byteLength;
  }-*/;
}
