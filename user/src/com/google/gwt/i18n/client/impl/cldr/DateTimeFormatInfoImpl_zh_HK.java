/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.i18n.client.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA

/**
 * Implementation of DateTimeFormatInfo for the "zh_HK" locale.
 */
public class DateTimeFormatInfoImpl_zh_HK extends DateTimeFormatInfoImpl_zh {

  @Override
  public String dateFormatMedium() {
    return "y年M月d日";
  }

  @Override
  public String dateFormatShort() {
    return "yy年M月d日";
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(timePattern).toString();
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(timePattern).toString();
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "西元前",
        "西元"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "西元前",
        "西元"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "ahh:mm:ss";
  }

  @Override
  public String formatMonthNumDay() {
    return "M/d";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "y年M月";
  }

  @Override
  public String formatYearMonthFull() {
    return "y年M月";
  }

  @Override
  public String formatYearMonthNum() {
    return "yyyy/M";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "y年M月d日EEE";
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "10",
        "11",
        "12"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "第1季",
        "第2季",
        "第3季",
        "第4季"
    };
  }

  @Override
  public String timeFormatFull() {
    return "zzzzah時mm分ss秒";
  }

  @Override
  public String timeFormatLong() {
    return "zah時mm分ss秒";
  }

  @Override
  public String timeFormatMedium() {
    return "ahh:mm:ss";
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "週日",
        "週一",
        "週二",
        "週三",
        "週四",
        "週五",
        "週六"
    };
  }
}
