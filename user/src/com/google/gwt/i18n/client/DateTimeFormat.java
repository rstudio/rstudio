/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.i18n.client.constants.DateTimeConstants;

import java.util.ArrayList;
import java.util.Date;

/**
 * Formats and parses dates and times using locale-sensitive patterns.
 * 
 * <h3>Patterns</h3>
 * 
 * <table>
 * <tr>
 * <th>Symbol</th>
 * <th>Meaning</th>
 * <th>Presentation</th>
 * <th>Example</th>
 * </tr>
 * 
 * <tr>
 * <td><code>G</code></td>
 * <td>era designator</td>
 * <td>Text</td>
 * <td><code>AD</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>y</code></td>
 * <td>year</td>
 * <td>Number</td>
 * <td><code>1996</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>M</code></td>
 * <td>month in year</td>
 * <td>Text or Number</td>
 * <td><code>July (or) 07</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>d</code></td>
 * <td>day in month</td>
 * <td>Number</td>
 * <td><code>10</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>h</code></td>
 * <td>hour in am/pm (1-12)</td>
 * <td>Number</td>
 * <td><code>12</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>H</code></td>
 * <td>hour in day (0-23)</td>
 * <td>Number</td>
 * <td><code>0</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>m</code></td>
 * <td>minute in hour</td>
 * <td>Number</td>
 * <td><code>30</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>s</code></td>
 * <td>second in minute</td>
 * <td>Number</td>
 * <td><code>55</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>S</code></td>
 * <td>fractional second</td>
 * <td>Number</td>
 * <td><code>978</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>E</code></td>
 * <td>day of week</td>
 * <td>Text</td>
 * <td><code>Tuesday</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>a</code></td>
 * <td>am/pm marker</td>
 * <td>Text</td>
 * <td><code>PM</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>k</code></td>
 * <td>hour in day (1-24)</td>
 * <td>Number</td>
 * <td><code>24</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>K</code></td>
 * <td>hour in am/pm (0-11)</td>
 * <td>Number</td>
 * <td><code>0</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>z</code></td>
 * <td>time zone</td>
 * <td>Text</td>
 * <td><code>Pacific Standard Time</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>Z</code></td>
 * <td>time zone (RFC 822)</td>
 * <td>Number</td>
 * <td><code>-0800</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>v</code></td>
 * <td>time zone (generic)</td>
 * <td>Text</td>
 * <td><code>Pacific Time</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>'</code></td>
 * <td>escape for text</td>
 * <td>Delimiter</td>
 * <td><code>'Date='</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>''</code></td>
 * <td>single quote</td>
 * <td>Literal</td>
 * <td><code>'o''clock'</code></td>
 * </tr>
 * </table>
 * 
 * <p>
 * The number of pattern letters influences the format, as follows:
 * </p>
 * 
 * <dl>
 * <dt>Text</dt>
 * <dd>if 4 or more, then use the full form; if less than 4, use short or
 * abbreviated form if it exists (e.g., <code>"EEEE"</code> produces
 * <code>"Monday"</code>, <code>"EEE"</code> produces <code>"Mon"</code>)</dd>
 * 
 * <dt>Number</dt>
 * <dd>the minimum number of digits. Shorter numbers are zero-padded to this
 * amount (e.g. if <code>"m"</code> produces <code>"6"</code>,
 * <code>"mm"</code> produces <code>"06"</code>). Year is handled
 * specially; that is, if the count of 'y' is 2, the Year will be truncated to 2
 * digits. (e.g., if <code>"yyyy"</code> produces <code>"1997"</code>,
 * <code>"yy"</code> produces <code>"97"</code>.) Unlike other fields,
 * fractional seconds are padded on the right with zero.</dd>
 * 
 * <dt>Text or Number</dt>
 * <dd>3 or more, use text, otherwise use number. (e.g. <code>"M"</code>
 * produces <code>"1"</code>, <code>"MM"</code> produces <code>"01"</code>,
 * <code>"MMM"</code> produces <code>"Jan"</code>, and <code>"MMMM"</code>
 * produces <code>"January"</code>.</dd>
 * </dl>
 * 
 * <p>
 * Any characters in the pattern that are not in the ranges of ['<code>a</code>'..'<code>z</code>']
 * and ['<code>A</code>'..'<code>Z</code>'] will be treated as quoted
 * text. For instance, characters like '<code>:</code>', '<code>.</code>', '<code> </code>'
 * (space), '<code>#</code>' and '<code>@</code>' will appear in the
 * resulting time text even they are not embraced within single quotes.
 * </p>
 * 
 * <h3>Parsing Dates and Times</h3>
 * <p>
 * This implementation could parse partial date/time. Current date will be
 * used to fill in the unavailable date part.  00:00:00 will be used to
 * fill in the time part.
 * </p>
 * 
 * <p>
 * As with formatting (described above), the count of pattern letters determine
 * the parsing behavior.
 * </p>
 * 
 * <dl>
 * <dt>Text</dt>
 * <dd>4 or more pattern letters--use full form, less than 4--use short or
 * abbreviated form if one exists. In parsing, we will always try long format,
 * then short.</dd>
 * 
 * <dt>Number</dt>
 * <dd>the minimum number of digits.</dd>
 * 
 * <dt>Text or Number</dt>
 * <dd>3 or more characters means use text, otherwise use number</dd>
 * </dl>
 * 
 * <p>
 * Although the current pattern specification doesn't not specify behavior for
 * all letters, it may in the future. It is strongly discouraged to used
 * unspecified letters as literal text without being surrounded by quotes.
 * </p>
 * 
 * <h3>Examples</h3>
 * <table>
 * <tr>
 * <th>Pattern</th>
 * <th>Formatted Text</th>
 * </tr>
 * 
 * <tr>
 * <td><code>"yyyy.MM.dd G 'at' HH:mm:ss vvvv"</code></td>
 * <td><code>1996.07.10 AD at 15:08:56 Pacific Time</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>"EEE, MMM d, ''yy"</code></td>
 * <td><code>Wed, July 10, '96</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>"h:mm a"</code></td>
 * <td><code>12:08 PM</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>"hh 'o''clock' a, zzzz"</code></td>
 * <td><code> 12 o'clock PM, Pacific Daylight Time</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>"K:mm a, vvv"</code></td>
 * <td><code> 0:00 PM, PT</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>"yyyyy.MMMMM.dd GGG hh:mm aaa"</code></td>
 * <td><code>01996.July.10 AD 12:08 PM</code></td>
 * </tr>
 * </table>
 * 
 * <h3>Additional Parsing Considerations</h3>
 * <p>
 * When parsing a date string using the abbreviated year pattern (<code>"yy"</code>),
 * the parser must interpret the abbreviated year relative to some century. It
 * does this by adjusting dates to be within 80 years before and 20 years after
 * the time the parser instance is created. For example, using a pattern of
 * <code>"MM/dd/yy"</code> and a <code>DateTimeFormat</code> object created
 * on Jan 1, 1997, the string <code>"01/11/12"</code> would be interpreted as
 * Jan 11, 2012 while the string <code>"05/04/64"</code> would be interpreted
 * as May 4, 1964. During parsing, only strings consisting of exactly two
 * digits, as defined by {@link java.lang.Character#isDigit(char)}, will be
 * parsed into the default century. If the year pattern does not have exactly
 * two 'y' characters, the year is interpreted literally, regardless of the
 * number of digits. For example, using the pattern <code>"MM/dd/yyyy"</code>,
 * "01/11/12" parses to Jan 11, 12 A.D.
 * </p>
 * 
 * <p>
 * When numeric fields abut one another directly, with no intervening delimiter
 * characters, they constitute a run of abutting numeric fields. Such runs are
 * parsed specially. For example, the format "HHmmss" parses the input text
 * "123456" to 12:34:56, parses the input text "12345" to 1:23:45, and fails to
 * parse "1234". In other words, the leftmost field of the run is flexible,
 * while the others keep a fixed width. If the parse fails anywhere in the run,
 * then the leftmost field is shortened by one character, and the entire run is
 * parsed again. This is repeated until either the parse succeeds or the
 * leftmost field is one character in length. If the parse still fails at that
 * point, the parse of the run fails.
 * </p>
 * 
 * <p>
 * In the current implementation, timezone parsing only supports
 * <code>GMT:hhmm</code>, <code>GMT:+hhmm</code>, and
 * <code>GMT:-hhmm</code>.
 * </p>
 */
