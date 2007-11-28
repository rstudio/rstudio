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

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

/**
 * Tests formatting functionality in {@link DateTimeFormat} for the German
 * language.
 */
public class DateTimeFormat_de_Test extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_de";
  }

  public void test_EEEEMMMddyy() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("Donnerstag,Juli 27, 2006", format("EEEE,MMMM dd, yyyy", date));
  }

  public void test_EEEMMMddyy() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("Do, Jul 27, 06", format("EEE, MMM d, yy", date));
  }

  public void test_HHmmss() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("13:10:10", format("HH:mm:ss", date));
  }

  public void test_hhmmssa() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("1:10:10 nachm.", format("h:mm:ss a", date));
  }

  public void test_MMddyyyyHHmmssZ() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    String tz = getTimezoneString(date, false);
    String actual = format("MM/dd/yyyy HH:mm:ss Z", date);
    assertEquals("07/27/2006 13:10:10 " + tz, actual);
  }

  public void test_MMddyyyyHHmmsszzz() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    String tz = getTimezoneString(date);
    String actual = format("MM/dd/yyyy HH:mm:ss zzz", date);
    assertEquals("07/27/2006 13:10:10 GMT" + tz, actual);
  }

  public void test_predefinedFormat() {
    Date date = getDateFromUTC(2006 - 1900, 7, 4, 13, 49, 24);
    String tz = getTimezoneString(date);

    String fullDateFormat = DateTimeFormat.getFullDateFormat().format(date);
    assertEquals("Freitag, 4. August 2006", fullDateFormat);

    String longDateFormat = DateTimeFormat.getLongDateFormat().format(date);
    assertEquals("4. August 2006", longDateFormat);

    String medDateFormat = DateTimeFormat.getMediumDateFormat().format(date);
    assertEquals("04.08.2006", medDateFormat);

    String shortDateFormat = DateTimeFormat.getShortDateFormat().format(date);
    assertEquals("04.08.06", shortDateFormat);

    String fullTimeFormat = DateTimeFormat.getFullTimeFormat().format(date);
    assertEquals("13:49 Uhr GMT" + tz, fullTimeFormat);

    String longTimeFormat = DateTimeFormat.getLongTimeFormat().format(date);
    assertEquals("13:49:24 GMT" + tz, longTimeFormat);

    String medTimeFormat = DateTimeFormat.getMediumTimeFormat().format(date);
    assertEquals("13:49:24", medTimeFormat);

    String shortTimeFormat = DateTimeFormat.getShortTimeFormat().format(date);
    assertEquals("13:49", shortTimeFormat);

    String fullFormat = DateTimeFormat.getFullDateTimeFormat().format(date);
    assertEquals("Freitag, 4. August 2006 13:49 Uhr GMT" + tz, fullFormat);

    String longFormat = DateTimeFormat.getLongDateTimeFormat().format(date);
    assertEquals("4. August 2006 13:49:24 GMT" + tz, longFormat);

    String medFormat = DateTimeFormat.getMediumDateTimeFormat().format(date);
    assertEquals("04.08.2006 13:49:24", medFormat);

    String shortFormat = DateTimeFormat.getShortDateTimeFormat().format(date);
    assertEquals("04.08.06 13:49", shortFormat);
  }

  public void test_QQQQyy() {
    Date date;

    date = getDateFromUTC(2006 - 1900, 0, 27, 13, 10, 10);
    assertEquals("1. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 1, 27, 13, 10, 10);
    assertEquals("1. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 2, 27, 13, 10, 10);
    assertEquals("1. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 3, 27, 13, 10, 10);
    assertEquals("2. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 4, 27, 13, 10, 10);
    assertEquals("2. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 5, 27, 13, 10, 10);
    assertEquals("2. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("3. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 7, 27, 13, 10, 10);
    assertEquals("3. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 8, 27, 13, 10, 10);
    assertEquals("3. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 9, 27, 13, 10, 10);
    assertEquals("4. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 10, 27, 13, 10, 10);
    assertEquals("4. Quartal 06", format("QQQQ yy", date));

    date = getDateFromUTC(2006 - 1900, 11, 27, 13, 10, 10);
    assertEquals("4. Quartal 06", format("QQQQ yy", date));
  }

  public void test_QQyyyy() {
    Date date = new Date(2006 - 1900, 0, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q1 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 1, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q1 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 2, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q1 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 3, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q2 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 4, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q2 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 5, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q2 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 6, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q3 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 7, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q3 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 8, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q3 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 9, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q4 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 10, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q4 2006", format("QQ yyyy", date));
    date = new Date(2006 - 1900, 11, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q4 2006", format("QQ yyyy", date));
  }

  public void test_quote() {
    Date date = new Date(2006 - 1900, 6, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("13 o'clock", format("HH 'o''clock'", date));
    assertEquals("13 oclock", format("HH 'oclock'", date));
    assertEquals("13 '", format("HH ''", date));
  }

  public void test_yyyyMMddG() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    String tz = getTimezoneString(date);

    String expected = "2006.07.27 n. Chr. at 13:10:10 GMT" + tz;
    String actual = format("yyyy.MM.dd G 'at' HH:mm:ss vvvv", date);
    assertEquals(expected, actual);
  }

  public void test_yyyyyMMMMM() {
    Date date = getDateFromUTC(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("2006.J.27 n. Chr. 01:10 nachm.", format(
        "yyyyy.MMMMM.dd GGG hh:mm aaa", date));
  }

  private String format(String pattern, Date toFormat) {
    DateTimeFormat fmt = DateTimeFormat.getFormat(pattern);
    return fmt.format(toFormat);
  }

  private String getTimezoneString(Date date) {
    return getTimezoneString(date, true);
  }

  private String getTimezoneString(Date date, boolean useColon) {
    int tzOffset = date.getTimezoneOffset();
    int tzAbsOffset = Math.abs(tzOffset);
    int tzAbsOffsetHrs = tzAbsOffset / 60;
    int tzAbsOffsetMins = tzAbsOffset % 60;
    String tzOffsetStr = (tzOffset < 0 ? "+" : "-")
        + (tzAbsOffsetHrs < 10 ? "0" : "") + Integer.toString(tzAbsOffsetHrs)
        + (useColon ? ":" : "") + (tzAbsOffsetMins < 10 ? "0" : "")
        + Integer.toString(tzAbsOffsetMins);
    return tzOffsetStr;
  }

  private Date getDateFromUTC(int year, int month, int day, int hrs, int mins,
      int secs) {
    long localTzOffset = new Date(year, month, day, hrs, mins, secs).getTimezoneOffset();
    long utc = Date.UTC(year, month, day, hrs, mins, secs);
    long localTzOffsetMillis = localTzOffset * 60 * 1000;
    long localTime = utc + localTzOffsetMillis;
    return new Date(localTime);
  }
}
