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

import com.google.gwt.i18n.client.constants.DateTimeConstants;
import com.google.gwt.i18n.client.impl.cldr.DateTimeFormatInfoImpl_en;

import java.util.HashMap;
import java.util.Map;

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
 * <td><code>L</code></td>
 * <td>standalone month in year</td>
 * <td>Text or Number</td>
 * <td><code>July (or) 07</code></td>
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
 * <td><code>c</code></td>
 * <td>standalone day of week</td>
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
 * <td><code>Pacific Standard Time(see comment)</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>Z</code></td>
 * <td>time zone (RFC 822)</td>
 * <td>Text</td>
 * <td><code>-0800(See comment)</code></td>
 * </tr>
 * 
 * <tr>
 * <td><code>v</code></td>
 * <td>time zone id</td>
 * <td>Text</td>
 * <td><code>America/Los_Angeles(See comment)</code></td>
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
 * amount (e.g. if <code>"m"</code> produces <code>"6"</code>, <code>"mm"</code>
 * produces <code>"06"</code>). Year is handled specially; that is, if the count
 * of 'y' is 2, the Year will be truncated to 2 digits. (e.g., if
 * <code>"yyyy"</code> produces <code>"1997"</code>, <code>"yy"</code> produces
 * <code>"97"</code>.) Unlike other fields, fractional seconds are padded on the
 * right with zero.</dd>
 * 
 * <dt>Text or Number</dt>
 * <dd>3 or more, use text, otherwise use number. (e.g. <code>"M"</code>
 * produces <code>"1"</code>, <code>"MM"</code> produces <code>"01"</code>,
 * <code>"MMM"</code> produces <code>"Jan"</code>, and <code>"MMMM"</code>
 * produces <code>"January"</code>.  Some pattern letters also treat a count
 * of 5 specially, meaning a single-letter abbreviation: <code>L</code>,
 * <code>M</code>, <code>E</code>, and <code>c</code>.</dd>
 * </dl>
 * 
 * <p>
 * Any characters in the pattern that are not in the ranges of ['<code>a</code>
 * '..'<code>z</code>'] and ['<code>A</code>'..'<code>Z</code>'] will be treated
 * as quoted text. For instance, characters like '<code>:</code>', '
 * <code>.</code>', '<code> </code>' (space), '<code>#</code>' and '
 * <code>@</code>' will appear in the resulting time text even they are not
 * embraced within single quotes.
 * </p>
 * 
 * <p>
 * [Time Zone Handling] Web browsers don't provide all the information we need
 * for proper time zone formating -- so GWT has a copy of the required data, for
 * your convenience. For simpler cases, one can also use a fallback
 * implementation that only keeps track of the current timezone offset. These
 * two approaches are called, respectively, Common TimeZones and Simple
 * TimeZones, although both are implemented with the same TimeZone class.
 * 
 * "TimeZone createTimeZone(String timezoneData)" returns a Common TimeZone
 * object, and "TimeZone createTimeZone(int timeZoneOffsetInMinutes)" returns a
 * Simple TimeZone object. The one provided by OS fall into to Simple TimeZone
 * category. For formatting purpose, following table shows the behavior of GWT
 * DateTimeFormat.
 * </p>
 * <table>
 * <tr>
 * <th>Pattern</th>
 * <th>Common TimeZone</th>
 * <th>Simple TimeZone</th>
 * </tr>
 * <tr>
 * <td>z, zz, zzz</td>
 * <td>PDT</td>
 * <td>UTC-7</td>
 * </tr>
 * <tr>
 * <td>zzzz</td>
 * <td>Pacific Daylight Time</td>
 * <td>UTC-7</td>
 * </tr>
 * <tr>
 * <td>Z, ZZ</td>
 * <td>-0700</td>
 * <td>-0700</td>
 * </tr>
 * <tr>
 * <td>ZZZ</td>
 * <td>-07:00</td>
 * <td>-07:00</td>
 * </tr>
 * <tr>
 * <td>ZZZZ</td>
 * <td>GMT-07:00</td>
 * <td>GMT-07:00</td>
 * </tr>
 * <tr>
 * <td>v, vv, vvv, vvvv</td>
 * <td>America/Los_Angeles</td>
 * <td>Etc/GMT+7</td>
 * </tr>
 * </table>
 * 
 * <h3>Parsing Dates and Times</h3>
 * <p>
 * The pattern does not need to specify every field.  If the year, month, or
 * day is missing from the pattern, the corresponding value will be taken from
 * the current date.  If the month is specified but the day is not, the day will
 * be constrained to the last day within the specified month.  If the hour,
 * minute, or second is missing, the value defaults to zero.
 * </p>
 * 
 * <p>
 * As with formatting (described above), the count of pattern letters determines
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
 * all letters, it may in the future. It is strongly discouraged to use
 * unspecified letters as literal text without quoting them.
 * </p>
 * <p>
 * [Note on TimeZone] The time zone support for parsing is limited. Only
 * standard GMT and RFC format are supported. Time zone specification using time
 * zone id (like America/Los_Angeles), time zone names (like PST, Pacific
 * Standard Time) are not supported. Normally, it is too much a burden for a
 * client application to load all the time zone symbols. And in almost all those
 * cases, it is a better choice to do such parsing on server side through
 * certain RPC mechanism. This decision is based on particular use cases we have
 * studied; in principle, it could be changed in future versions.
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
 * <td><code>1996.07.10 AD at 15:08:56 America/Los_Angeles</code></td>
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
 * <td><code>"K:mm a, vvvv"</code></td>
 * <td><code> 0:00 PM, America/Los_Angeles</code></td>
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
 * When parsing a date string using the abbreviated year pattern (
 * <code>"yy"</code>), the parser must interpret the abbreviated year relative
 * to some century. It does this by adjusting dates to be within 80 years before
 * and 20 years after the time the parser instance is created. For example,
 * using a pattern of <code>"MM/dd/yy"</code> and a <code>DateTimeFormat</code>
 * object created on Jan 1, 1997, the string <code>"01/11/12"</code> would be
 * interpreted as Jan 11, 2012 while the string <code>"05/04/64"</code> would be
 * interpreted as May 4, 1964. During parsing, only strings consisting of
 * exactly two digits, as defined by {@link java.lang.Character#isDigit(char)},
 * will be parsed into the default century. If the year pattern does not have
 * exactly two 'y' characters, the year is interpreted literally, regardless of
 * the number of digits. For example, using the pattern
 * <code>"MM/dd/yyyy"</code>, "01/11/12" parses to Jan 11, 12 A.D.
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
 * <code>GMT:hhmm</code>, <code>GMT:+hhmm</code>, and <code>GMT:-hhmm</code>.
 * </p>
 * 
 * <h3>Example</h3> {@example com.google.gwt.examples.DateTimeFormatExample}
 *
 * deprecated use {@link com.google.gwt.i18n.shared.DateTimeFormat} instead
 */
// Temporarily remove deprecation to keep from breaking teams that don't allow
// deprecated references.
// @Deprecated
public class DateTimeFormat extends com.google.gwt.i18n.shared.DateTimeFormat {

  /**
   * Predefined date/time formats -- see {@link CustomDateTimeFormat} if you
   * need some format that isn't supplied here.
   *
   * deprecated use {@link com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat} instead
   */
   // Temporarily remove deprecation to keep from breaking teams that don't allow
   // deprecated references.
   // @Deprecated
  public enum PredefinedFormat {
    // TODO(jat): Javadoc to explain these formats

    /**
     * ISO 8601 date format, fixed across all locales.
     * <p>Example: {@code 2008-10-03T10:29:40.046-04:00}
     * <p>http://code.google.com/p/google-web-toolkit/issues/detail?id=3068
     * <p>http://www.iso.org/iso/support/faqs/faqs_widely_used_standards/widely_used_standards_other/date_and_time_format.htm
     */
    ISO_8601,

    /**
     * RFC 2822 date format, fixed across all locales.
     * <p>Example: {@code Thu, 20 May 2010 17:54:50 -0700}
     * <p>http://tools.ietf.org/html/rfc2822#section-3.3
     */
    RFC_2822,

    DATE_FULL,
    DATE_LONG,
    DATE_MEDIUM,
    DATE_SHORT,

    TIME_FULL,
    TIME_LONG,
    TIME_MEDIUM,
    TIME_SHORT,

    DATE_TIME_FULL,
    DATE_TIME_LONG,
    DATE_TIME_MEDIUM,
    DATE_TIME_SHORT,

    DAY,
    HOUR_MINUTE,
    HOUR_MINUTE_SECOND,
    HOUR24_MINUTE,
    HOUR24_MINUTE_SECOND,
    MINUTE_SECOND,
    MONTH,
    MONTH_ABBR,
    MONTH_ABBR_DAY,
    MONTH_DAY,
    MONTH_NUM_DAY,
    MONTH_WEEKDAY_DAY,
    YEAR,
    YEAR_MONTH,
    YEAR_MONTH_ABBR,
    YEAR_MONTH_ABBR_DAY,
    YEAR_MONTH_DAY,
    YEAR_MONTH_NUM,
    YEAR_MONTH_NUM_DAY,
    YEAR_MONTH_WEEKDAY_DAY,
    YEAR_QUARTER,
    YEAR_QUARTER_ABBR,
  }

  private static final Map<String, DateTimeFormat> cache;

  static {
    cache = new HashMap<String, DateTimeFormat>();
  }

  /**
   * Get a DateTimeFormat instance for a predefined format.
   * 
   * <p>See {@link CustomDateTimeFormat} if you need a localized format that is
   * not supported here.
   * 
   * @param predef {@link PredefinedFormat} describing desired format
   * @return a DateTimeFormat instance for the specified format
   */
  public static DateTimeFormat getFormat(PredefinedFormat predef) {
    if (usesFixedEnglishStrings(predef)) {
      String pattern;
      switch (predef) {
        case RFC_2822:
          pattern = com.google.gwt.i18n.shared.DateTimeFormat.RFC2822_PATTERN;
          break;
        case ISO_8601:
          pattern = com.google.gwt.i18n.shared.DateTimeFormat.ISO8601_PATTERN;
          break;
        default:
          throw new IllegalStateException("Unexpected predef type " + predef);
      }
      return getFormat(pattern, new DateTimeFormatInfoImpl_en());
    }
    DateTimeFormatInfo dtfi = getDefaultDateTimeFormatInfo();
    String pattern;
    switch (predef) {
      case DATE_FULL:
        pattern = dtfi.dateFormatFull();
        break;
      case DATE_LONG:
        pattern = dtfi.dateFormatLong();
        break;
      case DATE_MEDIUM:
        pattern = dtfi.dateFormatMedium();
        break;
      case DATE_SHORT:
        pattern = dtfi.dateFormatShort();
        break;
      case DATE_TIME_FULL:
        pattern = dtfi.dateTimeFull(dtfi.timeFormatFull(),
            dtfi.dateFormatFull());
        break;
      case DATE_TIME_LONG:
        pattern = dtfi.dateTimeLong(dtfi.timeFormatLong(),
            dtfi.dateFormatLong());
        break;
      case DATE_TIME_MEDIUM:
        pattern = dtfi.dateTimeMedium(dtfi.timeFormatMedium(),
            dtfi.dateFormatMedium());
        break;
      case DATE_TIME_SHORT:
        pattern = dtfi.dateTimeShort(dtfi.timeFormatShort(),
            dtfi.dateFormatShort());
        break;
      case DAY:
        pattern = dtfi.formatDay();
        break;
      case HOUR24_MINUTE:
        pattern = dtfi.formatHour24Minute();
        break;
      case HOUR24_MINUTE_SECOND:
        pattern = dtfi.formatHour24MinuteSecond();
        break;
      case HOUR_MINUTE:
        pattern = dtfi.formatHour12Minute();
        break;
      case HOUR_MINUTE_SECOND:
        pattern = dtfi.formatHour12MinuteSecond();
        break;
      case MINUTE_SECOND:
        pattern = dtfi.formatMinuteSecond();
        break;
      case MONTH:
        pattern = dtfi.formatMonthFull();
        break;
      case MONTH_ABBR:
        pattern = dtfi.formatMonthAbbrev();
        break;
      case MONTH_ABBR_DAY:
        pattern = dtfi.formatMonthAbbrevDay();
        break;
      case MONTH_DAY:
        pattern = dtfi.formatMonthFullDay();
        break;
      case MONTH_NUM_DAY:
        pattern = dtfi.formatMonthNumDay();
        break;
      case MONTH_WEEKDAY_DAY:
        pattern = dtfi.formatMonthFullWeekdayDay();
        break;
      case TIME_FULL:
        pattern = dtfi.timeFormatFull();
        break;
      case TIME_LONG:
        pattern = dtfi.timeFormatLong();
        break;
      case TIME_MEDIUM:
        pattern = dtfi.timeFormatMedium();
        break;
      case TIME_SHORT:
        pattern = dtfi.timeFormatShort();
        break;
      case YEAR:
        pattern = dtfi.formatYear();
        break;
      case YEAR_MONTH:
        pattern = dtfi.formatYearMonthFull();
        break;
      case YEAR_MONTH_ABBR:
        pattern = dtfi.formatYearMonthAbbrev();
        break;
      case YEAR_MONTH_ABBR_DAY:
        pattern = dtfi.formatYearMonthAbbrevDay();
        break;
      case YEAR_MONTH_DAY:
        pattern = dtfi.formatYearMonthFullDay();
        break;
      case YEAR_MONTH_NUM:
        pattern = dtfi.formatYearMonthNum();
        break;
      case YEAR_MONTH_NUM_DAY:
        pattern = dtfi.formatYearMonthNumDay();
        break;
      case YEAR_MONTH_WEEKDAY_DAY:
        pattern = dtfi.formatYearMonthWeekdayDay();
        break;
      case YEAR_QUARTER:
        pattern = dtfi.formatYearQuarterFull();
        break;
      case YEAR_QUARTER_ABBR:
        pattern = dtfi.formatYearQuarterShort();
        break;
      default:
        throw new IllegalArgumentException("Unexpected predefined format "
            + predef);
    }
    return getFormat(pattern, dtfi);
  }

  /**
   * Returns a DateTimeFormat object using the specified pattern. If you need to
   * format or parse repeatedly using the same pattern, it is highly recommended
   * that you cache the returned <code>DateTimeFormat</code> object and reuse it
   * rather than calling this method repeatedly.
   * 
   * <p>Note that the pattern supplied is used as-is -- for example, if you
   * supply "MM/dd/yyyy" as the pattern, that is the order you will get the
   * fields, even in locales where the order is different.  It is recommended to
   * use {@link #getFormat(PredefinedFormat)} instead -- if you use this method,
   * you are taking responsibility for localizing the patterns yourself.
   * 
   * @param pattern string to specify how the date should be formatted
   * 
   * @return a <code>DateTimeFormat</code> object that can be used for format or
   *         parse date/time values matching the specified pattern
   * 
   * @throws IllegalArgumentException if the specified pattern could not be
   *           parsed
   */
  public static DateTimeFormat getFormat(String pattern) {
    return getFormat(pattern, getDefaultDateTimeFormatInfo());
  }

  /**
   * Retrieve the DateTimeFormat object for full date format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_FULL} instead
   */
  @Deprecated
  public static DateTimeFormat getFullDateFormat() {
    return getFormat(PredefinedFormat.DATE_FULL);
  }

  /**
   * Retrieve the DateTimeFormat object for full date and time format. The
   * pattern for this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_TIME_FULL} instead
   */
  @Deprecated
  public static DateTimeFormat getFullDateTimeFormat() {
    return getFormat(PredefinedFormat.DATE_TIME_FULL);
  }

  /**
   * Retrieve the DateTimeFormat object for full time format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#TIME_FULL} instead
   */
  @Deprecated
  public static DateTimeFormat getFullTimeFormat() {
    return getFormat(PredefinedFormat.TIME_FULL);
  }

  /**
   * Retrieve the DateTimeFormat object for long date format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_LONG} instead
   */
  @Deprecated
  public static DateTimeFormat getLongDateFormat() {
    return getFormat(PredefinedFormat.DATE_LONG);
  }

  /**
   * Retrieve the DateTimeFormat object for long date and time format. The
   * pattern for this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_TIME_LONG} instead
   */
  @Deprecated
  public static DateTimeFormat getLongDateTimeFormat() {
    return getFormat(PredefinedFormat.DATE_TIME_LONG);
  }

  /**
   * Retrieve the DateTimeFormat object for long time format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#TIME_LONG} instead
   */
  @Deprecated
  public static DateTimeFormat getLongTimeFormat() {
    return getFormat(PredefinedFormat.TIME_LONG);
  }

  /**
   * Retrieve the DateTimeFormat object for medium date format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_MEDIUM} instead
   */
  @Deprecated
  public static DateTimeFormat getMediumDateFormat() {
    return getFormat(PredefinedFormat.DATE_MEDIUM);
  }

  /**
   * Retrieve the DateTimeFormat object for medium date and time format. The
   * pattern for this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_TIME_MEDIUM} instead
   */
  @Deprecated
  public static DateTimeFormat getMediumDateTimeFormat() {
    return getFormat(PredefinedFormat.DATE_TIME_MEDIUM);
  }

  /**
   * Retrieve the DateTimeFormat object for medium time format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#TIME_MEDIUM} instead
   */
  @Deprecated
  public static DateTimeFormat getMediumTimeFormat() {
    return getFormat(PredefinedFormat.TIME_MEDIUM);
  }

  /**
   * Retrieve the DateTimeFormat object for short date format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_SHORT} instead
   */
  @Deprecated
  public static DateTimeFormat getShortDateFormat() {
    return getFormat(PredefinedFormat.DATE_SHORT);
  }

  /**
   * Retrieve the DateTimeFormat object for short date and time format. The
   * pattern for this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#DATE_TIME_SHORT} instead
   */
  @Deprecated
  public static DateTimeFormat getShortDateTimeFormat() {
    return getFormat(PredefinedFormat.DATE_TIME_SHORT);
  }

  /**
   * Retrieve the DateTimeFormat object for short time format. The pattern for
   * this format is predefined for each locale.
   * 
   * @return A DateTimeFormat object.
   * @deprecated use {@link #getFormat(PredefinedFormat)} with
   *     {@link PredefinedFormat#TIME_SHORT} instead
   */
  @Deprecated
  public static DateTimeFormat getShortTimeFormat() {
    return getFormat(PredefinedFormat.TIME_SHORT);
  }

  /**
   * Internal factory method that provides caching.
   * 
   * @param pattern
   * @param dtfi
   * @return DateTimeFormat instance
   */
  protected static DateTimeFormat getFormat(String pattern,
      DateTimeFormatInfo dtfi) {
    DateTimeFormatInfo defaultDtfi = getDefaultDateTimeFormatInfo();
    DateTimeFormat dtf = null;
    if (dtfi == defaultDtfi) {
      dtf = cache.get(pattern);
    }
    if (dtf == null) {
      dtf = new DateTimeFormat(pattern, dtfi);
      if (dtfi == defaultDtfi) {
        cache.put(pattern, dtf);
      }
    }
    return dtf;
  }

  private static DateTimeFormatInfo getDefaultDateTimeFormatInfo() {
    return LocaleInfo.getCurrentLocale().getDateTimeFormatInfo();
  }

  /**
   * Returns true if the predefined format is one that specifies always using
   * English names/separators.
   * <p>This should be a method on PredefinedFormat, but that would defeat the
   * enum optimizations GWT is currently capable of.
   * @param predef
   * @return true if the specified format requires English names/separators
   */
  private static boolean usesFixedEnglishStrings(PredefinedFormat predef) {
    switch (predef) {
      case RFC_2822:
        return true;
      case ISO_8601:
        return true;
      default:
        return false;
    }
  }

  /**
   * Constructs a format object using the specified pattern and the date time
   * constants for the default locale.
   * 
   * @param pattern string pattern specification
   */
  protected DateTimeFormat(String pattern) {
    this(pattern, getDefaultDateTimeFormatInfo());
  }

  /**
   * Constructs a format object using the specified pattern and user-supplied
   * date time constants.
   * 
   * @param pattern string pattern specification
   * @param dateTimeConstants locale specific symbol collection
   * @deprecated use {@link #DateTimeFormat(String, DateTimeFormatInfo)}
   */
  @Deprecated
  protected DateTimeFormat(String pattern, DateTimeConstants dateTimeConstants) {
    this(pattern, new DateTimeFormatInfoAdapter(dateTimeConstants));
  }

  /**
   * Constructs a format object using the specified pattern and user-supplied
   * date time constants.
   * 
   * @param pattern string pattern specification
   * @param dtfi DateTimeFormatInfo instance to use
   */
  protected DateTimeFormat(String pattern, DateTimeFormatInfo dtfi) {
    super(pattern, dtfi);
  }
}
