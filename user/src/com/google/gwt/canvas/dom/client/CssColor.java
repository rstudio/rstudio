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

/**
 * CSS Color object.
 * 
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * 
 * <p>
 * To handle devmode we must wrap JSO strings in an array. Therefore, when in devmode, CssColor is
 * actually an array with one element that is the JSO. In webmode, this is not needed.
 * </p>
 * 
 * @see <a href="http://www.w3.org/TR/CSS1/#color">Cascading Style Sheets, level 1</a>
 */
public class CssColor extends FillStrokeStyle {

  /**
   * Sets the RGB color value.
   * 
   * @param r red, int between 0 and 255
   * @param g green, int between 0 and 255
   * @param b blue, int between 0 and 255
   */
  public static final CssColor make(int r, int g, int b) {
    return make("rgb(" + r + "," + g + "," + b + ")");
  }

  /**
   * Creates a CssColor object.
   * 
   * Examples: blue, #ff0000, #f00, rgb(255,0,0)
   * 
   * @param cssColor the CSS color
   */
  public static final native CssColor make(String cssColor) /*-{
    // Due to needing to wrap CssColor when in devmode, we check whether the we are in devmode and
    // wrap the String in an array if necessary.
    return @com.google.gwt.core.client.GWT::isScript()() ? cssColor : [cssColor];
  }-*/;

  protected CssColor() {
  }

  /**
   * Returns the value of the CssColor, as a String.
   * 
   * @return the value of the color, as a String.
   */
  public final native String value() /*-{
    // Due to needing to wrap CssColor when in devmode, we check whether the we are in devmode and
    // unwrap the String if necessary.
    return @com.google.gwt.core.client.GWT::isScript()() ? this : this[0];
  }-*/;
}
