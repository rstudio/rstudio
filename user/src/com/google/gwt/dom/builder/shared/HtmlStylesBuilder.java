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
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the style object.
 */
class HtmlStylesBuilder implements StylesBuilder {

  /**
   * A map of camelCase style properties to their hyphenated equivalents.
   * 
   * The set of style property names is limited, and common ones are reused
   * frequently, so caching saves us from converting every style property name
   * from camelCase to hyphenated form.
   */
  private static Map<String, String> camelCaseMap;

  /**
   * Regex to match a word in a camelCase phrase. A word starts with an
   * uppercase or lowercase letter, followed by zero or more non-uppercase
   * letters. For example, in the camelCase phrase backgroundUrl, the pattern
   * matches "background" and "Url".
   * 
   * This pattern is not used to validate the style property name.
   * {@link SafeStylesUtils} performs a more detailed check.
   */
  private static RegExp camelCaseWord;

  /**
   * Regex to match a word that starts with an uppercase letter.
   */
  private static RegExp caseWord;

  /**
   * Convert a camelCase or hyphenated string to a hyphenated string.
   * 
   * @param name the camelCase or hyphenated string to convert
   * @return the hyphenated string
   */
  // Visible for testing
  static String toHyphenatedForm(String name) {
    // Static initializers.
    if (camelCaseWord == null) {
      camelCaseMap = new HashMap<String, String>();
      camelCaseWord = RegExp.compile("([A-Za-z])([^A-Z]*)", "g");
      caseWord = RegExp.compile("[A-Z]([^A-Z]*)");
    }

    // Early exit if already in hyphenated form.
    if (name.contains("-")) {
      return name;
    }

    // Check for the name in the cache.
    String hyphenated = camelCaseMap.get(name);

    // Convert the name to hyphenated format if not in the cache.
    if (hyphenated == null) {
      hyphenated = "";
      MatchResult matches;
      while ((matches = camelCaseWord.exec(name)) != null) {
        String word = matches.getGroup(0);
        if (caseWord.exec(word) == null) {
          // The first letter is already lowercase, probably the first word.
          hyphenated += word;
        } else {
          // Hyphenate the first letter.
          hyphenated += "-" + matches.getGroup(1).toLowerCase(Locale.ROOT);
          if (matches.getGroupCount() > 1) {
            hyphenated += matches.getGroup(2);
          }
        }
      }
      camelCaseMap.put(name, hyphenated);
    }

    return hyphenated;
  }

  private final HtmlBuilderImpl delegate;

  /**
   * Construct a new {@link HtmlStylesBuilder}.
   * 
   * @param delegate the delegate that builds the style
   */
  HtmlStylesBuilder(HtmlBuilderImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public StylesBuilder backgroundImage(SafeUri uri) {
    return delegate.styleProperty(SafeStylesUtils.forBackgroundImage(uri));
  }

  @Override
  public StylesBuilder borderStyle(BorderStyle value) {
    return delegate.styleProperty(SafeStylesUtils.forBorderStyle(value));
  }

  @Override
  public StylesBuilder borderWidth(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forBorderWidth(value, unit));
  }