public class DateTimeFormat {
  /**
   * Class PatternPart holds a "compiled" pattern part.
   */
  private class PatternPart {
    public String text;
    public int count; // 0 has a special meaning, it stands for literal
    public boolean abutStart;

    public PatternPart(String txt, int cnt) {
      text = txt;
      count = cnt;
      abutStart = false;
    }
  }

  private static final int FULL_DATE_FORMAT = 0;
  private static final int LONG_DATE_FORMAT = 1;
  private static final int MEDIUM_DATE_FORMAT = 2;
  private static final int SHORT_DATE_FORMAT = 3;
  private static final int FULL_TIME_FORMAT = 0;
  private static final int LONG_TIME_FORMAT = 1;
  private static final int MEDIUM_TIME_FORMAT = 2;

  private static final int SHORT_TIME_FORMAT = 3;
  private static final int NUMBER_BASE = 10;
  private static final int JS_START_YEAR = 1900;

  private static DateTimeFormat cachedFullDateFormat;
  private static DateTimeFormat cachedLongDateFormat;
  private static DateTimeFormat cachedMediumDateFormat;

  private static DateTimeFormat cachedShortDateFormat;
  private static DateTimeFormat cachedFullTimeFormat;
  private static DateTimeFormat cachedLongTimeFormat;
  private static DateTimeFormat cachedMediumTimeFormat;

  private static DateTimeFormat cachedShortTimeFormat;
  private static DateTimeFormat cachedFullDateTimeFormat;
  private static DateTimeFormat cachedLongDateTimeFormat;
  private static DateTimeFormat cachedMediumDateTimeFormat;
  private static DateTimeFormat cachedShortDateTimeFormat;

  private static final DateTimeConstants defaultDateTimeConstants = (DateTimeConstants) GWT.create(DateTimeConstants.class);

  private static final String PATTERN_CHARS = "GyMdkHmsSEDahKzZv";

  private static final String NUMERIC_FORMAT_CHARS = "MydhHmsSDkK";

  private static final String WHITE_SPACE = " \t\r\n";

  private static final String GMT = "GMT";

  private static final int MINUTES_PER_HOUR = 60;

  /**
   * Returns a format object using the specified pattern and the date time
   * constants for the default locale. If you need to format or parse repeatedly
   * using the same pattern, it is highly recommended that you cache the
   * returned <code>DateTimeFormat</code> object and reuse it rather than
   * calling this method repeatedly.
   * 
   * @param pattern string to specify how the date should be formatted
   * 
   * @return a <code>DateTimeFormat</code> object that can be used for format
   *         or parse date/time values matching the specified pattern
   * 
   * @throws IllegalArgumentException if the specified pattern could not be
   *           parsed
   */
  public static DateTimeFormat getFormat(String pattern) {
    return new DateTimeFormat(pattern, defaultDateTimeConstants);
  }

  public static DateTimeFormat getFullDateFormat() {
    if (cachedFullDateFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[FULL_DATE_FORMAT];
      cachedFullDateFormat = new DateTimeFormat(pattern);
    }
    return cachedFullDateFormat;
  }

