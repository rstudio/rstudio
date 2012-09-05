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

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.dom.client.Style.Clear;
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
import com.google.gwt.dom.client.Style.WhiteSpace;
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * Utility class containing static methods for creating {@link SafeStyles}.
 */
public final class SafeStylesUtils {

  /**
   * Standard implementation of this class.
   */
  static class Impl {
    public SafeStyles forOpacity(double value) {
      return new SafeStylesString("opacity: " + value + ";");
    }
  }

  /**
   * Server implementation of this class.
   * 
   * <p>
   * The server doesn't necessarily know the user agent of the client, so we
   * combine the results of all other implementations.
   * </p>
   */
  static class ImplServer extends Impl {

    private ImplIE6To8 implIE = new ImplIE6To8();

    @Override
    public SafeStyles forOpacity(double value) {
      SafeStylesBuilder sb = new SafeStylesBuilder();
      sb.append(super.forOpacity(value));
      sb.append(implIE.forOpacity(value));
      return sb.toSafeStyles();
    }
  }

  /**
   * IE6-IE8 implementation of this class.
   */
  static class ImplIE6To8 extends Impl {
    @Override
    public SafeStyles forOpacity(double value) {
      // IE6-IE8 uses an alpha filter instead of opacity.
      return new SafeStylesString("filter: alpha(opacity=" + (value * 100) + ");");
    }
  }

  private static Impl impl;

  /**
   * Sets the background-image CSS property.
   * 
   * @param uri the URI of the background image
   * @return a {@link SafeStyles} instance
   * @see #forTrustedBackgroundImage(String)
   */
  public static SafeStyles forBackgroundImage(SafeUri uri) {
    return fromTrustedNameAndValue("background-image", "url(\"" + uri.asString() + "\")");
  }

  /**
   * Sets the border-style CSS property.
   */
  public static SafeStyles forBorderStyle(BorderStyle value) {
    return fromTrustedNameAndValue("border-style", value.getCssName());
  }

  /**
   * Set the border-width css property.
   */
  public static SafeStyles forBorderWidth(double value, Unit unit) {
    return fromTrustedNameAndValue("border-width", value, unit);
  }

  /**
   * Set the bottom css property.
   */
  public static SafeStyles forBottom(double value, Unit unit) {
    return fromTrustedNameAndValue("bottom", value, unit);
  }

  /**
   * Sets the 'clear' CSS property.
   */
  public static SafeStyles forClear(Clear value) {
    return fromTrustedNameAndValue("clear", value.getCssName());
  }

  /**
   * Sets the cursor CSS property.
   */
  public static SafeStyles forCursor(Cursor value) {
    return fromTrustedNameAndValue("cursor", value.getCssName());
  }

  /**
   * Sets the display CSS property.
   */
  public static SafeStyles forDisplay(Display value) {
    return fromTrustedNameAndValue("display", value.getCssName());
  }

  /**
   * Set the float css property.
   */
  public static SafeStyles forFloat(Float value) {
    return fromTrustedNameAndValue("float", value.getCssName());
  }

  /**
   * Set the font-size css property.
   */
  public static SafeStyles forFontSize(double value, Unit unit) {
    return fromTrustedNameAndValue("font-size", value, unit);
  }

  /**
   * Sets the font-style CSS property.
   */
  public static SafeStyles forFontStyle(FontStyle value) {
    return fromTrustedNameAndValue("font-style", value.getCssName());
  }

  /**
   * Sets the font-weight CSS property.
   */
  public static SafeStyles forFontWeight(FontWeight value) {
    return fromTrustedNameAndValue("font-weight", value.getCssName());
  }

  /**
   * Set the height css property.
   */
  public static SafeStyles forHeight(double value, Unit unit) {
    return fromTrustedNameAndValue("height", value, unit);
  }

  /**
   * Set the left css property.
   */
  public static SafeStyles forLeft(double value, Unit unit) {
    return fromTrustedNameAndValue("left", value, unit);
  }

  /**
   * Set the line-height css property.
   */
  public static SafeStyles forLineHeight(double value, Unit unit) {
    return fromTrustedNameAndValue("line-height", value, unit);
  }

  /**
   * Sets the list-style-type CSS property.
   */
  public static SafeStyles forListStyleType(ListStyleType value) {
    return fromTrustedNameAndValue("list-style-type", value.getCssName());
  }

  /**
   * Set the margin css property.
   */
  public static SafeStyles forMargin(double value, Unit unit) {
    return fromTrustedNameAndValue("margin", value, unit);
  }

  /**
   * Set the margin-bottom css property.
   */
  public static SafeStyles forMarginBottom(double value, Unit unit) {
    return fromTrustedNameAndValue("margin-bottom", value, unit);
  }

  /**
   * Set the margin-left css property.
   */
  public static SafeStyles forMarginLeft(double value, Unit unit) {
    return fromTrustedNameAndValue("margin-left", value, unit);
  }

