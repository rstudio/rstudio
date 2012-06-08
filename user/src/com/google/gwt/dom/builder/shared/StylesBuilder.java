/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dom.builder.shared;

import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.FontStyle;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.ListStyleType;
import com.google.gwt.dom.client.Style.OutlineStyle;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.TableLayout;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.TextDecoration;
import com.google.gwt.dom.client.Style.TextJustify;
import com.google.gwt.dom.client.Style.TextOverflow;
import com.google.gwt.dom.client.Style.TextTransform;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * Builds the style attribute on an element.
 * 
 * <p>
 * The HTML implementation of class appends the style properties to the HTML
 * string. The DOM implementation of this class sets the element's styles
 * directly.
 * </p>
 */
public interface StylesBuilder {

  /**
   * Sets the background-image CSS property.
   * 
   * @param uri the URI of the background image
   * @return this {@link StylesBuilder}
   * @see #trustedBackgroundImage(String)
   */
  StylesBuilder backgroundImage(SafeUri uri);

  /**
   * Sets the border-style CSS property.
   */
  StylesBuilder borderStyle(BorderStyle value);

  /**
   * Set the border-width css property.
   */
  StylesBuilder borderWidth(double value, Unit unit);

  /**
   * Set the bottom css property.
   */
  StylesBuilder bottom(double value, Unit unit);

  /**
   * Sets the cursor CSS property.
   */
  StylesBuilder cursor(Cursor value);

  /**
   * Sets the display CSS property.
   */
  StylesBuilder display(Display value);

  /**
   * End the current style attribute.
   * 
   * @throws IllegalStateException if the style attribute is already closed
   */
  void endStyle();

  /**
   * Set the float css property.
   */
  StylesBuilder floatprop(Float value);

  /**
   * Set the font-size css property.
   */
  StylesBuilder fontSize(double value, Unit unit);

  /**
   * Sets the font-style CSS property.
   */
  StylesBuilder fontStyle(FontStyle value);

  /**
   * Sets the font-weight CSS property.
   */
  StylesBuilder fontWeight(FontWeight value);

  /**
   * Set the height css property.
   */
  StylesBuilder height(double value, Unit unit);

  /**
   * Set the left css property.
   */
  StylesBuilder left(double value, Unit unit);

  /**
   * Set the line-height css property.
   */
  StylesBuilder lineHeight(double value, Unit unit);

  /**
   * Sets the list-style-type CSS property.
   */
  StylesBuilder listStyleType(ListStyleType value);

  /**
   * Set the margin css property.
   */
  StylesBuilder margin(double value, Unit unit);

  /**
   * Set the margin-bottom css property.
   */
  StylesBuilder marginBottom(double value, Unit unit);

  /**
   * Set the margin-left css property.
   */
  StylesBuilder marginLeft(double value, Unit unit);

  /**
   * Set the margin-right css property.
   */
  StylesBuilder marginRight(double value, Unit unit);

  /**
   * Set the margin-top css property.
   */
  StylesBuilder marginTop(double value, Unit unit);

  /**
   * Set the opacity css property.
   */
  StylesBuilder opacity(double value);

  /**
   * Sets the outline-style CSS property.
   */
  StylesBuilder outlineStyle(OutlineStyle value);

  /**
   * Set the outline-width css property.
   */
  StylesBuilder outlineWidth(double value, Unit unit);

  /**
   * Sets the overflow CSS property.
   */
  StylesBuilder overflow(Overflow value);

  /**
   * Sets the overflow-x CSS property.
   */
  StylesBuilder overflowX(Overflow value);

  /**
   * Sets the overflow-y CSS property.
   */
  StylesBuilder overflowY(Overflow value);

  /**
   * Set the padding css property.
   */
  StylesBuilder padding(double value, Unit unit);

  /**
   * Set the padding-bottom css property.
   */
  StylesBuilder paddingBottom(double value, Unit unit);

  /**
   * Set the padding-left css property.
   */
  StylesBuilder paddingLeft(double value, Unit unit);

  /**
   * Set the padding-right css property.
   */
  StylesBuilder paddingRight(double value, Unit unit);

  /**
   * Set the padding-top css property.
   */
  StylesBuilder paddingTop(double value, Unit unit);

  /**
   * Sets the position CSS property.
   */
  StylesBuilder position(Position value);

  /**
   * Set the right css property.
   */
  StylesBuilder right(double value, Unit unit);

  /**
   * Set the table-layout CSS property.
   */
  StylesBuilder tableLayout(TableLayout value);

  /**
   * Set the text-align CSS property.
   */
  StylesBuilder textAlign(TextAlign value);
  
  /**
   * Set the text-decoration CSS property.
   */
  StylesBuilder textDecoration(TextDecoration value);