  public static DateTimeFormat getFullDateTimeFormat() {
    if (cachedFullDateTimeFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[FULL_DATE_FORMAT]
          + " " + defaultDateTimeConstants.timeFormats()[FULL_TIME_FORMAT];
      cachedFullDateTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedFullDateTimeFormat;
  }

  public static DateTimeFormat getFullTimeFormat() {
    if (cachedFullTimeFormat == null) {
      String pattern = defaultDateTimeConstants.timeFormats()[FULL_TIME_FORMAT];
      cachedFullTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedFullTimeFormat;
  }

  public static DateTimeFormat getLongDateFormat() {
    if (cachedLongDateFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[LONG_DATE_FORMAT];
      cachedLongDateFormat = new DateTimeFormat(pattern);
    }
    return cachedLongDateFormat;
  }

  public static DateTimeFormat getLongDateTimeFormat() {
    if (cachedLongDateTimeFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[LONG_DATE_FORMAT]
          + " " + defaultDateTimeConstants.timeFormats()[LONG_TIME_FORMAT];
      cachedLongDateTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedLongDateTimeFormat;
  }

  public static DateTimeFormat getLongTimeFormat() {
    if (cachedLongTimeFormat == null) {
      String pattern = defaultDateTimeConstants.timeFormats()[LONG_TIME_FORMAT];
      cachedLongTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedLongTimeFormat;
  }

  public static DateTimeFormat getMediumDateFormat() {
    if (cachedMediumDateFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[MEDIUM_DATE_FORMAT];
      cachedMediumDateFormat = new DateTimeFormat(pattern);
    }
    return cachedMediumDateFormat;
  }

  public static DateTimeFormat getMediumDateTimeFormat() {
    if (cachedMediumDateTimeFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[MEDIUM_DATE_FORMAT]
          + " " + defaultDateTimeConstants.timeFormats()[MEDIUM_TIME_FORMAT];
      cachedMediumDateTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedMediumDateTimeFormat;
  }

  public static DateTimeFormat getMediumTimeFormat() {
    if (cachedMediumTimeFormat == null) {
      String pattern = defaultDateTimeConstants.timeFormats()[MEDIUM_TIME_FORMAT];
      cachedMediumTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedMediumTimeFormat;
  }

  public static DateTimeFormat getShortDateFormat() {
    if (cachedShortDateFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[SHORT_DATE_FORMAT];
      cachedShortDateFormat = new DateTimeFormat(pattern);
    }
    return cachedShortDateFormat;
  }

  public static DateTimeFormat getShortDateTimeFormat() {
    if (cachedShortDateTimeFormat == null) {
      String pattern = defaultDateTimeConstants.dateFormats()[SHORT_DATE_FORMAT]
          + " " + defaultDateTimeConstants.timeFormats()[SHORT_TIME_FORMAT];
      cachedShortDateTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedShortDateTimeFormat;
  }

  public static DateTimeFormat getShortTimeFormat() {
    if (cachedShortTimeFormat == null) {
      String pattern = defaultDateTimeConstants.timeFormats()[SHORT_TIME_FORMAT];
      cachedShortTimeFormat = new DateTimeFormat(pattern);
    }
    return cachedShortTimeFormat;
  }

  private final ArrayList<PatternPart> patternParts = new ArrayList<PatternPart>();

  private final DateTimeConstants dateTimeConstants;

  private final String pattern;

  /**
   * Constructs a format object using the specified pattern and the date time
   * constants for the default locale.
   * 
   * @param pattern string pattern specification
   */
  protected DateTimeFormat(String pattern) {
    this(pattern, defaultDateTimeConstants);
  }

  /**
   * Constructs a format object using the specified pattern and user-supplied
   * date time constants.
   * 
   * @param pattern string pattern specification
   * @param dateTimeConstants locale specific symbol collection
   */
  protected DateTimeFormat(String pattern, DateTimeConstants dateTimeConstants) {
    this.pattern = pattern;
    this.dateTimeConstants = dateTimeConstants;

    /*
     * Even though the pattern is only compiled for use in parsing and parsing
     * is far less common than formatting, the pattern is still parsed eagerly
     * here to fail fast in case the pattern itself is malformed.
     */
    parsePattern(pattern);
  }

  /**
   * Format a date object.
   * 
   * @param date the date object being formatted
   * 
   * @return formatted date representation
   */
  public String format(Date date) {
    StringBuffer toAppendTo = new StringBuffer(64);
    int j, n = pattern.length();
    for (int i = 0; i < n;) {
      char ch = pattern.charAt(i);
      if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
        // ch is a date-time pattern character to be interpreted by subFormat().
        // Count the number of times it is repeated.
        for (j = i + 1; j < n && pattern.charAt(j) == ch; ++j) {
        }
        subFormat(toAppendTo, ch, j - i, date);
        i = j;
      } else if (ch == '\'') {
        // Handle an entire quoted string, included embedded
        // doubled apostrophes (as in 'o''clock').

        // i points after '.
        ++i;

        // If start with '', just add ' and continue.
        if (i < n && pattern.charAt(i) == '\'') {
          toAppendTo.append('\'');
          ++i;
          continue;
        }

        // Otherwise add the quoted string.
        boolean trailQuote = false;
        while (!trailQuote) {
          // j points to next ' or EOS.
          j = i;
          while (j < n && pattern.charAt(j) != '\'') {
            ++j;
          }

          if (j >= n) {
            // Trailing ' (pathological).
            throw new IllegalArgumentException("Missing trailing \'");
          }

          // Look ahead to detect '' within quotes.
          if (j + 1 < n && pattern.charAt(j + 1) == '\'') {
            ++j;
          } else {
            trailQuote = true;
          }
          toAppendTo.append(pattern.substring(i, j));
          i = j + 1;
        }
      } else {
        // Append unquoted literal characters.
        toAppendTo.append(ch);
        ++i;
      }
    }

    return toAppendTo.toString();
  }

  public String getPattern() {
    return pattern;
  }

  /**
   * Parses text to produce a {@link Date} value. An
   * {@link IllegalArgumentException} is thrown if either the text is empty or
   * if the parse does not consume all characters of the text.
   * 
   * @param text the string being parsed
   * @return a parsed date/time value
   * @throws IllegalArgumentException if the entire text could not be converted
   *           into a number
   */
  public Date parse(String text) {
    Date date = new Date();
    date.setHours(0);
    date.setMinutes(0);
    date.setSeconds(0);
    int charsConsumed = parse(text, 0, date);
    if (charsConsumed == 0 || charsConsumed < text.length()) {
      throw new IllegalArgumentException(text);
    }
    return date;
  }

  /**
   * This method parses the input string, fill its value into a {@link Date}.
   * 
   * @param text the string that need to be parsed
   * @param start the character position in "text" where parsing should start
   * @param date the date object that will hold parsed value
   * 
   * @return 0 if parsing failed, otherwise the number of characters advanced
   */
  public int parse(String text, int start, Date date) {
    DateRecord cal = new DateRecord();
    int[] parsePos = {start};

    // For parsing abutting numeric fields. 'abutPat' is the
    // offset into 'pattern' of the first of 2 or more abutting
    // numeric fields. 'abutStart' is the offset into 'text'
    // where parsing the fields begins. 'abutPass' starts off as 0
    // and increments each time we try to parse the fields.
    int abutPat = -1; // If >=0, we are in a run of abutting numeric fields.
    int abutStart = 0;
    int abutPass = 0;

    for (int i = 0; i < patternParts.size(); ++i) {
      PatternPart part = patternParts.get(i);

      if (part.count > 0) {
        if (abutPat < 0 && part.abutStart) {
          abutPat = i;
          abutStart = start;
          abutPass = 0;
        }

        // Handle fields within a run of abutting numeric fields. Take
        // the pattern "HHmmss" as an example. We will try to parse
        // 2/2/2 characters of the input text, then if that fails,
        // 1/2/2. We only adjust the width of the leftmost field; the
        // others remain fixed. This allows "123456" => 12:34:56, but
        // "12345" => 1:23:45. Likewise, for the pattern "yyyyMMdd" we
        // try 4/2/2, 3/2/2, 2/2/2, and finally 1/2/2.
        if (abutPat >= 0) {
          // If we are at the start of a run of abutting fields, then
          // shorten this field in each pass. If we can't shorten
          // this field any more, then the parse of this set of
          // abutting numeric fields has failed.
          int count = part.count;
          if (i == abutPat) {
            count -= abutPass++;
            if (count == 0) {
              return 0;
            }
          }

          if (!subParse(text, parsePos, part, count, cal)) {
            // If the parse fails anywhere in the run, back up to the
            // start of the run and retry.
            i = abutPat - 1;
            parsePos[0] = abutStart;
            continue;
          }
        } else {
          // Handle non-numeric fields and non-abutting numeric fields.
          abutPat = -1;
          if (!subParse(text, parsePos, part, 0, cal)) {
            return 0;
          }
        }
      } else {
        // Handle literal pattern characters. These are any
        // quoted characters and non-alphabetic unquoted characters.
        abutPat = -1;
        // A run of white space in the pattern matches a run
        // of white space in the input text.
        if (part.text.charAt(0) == ' ') {
          // Advance over run in input text.
          int s = parsePos[0];
          skipSpace(text, parsePos);

          // Must see at least one white space char in input.
          if (parsePos[0] > s) {
            continue;
          }
        } else if (text.startsWith(part.text, parsePos[0])) {
          parsePos[0] += part.text.length();
          continue;
        }

        // We fall through to this point if the match fails.
        return 0;
      }
    }

    if (!cal.calcDate(date)) {
      return 0;
    }

    // Return progress.
    return parsePos[0] - start;
  }

  /**
   * Method append current content in buf as pattern part if there is any, and
   * clear buf for next part.
   * 
   * @param buf pattern part text specification
   * @param count pattern part repeat count
   */
  private void addPart(StringBuffer buf, int count) {
    if (buf.length() > 0) {
      patternParts.add((new PatternPart(buf.toString(), count)));
      buf.setLength(0);
    }
  }

  /**
   * Generate GMT timezone string for given date.
   * 
   * @param buf where timezone string will be appended to
   * @param date whose value being evaluated
   */
  private void appendGMT(StringBuffer buf, Date date) {
    int value = -date.getTimezoneOffset();

    if (value < 0) {
      buf.append("GMT-");
      value = -value; // suppress the '-' sign for text display.
    } else {
      buf.append("GMT+");
    }

    zeroPaddingNumber(buf, value / MINUTES_PER_HOUR, 2);
    buf.append(':');
    zeroPaddingNumber(buf, value % MINUTES_PER_HOUR, 2);
  }

  /**
   * Formats (0..11) Hours field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void format0To11Hours(StringBuffer buf, int count, Date date) {
    int value = date.getHours() % 12;
    zeroPaddingNumber(buf, value, count);
  }

  /**
   * Formats (0..23) Hours field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void format0To23Hours(StringBuffer buf, int count, Date date) {
    int value = date.getHours();
    zeroPaddingNumber(buf, value, count);
  }

  /**
   * Formats (1..12) Hours field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void format1To12Hours(StringBuffer buf, int count, Date date) {
    int value = date.getHours() % 12;
    if (value == 0) {
      zeroPaddingNumber(buf, 12, count);
    } else {
      zeroPaddingNumber(buf, value, count);
    }
  }

  /**
   * Formats (1..24) Hours field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void format24Hours(StringBuffer buf, int count, Date date) {
    int value = date.getHours();
    if (value == 0) {
      zeroPaddingNumber(buf, 24, count);
    } else {
      zeroPaddingNumber(buf, value, count);
    }
  }

  /**
   * Formats AM/PM field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatAmPm(StringBuffer buf, int count, Date date) {
    if (date.getHours() >= 12 && date.getHours() < 24) {
      buf.append(dateTimeConstants.ampms()[1]);
    } else {
      buf.append(dateTimeConstants.ampms()[0]);
    }
  }

  /**
   * Formats Date field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatDate(StringBuffer buf, int count, Date date) {
    int value = date.getDate();
    zeroPaddingNumber(buf, value, count);
  }

  /**
   * Formats Day of week field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatDayOfWeek(StringBuffer buf, int count, Date date) {
    int value = date.getDay();
    if (count >= 4) {
      buf.append(dateTimeConstants.weekdays()[value]);
    } else {
      buf.append(dateTimeConstants.shortWeekdays()[value]);
    }
  }

  /**
   * Formats Era field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatEra(StringBuffer buf, int count, Date date) {
    int value = date.getYear() >= -JS_START_YEAR ? 1 : 0;
    if (count >= 4) {
      buf.append(dateTimeConstants.eraNames()[value]);
    } else {
      buf.append(dateTimeConstants.eras()[value]);
    }
  }

  /**
   * Formats Fractional seconds field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatFractionalSeconds(StringBuffer buf, int count, Date date) {
    // Fractional seconds should be left-justified, ie. zero must be padded
    // from left. For example, if value in milliseconds is 5, and count is 3,
    // the output need to be "005".
    int value = (int) (date.getTime() % 1000);
    if (count == 1) {
      value = (value + 50) / 100; // Round to 100ms.
      buf.append(Integer.toString(value));
    } else if (count == 2) {
      value = (value + 5) / 10; // Round to 10ms.
      zeroPaddingNumber(buf, value, 2);
    } else {
      zeroPaddingNumber(buf, value, 3);

      if (count > 3) {
        zeroPaddingNumber(buf, 0, count - 3);
      }
    }
  }

  /**
   * Formats Minutes field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatMinutes(StringBuffer buf, int count, Date date) {
    int value = date.getMinutes();
    zeroPaddingNumber(buf, value, count);
  }

  /**
   * Formats Month field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatMonth(StringBuffer buf, int count, Date date) {
    int value = date.getMonth();
    switch (count) {
      case 5:
        buf.append(dateTimeConstants.narrowMonths()[value]);
        break;
      case 4:
        buf.append(dateTimeConstants.standaloneMonths()[value]);
        break;
      case 3:
        buf.append(dateTimeConstants.shortMonths()[value]);
        break;
      default:
        zeroPaddingNumber(buf, value + 1, count);
    }
  }

  /**
   * Formats Quarter field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatQuarter(StringBuffer buf, int count, Date date) {
    int value = date.getMonth() / 3;
    if (count < 4) {
      buf.append(dateTimeConstants.shortQuarters()[value]);
    } else {
      buf.append(dateTimeConstants.quarters()[value]);
    }
  }

  /**
   * Formats Seconds field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatSeconds(StringBuffer buf, int count, Date date) {
    int value = date.getSeconds();
    zeroPaddingNumber(buf, value, count);
  }

  /**
   * Formats Standalone weekday field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatStandaloneDay(StringBuffer buf, int count, Date date) {
    int value = date.getDay();
    if (count == 5) {
      buf.append(dateTimeConstants.standaloneNarrowWeekdays()[value]);
    } else if (count == 4) {
      buf.append(dateTimeConstants.standaloneWeekdays()[value]);
    } else if (count == 3) {
      buf.append(dateTimeConstants.standaloneShortWeekdays()[value]);
    } else {
      zeroPaddingNumber(buf, value, 1);
    }
  }

  /**
   * Formats Standalone Month field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatStandaloneMonth(StringBuffer buf, int count, Date date) {
    int value = date.getMonth();
    if (count == 5) {
      buf.append(dateTimeConstants.standaloneNarrowMonths()[value]);
    } else if (count == 4) {
      buf.append(dateTimeConstants.standaloneMonths()[value]);
    } else if (count == 3) {
      buf.append(dateTimeConstants.standaloneShortMonths()[value]);
    } else {
      zeroPaddingNumber(buf, value + 1, count);
    }
  }

  /**
   * Formats Timezone field following RFC.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatTimeZoneRFC(StringBuffer buf, int count, Date date) {
    if (count < 4) {
      // 'short' (standard Java) form, must use ASCII digits
      int val = date.getTimezoneOffset();
      char sign = '-';
      if (val < 0) {
        val = -val;
        sign = '+';
      }

      val = (val / 3) * 5 + (val % MINUTES_PER_HOUR); // minutes => KKmm
      buf.append(sign);
      zeroPaddingNumber(buf, val, 4);
    } else {
      appendGMT(buf, date);
    }
  }

  /**
   * Formats Year field according to pattern specified. Javascript Date object
   * seems incapable handling 1BC and year before. It can show you year 0 which
   * does not exists. following we just keep consistent with javascript's
   * toString method. But keep in mind those things should be unsupported.
   * 
   * @param buf where formatted string will be appended to
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   */
  private void formatYear(StringBuffer buf, int count, Date date) {
    int value = date.getYear() + JS_START_YEAR;
    if (value < 0) {
      value = -value;
    }
    if (count == 2) {
      zeroPaddingNumber(buf, value % 100, 2);
    } else {
      // count != 2
      buf.append(Integer.toString(value));
    }
  }

  /**
   * Method getNextCharCountInPattern calculate character repeat count in
   * pattern.
   * 
   * @param pattern describe the format of date string that need to be parsed
   * @param start the position of pattern character
   * @return repeat count
   */
  private int getNextCharCountInPattern(String pattern, int start) {
    char ch = pattern.charAt(start);
    int next = start + 1;
    while (next < pattern.length() && pattern.charAt(next) == ch) {
      ++next;
    }
    return next - start;
  }

  /**
   * Method identifies the start of a run of abutting numeric fields. Take the
   * pattern "HHmmss" as an example. We will try to parse 2/2/2 characters of
   * the input text, then if that fails, 1/2/2. We only adjust the width of the
   * leftmost field; the others remain fixed. This allows "123456" => 12:34:56,
   * but "12345" => 1:23:45. Likewise, for the pattern "yyyyMMdd" we try 4/2/2,
   * 3/2/2, 2/2/2, and finally 1/2/2. The first field of connected numeric
   * fields will be marked as abutStart, its width can be reduced to accomodate
   * others.
   */
  private void identifyAbutStart() {
    // 'abut' parts are continuous numeric parts. abutStart is the switch
    // point from non-abut to abut.
    boolean abut = false;

    int len = patternParts.size();
    for (int i = 0; i < len; i++) {
      if (isNumeric(patternParts.get(i))) {
        // If next part is not following abut sequence, and isNumeric.
        if (!abut && i + 1 < len
            && isNumeric(patternParts.get(i + 1))) {
          abut = true;
          patternParts.get(i).abutStart = true;
        }
      } else {
        abut = false;
      }
    }
  }

  /**
   * Method checks if the pattern part is a numeric field.
   * 
   * @param part pattern part to be examined
   * @return <code>true</code> if the pattern part is numberic field
   */
  private final boolean isNumeric(PatternPart part) {
    if (part.count <= 0) {
      return false;
    }
    int i = NUMERIC_FORMAT_CHARS.indexOf(part.text.charAt(0));
    return (i > 0 || (i == 0 && part.count < 3));
  }

  /**
   * Method attempts to match the text at a given position against an array of
   * strings. Since multiple strings in the array may match (for example, if the
   * array contains "a", "ab", and "abc", all will match the input string
   * "abcd") the longest match is returned.
   * 
   * @param text the time text being parsed
   * @param start where to start parsing
   * @param data the string array to parsed
   * @param pos to receive where the match stopped
   * @return the new start position if matching succeeded; a negative number
   *         indicating matching failure
   */
  private int matchString(String text, int start, String[] data, int[] pos) {
    int count = data.length;

    // There may be multiple strings in the data[] array which begin with
    // the same prefix (e.g., Cerven and Cervenec (June and July) in Czech).
    // We keep track of the longest match, and return that. Note that this
    // unfortunately requires us to test all array elements.
    int bestMatchLength = 0, bestMatch = -1;
    String textInLowerCase = text.substring(start).toLowerCase();
    for (int i = 0; i < count; ++i) {
      int length = data[i].length();
      // Always compare if we have no match yet; otherwise only compare
      // against potentially better matches (longer strings).
      if (length > bestMatchLength
          && textInLowerCase.startsWith(data[i].toLowerCase())) {
        bestMatch = i;
        bestMatchLength = length;
      }
    }
    if (bestMatch >= 0) {
      pos[0] = start + bestMatchLength;
    }
    return bestMatch;
  }

  /**
   * Method parses a integer string and return integer value.
   * 
   * @param text string being parsed
   * @param pos parse position
   * 
   * @return integer value
   */
  private int parseInt(String text, int[] pos) {
    int ret = 0;
    int ind = pos[0];
    char ch = text.charAt(ind);
    while (ch >= '0' && ch <= '9') {
      ret = ret * 10 + (ch - '0');
      ind++;
      if (ind >= text.length()) {
        break;
      }
      ch = text.charAt(ind);
    }
    if (ind > pos[0]) {
      pos[0] = ind;
    } else {
      ret = -1;
    }
    return ret;
  }

  /**
   * Method parses the input pattern string a generate a vector of pattern
   * parts.
   * 
   * @param pattern describe the format of date string that need to be parsed
   */
  private void parsePattern(String pattern) {
    StringBuffer buf = new StringBuffer(32);
    boolean inQuote = false;

    for (int i = 0; i < pattern.length(); i++) {
      char ch = pattern.charAt(i);

      // Handle space, add literal part (if exist), and add space part.
      if (ch == ' ') {
        addPart(buf, 0);
        buf.append(' ');
        addPart(buf, 0);
        while (i + 1 < pattern.length() && pattern.charAt(i + 1) == ' ') {
          i++;
        }
        continue;
      }

      // If inside quote, except two quote connected, just copy or exit.
      if (inQuote) {
        if (ch == '\'') {
          if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
            // Quote appeared twice continuously, interpret as one quote.
            buf.append(ch);
            ++i;
          } else {
            inQuote = false;
          }
        } else {
          // Literal.
          buf.append(ch);
        }
        continue;
      }

      // Outside quote now.
      if (PATTERN_CHARS.indexOf(ch) > 0) {
        addPart(buf, 0);
        buf.append(ch);
        int count = getNextCharCountInPattern(pattern, i);
        addPart(buf, count);
        i += count - 1;
        continue;
      }

      // Two consecutive quotes is a quote literal, inside or outside of quotes.
      if (ch == '\'') {
        if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
          buf.append('\'');
          i++;
        } else {
          inQuote = true;
        }
      } else {
        buf.append(ch);
      }
    }

    addPart(buf, 0);

    identifyAbutStart();
  }

  /**
   * Method parses time zone offset.
   * 
   * @param text the time text to be parsed
   * @param pos Parse position
   * @param cal DateRecord object that holds parsed value
   * 
   * @return <code>true</code> if parsing successful, otherwise
   *         <code>false</code>
   */
  private boolean parseTimeZoneOffset(String text, int[] pos, DateRecord cal) {
    if (pos[0] >= text.length()) {
      cal.setTzOffset(0);
      return true;
    }

    int sign;
    switch (text.charAt(pos[0])) {
      case '+':
        sign = 1;
        break;
      case '-':
        sign = -1;
        break;
      default:
        cal.setTzOffset(0);
        return true;
    }
    ++(pos[0]);

    // Look for hours:minutes or hhmm.
    int st = pos[0];
    int value = parseInt(text, pos);
    if (value == 0 && pos[0] == st) {
      return false;
    }

    int offset;
    if (pos[0] < text.length() && text.charAt(pos[0]) == ':') {
      // This is the hours:minutes case.
      offset = value * MINUTES_PER_HOUR;
      ++(pos[0]);
      st = pos[0];
      value = parseInt(text, pos);
      if (value == 0 && pos[0] == st) {
        return false;
      }
      offset += value;
    } else {
      // This is the hhmm case.
      offset = value;
      // Assume "-23".."+23" refers to hours.
      if (offset < 24 && (pos[0] - st) <= 2) {
        offset *= MINUTES_PER_HOUR;
      } else {
        offset = offset % 100 + offset / 100 * MINUTES_PER_HOUR;
      }
    }

    offset *= sign;
    cal.setTzOffset(-offset);
    return true;
  }

  /**
   * Method skips space in the string as pointed by pos.
   * 
   * @param text input string
   * @param pos where skip start, and return back where skip stop
   */
  private void skipSpace(String text, int[] pos) {
    while (pos[0] < text.length()
        && WHITE_SPACE.indexOf(text.charAt(pos[0])) >= 0) {
      ++(pos[0]);
    }
  }

  /**
   * Formats a single field according to pattern specified.
   * 
   * @param buf where formatted string will be appended to
   * @param ch pattern for this field
   * @param count number of time pattern char repeats; this controls how a field
   *          should be formatted
   * @param date hold the date object to be formatted
   * 
   * @return <code>true</code> if pattern valid, otherwise <code>false</code>
   */
  private boolean subFormat(StringBuffer buf, char ch, int count, Date date) {
    switch (ch) {
      case 'G':
        formatEra(buf, count, date);
        break;
      case 'y':
        formatYear(buf, count, date);
        break;
      case 'M':
        formatMonth(buf, count, date);
        break;
      case 'k':
        format24Hours(buf, count, date);
        break;
      case 'S':
        formatFractionalSeconds(buf, count, date);
        break;
      case 'E':
        formatDayOfWeek(buf, count, date);
        break;
      case 'a':
        formatAmPm(buf, count, date);
        break;
      case 'h':
        format1To12Hours(buf, count, date);
        break;
      case 'K':
        format0To11Hours(buf, count, date);
        break;
      case 'H':
        format0To23Hours(buf, count, date);
        break;
      case 'c':
        formatStandaloneDay(buf, count, date);
        break;
      case 'L':
        formatStandaloneMonth(buf, count, date);
        break;
      case 'Q':
        formatQuarter(buf, count, date);
        break;
      case 'd':
        formatDate(buf, count, date);
        break;
      case 'm':
        formatMinutes(buf, count, date);
        break;
      case 's':
        formatSeconds(buf, count, date);
        break;
      case 'z':
      case 'v':
        appendGMT(buf, date);
        break;
      case 'Z':
        formatTimeZoneRFC(buf, count, date);
        break;
      default:
        return false;
    }
    return true;
  }

  /**
   * Converts one field of the input string into a numeric field value. Returns
   * <code>false</code> if failed.
   * 
   * @param text the time text to be parsed
   * @param pos Parse position
   * @param part the pattern part for this field
   * @param digitCount when greater than 0, numeric parsing must obey the count
   * @param cal DateRecord object that will hold parsed value
   * 
   * @return <code>true</code> if parsing successful
   */
  @SuppressWarnings("fallthrough")
  private boolean subParse(String text, int[] pos, PatternPart part,
      int digitCount, DateRecord cal) {

    skipSpace(text, pos);

    int start = pos[0];
    char ch = part.text.charAt(0);

    // Parse integer value if it is a numeric field.
    int value = -1; // initialize value to be -1,
    if (isNumeric(part)) {
      if (digitCount > 0) {
        if ((start + digitCount) > text.length()) {
          return false;
        }
        value = parseInt(text.substring(0, start + digitCount), pos);
      } else {
        value = parseInt(text, pos);
      }
    }

    switch (ch) {
      case 'G': // 'G' - ERA
        value = matchString(text, start, dateTimeConstants.eras(), pos);
        cal.setEra(value);
        return true;
      case 'M': // 'M' - MONTH
        return subParseMonth(text, pos, cal, value, start);
      case 'E':
        return subParseDayOfWeek(text, pos, start, cal);
      case 'a': // 'a' - AM_PM
        value = matchString(text, start, dateTimeConstants.ampms(), pos);
        cal.setAmpm(value);
        return true;
      case 'y': // 'y' - YEAR
        return subParseYear(text, pos, start, value, part, cal);
      case 'd': // 'd' - DATE
        cal.setDayOfMonth(value);
        return true;
      case 'S': // 'S' - FRACTIONAL_SECOND
        return subParseFractionalSeconds(value, start, pos[0], cal);
      case 'h': // 'h' - HOUR (1..12)
        if (value == 12) {
          value = 0;
        }
        // fall through
      case 'K': // 'K' - HOUR (0..11)
      case 'H': // 'H' - HOUR_OF_DAY (0..23)
        cal.setHours(value);
        return true;
      case 'k': // 'k' - HOUR_OF_DAY (1..24)
        cal.setHours(value);
        return true;
      case 'm': // 'm' - MINUTE
        cal.setMinutes(value);
        return true;
      case 's': // 's' - SECOND
        cal.setSeconds(value);
        return true;

      case 'z': // 'z' - ZONE_OFFSET
      case 'Z': // 'Z' - TIMEZONE_RFC
      case 'v': // 'v' - TIMEZONE_GENERIC
        return subParseTimeZoneInGMT(text, start, pos, cal);
      default:
        return false;
    }
  }

  /**
   * Method subParseDayOfWeek parses day of the week field.
   * 
   * @param text the time text to be parsed
   * @param pos Parse position
   * @param start from where parse start
   * @param cal DateRecord object that holds parsed value
   * 
   * @return <code>true</code> if parsing successful, otherwise
   *         <code>false</code>
   */
  private boolean subParseDayOfWeek(String text, int[] pos, int start,
      DateRecord cal) {
    int value;
    // 'E' - DAY_OF_WEEK
    // Want to be able to parse both short and long forms.
    // Try count == 4 (DDDD) first:
    value = matchString(text, start, dateTimeConstants.weekdays(), pos);
    if (value < 0) {
      value = matchString(text, start, dateTimeConstants.shortWeekdays(), pos);
    }
    if (value < 0) {
      return false;
    }
    cal.setDayOfWeek(value);
    return true;
  }

  /**
   * Method subParseFractionalSeconds parses fractional seconds field.
   * 
   * @param value parsed numberic value
   * @param start
   * @param end parse position
   * @param cal DateRecord object that holds parsed value
   * @return <code>true</code> if parsing successful, otherwise
   *         <code>false</code>
   */
  private boolean subParseFractionalSeconds(int value, int start, int end,
      DateRecord cal) {
    // Fractional seconds left-justify.
    int i = end - start;
    if (i < 3) {
      while (i < 3) {
        value *= 10;
        i++;
      }
    } else {
      int a = 1;
      while (i > 3) {
        a *= 10;
        i--;
      }
      value = (value + (a >> 1)) / a;
    }
    cal.setMilliseconds(value);
    return true;
  }

  /**
   * Method subParseMonth parses Month field.
   * 
   * @param text the time text to be parsed
   * @param pos Parse position
   * @param cal DateRecord object that will hold parsed value
   * @param value numeric value if this field is expressed using numberic
   *          pattern
   * @param start from where parse start
   * 
   * @return <code>true</code> if parsing successful
   */
  private boolean subParseMonth(String text, int[] pos, DateRecord cal,
      int value, int start) {
    // When month is symbols, i.e., MMM or MMMM, value will be -1.
    if (value < 0) {
      // Want to be able to parse both short and long forms.
      // Try count == 4 first:
      value = matchString(text, start, dateTimeConstants.months(), pos);
      if (value < 0) { // count == 4 failed, now try count == 3.
        value = matchString(text, start, dateTimeConstants.shortMonths(), pos);
      }
      if (value < 0) {
        return false;
      }
      cal.setMonth(value);
      return true;
    } else {
      cal.setMonth(value - 1);
      return true;
    }
  }

  /**
   * Method parses GMT type timezone.
   * 
   * @param text the time text to be parsed
   * @param start from where parse start
   * @param pos Parse position
   * @param cal DateRecord object that holds parsed value
   * 
   * @return <code>true</code> if parsing successful, otherwise
   *         <code>false</code>
   */
  private boolean subParseTimeZoneInGMT(String text, int start, int[] pos,
      DateRecord cal) {
    // First try to parse generic forms such as GMT-07:00. Do this first
    // in case localized DateFormatZoneData contains the string "GMT"
    // for a zone; in that case, we don't want to match the first three
    // characters of GMT+/-HH:MM etc.

    // For time zones that have no known names, look for strings
    // of the form:
    // GMT[+-]hours:minutes or
    // GMT[+-]hhmm or
    // GMT.
    if (text.startsWith(GMT, start)) {
      pos[0] = start + GMT.length();
      return parseTimeZoneOffset(text, pos, cal);
    }

    // At this point, check for named time zones by looking through
    // the locale data from the DateFormatZoneData strings.
    // Want to be able to parse both short and long forms.
    /*
     * i = subParseZoneString(text, start, cal); if (i != 0) return i;
     */

    // As a last resort, look for numeric timezones of the form
    // [+-]hhmm as specified by RFC 822. This code is actually
    // a little more permissive than RFC 822. It will try to do
    // its best with numbers that aren't strictly 4 digits long.
    return parseTimeZoneOffset(text, pos, cal);
  }

  /**
   * Method subParseYear parse year field. Year field is special because 1, two
   * digit year need to be resolved. 2, we allow year to take a sign. 3, year
   * field participate in abut processing. In my testing, negative year does not
   * seem working due to JDK (or redpill implementation) limitation. It is not a
   * big deal so we don't worry about it. But keep the logic here so that we
   * might want to replace DateRecord with our a calendar class.
   * 
   * @param text the time text to be parsed
   * @param pos parse position
   * @param start where this field star
   * @param value integer value of yea
   * @param part the pattern part for this field
   * @param cal DateRecord object that will hold parsed value
   * 
   * @return <code>true</code> if successful
   */
  private boolean subParseYear(String text, int[] pos, int start, int value,
      PatternPart part, DateRecord cal) {
    char ch = ' ';
    if (value < 0) {
      ch = text.charAt(pos[0]);
      // Check if it is a sign.
      if (ch != '+' && ch != '-') {
        return false;
      }
      ++(pos[0]);
      value = parseInt(text, pos);
      if (value < 0) {
        return false;
      }
      if (ch == '-') {
        value = -value;
      }
    }

    // no sign, only 2 digit was actually parsed, pattern say it has 2 digit.
    if (ch == ' ' && (pos[0] - start) == 2 && part.count == 2) {
      // Assume for example that the defaultCenturyStart is 6/18/1903.
      // This means that two-digit years will be forced into the range
      // 6/18/1903 to 6/17/2003. As a result, years 00, 01, and 02
      // correspond to 2000, 2001, and 2002. Years 04, 05, etc. correspond
      // to 1904, 1905, etc. If the year is 03, then it is 2003 if the
      // other fields specify a date before 6/18, or 1903 if they specify a
      // date afterwards. As a result, 03 is an ambiguous year. All other
      // two-digit years are unambiguous.
      Date date = new Date();
      int defaultCenturyStartYear = date.getYear() + 1900 - 80;
      int ambiguousTwoDigitYear = defaultCenturyStartYear % 100;
      cal.setAmbiguousYear(value == ambiguousTwoDigitYear);
      value += (defaultCenturyStartYear / 100) * 100
          + (value < ambiguousTwoDigitYear ? 100 : 0);
    }
    cal.setYear(value);
    return true;
  }

  /**
   * Formats a number with the specified minimum number of digits, using zero to
   * fill the gap.
   * 
   * @param buf where zero padded string will be written to
   * @param value the number value being formatted
   * @param minWidth minimum width of the formatted string; zero will be padded
   *          to reach this width
   */
  private void zeroPaddingNumber(StringBuffer buf, int value, int minWidth) {
    int b = NUMBER_BASE;
    for (int i = 0; i < minWidth - 1; i++) {
      if (value < b) {
        buf.append('0');
      }
      b *= NUMBER_BASE;
    }
    buf.append(Integer.toString(value));
  }
}
