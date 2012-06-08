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
package com.google.gwt.safecss.shared;

import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Clear;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.dom.client.Style.FontStyle;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.ListStyleType;
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
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * A builder that facilitates the building up of XSS-safe CSS attribute strings
 * from {@link SafeStyles}. It is used essentially like a {@link StringBuilder},
 * but access {@link SafeStyles} instead of Strings.
 * 
 * <p>
 * The accumulated XSS-safe {@link SafeStyles} can be obtained in the form of a
 * {@link SafeStyles} via the {@link #toSafeStyles()} method.
 * 
 * <p>
 * This class is not thread-safe.
 */
public final class SafeStylesBuilder {

  private final StringBuilder sb = new StringBuilder();

  /**
   * Constructs an empty {@link SafeStylesBuilder}.
   */
  public SafeStylesBuilder() {
  }

  /**
   * Appends the contents of another {@link SafeStyles} object, without applying
   * any escaping or sanitization to it.
   * 
   * @param styles the {@link SafeStyles} to append
   * @return a reference to this object
   */
  public SafeStylesBuilder append(SafeStyles styles) {
    sb.append(styles.asString());
    return this;
  }

  /**
   * <p>
   * Appends {@link SafeStyles} constructed from a trusted string, i.e., without
   * escaping the string. Only minimal checks are performed. The calling code
   * should be carefully reviewed to ensure the argument meets the
   * {@link SafeStyles} contract.
   * 
   * <p>
   * Generally, {@link SafeStyles} should be of the form
   * {@code cssPropertyName:value;}, where neither the name nor the value
   * contain malicious scripts.
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * <p>
   * The following example values comply with this type's contract:
   * <ul>
   * <li><code>width: 1em;</code></li>
   * <li><code>height:1em;</code></li>
   * <li><code>width: 1em;height: 1em;</code></li>
   * <li><code>background:url('http://url');</code></li>
   * </ul>
   * In addition, the empty string is safe for use in a CSS attribute.
   * 
   * <p>
   * The following example values do <em>not</em> comply with this type's
   * contract:
   * <ul>
   * <li><code>background: red</code> (missing a trailing semi-colon)</li>
   * <li><code>background:</code> (missing a value and a trailing semi-colon)</li>
   * <li><code>1em</code> (missing an attribute name, which provides context for
   * the value)</li>
   * </ul>
   * 
   * @param styles the input String
   * @return a {@link SafeStyles} instance
   */
  public SafeStylesBuilder appendTrustedString(String styles) {
    SafeStylesUtils.verifySafeStylesConstraints(styles);
    sb.append(styles);
    return this;
  }

  /**
   * Append the background-image CSS property.
   * 
   * @param uri the URI of the background image
   * @see #trustedBackgroundImage(String)
   */
  public SafeStylesBuilder backgroundImage(SafeUri uri) {
    return append(SafeStylesUtils.forBackgroundImage(uri));
  }

  /**
   * Append the border-style CSS property.
   */
  public SafeStylesBuilder borderStyle(BorderStyle value) {
    return append(SafeStylesUtils.forBorderStyle(value));
  }

  /**
   * Append the border-width css property.
   */
  public SafeStylesBuilder borderWidth(double value, Unit unit) {
    return append(SafeStylesUtils.forBorderWidth(value, unit));
  }

  /**
   * Append the bottom css property.
   */
  public SafeStylesBuilder bottom(double value, Unit unit) {
    return append(SafeStylesUtils.forBottom(value, unit));
  }

  /**
   * Append the 'clear' CSS property.
   */
  public SafeStylesBuilder clear(Clear value) {
    return append(SafeStylesUtils.forClear(value));
  }

  /**
   * Append the cursor CSS property.
   */
  public SafeStylesBuilder cursor(Cursor value) {
    return append(SafeStylesUtils.forCursor(value));
  }

  /**
   * Append the display CSS property.
   */
  public SafeStylesBuilder display(Display value) {
    return append(SafeStylesUtils.forDisplay(value));
  }

  /**
   * Append the float css property.
   * 
   * <p>
   * Note: This method has the suffix "prop" to avoid Java compilation errors.
   * The term "float" is a reserved word in Java representing the primitive
   * float.
   * </p>
   */
  public SafeStylesBuilder floatprop(Float value) {
    return append(SafeStylesUtils.forFloat(value));
  }

  /**
   * Append the font-size css property.
   */
  public SafeStylesBuilder fontSize(double value, Unit unit) {
    return append(SafeStylesUtils.forFontSize(value, unit));
  }

  /**
   * Append the font-style CSS property.
   */
  public SafeStylesBuilder fontStyle(FontStyle value) {
    return append(SafeStylesUtils.forFontStyle(value));
  }

  /**
   * Append the font-weight CSS property.
   */
  public SafeStylesBuilder fontWeight(FontWeight value) {
    return append(SafeStylesUtils.forFontWeight(value));
  }

  /**
   * Append the height css property.
   */
  public SafeStylesBuilder height(double value, Unit unit) {
    return append(SafeStylesUtils.forHeight(value, unit));
  }

  /**
   * Append the left css property.
   */
  public SafeStylesBuilder left(double value, Unit unit) {
    return append(SafeStylesUtils.forLeft(value, unit));
  }

  /**
   * Append the list-style-type CSS property.
   */
  public SafeStylesBuilder listStyleType(ListStyleType value) {
    return append(SafeStylesUtils.forListStyleType(value));
  }

  /**
   * Append the margin css property.
   */
  public SafeStylesBuilder margin(double value, Unit unit) {
    return append(SafeStylesUtils.forMargin(value, unit));
  }

  /**
   * Append the margin-bottom css property.
   */
  public SafeStylesBuilder marginBottom(double value, Unit unit) {
    return append(SafeStylesUtils.forMarginBottom(value, unit));
  }

  /**
   * Append the margin-left css property.
   */
  public SafeStylesBuilder marginLeft(double value, Unit unit) {
    return append(SafeStylesUtils.forMarginLeft(value, unit));
  }

  /**
   * Append the margin-right css property.
   */
  public SafeStylesBuilder marginRight(double value, Unit unit) {
    return append(SafeStylesUtils.forMarginRight(value, unit));
  }

  /**
   * Append the margin-top css property.
   */
  public SafeStylesBuilder marginTop(double value, Unit unit) {
    return append(SafeStylesUtils.forMarginTop(value, unit));
  }

  /**
   * Append the opacity css property.
   */
  public SafeStylesBuilder opacity(double value) {
    return append(SafeStylesUtils.forOpacity(value));
  }

  /**
   * Append the overflow CSS property.
   */
  public SafeStylesBuilder overflow(Overflow value) {
    return append(SafeStylesUtils.forOverflow(value));
  }

  /**
   * Append the overflow-x CSS property.
   */
  public SafeStylesBuilder overflowX(Overflow value) {
    return append(SafeStylesUtils.forOverflowX(value));
  }

  /**
   * Append the overflow-y CSS property.
   */
  public SafeStylesBuilder overflowY(Overflow value) {
    return append(SafeStylesUtils.forOverflowY(value));
  }

  /**
   * Append the padding css property.
   */
  public SafeStylesBuilder padding(double value, Unit unit) {
    return append(SafeStylesUtils.forPadding(value, unit));
  }

  /**
   * Append the padding-bottom css property.
   */
  public SafeStylesBuilder paddingBottom(double value, Unit unit) {
    return append(SafeStylesUtils.forPaddingBottom(value, unit));
  }

  /**
   * Append the padding-left css property.
   */
  public SafeStylesBuilder paddingLeft(double value, Unit unit) {
    return append(SafeStylesUtils.forPaddingLeft(value, unit));
  }

  /**
   * Append the padding-right css property.
   */
  public SafeStylesBuilder paddingRight(double value, Unit unit) {
    return append(SafeStylesUtils.forPaddingRight(value, unit));
  }

  /**
   * Append the padding-top css property.
   */
  public SafeStylesBuilder paddingTop(double value, Unit unit) {
    return append(SafeStylesUtils.forPaddingTop(value, unit));
  }

  /**
   * Append the position CSS property.
   */
  public SafeStylesBuilder position(Position value) {
    return append(SafeStylesUtils.forPosition(value));
  }

  /**
   * Append the right css property.
   */
  public SafeStylesBuilder right(double value, Unit unit) {
    return append(SafeStylesUtils.forRight(value, unit));
  }

  /**
   * Append the table-layout CSS property.
   */
  public SafeStylesBuilder tableLayout(TableLayout value) {
    return append(SafeStylesUtils.forTableLayout(value));
  }

  /**
   * Append the 'text-align' CSS property.
   */
  public SafeStylesBuilder textAlign(TextAlign value) {
    return append(SafeStylesUtils.forTextAlign(value));
  }

  /**
   * Append the text-decoration CSS property.
   */
  public SafeStylesBuilder textDecoration(TextDecoration value) {
    return append(SafeStylesUtils.forTextDecoration(value));
  }

  /**
   * Append the 'text-indent' CSS property.
   */
  public SafeStylesBuilder textIndent(double value, Unit unit) {
    return append(SafeStylesUtils.forTextIndent(value, unit));
  }

  /**
   * Append the 'text-justify' CSS3 property.
   */
  public SafeStylesBuilder textJustify(TextJustify value) {
    return append(SafeStylesUtils.forTextJustify(value));
  }

  /**
   * Append the 'text-overflow' CSS3 property.
   */
  public SafeStylesBuilder textOverflow(TextOverflow value) {
    return append(SafeStylesUtils.forTextOverflow(value));
  }

  /**
   * Append the 'text-transform' CSS property.
   */
  public SafeStylesBuilder textTransform(TextTransform value) {
    return append(SafeStylesUtils.forTextTransform(value));
  }

  /**
   * Append the top css property.
   */
  public SafeStylesBuilder top(double value, Unit unit) {
    return append(SafeStylesUtils.forTop(value, unit));
  }

  /**
   * Returns the safe CSS properties accumulated in the builder as a
   * {@link SafeStyles}.
   * 
   * @return a {@link SafeStyles} instance
   */
  public SafeStyles toSafeStyles() {
    return new SafeStylesString(sb.toString());
  }

  /**
   * <p>
   * Append the trusted background color, i.e., without escaping the value. No
   * checks are performed. The calling code should be carefully reviewed to
   * ensure the argument will satisfy the {@link SafeStyles} contract when they
   * are composed into the form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public SafeStylesBuilder trustedBackgroundColor(String value) {
    return append(SafeStylesUtils.forTrustedBackgroundColor(value));
  }

  /**
   * <p>
   * Append the trusted background image, i.e., without escaping the value. No
   * checks are performed. The calling code should be carefully reviewed to
   * ensure the argument will satisfy the {@link SafeStyles} contract when they
   * are composed into the form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * @param value the property value
   * @return a {@link SafeStyles} instance
   * @see #backgroundImage(SafeUri)
   */
  public SafeStylesBuilder trustedBackgroundImage(String value) {
    return append(SafeStylesUtils.forTrustedBackgroundImage(value));
  }

  /**
   * <p>
   * Append the trusted border color, i.e., without escaping the value. No
   * checks are performed. The calling code should be carefully reviewed to
   * ensure the argument will satisfy the {@link SafeStyles} contract when they
   * are composed into the form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public SafeStylesBuilder trustedBorderColor(String value) {
    return append(SafeStylesUtils.forTrustedBorderColor(value));
  }

  /**
   * <p>
   * Append the trusted font color, i.e., without escaping the value. No checks
   * are performed. The calling code should be carefully reviewed to ensure the
   * argument will satisfy the {@link SafeStyles} contract when they are
   * composed into the form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public SafeStylesBuilder trustedColor(String value) {
    return append(SafeStylesUtils.forTrustedColor(value));
  }

  /**
   * <p>
   * Append a {@link SafeStyles} constructed from a trusted name and a trusted
   * value, i.e., without escaping the name and value. No checks are performed.
   * The calling code should be carefully reviewed to ensure the argument will
   * satisfy the {@link SafeStyles} contract when they are composed into the
   * form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * </p>
   * 
   * <p>
   * The name should be in hyphenated format, not camelCase format.
   * </p>
   * 
   * @param name the property name
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public SafeStylesBuilder trustedNameAndValue(String name, double value, Unit unit) {
    return append(SafeStylesUtils.fromTrustedNameAndValue(name, value, unit));
  }

  /**
   * <p>
   * Append a {@link SafeStyles} constructed from a trusted name and a trusted
   * value, i.e., without escaping the name and value. No checks are performed.
   * The calling code should be carefully reviewed to ensure the argument will
   * satisfy the {@link SafeStyles} contract when they are composed into the
   * form: "&lt;name&gt;:&lt;value&gt;;".
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * </p>
   * 
   * <p>
   * The name should be in hyphenated format, not camelCase format.
   * </p>
   * 
   * @param name the property name
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public SafeStylesBuilder trustedNameAndValue(String name, String value) {
    return append(SafeStylesUtils.fromTrustedNameAndValue(name, value));
  }

  /**
   * Append the vertical-align CSS property.
   */
  public SafeStylesBuilder verticalAlign(double value, Unit unit) {
    return append(SafeStylesUtils.forVerticalAlign(value, unit));
  }

  /**
   * Append the vertical-align CSS property.
   */
  public SafeStylesBuilder verticalAlign(VerticalAlign value) {
    return append(SafeStylesUtils.forVerticalAlign(value));
  }

  /**
   * Append the visibility CSS property.
   */
  public SafeStylesBuilder visibility(Visibility value) {
    return append(SafeStylesUtils.forVisibility(value));
  }

  /**
   * Append the 'white-space' CSS property.
   */
  public SafeStylesBuilder whiteSpace(WhiteSpace whiteSpace) {
    return append(SafeStylesUtils.forWhiteSpace(whiteSpace));
  }

  /**
   * Append the width css property.
   */
  public SafeStylesBuilder width(double value, Unit unit) {
    return append(SafeStylesUtils.forWidth(value, unit));
  }

  /**
   * Append the z-index css property.
   */
  public SafeStylesBuilder zIndex(int value) {
    return append(SafeStylesUtils.forZIndex(value));
  }
}
