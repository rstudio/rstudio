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
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.client.LocaleInfo;

/**
 * Characteristic interface which indicates that a widget can be aligned
 * horizontally.
 * 
 * <h3>Use in UiBinder Templates</h3>
 * 
 * <p>
 * The names of the static members of {@link HorizontalAlignmentConstant}, as
 * well as simple alignment names (<code>left</code>, <code>center</code>,
 * <code>right</code>, <code>justify</code>), can be used as values for a
 * <code>horizontalAlignment</code> attribute of any widget that implements this
 * interface. (In fact, this will work for any widget method that takes a single
 * HorizontalAlignmentConstant value.)
 * <p>
 * For example,
 * 
 * <pre>
 * &lt;g:Label horizontalAlignment='ALIGN_RIGHT'>Hi there.&lt;/g:Label>
 * &lt;g:Label horizontalAlignment='right'>Hi there.&lt;/g:Label>
 * </pre>
 */
public interface HasHorizontalAlignment {

  /**
   * Type for values defined and used in {@link HasAutoHorizontalAlignment}.
   * Defined here so that HorizontalAlignmentConstant can be derived from it,
   * thus allowing HasAutoHorizontalAlignment methods to accept and return both
   * AutoHorizontalAlignmentConstant and HorizontalAlignmentConstant values -
   * without allowing the methods defined here to accept or return
   * AutoHorizontalAlignmentConstant values.
   */
  public static class AutoHorizontalAlignmentConstant {
    // The constructor is package-private to prevent uncontrolled inheritance
    // and instantiation of this class.
    AutoHorizontalAlignmentConstant() {
    }
  }

  /**
   * Possible return values for {@link #getHorizontalAlignment}, and parameter
   * values for {@link #setHorizontalAlignment}.
   */
  public static class HorizontalAlignmentConstant extends
      AutoHorizontalAlignmentConstant {

    public static HorizontalAlignmentConstant endOf(Direction direction) {
      return direction == Direction.LTR ? ALIGN_RIGHT :
          direction == Direction.RTL ? ALIGN_LEFT : ALIGN_LOCALE_END;
    }

    public static HorizontalAlignmentConstant startOf(Direction direction) {
      return direction == Direction.LTR ? ALIGN_LEFT :
          direction == Direction.RTL ? ALIGN_RIGHT : ALIGN_LOCALE_START;
    }

    private final String textAlignString;

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
      TextAlign.CENTER.getCssName());

  /**
   * Specifies that the widget's contents should be aligned as justify.
   */
  HorizontalAlignmentConstant ALIGN_JUSTIFY = new HorizontalAlignmentConstant(
      TextAlign.JUSTIFY.getCssName());

  /**
   * Specifies that the widget's contents should be aligned to the left.
   */
  HorizontalAlignmentConstant ALIGN_LEFT = new HorizontalAlignmentConstant(
      TextAlign.LEFT.getCssName());

  /**
   * Specifies that the widget's contents should be aligned to the right.
   */
  HorizontalAlignmentConstant ALIGN_RIGHT = new HorizontalAlignmentConstant(
      TextAlign.RIGHT.getCssName());

  /**
   * In a RTL layout, specifies that the widget's contents should be aligned to
   * the right. In a LTR layout, specifies that the widget's constants should be
   * aligned to the left.
   */
  HorizontalAlignmentConstant ALIGN_LOCALE_START = GWT.isClient()
      && LocaleInfo.getCurrentLocale().isRTL() ? ALIGN_RIGHT : ALIGN_LEFT;

  /**
   * In a RTL layout, specifies that the widget's contents should be aligned to
   * the left. In a LTR layout, specifies that the widget's constants should be
   * aligned to the right.
   */
  HorizontalAlignmentConstant ALIGN_LOCALE_END = GWT.isClient()
      && LocaleInfo.getCurrentLocale().isRTL() ? ALIGN_LEFT : ALIGN_RIGHT;

  /**
   * Synonym of {@link #ALIGN_LOCALE_START}.
   */
  HorizontalAlignmentConstant ALIGN_DEFAULT = ALIGN_LOCALE_START;

  /**
   * Gets the horizontal alignment.
   *
   * @return the current horizontal alignment (
   *         {@link HasHorizontalAlignment#ALIGN_LEFT},
   *         {@link HasHorizontalAlignment#ALIGN_CENTER},
   *         {@link HasHorizontalAlignment#ALIGN_RIGHT},
   *         {@link HasHorizontalAlignment#ALIGN_JUSTIFY}, or
   *         null).
   */
  HorizontalAlignmentConstant getHorizontalAlignment();

  /**
   * Sets the horizontal alignment.
   * <p> Use {@code null} to clear horizontal alignment, allowing it to be
   * determined by the standard HTML mechanisms such as inheritance and CSS
   * rules.  
   *
   * @param align the horizontal alignment (
   *         {@link HasHorizontalAlignment#ALIGN_LEFT},
   *         {@link HasHorizontalAlignment#ALIGN_CENTER},
   *         {@link HasHorizontalAlignment#ALIGN_RIGHT},
   *         {@link HasHorizontalAlignment#ALIGN_JUSTIFY},
   *         {@link HasHorizontalAlignment#ALIGN_LOCALE_START}, or
   *         {@link HasHorizontalAlignment#ALIGN_LOCALE_END}).
   */
  void setHorizontalAlignment(HorizontalAlignmentConstant align);
}
