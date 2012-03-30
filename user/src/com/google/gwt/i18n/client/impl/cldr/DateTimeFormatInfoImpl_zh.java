/*
 * Copyright 2012 Google Inc.
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
//  cldrVersion=21.0
//  number=$Revision: 6546 Google $
//  type=root
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $

/**
 * Implementation of DateTimeFormatInfo for the "zh" locale.
 */
public class DateTimeFormatInfoImpl_zh extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "上午",
        "下午"
    };
  }

  @Override
  public String dateFormatFull() {
    return "y年M月d日EEEE";
  }

  @Override
  public String dateFormatLong() {
    return "y年M月d日";
  }

  @Override
  public String dateFormatMedium() {
    return "yyyy-M-d";
  }

  @Override
  public String dateFormatShort() {
    return "yy-M-d";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + timePattern;
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
  public String formatDay() {
    return "d日";
  }

  @Override
  public String formatHour12Minute() {
    return "ah:mm";
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "ah:mm:ss";
  }

  @Override
  public String formatHour24Minute() {
    return "H:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "H:mm:ss";
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "M月d日";
  }

  @Override
  public String formatMonthFullDay() {
    return "M月d日";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "M月d日EEEE";
  }

  @Override
  public String formatYear() {
    return "y年";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "y年M月";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "y年M月d日";
  }

  @Override
  public String formatYearMonthFull() {
    return "y年M月";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y年M月d日";
  }

  @Override
  public String formatYearMonthNum() {
    return "yyyy-M";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "y年M月d日";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "y年M月d日EEE";
  }

  @Override
  public String formatYearQuarterFull() {
    return "y年QQQ";
  }

  @Override
  public String formatYearQuarterShort() {
    return "y年QQQ";
  }

  @Override
  public String[] monthsFull() {
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
  public String[] monthsFullStandalone() {
    return new String[] {
        "一月",
        "二月",
        "三月",
        "四月",
        "五月",
        "六月",
        "七月",
        "八月",
        "九月",
        "十月",
        "十一月",
        "十二月"
    };
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
  public String[] monthsNarrowStandalone() {
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
  public String[] monthsShort() {
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
  public String[] monthsShortStandalone() {
    return new String[] {
        "一月",
        "二月",
        "三月",
        "四月",
        "五月",
        "六月",
        "七月",
        "八月",
        "九月",
        "十月",
        "十一月",
        "十二月"
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
  public String[] quartersShort() {
    return new String[] {
        "1季",
        "2季",
        "3季",
        "4季"
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
  public String timeFormatShort() {
    return "ah:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "星期日",
        "星期一",
        "星期二",
        "星期三",
        "星期四",
        "星期五",
        "星期六"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "日",
        "一",
        "二",
        "三",
        "四",
        "五",
        "六"
    };
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
