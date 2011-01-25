/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.canvas.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Array-like object holding the actual image data for an ImageData object. For
 * each pixel, this object contains a red, green, blue and alpha value between 0
 * and 255 (in this order). Note that we use ints here to represent the data to
 * avoid complexities stemming from bytes being signed in Java.
 *
 * @see <a href="http://www.w3.org/TR/2dcontext/#canvaspixelarray">HTML Canvas
 *      2D CanvasPixelArray</a>
 */
public class CanvasPixelArray extends JavaScriptObject {

  protected CanvasPixelArray() {
  }

  /**
   * Returns the data value at index i.
   *
   * @param i the data index
   * @return the data value
   */
  public final native int get(int i) /*-{
    return this[i] || 0;
  }-*/;

  /**
   * Returns the length of the array.
   *
   * @return the array length
   */
  public final native int getLength() /*-{
    return this.length;
  }-*/;

  /**
   * Sets the data value at position i to the given value.
   *
   * Most browsers will clamp this value to the range 0...255, but that is not
   * enforced in this implementation.
   *
   * @param i index to set.
   * @param value value to set (use values from 0 to 255)
   */
  public final native void set(int i, int value) /*-{
    // FF3.0 doesn't clamp the range. We don't manually clamp it to maximize 
    // performance.
    this[i] = value;
  }-*/;
}