  @Override
  public StylesBuilder bottom(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forBottom(value, unit));
  }

  @Override
  public StylesBuilder cursor(Cursor value) {
    return delegate.styleProperty(SafeStylesUtils.forCursor(value));
  }

  @Override
  public StylesBuilder display(Display value) {
    return delegate.styleProperty(SafeStylesUtils.forDisplay(value));
  }

  @Override
  public void endStyle() {
    delegate.endStyle();
  }

  @Override
  public StylesBuilder floatprop(Float value) {
    return delegate.styleProperty(SafeStylesUtils.forFloat(value));
  }

  @Override
  public StylesBuilder fontSize(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forFontSize(value, unit));
  }

  @Override
  public StylesBuilder fontStyle(FontStyle value) {
    return delegate.styleProperty(SafeStylesUtils.forFontStyle(value));
  }

  @Override
  public StylesBuilder fontWeight(FontWeight value) {
    return delegate.styleProperty(SafeStylesUtils.forFontWeight(value));
  }

  @Override
  public StylesBuilder height(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forHeight(value, unit));
  }

  @Override
  public StylesBuilder left(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forLeft(value, unit));
  }

  @Override
  public StylesBuilder lineHeight(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forLineHeight(value, unit));
  }

  @Override
  public StylesBuilder listStyleType(ListStyleType value) {
    return delegate.styleProperty(SafeStylesUtils.forListStyleType(value));
  }

  @Override
  public StylesBuilder margin(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forMargin(value, unit));
  }

  @Override
  public StylesBuilder marginBottom(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forMarginBottom(value, unit));
  }

  @Override
  public StylesBuilder marginLeft(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forMarginLeft(value, unit));
  }

  @Override
  public StylesBuilder marginRight(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forMarginRight(value, unit));
  }

  @Override
  public StylesBuilder marginTop(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forMarginTop(value, unit));
  }

  @Override
  public StylesBuilder opacity(double value) {
    return delegate.styleProperty(SafeStylesUtils.forOpacity(value));
  }

  @Override
  public StylesBuilder outlineStyle(OutlineStyle value) {
    return delegate.styleProperty(SafeStylesUtils.forOutlineStyle(value));
  }

  @Override
  public StylesBuilder outlineWidth(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forOutlineWidth(value, unit));
  }

  @Override
  public StylesBuilder overflow(Overflow value) {
    return delegate.styleProperty(SafeStylesUtils.forOverflow(value));
  }

  @Override
  public StylesBuilder overflowX(Overflow value) {
    return delegate.styleProperty(SafeStylesUtils.forOverflowX(value));
  }

  @Override
  public StylesBuilder overflowY(Overflow value) {
    return delegate.styleProperty(SafeStylesUtils.forOverflowY(value));
  }

  @Override
  public StylesBuilder padding(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forPadding(value, unit));
  }

  @Override
  public StylesBuilder paddingBottom(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forPaddingBottom(value, unit));
  }

  @Override
  public StylesBuilder paddingLeft(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forPaddingLeft(value, unit));
  }

  @Override
  public StylesBuilder paddingRight(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forPaddingRight(value, unit));
  }

  @Override
  public StylesBuilder paddingTop(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forPaddingTop(value, unit));
  }

  @Override
  public StylesBuilder position(Position value) {
    return delegate.styleProperty(SafeStylesUtils.forPosition(value));
  }

  @Override
  public StylesBuilder right(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forRight(value, unit));
  }

  @Override
  public StylesBuilder tableLayout(TableLayout value) {
    return delegate.styleProperty(SafeStylesUtils.forTableLayout(value));
  }

  @Override
  public StylesBuilder textAlign(TextAlign value) {
    return delegate.styleProperty(SafeStylesUtils.forTextAlign(value));
  }

  @Override
  public StylesBuilder textDecoration(TextDecoration value) {
    return delegate.styleProperty(SafeStylesUtils.forTextDecoration(value));
  }

  @Override
  public StylesBuilder textIndent(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forTextIndent(value, unit));
  }

  @Override
  public StylesBuilder textJustify(TextJustify value) {
    return delegate.styleProperty(SafeStylesUtils.forTextJustify(value));
  }

  @Override
  public StylesBuilder textOverflow(TextOverflow value) {
    return delegate.styleProperty(SafeStylesUtils.forTextOverflow(value));
  }

  @Override
  public StylesBuilder textTransform(TextTransform value) {
    return delegate.styleProperty(SafeStylesUtils.forTextTransform(value));
  }

  @Override
  public StylesBuilder top(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forTop(value, unit));
  }

  @Override
  public StylesBuilder trustedBackgroundColor(String value) {
    return delegate.styleProperty(SafeStylesUtils.forTrustedBackgroundColor(value));
  }

  @Override
  public StylesBuilder trustedBackgroundImage(@IsSafeUri String value) {
    return delegate.styleProperty(SafeStylesUtils.forTrustedBackgroundImage(value));
  }

  @Override
  public StylesBuilder trustedBorderColor(String value) {
    return delegate.styleProperty(SafeStylesUtils.forTrustedBorderColor(value));
  }

  @Override
  public StylesBuilder trustedColor(String value) {
    return delegate.styleProperty(SafeStylesUtils.forTrustedColor(value));
  }

  @Override
  public StylesBuilder trustedOutlineColor(String value) {
    return delegate.styleProperty(SafeStylesUtils.forTrustedOutlineColor(value));
  }

  @Override
  public StylesBuilder trustedProperty(String name, double value, Unit unit) {
    name = toHyphenatedForm(name);
    return delegate.styleProperty(SafeStylesUtils.fromTrustedNameAndValue(name, value, unit));
  }

  @Override
  public StylesBuilder trustedProperty(String name, String value) {
    name = toHyphenatedForm(name);
    return delegate.styleProperty(SafeStylesUtils.fromTrustedNameAndValue(name, value));
  }

  @Override
  public StylesBuilder verticalAlign(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forVerticalAlign(value, unit));
  }

  @Override
  public StylesBuilder verticalAlign(VerticalAlign value) {
    return delegate.styleProperty(SafeStylesUtils.forVerticalAlign(value));
  }

  @Override
  public StylesBuilder visibility(Visibility value) {
    return delegate.styleProperty(SafeStylesUtils.forVisibility(value));
  }

  @Override
  public StylesBuilder width(double value, Unit unit) {
    return delegate.styleProperty(SafeStylesUtils.forWidth(value, unit));
  }

  @Override
  public StylesBuilder zIndex(int value) {
    return delegate.styleProperty(SafeStylesUtils.forZIndex(value));
  }
}