  /**
   * Set the margin-right css property.
   */
  public static SafeStyles forMarginRight(double value, Unit unit) {
    return fromTrustedNameAndValue("margin-right", value, unit);
  }

  /**
   * Set the margin-top css property.
   */
  public static SafeStyles forMarginTop(double value, Unit unit) {
    return fromTrustedNameAndValue("margin-top", value, unit);
  }

  /**
   * Set the opacity css property.
   */
  public static SafeStyles forOpacity(double value) {
    return impl().forOpacity(value);
  }

  /**
   * Sets the outline-style CSS property.
   */
  public static SafeStyles forOutlineStyle(OutlineStyle value) {
    return fromTrustedNameAndValue("outline-style", value.getCssName());
  }

  /**
   * Set the outline-width css property.
   */
  public static SafeStyles forOutlineWidth(double value, Unit unit) {
    return fromTrustedNameAndValue("outline-width", value, unit);
  }

  /**
   * Sets the overflow CSS property.
   */
  public static SafeStyles forOverflow(Overflow value) {
    return fromTrustedNameAndValue("overflow", value.getCssName());
  }

  /**
   * Sets the overflow-x CSS property.
   */
  public static SafeStyles forOverflowX(Overflow value) {
    return fromTrustedNameAndValue("overflow-x", value.getCssName());
  }

  /**
   * Sets the overflow-y CSS property.
   */
  public static SafeStyles forOverflowY(Overflow value) {
    return fromTrustedNameAndValue("overflow-y", value.getCssName());
  }

  /**
   * Set the padding css property.
   */
  public static SafeStyles forPadding(double value, Unit unit) {
    return fromTrustedNameAndValue("padding", value, unit);
  }

  /**
   * Set the padding-bottom css property.
   */
  public static SafeStyles forPaddingBottom(double value, Unit unit) {
    return fromTrustedNameAndValue("padding-bottom", value, unit);
  }

  /**
   * Set the padding-left css property.
   */
  public static SafeStyles forPaddingLeft(double value, Unit unit) {
    return fromTrustedNameAndValue("padding-left", value, unit);
  }

  /**
   * Set the padding-right css property.
   */
  public static SafeStyles forPaddingRight(double value, Unit unit) {
    return fromTrustedNameAndValue("padding-right", value, unit);
  }

  /**
   * Set the padding-top css property.
   */
  public static SafeStyles forPaddingTop(double value, Unit unit) {
    return fromTrustedNameAndValue("padding-top", value, unit);
  }

  /**
   * Sets the position CSS property.
   */
  public static SafeStyles forPosition(Position value) {
    return fromTrustedNameAndValue("position", value.getCssName());
  }

  /**
   * Set the right css property.
   */
  public static SafeStyles forRight(double value, Unit unit) {
    return fromTrustedNameAndValue("right", value, unit);
  }

  /**
   * Set the table-layout CSS property.
   */
  public static SafeStyles forTableLayout(TableLayout value) {
    return fromTrustedNameAndValue("table-layout", value.getCssName());
  }

  /**
   * Sets the 'text-align' CSS property.
   */
  public static SafeStyles forTextAlign(TextAlign value) {
    return fromTrustedNameAndValue("text-align", value.getCssName());
  }

  /**
   * Sets the 'text-decoration' CSS property.
   */
  public static SafeStyles forTextDecoration(TextDecoration value) {
    return fromTrustedNameAndValue("text-decoration", value.getCssName());
  }

  /**
   * Set the 'text-indent' CSS property.
   */
  public static SafeStyles forTextIndent(double value, Unit unit) {
    return fromTrustedNameAndValue("text-indent", value + unit.getType());
  }

  /**
   * Set the 'text-justify' CSS3 property.
   */
  public static SafeStyles forTextJustify(TextJustify value) {
    return fromTrustedNameAndValue("text-justify", value.getCssName());
  }

  /**
   * Set the 'text-overflow' CSS3 property.
   */
  public static SafeStyles forTextOverflow(TextOverflow value) {
    return fromTrustedNameAndValue("text-overflow", value.getCssName());
  }

  /**
   * Set the 'text-transform' CSS property.
   */
  public static SafeStyles forTextTransform(TextTransform value) {
    return fromTrustedNameAndValue("text-transform", value.getCssName());
  }

