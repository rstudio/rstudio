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
package com.google.gwt.dom.builder.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.builder.shared.StylesBuilder;
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
import com.google.gwt.safehtml.shared.SafeUri;

/**
 * Builds the style object.
 */
class DomStylesBuilder implements StylesBuilder {

  /**
   * A map of hyphenated style properties to their camelCase equivalents.
   * 
   * The set of style property names is limited, and common ones are reused
   * frequently, so caching saves us from converting every style property name
   * from hyphenated to camelCase form.
   * 
   * Use a {@link JavaScriptObject} to avoid the dynamic casts associated with
   * the emulated version of {@link java.util.Map}.
   */
  private static JavaScriptObject hyphenatedMap;

  /**
   * Regex to match a word in a hyphenated phrase. A word starts with an a
   * hyphen or a letter, followed by zero or more characters letters. For
   * example, in the phrase background-url, the pattern matches "background" and
   * "-url".
   */
  private static RegExp maybeHyphenatedWord;

  /**
   * Convert a hyphenated or camelCase string to a camelCase string.
   * 
   * @param name the hyphenated or camelCase string to convert
   * @return the hyphenated string
   */
  // Visible for testing
  static String toCamelCaseForm(String name) {
    // Static initializers.
    if (hyphenatedMap == null) {
      hyphenatedMap = JavaScriptObject.createObject();
      maybeHyphenatedWord = RegExp.compile("([-]?)([a-z])([a-z0-9]*)", "g");
    }

    // Early exit if already in camelCase form.
    if (!name.contains("-")) {
      return name;
    }

    // Check for the name in the cache.
    String camelCase = getCamelCaseName(hyphenatedMap, name);

    // Convert the name to camelCase format if not in the cache.
    if (camelCase == null) {
      /*
       * Strip of any leading hyphens, which are used in browser specified style
       * properties such as "-webkit-border-radius". In this case, the first
       * word "webkit" should remain lowercase.
       */
      if (name.startsWith("-") && name.length() > 1) {
        name = name.substring(1);
      }

      camelCase = "";
      MatchResult matches;
      while ((matches = maybeHyphenatedWord.exec(name)) != null) {
        String word = matches.getGroup(0);
        if (!word.startsWith("-")) {
          // The word is not hyphenated. Probably the first word.
          camelCase += word;
        } else {
          // Remove hyphen and uppercase next letter.
          camelCase += matches.getGroup(2).toUpperCase();
          if (matches.getGroupCount() > 2) {
            camelCase += matches.getGroup(3);
          }
        }
      }
      putCamelCaseName(hyphenatedMap, name, camelCase);
    }

    return camelCase;
  }

  /**
   * Get the camelCase form of a style name to a map.
   * 
   * @param name the user specified style name
   * @return the camelCase name, or null if not set
   */
  private static native String getCamelCaseName(JavaScriptObject map, String name) /*-{
    return map[name] || null;
  }-*/;

  /**
   * Save the camelCase form of a style name to a map.
   * 
   * @param name the user specified style name
   * @param camelCase the camelCase name
   */
  private static native void putCamelCaseName(JavaScriptObject map, String name, String camelCase) /*-{
    map[name] = camelCase;
  }-*/;

  private final DomBuilderImpl delegate;

