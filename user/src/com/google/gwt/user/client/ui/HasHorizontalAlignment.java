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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;

/**
 * Characteristic interface which indicates that a widget can be aligned
 * horizontally.
 * 
 * <h3>Use in UiBinder Templates</h3>
 * 
 * <p>
 * The names of the static members of {@link HorizontalAlignmentConstant}
 * can be used as values for a <code>horizontalAlignment</code> attribute
 * of any widget that implements this interface. (In fact, this will work 
 * for any widget method that takes a single  HorizontalAlignmentConstant value.)
 * <p>
 * For example,<pre>
 * &lt;g:Label horizontalAlignment='ALIGN_RIGHT'>Hi there.&lt;/g:Label>
 * </pre>
 */
public interface HasHorizontalAlignment {

  /**
   * Horizontal alignment constant.
   */
  public static class HorizontalAlignmentConstant {
    private String textAlignString;

    private HorizontalAlignmentConstant(String textAlignString) {
      this.textAlignString = textAlignString;
    }

    /**
     * Gets the CSS 'text-align' string associated with this constant.
     * 
     * @return the CSS 'text-align' value
     */
    public String getTextAlignString() {
      return textAlignString;
    }
  }

  /**
   * Specifies that the widget's contents should be aligned in the center.
   */
  HorizontalAlignmentConstant ALIGN_CENTER = new HorizontalAlignmentConstant(
      "center");

  /**
   * Specifies that the widget's contents should be aligned to the left.
   */
  HorizontalAlignmentConstant ALIGN_LEFT = new HorizontalAlignmentConstant(
      "left");

  /**
   * Specifies that the widget's contents should be aligned to the right.
   */
  HorizontalAlignmentConstant ALIGN_RIGHT = new HorizontalAlignmentConstant(
      "right");

  /**
   * In a RTL layout, specifies that the widget's contents should be aligned
   * to the right. In a LTR layout, specifies that the widget's constants 
   * should be aligned to the left.
   */
  HorizontalAlignmentConstant ALIGN_DEFAULT = 
      (GWT.isClient() && (LocaleInfo.getCurrentLocale().isRTL()))
        ? ALIGN_RIGHT : ALIGN_LEFT;
      
  /**
   * Gets the horizontal alignment.
   * 
   * @return the current horizontal alignment.
   */
  HorizontalAlignmentConstant getHorizontalAlignment();

  /**
   * Sets the horizontal alignment.
   * 
   * @param align the horizontal alignment (
   *          {@link HasHorizontalAlignment#ALIGN_LEFT},
   *          {@link HasHorizontalAlignment#ALIGN_CENTER},
   *          {@link HasHorizontalAlignment#ALIGN_RIGHT}), or
   *          {@link HasHorizontalAlignment#ALIGN_DEFAULT}).
   */
  void setHorizontalAlignment(HorizontalAlignmentConstant align);
}
