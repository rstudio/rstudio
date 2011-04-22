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
import com.google.gwt.i18n.shared.DateTimeFormatTestBase;

import java.util.Date;

/**
 * Tests formatting functionality in {@link DateTimeFormat} for the German
 * language.
 */
@SuppressWarnings("deprecation")
public class DateTimeFormat_de_Test extends DateTimeFormatTestBase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_de";
  }

  public void test_EEEEMMMddyy() {
    Date date = new Date(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("Donnerstag,Juli 27, 2006", DateTimeFormat.getFormat(
        "EEEE,MMMM dd, yyyy").format(date));
  }

  public void test_EEEMMMddyy() {
    Date date = new Date(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("Do., Jul 27, 06",
        DateTimeFormat.getFormat("EEE, MMM d, yy").format(date));
  }

  public void test_HHmmss() {
    Date date = new Date(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("13:10:10", DateTimeFormat.getFormat("HH:mm:ss").format(date));
  }

  public void test_hhmmssa() {
    Date date = new Date(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("1:10:10 nachm.",
        DateTimeFormat.getFormat("h:mm:ss a").format(date));
  }

  public void test_predefinedFormat() {
    Date date = new Date(2006 - 1900, 7, 4, 13, 49, 24);

    String fullDateFormat = DateTimeFormat.getFullDateFormat().format(date);
    assertEquals("Freitag, 4. August 2006", fullDateFormat);

    String longDateFormat = DateTimeFormat.getLongDateFormat().format(date);
    assertEquals("4. August 2006", longDateFormat);

    String medDateFormat = DateTimeFormat.getMediumDateFormat().format(date);
    assertEquals("04.08.2006", medDateFormat);

    String shortDateFormat = DateTimeFormat.getShortDateFormat().format(date);
    assertEquals("04.08.06", shortDateFormat);

    String medTimeFormat = DateTimeFormat.getMediumTimeFormat().format(date);
    assertEquals("13:49:24", medTimeFormat);

    String shortTimeFormat = DateTimeFormat.getShortTimeFormat().format(date);
    assertEquals("13:49", shortTimeFormat);

    String medFormat = DateTimeFormat.getMediumDateTimeFormat().format(date);
    assertEquals("04.08.2006 13:49:24", medFormat);

    String shortFormat = DateTimeFormat.getShortDateTimeFormat().format(date);
    assertEquals("04.08.06 13:49", shortFormat);
  }

  public void test_QQQQyy() {
    Date date;

    date = new Date(2006 - 1900, 0, 27, 13, 10, 10);
    assertEquals("1. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 1, 27, 13, 10, 10);
    assertEquals("1. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 2, 27, 13, 10, 10);
    assertEquals("1. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 3, 27, 13, 10, 10);
    assertEquals("2. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 4, 27, 13, 10, 10);
    assertEquals("2. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 5, 27, 13, 10, 10);
    assertEquals("2. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("3. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 7, 27, 13, 10, 10);
    assertEquals("3. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 8, 27, 13, 10, 10);
    assertEquals("3. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 9, 27, 13, 10, 10);
    assertEquals("4. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 10, 27, 13, 10, 10);
    assertEquals("4. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));

    date = new Date(2006 - 1900, 11, 27, 13, 10, 10);
    assertEquals("4. Quartal 06", DateTimeFormat.getFormat("QQQQ yy").format(
        date));
  }

  public void test_QQyyyy() {
    Date date = new Date(2006 - 1900, 0, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q1 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 1, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q1 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 2, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q1 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 3, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q2 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 4, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q2 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 5, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q2 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 6, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q3 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 7, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q3 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 8, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q3 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 9, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q4 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 10, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q4 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
    date = new Date(2006 - 1900, 11, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("Q4 2006", DateTimeFormat.getFormat("QQ yyyy").format(date));
  }

  public void test_quote() {
    Date date = new Date(2006 - 1900, 6, 27);
    date.setHours(13);
    date.setMinutes(10);
    date.setSeconds(10);
    assertEquals("13 o'clock",
        DateTimeFormat.getFormat("HH 'o''clock'").format(date));
    assertEquals("13 oclock", DateTimeFormat.getFormat("HH 'oclock'").format(
        date));
    assertEquals("13 '", DateTimeFormat.getFormat("HH ''").format(date));
  }

  public void test_yyyyyMMMMM() {
    Date date = new Date(2006 - 1900, 6, 27, 13, 10, 10);
    assertEquals("02006.J.27 n. Chr. 01:10 nachm.", DateTimeFormat.getFormat(
        "yyyyy.MMMMM.dd GGG hh:mm aaa").format(date));
  }

  public void testCustomFormats() {
    MyFormats m = GWT.create(MyFormats.class);
    Date d = new Date(2010 - 1900, 1, 15, 12, 0, 0);
    assertEquals("15. Feb 2010", m.yearMonthDayAbbrev().format(d));
    assertEquals("15. Februar 2010", m.yearMonthDayFull().format(d));
    assertEquals("15. Februar 2010", m.yearMonthDayFull2().format(d));
  }

  public void testMessageDateTime() {
    MyMessages m = GWT.create(MyMessages.class);
    Date d = new Date(2010 - 1900, 1, 15, 12, 0, 0);
    assertEquals("Es ist 15. Feb 2010", m.getCustomizedDate(d));
  }
}
