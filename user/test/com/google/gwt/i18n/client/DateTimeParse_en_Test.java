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
 * Tests parsing functionality in {@link DateTimeFormat} for the English
 * language.
 */
public class DateTimeParse_en_Test extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_en";
  }

  private int parse(String pattern, String toParse, int startIndex, Date output) {
    DateTimeFormat fmt = DateTimeFormat.getFormat(pattern);
    return fmt.parse(toParse, startIndex, output);
  }

  private String format(String pattern, Date toFormat) {
    DateTimeFormat fmt = DateTimeFormat.getFormat(pattern);
    return fmt.format(toFormat);
  }

  public void testParseConsumesAllCharacters() {
    String toParse = "July 11, 1938";
    DateTimeFormat longDateFormat = DateTimeFormat.getLongDateFormat();

    try {
      Date actualDate = longDateFormat.parse(toParse);
      String actualFormat = longDateFormat.format(actualDate);
      assertEquals(toParse, actualFormat);
    } catch (IllegalArgumentException e) {
      fail("Should not have thrown an exception");
    }

    try {
      String toParseMangled = toParse + " asdfasdfasdf";
      longDateFormat.parse(toParseMangled);
      fail("Should have thrown an exception on failure to parse");
    } catch (IllegalArgumentException e) {
      // Success.
    }
  }

  public void testAbutField() {
    Date date = new Date();

    assertTrue(parse("hhmm", "1122", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmm", "122", 0, date) > 0);
    assertTrue(date.getHours() == 1);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmmss", "112233", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);
    assertTrue(date.getSeconds() == 33);

    assertTrue(parse("hhmmss", "12233", 0, date) > 0);
    assertTrue(date.getHours() == 1);
    assertTrue(date.getMinutes() == 22);
    assertTrue(date.getSeconds() == 33);

    assertTrue(parse("yyyyMMdd", "19991202", 0, date) > 0);
    assertTrue(date.getYear() == (1999 - 1900));
    assertTrue(date.getMonth() == 12 - 1);
    assertTrue(date.getDate() == 02);

    assertTrue(parse("yyyyMMdd", "9991202", 0, date) > 0);
    assertTrue(date.getYear() == (999 - 1900));
    assertTrue(date.getMonth() == 12 - 1);
    assertTrue(date.getDate() == 02);

    assertTrue(parse("yyyyMMdd", "991202", 0, date) > 0);
    assertTrue(date.getYear() == (99 - 1900));
    assertTrue(date.getMonth() == 12 - 1);
    assertTrue(date.getDate() == 02);

    assertTrue(parse("yyyyMMdd", "91202", 0, date) > 0);
    assertTrue(date.getYear() == (9 - 1900));
    assertTrue(date.getMonth() == 12 - 1);
    assertTrue(date.getDate() == 02);
  }

  public void testAmbiguousYear() {
    Date date = new Date();
    if (date.getMonth() == 0 && date.getDate() == 1 || date.getMonth() == 11
        && date.getDate() >= 31) {
      // we are using current time to resolve ambiguous year.
      // This test case is not designed to work on new year's eve and new year.
      return;
    }

    Date orgDate = new Date();
    orgDate.setYear(orgDate.getYear() + 20);

    orgDate.setMonth(0);
    orgDate.setDate(1);
    String str = format("MM/dd/yy", orgDate);
    assertTrue(parse("MM/dd/yy", str, 0, date) > 0);
    assertTrue(date.getYear() == orgDate.getYear());

    orgDate.setMonth(11);
    orgDate.setDate(31);
    str = format("MM/dd/yy", orgDate);
    assertTrue(parse("MM/dd/yy", str, 0, date) > 0);
    assertTrue(date.getYear() + 100 == orgDate.getYear());

    assertTrue(parse("yy,MM,dd", "2097,07,21", 0, date) > 0);
    assertTrue(date.getYear() == 2097 - 1900);
  }

  public void testEnglishDate() {
    Date date = new Date();

    assertTrue(parse("yyyy MMM dd hh:mm", "2006 Jul 10 15:44", 0, date) > 0);
    assertTrue(date.getYear() == 2006 - 1900);
    assertTrue(date.getMonth() == 7 - 1);
    assertTrue(date.getDate() == 10);
    assertTrue(date.getHours() == 15);
    assertTrue(date.getMinutes() == 44);
  }

  /**
   * Add as many tests as you like.
   */
  public void testFractionalSeconds() {
    Date date = new Date();

    assertTrue(parse("hh:mm:ss.SSS", "11:12:13.956", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 12);
    assertTrue(date.getSeconds() == 13);
    assertTrue((date.getTime() % 1000) == 956);

    assertTrue(parse("hh:mm:ss.SSS", "11:12:13.95", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 12);
    assertTrue(date.getSeconds() == 13);
    assertTrue((date.getTime() % 1000) == 950);

    assertTrue(parse("hh:mm:ss.SSS", "11:12:13.9", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 12);
    assertTrue(date.getSeconds() == 13);
    assertTrue((date.getTime() % 1000) == 900);
  }

  public void testHourParsingFHH() {
    Date date = new Date();
    assertTrue(parse("HHmm", "0022", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmm", "1122", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmm", "1222", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmm", "2322", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmm", "2422", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "0022am", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "1122am", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "1222am", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "2322am", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "2422am", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "0022pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "1122pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "1222pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "2322pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("HHmma", "2422pm", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);
  }

  public void testHourParsingFhh() {
    Date date = new Date();

    assertTrue(parse("hhmm", "0022", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmm", "1122", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmm", "1222", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmm", "2322", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmm", "2422", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "0022am", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "1122am", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "1222am", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "2322am", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "2422am", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "0022pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "1122pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "1222pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "2322pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("hhmma", "2422pm", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);
  }

  public void testHourParsingFKK() {
    Date date = new Date();
    assertTrue(parse("KKmm", "0022", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmm", "1122", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmm", "1222", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmm", "2322", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmm", "2422", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "0022am", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "1122am", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "1222am", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "2322am", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "2422am", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "0022pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "1122pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "1222pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "2322pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("KKmma", "2422pm", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);
  }

  public void testHourParsingFkk() {
    Date date = new Date();
    assertTrue(parse("kkmm", "0022", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmm", "1122", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmm", "1222", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmm", "2322", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmm", "2422", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "0022am", 0, date) > 0);
    assertTrue(date.getHours() == 00);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "1122am", 0, date) > 0);
    assertTrue(date.getHours() == 11);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "1222am", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "2322am", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "2422am", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "0022pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "1122pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "1222pm", 0, date) > 0);
    assertTrue(date.getHours() == 12);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "2322pm", 0, date) > 0);
    assertTrue(date.getHours() == 23);
    assertTrue(date.getMinutes() == 22);

    assertTrue(parse("kkmma", "2422pm", 0, date) > 0);
    assertTrue(date.getHours() == 0);
    assertTrue(date.getMinutes() == 22);
  }

  public void testLeapYear() {
    Date date = new Date();

    assertTrue(parse("MMdd, yyyy", "0229, 2001", 0, date) > 0);
    assertTrue(date.getMonth() == 3 - 1);
    assertTrue(date.getDate() == 1);

    assertTrue(parse("MMdd, yyyy", "0229, 2000", 0, date) > 0);
    assertTrue(date.getMonth() == 2 - 1);
    assertTrue(date.getDate() == 29);
  }

  public void testPartialParsing() {
    // Only specify a date
    DateTimeFormat fmt = DateTimeFormat.getFormat("MM-dd-yyyy");
    Date dateActual = new Date(87, 10, 22);
    Date dateOnly = fmt.parse("11-22-1987");
    assertEquals(dateOnly.getHours(), 0);
    assertEquals(dateOnly.getMinutes(), 0);
    assertEquals(dateOnly.getSeconds(), 0);
    assertEquals(dateOnly.getTime(), dateActual.getTime());

    // Only specify a time, should use current date
    fmt = DateTimeFormat.getFormat("hha");
    dateOnly = fmt.parse("4PM");
    assertEquals(dateOnly.getHours(), 16);
    assertEquals(dateOnly.getMinutes(), 0);
    assertEquals(dateOnly.getSeconds(), 0);
  }

  public void testRFC3339() {
    Date date = new Date();

    {
      // Parse a string formatted according to RFC3389.
      String rfc3339str = "1985-04-12T23:20:50.52-08:00";
      assertTrue(parse("yyyy-MM-dd'T'HH:mm:ss.SSZ", rfc3339str, 0, date) > 0);

      // Create the expected time as UTC. The "+8" is due to the tz offset.
      long expectedTimeUTC = Date.UTC(1985 - 1900, 3, 12, 23 + 8, 20, 50);
      Date expectedDate = new Date(expectedTimeUTC);
      assertEquals(expectedDate.getYear(), date.getYear());
      assertEquals(expectedDate.getMonth(), date.getMonth());
      assertEquals(expectedDate.getDate(), date.getDate());
      assertEquals(expectedDate.getHours(), date.getHours());
      assertEquals(expectedDate.getMinutes(), date.getMinutes());
      assertEquals(expectedDate.getSeconds(), date.getSeconds());
      // Make sure our parse captured the extra 520 milliseconds.
      assertEquals(520, date.getTime() % 1000);
    }

    {
      // Parse a string formatted according to RFC3389.
      String rfc3339str = "1985-04-12T23:20:50.52Z";
      if (rfc3339str.endsWith("Z")) {
        StringBuffer strbuf = new StringBuffer(rfc3339str);
        strbuf.deleteCharAt(rfc3339str.length() - 1);
        strbuf.append("+00:00");
        rfc3339str = strbuf.toString();
      }
      assertTrue(parse("yyyy-MM-dd'T'HH:mm:ss.SSZ", rfc3339str, 0, date) > 0);

      // Create the expected time as UTC. The "+0" is because it's already GMT.
      long expectedTimeUTC = Date.UTC(85, 3, 12, 23 + 0, 20, 50);
      Date expectedDate = new Date(expectedTimeUTC);
      assertEquals(expectedDate.getYear(), date.getYear());
      assertEquals(expectedDate.getMonth(), date.getMonth());
      assertEquals(expectedDate.getDate(), date.getDate());
      assertEquals(expectedDate.getHours(), date.getHours());
      assertEquals(expectedDate.getMinutes(), date.getMinutes());
      assertEquals(expectedDate.getSeconds(), date.getSeconds());
      // Make sure our parse captured the extra 520 milliseconds.
      assertEquals(520, date.getTime() % 1000);
    }
  }

  public void testTimeZone() {
    Date date = new Date();

    assertTrue(parse("MM/dd/yyyy, hh:mm:ss zzz",
        "07/21/2003, 11:22:33 GMT-0700", 0, date) > 0);
    int gmtNegative07 = date.getHours();

    assertTrue(parse("MM/dd/yyyy, hh:mm:ss zzz",
        "07/21/2003, 11:22:33 GMT-0600", 0, date) > 0);
    int gmtNegative06 = date.getHours();
    assertTrue((gmtNegative07 + 24 - gmtNegative06) % 24 == 1);

    assertTrue(parse("MM/dd/yyyy, hh:mm:ss zzz",
        "07/21/2003, 11:22:33 GMT-0800", 0, date) > 0);
    int gmtNegative08 = date.getHours();
    assertTrue((gmtNegative08 + 24 - gmtNegative07) % 24 == 1);

    assertTrue(parse("MM/dd/yyyy, HH:mm:ss zzz",
        "07/21/2003, 11:22:33 GMT+0800", 0, date) > 0);
    int gmtPositive08 = date.getHours();
    assertTrue((gmtNegative08 + 24 - gmtPositive08) % 24 == 16);
  }

  public void testWeekDay() {
    Date date = new Date();

    assertTrue(parse("EEEE, MM/dd/yyyy", "Wednesday, 08/16/2006", 0, date) > 0);
    assertTrue(date.getYear() == 2006 - 1900);
    assertTrue(date.getMonth() == 8 - 1);
    assertTrue(date.getDate() == 16);
    assertTrue(parse("EEEE, MM/dd/yyyy", "Tuesday, 08/16/2006", 0, date) == 0);
    assertTrue(parse("EEEE, MM/dd/yyyy", "Thursday, 08/16/2006", 0, date) == 0);
    assertTrue(parse("EEEE, MM/dd/yyyy", "Wed, 08/16/2006", 0, date) > 0);
    assertTrue(parse("EEEE, MM/dd/yyyy", "Wasdfed, 08/16/2006", 0, date) == 0);

    date.setDate(25);
    assertTrue(parse("EEEE, MM/yyyy", "Wed, 09/2006", 0, date) > 0);
    assertTrue(date.getDate() == 27);

    date.setDate(30);
    assertTrue(parse("EEEE, MM/yyyy", "Wed, 09/2006", 0, date) > 0);
    assertTrue(date.getDate() == 27);

    date.setDate(30);
    assertTrue(parse("EEEE, MM/yyyy", "Mon, 09/2006", 0, date) > 0);
    assertTrue(date.getDate() == 25);
  }

  public void testYearParsing() {
    Date date = new Date();

    assertTrue(parse("yyMMdd", "991202", 0, date) > 0);
    assertTrue(date.getYear() == (1999 - 1900));
    assertTrue(date.getMonth() == 12 - 1);
    assertTrue(date.getDate() == 02);

    assertTrue(parse("yyyyMMdd", "20051202", 0, date) > 0);
    assertTrue(date.getYear() == (2005 - 1900));
    assertTrue(date.getMonth() == 12 - 1);
    assertTrue(date.getDate() == 02);
  }

}
