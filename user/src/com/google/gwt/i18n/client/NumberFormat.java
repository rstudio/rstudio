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
package com.google.gwt.i18n.client;

import com.google.gwt.i18n.client.constants.NumberConstants;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Formats and parses numbers using locale-sensitive patterns.
 *
 * This class provides comprehensive and flexible support for a wide variety of
 * localized formats, including
 * <ul>
 * <li><b>Locale-specific symbols</b> such as decimal point, group separator,
 * digit representation, currency symbol, percent, and permill</li>
 * <li><b>Numeric variations</b> including integers ("123"), fixed-point
 * numbers ("123.4"), scientific notation ("1.23E4"), percentages ("12%"), and
 * currency amounts ("$123")</li>
 * <li><b>Predefined standard patterns</b> that can be used both for parsing
 * and formatting, including {@link #getDecimalFormat() decimal},
 * {@link #getCurrencyFormat() currency},
 * {@link #getPercentFormat() percentages}, and
 * {@link #getScientificFormat() scientific}</li>
 * <li><b>Custom patterns</b> and supporting features designed to make it
 * possible to parse and format numbers in any locale, including support for
 * Western, Arabic, and Indic digits</li>
 * </ul>
 *
 * <h3>Patterns</h3>
 * <p>
 * Formatting and parsing are based on customizable patterns that can include a
 * combination of literal characters and special characters that act as
 * placeholders and are replaced by their localized counterparts. Many
 * characters in a pattern are taken literally; they are matched during parsing
 * and output unchanged during formatting. Special characters, on the other
 * hand, stand for other characters, strings, or classes of characters. For
 * example, the '<code>#</code>' character is replaced by a localized digit.
 * </p>
 *
 * <p>
 * Often the replacement character is the same as the pattern character. In the
 * U.S. locale, for example, the '<code>,</code>' grouping character is
 * replaced by the same character '<code>,</code>'. However, the replacement
 * is still actually happening, and in a different locale, the grouping
 * character may change to a different character, such as '<code>.</code>'.
 * Some special characters affect the behavior of the formatter by their
 * presence. For example, if the percent character is seen, then the value is
 * multiplied by 100 before being displayed.
 * </p>
 *
 * <p>
 * The characters listed below are used in patterns. Localized symbols use the
 * corresponding characters taken from corresponding locale symbol collection,
 * which can be found in the properties files residing in the
 * <code><nobr>com.google.gwt.i18n.client.constants</nobr></code>. To insert
 * a special character in a pattern as a literal (that is, without any special
 * meaning) the character must be quoted. There are some exceptions to this
 * which are noted below.
 * </p>
 *
 * <table>
 * <tr>
 * <th>Symbol</th>
 * <th>Location</th>
 * <th>Localized?</th>
 * <th>Meaning</th>
 * </tr>
 *
 * <tr>
 * <td><code>0</code></td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Digit</td>
 * </tr>
 *
 * <tr>
 * <td><code>#</code></td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Digit, zero shows as absent</td>
 * </tr>
 *
 * <tr>
 * <td><code>.</code></td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Decimal separator or monetary decimal separator</td>
 * </tr>
 *
 * <tr>
 * <td><code>-</code></td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Minus sign</td>
 * </tr>
 *
 * <tr>
 * <td><code>,</code></td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Grouping separator</td>
 * </tr>
 *
 * <tr>
 * <td><code>E</code></td>
 * <td>Number</td>
 * <td>Yes</td>
 * <td>Separates mantissa and exponent in scientific notation; need not be
 * quoted in prefix or suffix</td>
 * </tr>
 *
 * <tr>
 * <td><code>;</code></td>
 * <td>Subpattern boundary</td>
 * <td>Yes</td>
 * <td>Separates positive and negative subpatterns</td>
 * </tr>
 *
 * <tr>
 * <td><code>%</code></td>
 * <td>Prefix or suffix</td>
 * <td>Yes</td>
 * <td>Multiply by 100 and show as percentage</td>
 * </tr>
 *
 * <tr>
 * <td><nobr><code>\u2030</code> (\u005Cu2030)</nobr></td>
 * <td>Prefix or suffix</td>
 * <td>Yes</td>
 * <td>Multiply by 1000 and show as per mille</td>
 * </tr>
 *
 * <tr>
 * <td><nobr><code>\u00A4</code> (\u005Cu00A4)</nobr></td>
 * <td>Prefix or suffix</td>
 * <td>No</td>
 * <td>Currency sign, replaced by currency symbol; if doubled, replaced by
 * international currency symbol; if present in a pattern, the monetary decimal
 * separator is used instead of the decimal separator</td>
 * </tr>
 *
 * <tr>
 * <td><code>'</code></td>
 * <td>Prefix or suffix</td>
 * <td>No</td>
 * <td>Used to quote special characters in a prefix or suffix; for example,
 * <code>"'#'#"</code> formats <code>123</code> to <code>"#123"</code>;
 * to create a single quote itself, use two in succession, such as
 * <code>"# o''clock"</code></td>
 * </tr>
 *
 * </table>
 *
 * <p>
 * A <code>NumberFormat</code> pattern contains a postive and negative
 * subpattern separated by a semicolon, such as
 * <code>"#,##0.00;(#,##0.00)"</code>. Each subpattern has a prefix, a
 * numeric part, and a suffix. If there is no explicit negative subpattern, the
 * negative subpattern is the localized minus sign prefixed to the positive
 * subpattern. That is, <code>"0.00"</code> alone is equivalent to
 * <code>"0.00;-0.00"</code>. If there is an explicit negative subpattern, it
 * serves only to specify the negative prefix and suffix; the number of digits,
 * minimal digits, and other characteristics are ignored in the negative
 * subpattern. That means that <code>"#,##0.0#;(#)"</code> has precisely the
 * same result as <code>"#,##0.0#;(#,##0.0#)"</code>.
 * </p>
 *
 * <p>
 * The prefixes, suffixes, and various symbols used for infinity, digits,
 * thousands separators, decimal separators, etc. may be set to arbitrary
 * values, and they will appear properly during formatting. However, care must
 * be taken that the symbols and strings do not conflict, or parsing will be
 * unreliable. For example, the decimal separator and thousands separator should
 * be distinct characters, or parsing will be impossible.
 * </p>
 *
 * <p>
 * The grouping separator is a character that separates clusters of integer
 * digits to make large numbers more legible. It commonly used for thousands,
 * but in some locales it separates ten-thousands. The grouping size is the
 * number of digits between the grouping separators, such as 3 for "100,000,000"
 * or 4 for "1 0000 0000".
 * </p>
 *
 * <h3>Pattern Grammar (BNF)</h3>
 * <p>
 * The pattern itself uses the following grammar:
 * </p>
 *
 * <table>
 * <tr>
 * <td>pattern</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">subpattern ('<code>;</code>'
 * subpattern)?</td>
 * </tr>
 * <tr>
 * <td>subpattern</td>
 * <td>:=</td>
 * <td>prefix? number exponent? suffix?</td>
 * </tr>
 * <tr>
 * <td>number</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">(integer ('<code>.</code>' fraction)?) |
 * sigDigits</td>
 * </tr>
 * <tr>
 * <td>prefix</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">'<code>\u005Cu0000</code>'..'<code>\u005CuFFFD</code>' -
 * specialCharacters</td>
 * </tr>
 * <tr>
 * <td>suffix</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">'<code>\u005Cu0000</code>'..'<code>\u005CuFFFD</code>' -
 * specialCharacters</td>
 * </tr>
 * <tr>
 * <td>integer</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">'<code>#</code>'* '<code>0</code>'*'<code>0</code>'</td>
 * </tr>
 * <tr>
 * <td>fraction</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">'<code>0</code>'* '<code>#</code>'*</td>
 * </tr>
 * <tr>
 * <td>sigDigits</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">'<code>#</code>'* '<code>@</code>''<code>@</code>'* '<code>#</code>'*</td>
 * </tr>
 * <tr>
 * <td>exponent</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">'<code>E</code>' '<code>+</code>'? '<code>0</code>'* '<code>0</code>'</td>
 * </tr>
 * <tr>
 * <td>padSpec</td>
 * <td>:=</td>
 * <td style="white-space: nowrap">'<code>*</code>' padChar</td>
 * </tr>
 * <tr>
 * <td>padChar</td>
 * <td>:=</td>
 * <td>'<code>\u005Cu0000</code>'..'<code>\u005CuFFFD</code>' - quote</td>
 * </tr>
 * </table>
 *
 * <p>
 * Notation:
 * </p>
 *
 * <table>
 * <tr>
 * <td>X*</td>
 * <td style="white-space: nowrap">0 or more instances of X</td>
 * </tr>
 *
 * <tr>
 * <td>X?</td>
 * <td style="white-space: nowrap">0 or 1 instances of X</td>
 * </tr>
 *
 * <tr>
 * <td>X|Y</td>
 * <td style="white-space: nowrap">either X or Y</td>
 * </tr>
 *
 * <tr>
 * <td>C..D</td>
 * <td style="white-space: nowrap">any character from C up to D, inclusive</td>
 * </tr>
 *
 * <tr>
 * <td>S-T</td>
 * <td style="white-space: nowrap">characters in S, except those in T</td>
 * </tr>
 * </table>
 *
 * <p>
 * The first subpattern is for positive numbers. The second (optional)
 * subpattern is for negative numbers.
 * </p>
 *
 *  <h3>Example</h3> {@example com.google.gwt.examples.NumberFormatExample}
 *
 *
 */
public class NumberFormat {

  // Sets of constants as defined for the current locale from CLDR.
  protected static final NumberConstants localizedNumberConstants = LocaleInfo.getCurrentLocale().getNumberConstants();

  /**
   * Current NumberConstants interface to use, see
   * {@link #setForcedLatinDigits(boolean)} for changing it.
   */
  protected static NumberConstants defaultNumberConstants = localizedNumberConstants;

  // Cached instances of standard formatters.
  private static NumberFormat cachedCurrencyFormat;
  private static NumberFormat cachedDecimalFormat;
  private static NumberFormat cachedPercentFormat;
  private static NumberFormat cachedScientificFormat;

  // Constants for characters used in programmatic (unlocalized) patterns.
  private static final char CURRENCY_SIGN = '\u00A4';

  // Number constants mapped to use latin digits/separators.
  private static NumberConstants latinNumberConstants = null;
  // Localized characters for dot and comma in number patterns, used to produce
  // the latin mapping for arbitrary locales.  Any separator not in either of
  // these strings will be mapped to non-breaking space (U+00A0).
  private static final String LOCALIZED_COMMA_EQUIVALENTS = ",\u060C\u066B\u3001\uFE10\uFE11\uFE50\uFE51\uFF0C\uFF64";

  private static final String LOCALIZED_DOT_EQUIVALENTS = ".\u2024\u3002\uFE12\uFE52\uFF0E\uFF61";
  private static final char PATTERN_DECIMAL_SEPARATOR = '.';
  private static final char PATTERN_DIGIT = '#';
  private static final char PATTERN_EXPONENT = 'E';
  private static final char PATTERN_GROUPING_SEPARATOR = ',';
  private static final char PATTERN_MINUS = '-';
  private static final char PATTERN_PER_MILLE = '\u2030';
  private static final char PATTERN_PERCENT = '%';
  private static final char PATTERN_SEPARATOR = ';';
  private static final char PATTERN_ZERO_DIGIT = '0';

  private static final char QUOTE = '\'';

  /**
   * Returns true if all new NumberFormat instances will use latin digits and
   * related characters rather than the localized ones.
   */
  public static boolean forcedLatinDigits() {
    return defaultNumberConstants != localizedNumberConstants;
  }

  /**
   * Provides the standard currency format for the current locale.
   *
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the default locale
   */
  public static NumberFormat getCurrencyFormat() {
    if (cachedCurrencyFormat == null) {
      cachedCurrencyFormat = getCurrencyFormat(CurrencyList.get().getDefault());
    }
    return cachedCurrencyFormat;
  }

  /**
   * Provides the standard currency format for the current locale using a
   * specified currency.
   *
   * @param currencyData currency data to use
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   */
  public static NumberFormat getCurrencyFormat(CurrencyData currencyData) {
    return new NumberFormat(defaultNumberConstants.currencyPattern(),
        currencyData, false);
  }

  /**
   * Provides the standard currency format for the current locale using a
   * specified currency.
   *
   * @param currencyCode valid currency code, as defined in
   *     com.google.gwt.i18n.client.constants.CurrencyCodeMapConstants.properties
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   * @throws IllegalArgumentException if the currency code is unknown
   */
  public static NumberFormat getCurrencyFormat(String currencyCode) {
    return getCurrencyFormat(lookupCurrency(currencyCode));
  }


  /**
   * Provides the standard decimal format for the default locale.
   *
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         decimal format for the default locale
   */
  public static NumberFormat getDecimalFormat() {
    if (cachedDecimalFormat == null) {
      cachedDecimalFormat = new NumberFormat(
          defaultNumberConstants.decimalPattern(),
          CurrencyList.get().getDefault(), false);
    }
    return cachedDecimalFormat;
  }

  /**
   * Gets a <code>NumberFormat</code> instance for the default locale using
   * the specified pattern and the default currencyCode.
   *
   * @param pattern pattern for this formatter
   * @return a NumberFormat instance
   * @throws IllegalArgumentException if the specified pattern is invalid
   */
  public static NumberFormat getFormat(String pattern) {
    return new NumberFormat(pattern, CurrencyList.get().getDefault(), true);
  }

  /**
   * Gets a custom <code>NumberFormat</code> instance for the default locale
   * using the specified pattern and currency code.
   *
   * @param pattern pattern for this formatter
   * @param currencyData currency data
   * @return a NumberFormat instance
   * @throws IllegalArgumentException if the specified pattern is invalid
   */
  public static NumberFormat getFormat(String pattern,
      CurrencyData currencyData) {
    return new NumberFormat(pattern, currencyData, true);
  }

  /**
   * Gets a custom <code>NumberFormat</code> instance for the default locale
   * using the specified pattern and currency code.
   *
   * @param pattern pattern for this formatter
   * @param currencyCode international currency code
   * @return a NumberFormat instance
   * @throws IllegalArgumentException if the specified pattern is invalid
   *     or the currency code is unknown
   */
  public static NumberFormat getFormat(String pattern, String currencyCode) {
    return new NumberFormat(pattern, lookupCurrency(currencyCode), true);
  }

  /**
   * Provides the global currency format for the current locale, using its
   * default currency.
   * 
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   */
  public static NumberFormat getGlobalCurrencyFormat() {
    return getGlobalCurrencyFormat(CurrencyList.get().getDefault());
  }

  /**
   * Provides the global currency format for the current locale, using a
   * specified currency.
   *
   * @param currencyData currency data to use
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   */
  public static NumberFormat getGlobalCurrencyFormat(CurrencyData currencyData) {
    return new NumberFormat(defaultNumberConstants.globalCurrencyPattern(),
        currencyData, false);
  }
  
  /**
   * Provides the global currency format for the current locale, using a
   * specified currency.
   *
   * @param currencyCode valid currency code, as defined in
   *     com.google.gwt.i18n.client.constants.CurrencyCodeMapConstants.properties
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   * @throws IllegalArgumentException if the currency code is unknown
   */
  public static NumberFormat getGlobalCurrencyFormat(String currencyCode) {
    return getGlobalCurrencyFormat(lookupCurrency(currencyCode));
  }

  /**
   * Provides the standard percent format for the default locale.
   *
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         percent format for the default locale
   */
  public static NumberFormat getPercentFormat() {
    if (cachedPercentFormat == null) {
      cachedPercentFormat = new NumberFormat(
          defaultNumberConstants.percentPattern(),
          CurrencyList.get().getDefault(), false);
    }
    return cachedPercentFormat;
  }

  /**
   * Provides the standard scientific format for the default locale.
   *
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         scientific format for the default locale
   */
  public static NumberFormat getScientificFormat() {
    if (cachedScientificFormat == null) {
      cachedScientificFormat = new NumberFormat(
          defaultNumberConstants.scientificPattern(),
          CurrencyList.get().getDefault(), false);
    }
    return cachedScientificFormat;
  }

  /**
   * Provides the simple currency format for the current locale using its
   * default currency. Note that these formats may be ambiguous if the
   * currency isn't clear from other content on the page.
   *
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   */
  public static NumberFormat getSimpleCurrencyFormat() {
    return getSimpleCurrencyFormat(CurrencyList.get().getDefault());
  }

  /**
   * Provides the simple currency format for the current locale using a
   * specified currency. Note that these formats may be ambiguous if the
   * currency isn't clear from other content on the page.
   *
   * @param currencyData currency data to use
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   */
  public static NumberFormat getSimpleCurrencyFormat(CurrencyData currencyData) {
    return new NumberFormat(defaultNumberConstants.simpleCurrencyPattern(),
        currencyData, false);
  }

  /**
   * Provides the simple currency format for the current locale using a
   * specified currency. Note that these formats may be ambiguous if the
   * currency isn't clear from other content on the page.
   * 
   * @param currencyCode valid currency code, as defined in
   *        com.google.gwt.i18n.client
   *        .constants.CurrencyCodeMapConstants.properties
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the current locale
   * @throws IllegalArgumentException if the currency code is unknown
   */
  public static NumberFormat getSimpleCurrencyFormat(String currencyCode) {
    return getSimpleCurrencyFormat(lookupCurrency(currencyCode));
  }

  /**
   * Specify whether all new NumberFormat instances will use latin digits
   * and related characters rather than the localized ones.
   *
   * @param useLatinDigits true if latin digits/etc should be used, false if
   *    localized digits/etc should be used.
   */
  public static void setForcedLatinDigits(boolean useLatinDigits) {
    // Invalidate cached formats if changing
    if (useLatinDigits != forcedLatinDigits()) {
      cachedCurrencyFormat = null;
      cachedDecimalFormat = null;
      cachedPercentFormat = null;
      cachedScientificFormat = null;
    }
    if (useLatinDigits) {
      if (latinNumberConstants == null) {
        latinNumberConstants = createLatinNumberConstants(
            localizedNumberConstants);
      }
      defaultNumberConstants = latinNumberConstants;
    } else {
      defaultNumberConstants = localizedNumberConstants;
    }
  }

  /**
   * Create a delocalized NumberConstants instance from a localized one.
   *
   * @param orig localized NumberConstants instance
   * @return NumberConstants instance using latin digits/etc
   */
  protected static NumberConstants createLatinNumberConstants(
      final NumberConstants orig) {
    final String groupingSeparator = remapSeparator(
        orig.groupingSeparator());
    final String decimalSeparator = remapSeparator(
        orig.decimalSeparator());
    final String monetaryGroupingSeparator = remapSeparator(
        orig.monetaryGroupingSeparator());
    final String monetarySeparator = remapSeparator(
        orig.monetarySeparator());
    return new NumberConstants() {
      @Override
      public String currencyPattern() {
        return orig.currencyPattern();
      }

      @Override
      public String decimalPattern() {
        return orig.decimalPattern();
      }

      @Override
      public String decimalSeparator() {
        return decimalSeparator;
      }

      @Override
      public String defCurrencyCode() {
        return orig.defCurrencyCode();
      }

      @Override
      public String exponentialSymbol() {
        return orig.exponentialSymbol();
      }

      @Override
      public String globalCurrencyPattern() {
        return orig.globalCurrencyPattern();
      }

      @Override
      public String groupingSeparator() {
        return groupingSeparator;
      }

      @Override
      public String infinity() {
        return orig.infinity();
      }

      @Override
      public String minusSign() {
        return orig.minusSign();
      }

      @Override
      public String monetaryGroupingSeparator() {
        return monetaryGroupingSeparator;
      }

      @Override
      public String monetarySeparator() {
        return monetarySeparator;
      }

      @Override
      public String notANumber() {
        return orig.notANumber();
      }

      @Override
      public String percent() {
        return orig.percent();
      }

      @Override
      public String percentPattern() {
        return orig.percentPattern();
      }

      @Override
      public String perMill() {
        return orig.perMill();
      }

      @Override
      public String plusSign() {
        return orig.plusSign();
      }

      @Override
      public String scientificPattern() {
        return orig.scientificPattern();
      }

      @Override
      public String simpleCurrencyPattern() {
        return orig.simpleCurrencyPattern();
      }

      @Override
      public String zeroDigit() {
        return "0";
      }
    };
  }

  /**
   * Remap a localized separator to an equivalent latin one.
   *
   * @param separator
   * @return delocalized separator character
   */
  protected static String remapSeparator(String separator) {
    char ch = separator.length() > 0 ? separator.charAt(0) : 0xFFFF;
    if (LOCALIZED_DOT_EQUIVALENTS.indexOf(ch) >= 0) {
      return ".";
    }
    if (LOCALIZED_COMMA_EQUIVALENTS.indexOf(ch) >= 0) {
      return ",";
    }
    return "\u00A0";
  }

  /**
   * Appends a scaled string representation to a buffer, returning the scale
   * (which is the number of places to the right of the end of the string the
   * decimal point should be moved -- i.e., 3.5 would be added to the buffer
   * as "35" and a returned scale of -1).
   *
   * @param buf
   * @param val
   * @return scale to apply to the result
   */
  // @VisibleForTesting
  static int toScaledString(StringBuilder buf, double val) {
    int startLen = buf.length();
    buf.append(toPrecision(val, 20));
    int scale = 0;

    // remove exponent if present, adjusting scale
    int expIdx = buf.indexOf("e", startLen);
    if (expIdx < 0) {
      expIdx = buf.indexOf("E", startLen);
    }
    if (expIdx >= 0) {
      int expDigits = expIdx + 1;
      if (expDigits < buf.length() && buf.charAt(expDigits) == '+') {
        ++expDigits;
      }
      if (expDigits < buf.length()) {
        scale = Integer.parseInt(buf.substring(expDigits));
      }
      buf.delete(expIdx, buf.length());
    }

    // remove decimal point if present, adjusting scale
    int dot = buf.indexOf(".", startLen);
    if (dot >= 0) {
      buf.deleteCharAt(dot);
      scale -= buf.length() - dot;
    }
    return scale;
  }

  /**
   * Lookup a currency code.
   *
   * @param currencyCode ISO4217 currency code
   * @return a CurrencyData instance
   * @throws IllegalArgumentException if the currency code is unknown
   */
  private static CurrencyData lookupCurrency(String currencyCode) {
    CurrencyData currencyData = CurrencyList.get().lookup(currencyCode);
    if (currencyData == null) {
      throw new IllegalArgumentException("Currency code " + currencyCode
          + " is unkown in locale "
          + LocaleInfo.getCurrentLocale().getLocaleName());
    }
    return currencyData;
  }

  /**
   * Convert a double to a string with {@code digits} precision.  The resulting
   * string may still be in exponential notation.
   *
   * @param d double value
   * @param digits number of digits of precision to include
   * @return non-localized string representation of {@code d}
   */
  private static native String toPrecision(double d, int digits) /*-{
    return d.toPrecision(digits);
  }-*/;

  /**
   * Information about the currency being used.
   */
  private CurrencyData currencyData;

  /**
   * Holds the current decimal position during one call to
   * {@link #format(boolean, StringBuilder, int)}.
   */
  private transient int decimalPosition;

  /**
   * Forces the decimal separator to always appear in a formatted number.
   */
  private boolean decimalSeparatorAlwaysShown = false;

  /**
   * Holds the current digits length during one call to
   * {@link #format(boolean, StringBuilder, int)}.
   */
  private transient int digitsLength;

  /**
   * Holds the current exponent during one call to
   * {@link #format(boolean, StringBuilder, int)}.
   */
  private transient int exponent;
  /**
   * The number of digits between grouping separators in the integer portion of
   * a number.
   */
  private int groupingSize = 3;
  private boolean isCurrencyFormat = false;
  private int maximumFractionDigits = 3; // invariant, >= minFractionDigits.

  private int maximumIntegerDigits = 40;

  private int minExponentDigits;

  private int minimumFractionDigits = 0;

  private int minimumIntegerDigits = 1;

  // The multiplier for use in percent, per mille, etc.
  private int multiplier = 1;

  private String negativePrefix = "-";

  private String negativeSuffix = "";

  // Locale specific symbol collection.
  private final NumberConstants numberConstants;

  // The pattern to use for formatting and parsing.
  private final String pattern;

  private String positivePrefix = "";

  private String positiveSuffix = "";

  // True to force the use of exponential (i.e. scientific) notation.
  private boolean useExponentialNotation = false;

  /**
   * Constructs a format object based on the specified settings.
   *
   * @param numberConstants the locale-specific number constants to use for this
   *          format -- **NOTE** subclasses passing their own instance here
   *          should pay attention to {@link #forcedLatinDigits()} and remap
   *          localized symbols using
   *          {@link #createLatinNumberConstants(NumberConstants)}
   * @param pattern pattern that specify how number should be formatted
   * @param cdata currency data that should be used
   * @param userSuppliedPattern true if the pattern was supplied by the user
   */
  protected NumberFormat(NumberConstants numberConstants, String pattern, CurrencyData cdata,
      boolean userSuppliedPattern) {
    if (cdata == null) {
      throw new IllegalArgumentException("Unknown currency code");
    }
    this.numberConstants = numberConstants;
    this.pattern = pattern;
    currencyData = cdata;

    // TODO: handle per-currency flags, such as symbol prefix/suffix and spacing
    parsePattern(this.pattern);
    if (!userSuppliedPattern && isCurrencyFormat) {
      minimumFractionDigits = currencyData.getDefaultFractionDigits();
      maximumFractionDigits = minimumFractionDigits;
    }
  }

  /**
   * Constructs a format object for the default locale based on the specified
   * settings.
   *
   * @param pattern pattern that specify how number should be formatted
   * @param cdata currency data that should be used
   * @param userSuppliedPattern true if the pattern was supplied by the user
   */
  protected NumberFormat(String pattern, CurrencyData cdata, boolean userSuppliedPattern) {
    this(defaultNumberConstants, pattern, cdata, userSuppliedPattern);
  }

  /**
   * This method formats a double to produce a string.
   *
   * @param number The double to format
   * @return the formatted number string
   */
  public String format(double number) {
    if (Double.isNaN(number)) {
      return numberConstants.notANumber();
    }
    boolean isNegative = ((number < 0.0)
        || (number == 0.0 && 1 / number < 0.0));
    if (isNegative) {
      number = -number;
    }
    StringBuilder buf = new StringBuilder();
    if (Double.isInfinite(number)) {
      buf.append(isNegative ? negativePrefix : positivePrefix);
      buf.append(numberConstants.infinity());
      buf.append(isNegative ? negativeSuffix : positiveSuffix);
      return buf.toString();
    }
    number *= multiplier;
    int scale = toScaledString(buf, number);

    // pre-round value to deal with .15 being represented as .149999... etc
    // check at 3 more digits than will be required in the output
    int preRound = buf.length() + scale + maximumFractionDigits + 3;
    if (preRound > 0 && preRound < buf.length()
        && buf.charAt(preRound) == '9') {
      propagateCarry(buf, preRound - 1);
      scale += buf.length() - preRound;
      buf.delete(preRound, buf.length());
    }

    format(isNegative, buf, scale);
    return buf.toString();
  }

  /**
   * This method formats a Number to produce a string.
   * <p>
   * Any {@link Number} which is not a {@link BigDecimal}, {@link BigInteger},
   * or {@link Long} instance is formatted as a {@code double} value.
   *
   * @param number The Number instance to format
   * @return the formatted number string
   */
  public String format(Number number) {
    if (number instanceof BigDecimal) {
      BigDecimal bigDec = (BigDecimal) number;
      boolean isNegative = bigDec.signum() < 0;
      if (isNegative) {
        bigDec = bigDec.negate();
      }
      bigDec = bigDec.multiply(BigDecimal.valueOf(multiplier));
      StringBuilder buf = new StringBuilder();
      buf.append(bigDec.unscaledValue().toString());
      format(isNegative, buf, -bigDec.scale());
      return buf.toString();
    } else if (number instanceof BigInteger) {
      BigInteger bigInt = (BigInteger) number;
      boolean isNegative = bigInt.signum() < 0;
      if (isNegative) {
        bigInt = bigInt.negate();
      }
      bigInt = bigInt.multiply(BigInteger.valueOf(multiplier));
      StringBuilder buf = new StringBuilder();
      buf.append(bigInt.toString());
      format(isNegative, buf, 0);
      return buf.toString();
    } else if (number instanceof Long) {
      return format(number.longValue(), 0);
    } else {
      return format(number.doubleValue());
    }
  }

  /**
   * Returns the pattern used by this number format.
   */
  public String getPattern() {
    return pattern;
  }

  /**
   * Change the number of fractional digits used for formatting with this
   * instance.
   * 
   * @param digits the exact number of fractional digits for formatted
   *     values; must be >= 0
   * @return {@code this}, for chaining purposes
   */
  public NumberFormat overrideFractionDigits(int digits) {
    return overrideFractionDigits(digits, digits);
  }

  /**
   * Change the number of fractional digits used for formatting with this
   * instance. Digits after {@code minDigits} that are zero will be omitted from
   * the formatted value.
   * 
   * @param minDigits the minimum number of fractional digits for formatted
   *     values; must be >= 0
   * @param maxDigits the maximum number of fractional digits for formatted
   *     values; must be >= {@code minDigits}
   * @return {@code this}, for chaining purposes
   */
  public NumberFormat overrideFractionDigits(int minDigits, int maxDigits) {
    assert minDigits >= 0;
    assert maxDigits >= minDigits;
    minimumFractionDigits = minDigits;
    maximumFractionDigits = maxDigits;
    return this;
  }

  /**
   * Parses text to produce a numeric value. A {@link NumberFormatException} is
   * thrown if either the text is empty or if the parse does not consume all
   * characters of the text.
   *
   * @param text the string being parsed
   * @return a double value representing the parsed number
   * @throws NumberFormatException if the entire text could not be converted
   *     into a double
   */
  public double parse(String text) throws NumberFormatException {
    int[] pos = {0};
    double result = parse(text, pos);
    if (pos[0] == 0 || pos[0] != text.length()) {
      throw new NumberFormatException(text);
    }
    return result;
  }

  /**
   * Parses text to produce a numeric value.
   *
   * <p>
   * The method attempts to parse text starting at the index given by pos. If
   * parsing succeeds, then the index of <code>pos</code> is updated to the
   * index after the last character used (parsing does not necessarily use all
   * characters up to the end of the string), and the parsed number is returned.
   * The updated <code>pos</code> can be used to indicate the starting point
   * for the next call to this method. If an error occurs, then the index of
   * <code>pos</code> is not changed.
   * </p>
   *
   * @param text the string to be parsed
   * @param inOutPos position to pass in and get back
   * @return a double value representing the parsed number
   * @throws NumberFormatException if the text segment could not be converted
   *     into a double
   */
  public double parse(String text, int[] inOutPos) throws NumberFormatException {
    double ret = 0.0;

    boolean gotPositivePrefix = text.startsWith(positivePrefix, inOutPos[0]);
    boolean gotNegativePrefix = text.startsWith(negativePrefix, inOutPos[0]);
    boolean gotPositiveSuffix = text.endsWith(positiveSuffix);
    boolean gotNegativeSuffix = text.endsWith(negativeSuffix);
    boolean gotPositive = gotPositivePrefix && gotPositiveSuffix;
    boolean gotNegative = gotNegativePrefix && gotNegativeSuffix;

    // Handle conflicts where we get both patterns, which usually
    // happens when one is a prefix of the other (such as the positive
    // pattern having empty prefix/suffixes).
    if (gotPositive && gotNegative) {
      if (positivePrefix.length() > negativePrefix.length()) {
        gotNegative = false;
      } else if (positivePrefix.length() < negativePrefix.length()) {
        gotPositive = false;
      } else if (positiveSuffix.length() > negativeSuffix.length()) {
        gotNegative = false;
      } else if (positiveSuffix.length() < negativeSuffix.length()) {
        gotPositive = false;
      } else {
        // can't tell patterns apart, must be positive
        gotNegative = false;
      }
    } else if (!gotPositive && !gotNegative) {
      throw new NumberFormatException(text
          + " does not have either positive or negative affixes");
    }

    // Contains just the value to parse, stripping any prefix or suffix
    String valueOnly = null;
    if (gotPositive) {
      inOutPos[0] += positivePrefix.length();
      valueOnly = text.substring(inOutPos[0],
          text.length() - positiveSuffix.length());
    } else {
      inOutPos[0] += negativePrefix.length();
      valueOnly = text.substring(inOutPos[0],
          text.length() - negativeSuffix.length());
    }

    // Process digits or special values, and find decimal position.
    if (valueOnly.equals(numberConstants.infinity())) {
      inOutPos[0] += numberConstants.infinity().length();
      ret = Double.POSITIVE_INFINITY;
    } else if (valueOnly.equals(numberConstants.notANumber())) {
      inOutPos[0] += numberConstants.notANumber().length();
      ret = Double.NaN;
    } else {
      int[] tempPos = {0};
      ret = parseNumber(valueOnly, tempPos);
      inOutPos[0] += tempPos[0];
    }

    // Check for suffix.
    if (gotPositive) {
      inOutPos[0] += positiveSuffix.length();
    } else if (gotNegative) {
      inOutPos[0] += negativeSuffix.length();
    }

    if (gotNegative) {
      ret = -ret;
    }

    return ret;
  }

  /**
   * Format a number with its significant digits already represented in string
   * form.  This is done so both double and BigInteger/Decimal formatting can
   * share code without requiring all users to pay the code size penalty for
   * BigDecimal/etc.
   * <p>
   * Example values passed in:
   * <ul>
   * <li>-13e2
   * <br>{@code isNegative=true, digits="13", scale=2}
   * <li>3.14158
   * <br>{@code isNegative=false, digits="314158", scale=-5}
   * <li>.0001
   * <br>{@code isNegative=false, digits="1" ("0001" would be ok), scale=-4}
   * </ul>
   *
   * @param isNegative true if the value to be formatted is negative
   * @param digits a StringBuilder containing just the significant digits in
   *     the value to be formatted, the formatted result will be left here
   * @param scale the number of places to the right the decimal point should
   *     be moved in the digit string -- negative means the value contains
   *     fractional digits
   */
  protected void format(boolean isNegative, StringBuilder digits, int scale) {
    char decimalSeparator;
    char groupingSeparator;
    if (isCurrencyFormat) {
      decimalSeparator = numberConstants.monetarySeparator().charAt(0);
      groupingSeparator = numberConstants.monetaryGroupingSeparator().charAt(0);
    } else {
      decimalSeparator = numberConstants.decimalSeparator().charAt(0);
      groupingSeparator = numberConstants.groupingSeparator().charAt(0);
    }

    // Set these transient fields, which will be adjusted/used by the routines
    // called in this method.
    exponent = 0;
    digitsLength = digits.length();
    decimalPosition = digitsLength + scale;

    boolean useExponent = this.useExponentialNotation;
    int currentGroupingSize = this.groupingSize;
    if (decimalPosition > 1024) {
      // force really large numbers to be in exponential form
      useExponent = true;
    }

    if (useExponent) {
      computeExponent(digits);
    }
    processLeadingZeros(digits);
    roundValue(digits);
    insertGroupingSeparators(digits, groupingSeparator, currentGroupingSize);
    adjustFractionDigits(digits);
    addZeroAndDecimal(digits, decimalSeparator);
    if (useExponent) {
      addExponent(digits);
      // the above call has invalidated digitsLength == digits.length()
    }
    char zeroChar = numberConstants.zeroDigit().charAt(0);
    if (zeroChar != '0') {
      localizeDigits(digits, zeroChar);
    }

    // add prefix/suffix
    digits.insert(0, isNegative ? negativePrefix : positivePrefix);
    digits.append(isNegative ? negativeSuffix : positiveSuffix);
  }

  /**
   * Parses text to produce a numeric value. A {@link NumberFormatException} is
   * thrown if either the text is empty or if the parse does not consume all
   * characters of the text.
   *
   * param text the string to be parsed
   * return a parsed number value, which may be a Double, BigInteger, or
   *     BigDecimal
   * throws NumberFormatException if the text segment could not be converted
   *     into a number
   */
//  public Number parseBig(String text) throws NumberFormatException {
//    // TODO(jat): implement
//    return Double.valueOf(parse(text));
//  }

  /**
   * Parses text to produce a numeric value.
   *
   * <p>
   * The method attempts to parse text starting at the index given by pos. If
   * parsing succeeds, then the index of <code>pos</code> is updated to the
   * index after the last character used (parsing does not necessarily use all
   * characters up to the end of the string), and the parsed number is returned.
   * The updated <code>pos</code> can be used to indicate the starting point
   * for the next call to this method. If an error occurs, then the index of
   * <code>pos</code> is not changed.
   * </p>
   *
   * param text the string to be parsed
   * pparam inOutPos position to pass in and get back
   * return a parsed number value, which may be a Double, BigInteger, or
   *     BigDecimal
   * throws NumberFormatException if the text segment could not be converted
   *     into a number
   */
//  public Number parseBig(String text, int[] inOutPos)
//      throws NumberFormatException {
//    // TODO(jat): implement
//    return Double.valueOf(parse(text, inOutPos));
//  }

  /**
   * Format a possibly scaled long value.
   *
   * @param value value to format
   * @param scale the number of places to the right the decimal point should
   *     be moved in the digit string -- negative means the value contains
   *     fractional digits
   * @return formatted value
   */
  protected String format(long value, int scale) {
    boolean isNegative = value < 0;
    if (isNegative) {
      value = -value;
    }
    value *= multiplier;
    StringBuilder buf = new StringBuilder();
    buf.append(String.valueOf(value));
    format(isNegative, buf, scale);
    return buf.toString();
  }

  /**
   * Returns the number of digits between grouping separators in the integer
   * portion of a number.
   */
  protected int getGroupingSize() {
    return groupingSize;
  }

  /**
   * Returns the prefix to use for negative values.
   */
  protected String getNegativePrefix() {
    return negativePrefix;
  }

  /**
   * Returns the suffix to use for negative values.
   */
  protected String getNegativeSuffix() {
    return negativeSuffix;
  }

  /**
   * Returns the NumberConstants instance for this formatter.
   */
  protected NumberConstants getNumberConstants() {
    return numberConstants;
  }

  /**
   * Returns the prefix to use for positive values.
   */
  protected String getPositivePrefix() {
    return positivePrefix;
  }

  /**
   * Returns the suffix to use for positive values.
   */
  protected String getPositiveSuffix() {
    return positiveSuffix;
  }

  /**
   * Returns true if the decimal separator should always be shown.
   */
  protected boolean isDecimalSeparatorAlwaysShown() {
    return decimalSeparatorAlwaysShown;
  }

  /**
   * Add exponent suffix.
   *
   * @param digits
   */
  private void addExponent(StringBuilder digits) {
    digits.append(numberConstants.exponentialSymbol());
    if (exponent < 0) {
      exponent = -exponent;
      digits.append(numberConstants.minusSign());
    }
    String exponentDigits = String.valueOf(exponent);
    for (int i = exponentDigits.length(); i < minExponentDigits; ++i) {
      digits.append('0');
    }
    digits.append(exponentDigits);
  }

  /**
   * @param digits
   * @param decimalSeparator
   */
  private void addZeroAndDecimal(StringBuilder digits, char decimalSeparator) {
    // add zero and decimal point if required
    if (digitsLength == 0) {
      digits.insert(0, '0');
      ++decimalPosition;
      ++digitsLength;
    }
    if (decimalPosition < digitsLength || decimalSeparatorAlwaysShown) {
      digits.insert(decimalPosition, decimalSeparator);
      ++digitsLength;
    }
  }

  /**
   * Adjust the fraction digits, adding trailing zeroes if necessary or removing
   * excess trailing zeroes.
   *
   * @param digits
   */
  private void adjustFractionDigits(StringBuilder digits) {
    // adjust fraction digits as required
    int requiredDigits = decimalPosition + minimumFractionDigits;
    if (digitsLength < requiredDigits) {
      // add trailing zeros
      while (digitsLength < requiredDigits) {
        digits.append('0');
        ++digitsLength;
      }
    } else {
      // remove excess trailing zeros
      int toRemove = decimalPosition + maximumFractionDigits;
      if (toRemove > digitsLength) {
        toRemove = digitsLength;
      }
      while (toRemove > requiredDigits
          && digits.charAt(toRemove - 1) == '0') {
        --toRemove;
      }
      if (toRemove < digitsLength) {
        digits.delete(toRemove, digitsLength);
        digitsLength = toRemove;
      }
    }
  }

  /**
   * Compute the exponent to use and adjust decimal position if we are using
   * exponential notation.
   *
   * @param digits
   */
  private void computeExponent(StringBuilder digits) {
    // always trim leading zeros
    int strip = 0;
    while (strip < digitsLength - 1 && digits.charAt(strip) == '0') {
      ++strip;
    }
    if (strip > 0) {
      digits.delete(0, strip);
      digitsLength -= strip;
      exponent -= strip;
    }

    // decimal should wind up between minimum & maximumIntegerDigits
    if (maximumIntegerDigits > minimumIntegerDigits
        && maximumIntegerDigits > 0) {
      // in this case, the exponent should be a multiple of
      // maximumIntegerDigits and 1 <= decimal <= maximumIntegerDigits
      exponent += decimalPosition - 1;
      int remainder = exponent % maximumIntegerDigits;
      if (remainder < 0) {
        remainder += maximumIntegerDigits;
      }
      decimalPosition = remainder + 1;
      exponent -= remainder;
    } else {
      exponent += decimalPosition - minimumIntegerDigits;
      decimalPosition = minimumIntegerDigits;
    }

    // special-case 0 to have an exponent of 0
    if (digitsLength == 1 && digits.charAt(0) == '0') {
      exponent = 0;
      decimalPosition = minimumIntegerDigits;
    }
  }

  /**
   * This method return the digit that represented by current character, it
   * could be either '0' to '9', or a locale specific digit.
   *
   * @param ch character that represents a digit
   * @return the digit value
   */
  private int getDigit(char ch) {
    if ('0' <= ch && ch <= '0' + 9) {
      return (ch - '0');
    } else {
      char zeroChar = numberConstants.zeroDigit().charAt(0);
      return ((zeroChar <= ch && ch <= zeroChar + 9) ? (ch - zeroChar) : -1);
    }
  }

  /**
   * Insert grouping separators if needed.
   *
   * @param digits
   * @param groupingSeparator
   * @param g
   */
  private void insertGroupingSeparators(StringBuilder digits,
      char groupingSeparator, int g) {
    if (g > 0) {
      for (int i = g; i < decimalPosition; i += g + 1) {
        digits.insert(decimalPosition - i, groupingSeparator);
        ++decimalPosition;
        ++digitsLength;
      }
    }
  }

  /**
   * Replace locale-independent digits with locale-specific ones.
   *
   * @param digits StringBuilder containing formatted number
   * @param zero locale-specific zero character -- the rest of the digits must
   *     be consecutive
   */
  private void localizeDigits(StringBuilder digits, char zero) {
    // don't use digitsLength since we may have added an exponent
    int n = digits.length();
    for (int i = 0; i < n; ++i) {
      char ch = digits.charAt(i);
      if (ch >= '0' && ch <= '9') {
        digits.setCharAt(i, (char) (ch - '0' + zero));
      }
    }
  }

  /**
   * This method parses affix part of pattern.
   *
   * @param pattern pattern string that need to be parsed
   * @param start start position to parse
   * @param affix store the parsed result
   * @param inNegativePattern true if we are parsing the negative pattern and
   *     therefore only care about the prefix and suffix
   * @return how many characters parsed
   */
  private int parseAffix(String pattern, int start, StringBuilder affix,
      boolean inNegativePattern) {
    affix.delete(0, affix.length());
    boolean inQuote = false;
    int len = pattern.length();

    for (int pos = start; pos < len; ++pos) {
      char ch = pattern.charAt(pos);
      if (ch == QUOTE) {
        if ((pos + 1) < len && pattern.charAt(pos + 1) == QUOTE) {
          ++pos;
          affix.append("'"); // 'don''t'
        } else {
          inQuote = !inQuote;
        }
        continue;
      }

      if (inQuote) {
        affix.append(ch);
      } else {
        switch (ch) {
          case PATTERN_DIGIT:
          case PATTERN_ZERO_DIGIT:
          case PATTERN_GROUPING_SEPARATOR:
          case PATTERN_DECIMAL_SEPARATOR:
          case PATTERN_SEPARATOR:
            return pos - start;
          case CURRENCY_SIGN:
            isCurrencyFormat = true;
            if ((pos + 1) < len && pattern.charAt(pos + 1) == CURRENCY_SIGN) {
              ++pos;
              if (pos < len - 2 && pattern.charAt(pos + 1) == CURRENCY_SIGN
                  && pattern.charAt(pos + 2) == CURRENCY_SIGN) {
                pos += 2;
                affix.append(currencyData.getSimpleCurrencySymbol());
              } else {
                affix.append(currencyData.getCurrencyCode());
              }
            } else {
              affix.append(currencyData.getCurrencySymbol());
            }
            break;
          case PATTERN_PERCENT:
            if (!inNegativePattern) {
              if (multiplier != 1) {
                throw new IllegalArgumentException(
                    "Too many percent/per mille characters in pattern \""
                    + pattern + '"');
              }
              multiplier = 100;
            }
            affix.append(numberConstants.percent());
            break;
          case PATTERN_PER_MILLE:
            if (!inNegativePattern) {
              if (multiplier != 1) {
                throw new IllegalArgumentException(
                    "Too many percent/per mille characters in pattern \""
                    + pattern + '"');
              }
              multiplier = 1000;
            }
            affix.append(numberConstants.perMill());
            break;
          case PATTERN_MINUS:
            affix.append("-");
            break;
          default:
            affix.append(ch);
        }
      }
    }
    return len - start;
  }

  /**
   * This function parses a "localized" text into a <code>double</code>. It
   * needs to handle locale specific decimal, grouping, exponent and digit.
   *
   * @param text the text that need to be parsed
   * @param pos in/out parsing position. in case of failure, this shouldn't be
   *          changed
   * @return double value, could be 0.0 if nothing can be parsed
   */
  private double parseNumber(String text, int[] pos) {
    double ret;
    boolean sawDecimal = false;
    boolean sawExponent = false;
    boolean sawDigit = false;
    int scale = 1;
    String decimal = isCurrencyFormat ? numberConstants.monetarySeparator()
        : numberConstants.decimalSeparator();
    String grouping = isCurrencyFormat
        ? numberConstants.monetaryGroupingSeparator()
        : numberConstants.groupingSeparator();
    String exponentChar = numberConstants.exponentialSymbol();

    StringBuilder normalizedText = new StringBuilder();
    for (; pos[0] < text.length(); ++pos[0]) {
      char ch = text.charAt(pos[0]);
      int digit = getDigit(ch);
      if (digit >= 0 && digit <= 9) {
        normalizedText.append((char) (digit + '0'));
        sawDigit = true;
      } else if (ch == decimal.charAt(0)) {
        if (sawDecimal || sawExponent) {
          break;
        }
        normalizedText.append('.');
        sawDecimal = true;
      } else if (ch == grouping.charAt(0)) {
        if (sawDecimal || sawExponent) {
          break;
        }
        continue;
      } else if (ch == exponentChar.charAt(0)) {
        if (sawExponent) {
          break;
        }
        normalizedText.append('E');
        sawExponent = true;
      } else if (ch == '+' || ch == '-') {
        normalizedText.append(ch);
      } else if (ch == numberConstants.percent().charAt(0)) {
        if (scale != 1) {
          break;
        }
        scale = 100;
        if (sawDigit) {
          ++pos[0];
          break;
        }
      } else if (ch == numberConstants.perMill().charAt(0)) {
        if (scale != 1) {
          break;
        }
        scale = 1000;
        if (sawDigit) {
          ++pos[0];
          break;
        }
      } else {
        break;
      }
    }

    // parseDouble could throw NumberFormatException, rethrow with correct text.
    try {
      ret = Double.parseDouble(normalizedText.toString());
    } catch (NumberFormatException e) {
      throw new NumberFormatException(text);
    }
    ret = ret / scale;
    return ret;
  }

  /**
   * Method parses provided pattern, result is stored in member variables.
   *
   * @param pattern
   */
  private void parsePattern(String pattern) {
    int pos = 0;
    StringBuilder affix = new StringBuilder();

    pos += parseAffix(pattern, pos, affix, false);
    positivePrefix = affix.toString();
    pos += parseTrunk(pattern, pos, false);
    pos += parseAffix(pattern, pos, affix, false);
    positiveSuffix = affix.toString();

    if (pos < pattern.length() && pattern.charAt(pos) == PATTERN_SEPARATOR) {
      ++pos;
      pos += parseAffix(pattern, pos, affix, true);
      negativePrefix = affix.toString();
      // the negative pattern is only used for prefix/suffix
      pos += parseTrunk(pattern, pos, true);
      pos += parseAffix(pattern, pos, affix, true);
      negativeSuffix = affix.toString();
    } else {
      negativePrefix = numberConstants.minusSign() + positivePrefix;
      negativeSuffix = positiveSuffix;
    }
  }

  /**
   * This method parses the trunk part of a pattern.
   *
   * @param pattern pattern string that need to be parsed
   * @param start where parse started
   * @param ignorePattern true if we are only parsing this for length
   *     and correctness, such as in the negative portion of the pattern
   * @return how many characters parsed
   */
  private int parseTrunk(String pattern, int start, boolean ignorePattern) {
    int decimalPos = -1;
    int digitLeftCount = 0, zeroDigitCount = 0, digitRightCount = 0;
    byte groupingCount = -1;

    int len = pattern.length();
    int pos = start;
    boolean loop = true;
    for (; (pos < len) && loop; ++pos) {
      char ch = pattern.charAt(pos);
      switch (ch) {
        case PATTERN_DIGIT:
          if (zeroDigitCount > 0) {
            ++digitRightCount;
          } else {
            ++digitLeftCount;
          }
          if (groupingCount >= 0 && decimalPos < 0) {
            ++groupingCount;
          }
          break;
        case PATTERN_ZERO_DIGIT:
          if (digitRightCount > 0) {
            throw new IllegalArgumentException("Unexpected '0' in pattern \""
                + pattern + '"');
          }
          ++zeroDigitCount;
          if (groupingCount >= 0 && decimalPos < 0) {
            ++groupingCount;
          }
          break;
        case PATTERN_GROUPING_SEPARATOR:
          groupingCount = 0;
          break;
        case PATTERN_DECIMAL_SEPARATOR:
          if (decimalPos >= 0) {
            throw new IllegalArgumentException(
                "Multiple decimal separators in pattern \"" + pattern + '"');
          }
          decimalPos = digitLeftCount + zeroDigitCount + digitRightCount;
          break;
        case PATTERN_EXPONENT:
          if (!ignorePattern) {
            if (useExponentialNotation) {
              throw new IllegalArgumentException("Multiple exponential "
                  + "symbols in pattern \"" + pattern + '"');
            }
            useExponentialNotation = true;
            minExponentDigits = 0;
          }

          // Use lookahead to parse out the exponential part
          // of the pattern, then jump into phase 2.
          while ((pos + 1) < len
              && pattern.charAt(pos + 1) == PATTERN_ZERO_DIGIT) {
            ++pos;
            if (!ignorePattern) {
              ++minExponentDigits;
            }
          }

          if (!ignorePattern && (digitLeftCount + zeroDigitCount) < 1
              || minExponentDigits < 1) {
            throw new IllegalArgumentException("Malformed exponential "
                + "pattern \"" + pattern + '"');
          }
          loop = false;
          break;
        default:
          --pos;
          loop = false;
          break;
      }
    }

    if (zeroDigitCount == 0 && digitLeftCount > 0 && decimalPos >= 0) {
      // Handle "###.###" and "###." and ".###".
      int n = decimalPos;
      if (n == 0) { // Handle ".###"
        ++n;
      }
      digitRightCount = digitLeftCount - n;
      digitLeftCount = n - 1;
      zeroDigitCount = 1;
    }

    // Do syntax checking on the digits.
    if ((decimalPos < 0 && digitRightCount > 0)
        || (decimalPos >= 0 && (decimalPos < digitLeftCount || decimalPos > (digitLeftCount + zeroDigitCount)))
        || groupingCount == 0) {
      throw new IllegalArgumentException("Malformed pattern \"" + pattern + '"');
    }

    if (ignorePattern) {
      return pos - start;
    }

    int totalDigits = digitLeftCount + zeroDigitCount + digitRightCount;

    maximumFractionDigits = (decimalPos >= 0 ? (totalDigits - decimalPos) : 0);
    if (decimalPos >= 0) {
      minimumFractionDigits = digitLeftCount + zeroDigitCount - decimalPos;
      if (minimumFractionDigits < 0) {
        minimumFractionDigits = 0;
      }
    }

    /*
     * The effectiveDecimalPos is the position the decimal is at or would be at
     * if there is no decimal. Note that if decimalPos<0, then digitTotalCount ==
     * digitLeftCount + zeroDigitCount.
     */
    int effectiveDecimalPos = decimalPos >= 0 ? decimalPos : totalDigits;
    minimumIntegerDigits = effectiveDecimalPos - digitLeftCount;
    if (useExponentialNotation) {
      maximumIntegerDigits = digitLeftCount + minimumIntegerDigits;

      // In exponential display, integer part can't be empty.
      if (maximumFractionDigits == 0 && minimumIntegerDigits == 0) {
        minimumIntegerDigits = 1;
      }
    }

    this.groupingSize = (groupingCount > 0) ? groupingCount : 0;
    decimalSeparatorAlwaysShown = (decimalPos == 0 || decimalPos == totalDigits);

    return pos - start;
  }

  /**
   * Remove excess leading zeros or add some if we don't have enough.
   *
   * @param digits
   */
  private void processLeadingZeros(StringBuilder digits) {
    // make sure we have enough trailing zeros
    if (decimalPosition > digitsLength) {
      while (digitsLength < decimalPosition) {
        digits.append('0');
        ++digitsLength;
      }
    }

    if (!useExponentialNotation) {
      // make sure we have the right number of leading zeros
      if (decimalPosition < minimumIntegerDigits) {
        // add leading zeros
        StringBuilder prefix = new StringBuilder();
        while (decimalPosition < minimumIntegerDigits) {
          prefix.append('0');
          ++decimalPosition;
          ++digitsLength;
        }
        digits.insert(0, prefix);
      } else if (decimalPosition > minimumIntegerDigits) {
        // trim excess leading zeros
        int strip = decimalPosition - minimumIntegerDigits;
        for (int i = 0; i < strip; ++i) {
          if (digits.charAt(i) != '0') {
            strip = i;
            break;
          }
        }
        if (strip > 0) {
          digits.delete(0, strip);
          digitsLength -= strip;
          decimalPosition -= strip;
        }
      }
    }
  }

  /**
   * Propagate a carry from incrementing the {@code i+1}'th digit.
   *
   * @param digits
   * @param i digit to start incrementing
   */
  private void propagateCarry(StringBuilder digits, int i) {
    boolean carry = true;
    while (carry && i >= 0) {
      char digit = digits.charAt(i);
      if (digit == '9') {
        // set this to zero and keep going
        digits.setCharAt(i--, '0');
      } else {
        digits.setCharAt(i, (char) (digit + 1));
        carry = false;
      }
    }
    if (carry) {
      // ran off the front, prepend a 1
      digits.insert(0, '1');
      ++decimalPosition;
      ++digitsLength;
    }
  }

  /**
   * Round the value at the requested place, propagating any carry backward.
   *
   * @param digits
   */
  private void roundValue(StringBuilder digits) {
    // TODO(jat): other rounding modes?
    if (digitsLength > decimalPosition + maximumFractionDigits
        && digits.charAt(decimalPosition + maximumFractionDigits) >= '5') {
      int i = decimalPosition + maximumFractionDigits - 1;
      propagateCarry(digits, i);
    }
  }
}
