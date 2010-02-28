/*
 * Copyright 2006 Google Inc.
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
 * Tests formatting functionality in {@link DateTimeFormat} for the Chinese
 * language.
 */
public class DateTimeParse_zh_CN_Test extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_zh_CN";
  }

  public void testChineseDateParse() {
    Date date = new Date();

    {
      String time_15_26_28 = "GMT-07:00 \u4e0b\u534803\u65f626\u520628\u79d2";
      DateTimeFormat.getFullTimeFormat().parse(time_15_26_28, 0, date);

      /*
       * Create the expected time as UTC. The "+7" is due to the tz offset.
       * NOTE: we use the same date explicitly because Java and JavaScript
       * disagree about whether or not daylight savings time is in effect on day
       * 0 of the epoch.
       */
      long expectedTimeUTC = Date.UTC(date.getYear(), date.getMonth(),
          date.getDate(), 15 + 7, 26, 28);
      Date expectedDate = new Date(expectedTimeUTC);
      assertEquals(expectedDate.getHours(), date.getHours());
      assertEquals(expectedDate.getMinutes(), date.getMinutes());
      assertEquals(expectedDate.getSeconds(), date.getSeconds());
    }

    {
      String date_2006_07_24 = "2006\u5E747\u670824\u65e5\u661f\u671f\u4e00";
      assertTrue(DateTimeFormat.getFullDateFormat().parse(date_2006_07_24, 0,
          date) > 0);

      // Create the expected date object, adjusting for the local timezone.
      long localTzOffset = new Date().getTimezoneOffset();
      long expectedTimeUTC = Date.UTC(2006 - 1900, 7 - 1, 24, 0, 0, 0);
      long localTzOffsetMillis = localTzOffset * 60 * 1000;
      expectedTimeUTC += localTzOffsetMillis;
      Date expectedDate = new Date(expectedTimeUTC);

      // Compare the actual and expected results.
      addCheckpoint("2a");
      assertEquals(expectedDate.getYear(), date.getYear());
      addCheckpoint("2b");
      assertEquals(expectedDate.getMonth(), date.getMonth());
      addCheckpoint("2c");
      assertEquals(expectedDate.getDate(), date.getDate());
    }

    {
      String date_2006_07_24 = "2006\u5E747\u670824\u65e5";
      DateTimeFormat.getLongDateFormat().parse(date_2006_07_24, 0, date);

      // Create the expected date object, adjusting for the local timezone.
      long localTzOffset = new Date().getTimezoneOffset();
      long expectedTimeUTC = Date.UTC(2006 - 1900, 7 - 1, 24, 0, 0, 0);
      long localTzOffsetMillis = localTzOffset * 60 * 1000;
      expectedTimeUTC += localTzOffsetMillis;
      Date expectedDate = new Date(expectedTimeUTC);

      // Compare the actual and expected results.
      addCheckpoint("3a");
      assertEquals(expectedDate.getYear(), date.getYear());
      addCheckpoint("3b");
      assertEquals(expectedDate.getMonth(), date.getMonth());
      addCheckpoint("3c");
      assertEquals(expectedDate.getDate(), date.getDate());
    }
  }
}
