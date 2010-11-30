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
 * Represents a CssColor, a CanvasGradient, or a CanvasPattern that is used for stroke and fill.
 *
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 */
public class FillStrokeStyle extends JavaScriptObject {
  public static final int TYPE_CSSCOLOR = 0, TYPE_GRADIENT = 1, TYPE_PATTERN = 2;

  protected FillStrokeStyle() { }

  /**
   * Returns the type of this FillStrokeStyle as an integer.
   * 
   * @return The type of the object.
   */
  public final native int getType() /*-{
    // Due to needing to wrap the contained CssColor when in devmode, we must unwrap the color to
    // check its type when in devmode (when isScript == false.) @see CssColor
    var unwrappedColor = @com.google.gwt.core.client.GWT::isScript()() ? this : this[0];
    if (unwrappedColor && typeof(unwrappedColor) == 'string') {
      return @com.google.gwt.canvas.dom.client.FillStrokeStyle::TYPE_CSSCOLOR;
    } else if (typeof(this.addColorStop) == 'function') {
      return @com.google.gwt.canvas.dom.client.FillStrokeStyle::TYPE_GRADIENT;
    } else {
      return @com.google.gwt.canvas.dom.client.FillStrokeStyle::TYPE_PATTERN;
    }
  }-*/;
}
