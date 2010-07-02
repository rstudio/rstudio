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
package com.google.gwt.user.client.ui;

/**
 * Characteristic interface which indicates that a widget has an associated
 * vertical alignment.
 * 
 * <h3>Use in UiBinder Templates</h3>
 * 
 * <p>
 * The names of the static members of {@link VerticalAlignmentConstant}, as well
 * as simple alignment names (<code>top</code>, <code>middle</code>,
 * <code>bottom</code>), can be used as values for a
 * <code>verticalAlignment</code> attribute of any widget that implements this
 * interface. (In fact, this will work for any widget method that takes a single
 * VerticalAlignmentConstant value.)
 * <p>
 * For example,
 * 
 * <pre>
 * &lt;g:VerticalPanel verticalAlignment='ALIGN_BOTTOM' />
 * &lt;g:VerticalPanel verticalAlignment='bottom' />
 * </pre>
 */
public interface HasVerticalAlignment {

  /**
   * Horizontal alignment constant.
   */
  public static class VerticalAlignmentConstant {
    private final String verticalAlignString;

    private VerticalAlignmentConstant(String verticalAlignString) {
      this.verticalAlignString = verticalAlignString;
    }

    /**
     * Gets the CSS 'vertical-align' string associated with this constant.
     * 
     * @return the CSS 'vertical-align' value
     */
    public String getVerticalAlignString() {
      return verticalAlignString;
    }
  }

  /**
   * Specifies that the widget's contents should be aligned to the bottom.
   */
  VerticalAlignmentConstant ALIGN_BOTTOM = new VerticalAlignmentConstant(
      "bottom");

  /**
   * Specifies that the widget's contents should be aligned in the middle.
   */
  VerticalAlignmentConstant ALIGN_MIDDLE = new VerticalAlignmentConstant(
      "middle");

  /**
   * Specifies that the widget's contents should be aligned to the top.
   */
  VerticalAlignmentConstant ALIGN_TOP = new VerticalAlignmentConstant("top");

  /**
   * Gets the vertical alignment.
   * 
   * @return the current vertical alignment.
   */
  VerticalAlignmentConstant getVerticalAlignment();

  /**
   * Sets the vertical alignment.
   * 
   * @param align the vertical alignment (
   *          {@link HasVerticalAlignment#ALIGN_TOP},
   *          {@link HasVerticalAlignment#ALIGN_MIDDLE}, or
   *          {@link HasVerticalAlignment#ALIGN_BOTTOM}).
   */
  void setVerticalAlignment(VerticalAlignmentConstant align);
}