  /**
   * Set the top css property.
   */
  public static SafeStyles forTop(double value, Unit unit) {
    return fromTrustedNameAndValue("top", value, unit);
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted background color,
   * i.e., without escaping the value. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument will satisfy the
   * {@link SafeStyles} contract when they are composed into the form:
   * "&lt;name&gt;:&lt;value&gt;;".
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
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public static SafeStyles forTrustedBackgroundColor(String value) {
    return fromTrustedNameAndValue("background-color", value);
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted background image,
   * i.e., without escaping the value. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument will satisfy the
   * {@link SafeStyles} contract when they are composed into the form:
   * "&lt;name&gt;:&lt;value&gt;;".
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
   * @param value the property value
   * @return a {@link SafeStyles} instance
   * @see #forBackgroundImage(SafeUri)
   */
  public static SafeStyles forTrustedBackgroundImage(String value) {
    return fromTrustedNameAndValue("background-image", value);
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted border color, i.e.,
   * without escaping the value. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument will satisfy the
   * {@link SafeStyles} contract when they are composed into the form:
   * "&lt;name&gt;:&lt;value&gt;;".
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
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public static SafeStyles forTrustedBorderColor(String value) {
    return fromTrustedNameAndValue("border-color", value);
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted font color, i.e.,
   * without escaping the value. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument will satisfy the
   * {@link SafeStyles} contract when they are composed into the form:
   * "&lt;name&gt;:&lt;value&gt;;".
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
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public static SafeStyles forTrustedColor(String value) {
    return fromTrustedNameAndValue("color", value);
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted outline color,
   * i.e., without escaping the value. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument will satisfy the
   * {@link SafeStyles} contract when they are composed into the form:
   * "&lt;name&gt;:&lt;value&gt;;".
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
   * @param value the property value
   * @return a {@link SafeStyles} instance
   */
  public static SafeStyles forTrustedOutlineColor(String value) {
    return fromTrustedNameAndValue("outline-color", value);
  }

  /**
   * Sets the vertical-align CSS property.
   */
  public static SafeStyles forVerticalAlign(double value, Unit unit) {
    return fromTrustedNameAndValue("vertical-align", value, unit);
  }

  /**
   * Sets the vertical-align CSS property.
   */
  public static SafeStyles forVerticalAlign(VerticalAlign value) {
    return fromTrustedNameAndValue("vertical-align", value.getCssName());
  }

  /**
   * Sets the visibility CSS property.
   */
  public static SafeStyles forVisibility(Visibility value) {
    return fromTrustedNameAndValue("visibility", value.getCssName());
  }

  /**
   * Set the 'white-space' CSS property.
   */
  public static SafeStyles forWhiteSpace(WhiteSpace value) {
    return fromTrustedNameAndValue("white-space", value.getCssName());
  }

  /**
   * Set the width css property.
   */
  public static SafeStyles forWidth(double value, Unit unit) {
    return fromTrustedNameAndValue("width", value, unit);
  }

  /**
   * Set the z-index css property.
   */
  public static SafeStyles forZIndex(int value) {
    return new SafeStylesString("z-index: " + value + ";");
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted name and a trusted
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
   * @param value the value
   * @param unit the units of the value
   * @return a {@link SafeStyles} instance
   */
  public static SafeStyles fromTrustedNameAndValue(String name, double value, Unit unit) {
    SafeStylesHostedModeUtils.maybeCheckValidStyleName(name);
    return new SafeStylesString(name + ":" + value + unit.getType() + ";");
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted name and a trusted
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
  public static SafeStyles fromTrustedNameAndValue(String name, String value) {
    SafeStylesHostedModeUtils.maybeCheckValidStyleName(name);
    SafeStylesHostedModeUtils.maybeCheckValidStyleValue(value);
    return fromTrustedString(name + ":" + value + ";");
  }

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted string, i.e.,
   * without escaping the string. No checks are performed. The calling code
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
   * @param s the input String
   * @return a {@link SafeStyles} instance
   */
  public static SafeStyles fromTrustedString(String s) {
    return new SafeStylesString(s);
  }

  /**
   * Verify that the basic constraints of a {@link SafeStyles} are met. This
   * method is not a guarantee that the specified css is safe for use in a CSS
   * style attribute. It is a minimal set of assertions to check for common
   * errors.
   * 
   * @param styles the CSS properties string
   * @throws NullPointerException if the css is null
   * @throws AssertionError if the css does not meet the contraints
   */
  static void verifySafeStylesConstraints(String styles) {
    if (styles == null) {
      throw new NullPointerException("css is null");
    }

    // CSS properties must end in a semi-colon or they cannot be safely
    // composed with other properties.
    assert ((styles.trim().length() == 0) || styles.endsWith(";")) : "Invalid CSS Property: '"
        + styles + "'. CSS properties must be an empty string or end with a semi-colon (;).";
    assert !styles.contains("<") && !styles.contains(">") : "Invalid CSS Property: '" + styles
        + "'. CSS should not contain brackets (< or >).";
  }

  private static Impl impl() {
    if (impl == null) {
      if (GWT.isClient()) {
        impl = GWT.create(Impl.class);
      } else {
        impl = new ImplServer();
      }
    }
    return impl;
  }

  // prevent instantiation
  private SafeStylesUtils() {
  }
}