  /**
   * Set the text-indent CSS property.
   */
  StylesBuilder textIndent(double value, Unit unit);

  /**
   * Set the text-justify CSS3 property.
   */
  StylesBuilder textJustify(TextJustify value);

  /**
   * Set the text-overflow CSS3 property.
   */
  StylesBuilder textOverflow(TextOverflow value);

  /**
   * Set the text-transform CSS property.
   */
  StylesBuilder textTransform(TextTransform value);

  /**
   * Set the top css property.
   */
  StylesBuilder top(double value, Unit unit);

  /**
   * <p>
   * Sets the "background-color" style property to the specified color string.
   * Does not check or escape the color string. The calling code should be
   * carefully reviewed to ensure that the provided color string won't cause a
   * security issue if included in a style attribute.
   * </p>
   * 
   * <p>
   * For details and constraints, see
   * {@link com.google.gwt.safecss.shared.SafeStyles}.
   * </p>
   * 
   * @return this {@link StylesBuilder}
   */
  StylesBuilder trustedBackgroundColor(String value);

  /**
   * <p>
   * Sets the "background-image" style property to the specified value. Does not
   * check or escape the value. The calling code should be carefully reviewed to
   * ensure that the provided value string won't cause a security issue if
   * included in a style attribute.
   * </p>
   * 
   * <p>
   * For details and constraints, see
   * {@link com.google.gwt.safecss.shared.SafeStyles}.
   * </p>
   * 
   * @return this {@link StylesBuilder}
   */
  StylesBuilder trustedBackgroundImage(String value);

  /**
   * <p>
   * Sets the "border-color" style property to the specified color string. Does
   * not check or escape the color string. The calling code should be carefully
   * reviewed to ensure that the provided color string won't cause a security
   * issue if included in a style attribute.
   * </p>
   * 
   * <p>
   * For details and constraints, see
   * {@link com.google.gwt.safecss.shared.SafeStyles}.
   * </p>
   * 
   * @return this {@link StylesBuilder}
   */
  StylesBuilder trustedBorderColor(String value);

  /**
   * <p>
   * Sets the "color" style property, which controls font color, to the
   * specified color string. Does not check or escape the color string. The
   * calling code should be carefully reviewed to ensure that the provided color
   * string won't cause a security issue if included in a style attribute.
   * </p>
   * 
   * <p>
   * For details and constraints, see
   * {@link com.google.gwt.safecss.shared.SafeStyles}.
   * </p>
   * 
   * @return this {@link StylesBuilder}
   */
  StylesBuilder trustedColor(String value);

  /**
   * <p>
   * Sets the "outline-color" style property to the specified color string. Does
   * not check or escape the color string. The calling code should be carefully
   * reviewed to ensure that the provided color string won't cause a security
   * issue if included in a style attribute.
   * </p>
   * 
   * <p>
   * For details and constraints, see
   * {@link com.google.gwt.safecss.shared.SafeStyles}.
   * </p>
   * 
   * @return this {@link StylesBuilder}
   */
  StylesBuilder trustedOutlineColor(String value);

  /**
   * <p>
   * Set a style property from a trusted name and a trusted value, i.e., without
   * escaping the name and value. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument will satisfy the
   * {@link com.google.gwt.safecss.shared.SafeStyles} contract when they are
   * composed into the form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * SafeStyles may never contain literal angle brackets. Otherwise, it could be
   * unsafe to place a SafeStyles into a &lt;style&gt; tag (where it can't be
   * HTML escaped). For example, if the SafeStyles containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * @param unit the units of the value
   * @return this {@link StylesBuilder}
   */
  StylesBuilder trustedProperty(String name, double value, Unit unit);

  /**
   * <p>
   * Set a style property from a trusted name and a trusted value, i.e., without
   * escaping the name and value. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument will satisfy the
   * {@link com.google.gwt.safecss.shared.SafeStyles} contract when they are
   * composed into the form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * SafeStyles may never contain literal angle brackets. Otherwise, it could be
   * unsafe to place a SafeStyles into a &lt;style&gt; tag (where it can't be
   * HTML escaped). For example, if the SafeStyles containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * @return this {@link StylesBuilder}
   */
  StylesBuilder trustedProperty(String name, String value);

  /**
   * Sets the vertical-align CSS property.
   */
  StylesBuilder verticalAlign(double value, Unit unit);

  /**
   * Sets the vertical-align CSS property.
   */
  StylesBuilder verticalAlign(VerticalAlign value);

  /**
   * Sets the visibility CSS property.
   */
  StylesBuilder visibility(Visibility value);

  /**
   * Set the width css property.
   */
  StylesBuilder width(double value, Unit unit);

  /**
   * Set the z-index css property.
   */
  StylesBuilder zIndex(int value);
}