  /**
   * Construct a new {@link DomStylesBuilder}.
   * 
   * @param delegate the delegate that builds the style
   */
  DomStylesBuilder(DomBuilderImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public StylesBuilder backgroundImage(SafeUri uri) {
    delegate.assertCanAddStyleProperty().setBackgroundImage(uri.asString());
    return this;
  }

  @Override
  public StylesBuilder borderStyle(BorderStyle value) {
    delegate.assertCanAddStyleProperty().setBorderStyle(value);
    return this;
  }

  @Override
  public StylesBuilder borderWidth(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setBorderWidth(value, unit);
    return this;
  }

  @Override
  public StylesBuilder bottom(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setBottom(value, unit);
    return this;
  }

  @Override
  public StylesBuilder cursor(Cursor value) {
    delegate.assertCanAddStyleProperty().setCursor(value);
    return this;
  }

  @Override
  public StylesBuilder display(Display value) {
    delegate.assertCanAddStyleProperty().setDisplay(value);
    return this;
  }

  @Override
  public void endStyle() {
    delegate.endStyle();
  }

  @Override
  public StylesBuilder floatprop(Float value) {
    delegate.assertCanAddStyleProperty().setFloat(value);
    return this;
  }

  @Override
  public StylesBuilder fontSize(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setFontSize(value, unit);
    return this;
  }

  @Override
  public StylesBuilder fontStyle(FontStyle value) {
    delegate.assertCanAddStyleProperty().setFontStyle(value);
    return this;
  }

  @Override
  public StylesBuilder fontWeight(FontWeight value) {
    delegate.assertCanAddStyleProperty().setFontWeight(value);
    return this;
  }

  @Override
  public StylesBuilder height(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setHeight(value, unit);
    return this;
  }

  @Override
  public StylesBuilder left(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setLeft(value, unit);
    return this;
  }

  @Override
  public StylesBuilder lineHeight(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setLineHeight(value, unit);
    return this;
  }

  @Override
  public StylesBuilder listStyleType(ListStyleType value) {
    delegate.assertCanAddStyleProperty().setListStyleType(value);
    return this;
  }

  @Override
  public StylesBuilder margin(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setMargin(value, unit);
    return this;
  }

  @Override
  public StylesBuilder marginBottom(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setMarginBottom(value, unit);
    return this;
  }

  @Override
  public StylesBuilder marginLeft(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setMarginLeft(value, unit);
    return this;
  }

  @Override
  public StylesBuilder marginRight(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setMarginRight(value, unit);
    return this;
  }

  @Override
  public StylesBuilder marginTop(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setMarginTop(value, unit);
    return this;
  }

  @Override
  public StylesBuilder opacity(double value) {
    delegate.assertCanAddStyleProperty().setOpacity(value);
    return this;
  }

  @Override
  public StylesBuilder outlineStyle(OutlineStyle value) {
    delegate.assertCanAddStyleProperty().setOutlineStyle(value);
    return this;
  }

  @Override
  public StylesBuilder outlineWidth(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setOutlineWidth(value, unit);
    return this;
  }

  @Override
  public StylesBuilder overflow(Overflow value) {
    delegate.assertCanAddStyleProperty().setOverflow(value);
    return this;
  }

  @Override
  public StylesBuilder overflowX(Overflow value) {
    delegate.assertCanAddStyleProperty().setOverflowX(value);
    return this;
  }

  @Override
  public StylesBuilder overflowY(Overflow value) {
    delegate.assertCanAddStyleProperty().setOverflowY(value);
    return this;
  }

  @Override
  public StylesBuilder padding(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setPadding(value, unit);
    return this;
  }

  @Override
  public StylesBuilder paddingBottom(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setPaddingBottom(value, unit);
    return this;
  }

  @Override
  public StylesBuilder paddingLeft(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setPaddingLeft(value, unit);
    return this;
  }

  @Override
  public StylesBuilder paddingRight(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setPaddingRight(value, unit);
    return this;
  }

  @Override
  public StylesBuilder paddingTop(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setPaddingTop(value, unit);
    return this;
  }

  @Override
  public StylesBuilder position(Position value) {
    delegate.assertCanAddStyleProperty().setPosition(value);
    return this;
  }

  @Override
  public StylesBuilder right(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setRight(value, unit);
    return this;
  }

  @Override
  public StylesBuilder tableLayout(TableLayout value) {
    delegate.assertCanAddStyleProperty().setTableLayout(value);
    return this;
  }

  @Override
  public StylesBuilder textAlign(TextAlign value) {
    delegate.assertCanAddStyleProperty().setTextAlign(value);
    return this;
  }

  @Override
  public StylesBuilder textDecoration(TextDecoration value) {
    delegate.assertCanAddStyleProperty().setTextDecoration(value);
    return this;
  }

  @Override
  public StylesBuilder textIndent(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setTextIndent(value, unit);
    return this;
  }

  @Override
  public StylesBuilder textJustify(TextJustify value) {
    delegate.assertCanAddStyleProperty().setTextJustify(value);
    return this;
  }

  @Override
  public StylesBuilder textOverflow(TextOverflow value) {
    delegate.assertCanAddStyleProperty().setTextOverflow(value);
    return this;
  }

  @Override
  public StylesBuilder textTransform(TextTransform value) {
    delegate.assertCanAddStyleProperty().setTextTransform(value);
    return this;
  }

  @Override
  public StylesBuilder top(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setTop(value, unit);
    return this;
  }

  @Override
  public StylesBuilder trustedBackgroundColor(String value) {
    delegate.assertCanAddStyleProperty().setBackgroundColor(value);
    return this;
  }

  @Override
  public StylesBuilder trustedBackgroundImage(String value) {
    delegate.assertCanAddStyleProperty().setBackgroundImage(value);
    return this;
  }

  @Override
  public StylesBuilder trustedBorderColor(String value) {
    delegate.assertCanAddStyleProperty().setBorderColor(value);
    return this;
  }

  @Override
  public StylesBuilder trustedColor(String value) {
    delegate.assertCanAddStyleProperty().setColor(value);
    return this;
  }

  @Override
  public StylesBuilder trustedOutlineColor(String value) {
    delegate.assertCanAddStyleProperty().setOutlineColor(value);
    return this;
  }

  @Override
  public StylesBuilder trustedProperty(String name, double value, Unit unit) {
    name = toCamelCaseForm(name);
    delegate.assertCanAddStyleProperty().setProperty(name, value, unit);
    return this;
  }

  @Override
  public StylesBuilder trustedProperty(String name, String value) {
    name = toCamelCaseForm(name);
    delegate.assertCanAddStyleProperty().setProperty(name, value);
    return this;
  }

  @Override
  public StylesBuilder verticalAlign(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setVerticalAlign(value, unit);
    return this;
  }

  @Override
  public StylesBuilder verticalAlign(VerticalAlign value) {
    delegate.assertCanAddStyleProperty().setVerticalAlign(value);
    return this;
  }

  @Override
  public StylesBuilder visibility(Visibility value) {
    delegate.assertCanAddStyleProperty().setVisibility(value);
    return this;
  }

  @Override
  public StylesBuilder width(double value, Unit unit) {
    delegate.assertCanAddStyleProperty().setWidth(value, unit);
    return this;
  }

  @Override
  public StylesBuilder zIndex(int value) {
    delegate.assertCanAddStyleProperty().setZIndex(value);
    return this;
  }
}
