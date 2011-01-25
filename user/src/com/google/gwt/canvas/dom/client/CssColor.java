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
 * To handle dev mode we must wrap JSO strings in an array. Therefore, when in
 * dev mode, CssColor is actually an array with one element that is the JSO. In
 * Production Mode, this is not needed.
 * </p>
 *
 * @see <a href="http://www.w3.org/TR/CSS1/#color">Cascading Style Sheets, level
 *      1</a>
 */
public class CssColor extends FillStrokeStyle {

  /**
   * Sets the RGB color value.
   * 
   * @param r red, integer between 0 and 255
   * @param g green, integer between 0 and 255
   * @param b blue, integer between 0 and 255
   * @return a {@link CssColor} object
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
   * @return a {@link CssColor} object
   */
  public static final native CssColor make(String cssColor) /*-{
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
    return @com.google.gwt.core.client.GWT::isScript()() ? this : this[0];
  }-*/;
}
