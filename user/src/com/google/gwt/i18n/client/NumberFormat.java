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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.constants.NumberConstants;
import com.google.gwt.i18n.client.impl.CurrencyData;
import com.google.gwt.i18n.client.impl.CurrencyList;

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
 * <td><code>E</code></td>
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

  // Number constants mapped to use latin digits/separators.
  private static NumberConstants latinNumberConstants = null;

  // Localized characters for dot and comma in number patterns, used to produce
  // the latin mapping for arbitrary locales.  Any separator not in either of
  // these strings will be mapped to non-breaking space (U+00A0).
  private static final String LOCALIZED_COMMA_EQUIVALENTS = ",\u060C\u066B\u3001\uFE10\uFE11\uFE50\uFE51\uFF0C\uFF64";
  private static final String LOCALIZED_DOT_EQUIVALENTS = ".\u2024\u3002\uFE12\uFE52\uFF0E\uFF61";

  // Constants for characters used in programmatic (unlocalized) patterns.
  private static final char CURRENCY_SIGN = '\u00A4';
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
   * @return true if all new NumberFormat instances will use latin digits
   *     and related characters rather than the localized ones. 
   */
  public static boolean forcedLatinDigits() {
    return defaultNumberConstants != localizedNumberConstants;
  }
  
  /**
   * Provides the standard currency format for the default locale.
   * 
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the default locale
   */
  public static NumberFormat getCurrencyFormat() {
    if (cachedCurrencyFormat == null) {
      cachedCurrencyFormat = new NumberFormat(
          defaultNumberConstants.currencyPattern(), CurrencyList.get().getDefault(), false);
    }
    return cachedCurrencyFormat;
  }
  
  /**
   * Provides the standard currency format for the default locale using a
   * specified currency.
   * 
   * @param currencyCode valid currency code, as defined in 
   *     com.google.gwt.i18n.client.constants.CurrencyCodeMapConstants.properties
   * @return a <code>NumberFormat</code> capable of producing and consuming
   *         currency format for the default locale
   */
  public static NumberFormat getCurrencyFormat(String currencyCode) {
    // TODO(jat): consider caching values per currency code.
    return new NumberFormat(defaultNumberConstants.currencyPattern(),
        CurrencyList.get().lookup(currencyCode), false);
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
   * @param currencyCode international currency code
   * @return a NumberFormat instance
   * @throws IllegalArgumentException if the specified pattern is invalid
   */
  public static NumberFormat getFormat(String pattern, String currencyCode) {
    return new NumberFormat(pattern, CurrencyList.get().lookup(currencyCode), true);
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
  protected static NumberConstants createLatinNumberConstants(final NumberConstants orig) {
    final String groupingSeparator = remapSeparator(
        orig.groupingSeparator());
    final String decimalSeparator = remapSeparator(
        orig.decimalSeparator());
    final String monetaryGroupingSeparator = remapSeparator(
        orig.monetaryGroupingSeparator());
    final String monetarySeparator = remapSeparator(
        orig.monetarySeparator());
    return new NumberConstants() {
      public String currencyPattern() {
        return orig.currencyPattern();
      }

      public String decimalPattern() {
        return orig.decimalPattern();
      }

      public String decimalSeparator() {
        return decimalSeparator;
      }

      public String defCurrencyCode() {
        return orig.defCurrencyCode();
      }

      public String exponentialSymbol() {
        return orig.exponentialSymbol();
      }

      public String groupingSeparator() {
        return groupingSeparator;
      }

      public String infinity() {
        return orig.infinity();
      }

      public String minusSign() {
        return orig.minusSign();
      }

      public String monetaryGroupingSeparator() {
        return monetaryGroupingSeparator;
      }

      public String monetarySeparator() {
        return monetarySeparator;
      }

      public String notANumber() {
        return orig.notANumber();
      }

      public String percent() {
        return orig.percent();
      }

      public String percentPattern() {
        return orig.percentPattern();
      }

      public String perMill() {
        return orig.perMill();
      }

      public String plusSign() {
        return orig.plusSign();
      }

      public String scientificPattern() {
        return orig.scientificPattern();
      }

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

  private static native String toFixed(double d, int digits) /*-{
    return d.toFixed(digits);
  }-*/;

  // The currency code.
  private final String currencyCode;

  // Currency setting.
  private final String currencySymbol;

  // Forces the decimal separator to always appear in a formatted number.
  private boolean decimalSeparatorAlwaysShown = false;

  // The number of digits between grouping separators in the integer
  // portion of a number.
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
    currencyCode = cdata.getCurrencyCode();
    currencySymbol = cdata.getCurrencySymbol();

    // TODO: handle per-currency flags, such as symbol prefix/suffix and spacing
    parsePattern(this.pattern);
    if (!userSuppliedPattern && isCurrencyFormat) {
      minimumFractionDigits = cdata.getDefaultFractionDigits();
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
    StringBuffer result = new StringBuffer();

    if (Double.isNaN(number)) {
      result.append(numberConstants.notANumber());
      return result.toString();
    }

    boolean isNegative = ((number < 0.0) || (number == 0.0 && 1 / number < 0.0));

    result.append(isNegative ? negativePrefix : positivePrefix);
    if (Double.isInfinite(number)) {
      result.append(numberConstants.infinity());
    } else {
      if (isNegative) {
        number = -number;
      }
      number *= multiplier;
      if (useExponentialNotation) {
        subformatExponential(number, result);
      } else {
        subformatFixed(number, result, minimumIntegerDigits);
      }
    }

    result.append(isNegative ? negativeSuffix : positiveSuffix);

    return result.toString();
  }

  /**
   * Returns the pattern used by this number format.
   */
  public String getPattern() {
    return pattern;
  }

  /**
   * Parses text to produce a numeric value. A {@link NumberFormatException} is
   * thrown if either the text is empty or if the parse does not consume all
   * characters of the text.
   * 
   * @param text the string being parsed
   * @return a parsed number value
   * @throws NumberFormatException if the entire text could not be converted
   *           into a number
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
   * @return a double value representing the parsed number, or <code>0.0</code>
   *         if the parse fails
   * @throws NumberFormatException if the text segment could not be converted into a number
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

  protected int getGroupingSize() {
    return groupingSize;
  }

  protected String getNegativePrefix() {
    return negativePrefix;
  }

  protected String getNegativeSuffix() {
    return negativeSuffix;
  }

  protected NumberConstants getNumberConstants() {
    return numberConstants;
  }

  protected String getPositivePrefix() {
    return positivePrefix;
  }

  protected String getPositiveSuffix() {
    return positiveSuffix;
  }

  protected boolean isDecimalSeparatorAlwaysShown() {
    return decimalSeparatorAlwaysShown;
  }

  /**
   * This method formats the exponent part of a double.
   * 
   * @param exponent exponential value
   * @param result formatted exponential part will be append to it
   */
  private void addExponentPart(int exponent, StringBuffer result) {
    result.append(numberConstants.exponentialSymbol());

    if (exponent < 0) {
      exponent = -exponent;
      result.append(numberConstants.minusSign());
    }

    String exponentDigits = String.valueOf(exponent);
    int len = exponentDigits.length();
    for (int i = len; i < minExponentDigits; ++i) {
      result.append(numberConstants.zeroDigit());
    }
    int zeroDelta = numberConstants.zeroDigit().charAt(0) - '0';
    for (int i = 0; i < len; ++i) {
      result.append((char) (exponentDigits.charAt(i) + zeroDelta));
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
   * This does the work of String.valueOf(long), but given a double as input
   * and avoiding our emulated longs.  Contrasted with String.valueOf(double),
   * it ensures (a) there will be no trailing .0, and (b) unwinds E-notation.
   *  
   * @param number the integral value to convert
   * @return the string representing that integer
   */
  private String makeIntString(double number) {
    String intPart = String.valueOf(number);
    if (GWT.isScript()) {
      return intPart; // JavaScript does the right thing for integral doubles
    }
    // ...but bytecode (hosted mode) does not... String.valueOf(double) will 
    // either end in .0 (non internationalized) which we don't want but is 
    // easy, or, for large numbers, it will be E-notation, which is annoying.
    int digitLen = intPart.length();
    
    if (intPart.charAt(digitLen - 2) == '.') {
      return intPart.substring(0, digitLen - 2);
    } 

    // if we have E notation, (1) the exponent will be positive (else 
    // intValue is 0, which doesn't need E notation), and (2) there will
    // be a radix dot (String.valueOf() isn't interationalized)
    int radix = intPart.indexOf('.');
    int exp = intPart.indexOf('E');
    int digits = 0;
    for (int i = exp + 1; i < intPart.length(); i++) {
      digits = digits * 10 + (intPart.charAt(i) - '0');
    }
    digits++;  // exp of zero is one int digit...
    StringBuffer newIntPart = new StringBuffer();
    newIntPart.append(intPart.substring(0, radix));
    newIntPart.append(intPart.substring(radix + 1, exp));
    while (newIntPart.length() < digits) {
      newIntPart.append('0');
    }
    newIntPart.setLength(digits);
    return newIntPart.toString();
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
  private int parseAffix(String pattern, int start, StringBuffer affix,
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
              affix.append(currencyCode);
            } else {
              affix.append(currencySymbol);
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

    StringBuffer normalizedText = new StringBuffer();
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
    StringBuffer affix = new StringBuffer();

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
   * This method formats a <code>double</code> in exponential format.
   * 
   * @param number value need to be formated
   * @param result where the formatted string goes
   */
  private void subformatExponential(double number, StringBuffer result) {
    if (number == 0.0) {
      subformatFixed(number, result, minimumIntegerDigits);
      addExponentPart(0, result);
      return;
    }

    int exponent = (int) Math.floor(Math.log(number) / Math.log(10));
    number /= Math.pow(10, exponent);

    int minIntDigits = minimumIntegerDigits;
    if (maximumIntegerDigits > 1 && maximumIntegerDigits > minimumIntegerDigits) {
      // A repeating range is defined; adjust to it as follows.
      // If repeat == 3, we have 6,5,4=>3; 3,2,1=>0; 0,-1,-2=>-3;
      // -3,-4,-5=>-6, etc. This takes into account that the
      // exponent we have here is off by one from what we expect;
      // it is for the format 0.MMMMMx10^n.
      while ((exponent % maximumIntegerDigits) != 0) {
        number *= 10;
        exponent--;
      }
      minIntDigits = 1;
    } else {
      // No repeating range is defined; use minimum integer digits.
      if (minimumIntegerDigits < 1) {
        exponent++;
        number /= 10;
      } else {
        for (int i = 1; i < minimumIntegerDigits; i++) {
          exponent--;
          number *= 10;
        }
      }
    }

    subformatFixed(number, result, minIntDigits);
    addExponentPart(exponent, result);
  }

  /**
   * This method formats a <code>double</code> into a fractional
   * representation.
   * 
   * @param number value need to be formated
   * @param result result will be written here
   * @param minIntDigits minimum integer digits
   */
  private void subformatFixed(double number, StringBuffer result,
      int minIntDigits) {
    double power = Math.pow(10, maximumFractionDigits);
    // Use 3 extra digits to allow us to do our own rounding since
    // Java rounds up on .5 whereas some browsers might use 'round to even'
    // or other rules.
    
    // There are cases where more digits would be required to get
    // guaranteed results, but this at least makes such cases rarer.
    String fixedString = toFixed(number, maximumFractionDigits + 3);

    double intValue = 0, fracValue = 0;
    int exponentIndex = fixedString.indexOf('e');
    if (exponentIndex != -1) {
      // Large numbers may be returned in exponential notation: such numbers
      // are integers anyway
      intValue = Math.floor(number);
    } else {
      int decimalIndex = fixedString.indexOf('.');
      int len = fixedString.length();
      if (decimalIndex == -1) {
        decimalIndex = len;
      }
      if (decimalIndex > 0) {
        intValue = Double.parseDouble(fixedString.substring(0, decimalIndex));
      }
      if (decimalIndex < len - 1) {
        fracValue = Double.parseDouble(fixedString.substring(decimalIndex + 1));
        fracValue = (((int) fracValue) + 500) / 1000;
        if (fracValue >= power) {
          fracValue -= power;
          intValue++;
        }
      }
    }

    boolean fractionPresent = (minimumFractionDigits > 0) || (fracValue > 0);

    String intPart = makeIntString(intValue);
    String grouping = isCurrencyFormat
        ? numberConstants.monetaryGroupingSeparator()
        : numberConstants.groupingSeparator();
    String decimal = isCurrencyFormat ? numberConstants.monetarySeparator()
        : numberConstants.decimalSeparator();

    int zeroDelta = numberConstants.zeroDigit().charAt(0) - '0';
    int digitLen = intPart.length();
    
    if (intValue > 0 || minIntDigits > 0) {
      for (int i = digitLen; i < minIntDigits; i++) {
        result.append(numberConstants.zeroDigit());
      }

      for (int i = 0; i < digitLen; i++) {
        result.append((char) (intPart.charAt(i) + zeroDelta));

        if (digitLen - i > 1 && groupingSize > 0
            && ((digitLen - i) % groupingSize == 1)) {
          result.append(grouping);
        }
      }
    } else if (!fractionPresent) {
      // If there is no fraction present, and we haven't printed any
      // integer digits, then print a zero.
      result.append(numberConstants.zeroDigit());
    }

    // Output the decimal separator if we always do so.
    if (decimalSeparatorAlwaysShown || fractionPresent) {
      result.append(decimal);
    }

    // To make sure it lead zero will be kept.
    String fracPart = makeIntString(Math.floor(fracValue + power + 0.5d));
    int fracLen = fracPart.length();
    while (fracPart.charAt(fracLen - 1) == '0' && fracLen > minimumFractionDigits + 1) {
      fracLen--;
    }

    for (int i = 1; i < fracLen; i++) {
      result.append((char) (fracPart.charAt(i) + zeroDelta));
    }
  }
}
