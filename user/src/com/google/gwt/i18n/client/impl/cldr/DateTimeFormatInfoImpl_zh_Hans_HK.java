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
 * Implementation of DateTimeFormatInfo for the "zh_Hans_HK" locale.
 */
public class DateTimeFormatInfoImpl_zh_Hans_HK extends DateTimeFormatInfoImpl_zh_HK {

  @Override
  public String dateFormatMedium() {
    return "yyyy-M-d";
  }

  @Override
  public String dateFormatShort() {
    return "yy-M-d";
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(" ").append(timePattern).toString();
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append(" ").append(timePattern).toString();
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "公元前",
        "公元"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "公元前",
        "公元"
    };
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "ah:mm:ss";
  }

  @Override
  public String formatMonthNumDay() {
    return "M-d";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "y年MMM";
  }

  @Override
  public String formatYearMonthFull() {
    return "y年MMMM";
  }

  @Override
  public String formatYearMonthNum() {
    return "yyyy-M";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "y年MMMd日EEE";
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "1月",
        "2月",
        "3月",
        "4月",
        "5月",
        "6月",
        "7月",
        "8月",
        "9月",
        "10月",
        "11月",
        "12月"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "第1季度",
        "第2季度",
        "第3季度",
        "第4季度"
    };
  }

  @Override
  public String timeFormatFull() {
    return "zzzzah时mm分ss秒";
  }

  @Override
  public String timeFormatLong() {
    return "zah时mm分ss秒";
  }

  @Override
  public String timeFormatMedium() {
    return "ah:mm:ss";
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "周日",
        "周一",
        "周二",
        "周三",
        "周四",
        "周五",
        "周六"
    };
  }
}
