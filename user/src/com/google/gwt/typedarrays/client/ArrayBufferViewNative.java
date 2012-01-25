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
import com.google.gwt.typedarrays.shared.ArrayBufferView;

/**
 * Base class for JS implementation of various views.
 */
public class ArrayBufferViewNative extends JavaScriptObject implements ArrayBufferView {

  protected ArrayBufferViewNative() {
  }

  @Override
  public final native ArrayBuffer buffer() /*-{
    return this.buffer;
  }-*/;

  @Override
  public final native int byteLength() /*-{
    return this.byteLength;
  }-*/;

  @Override
  public final native int byteOffset() /*-{
    return this.byteOffset;
  }-*/;
}
