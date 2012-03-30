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
package com.google.gwt.i18n.shared.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=21.0
//  number=$Revision: 6465 $
//  date=$Date: 2012-01-27 12:47:35 -0500 (Fri, 27 Jan 2012) $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "zh_Hans_SG" locale.
 */
public class DateTimeFormatInfoImpl_zh_Hans_SG extends DateTimeFormatInfoImpl_zh_Hans {

  @Override
  public String dateFormatMedium() {
    return "y年M月d日";
  }

  @Override
  public String dateFormatShort() {
    return "dd/MM/yy";
  }

  @Override
  public String formatHour12Minute() {
    return "ahh:mm";
  }

  @Override
  public String formatHour24Minute() {
    return "HH:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "HH:mm:ss";
  }

  @Override
  public String formatYear() {
    return "y";
  }

  @Override
  public String formatYearMonthNum() {
    return "y年M月";
  }

  @Override
  public String formatYearQuarterFull() {
    return "y年第QQQQ季度";
  }

  @Override
  public String formatYearQuarterShort() {
    return "y年第Q季度";
  }

  @Override
  public String[] monthsFull() {
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
  public String[] monthsNarrowStandalone() {
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
  public String[] monthsShortStandalone() {
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
        "第一季度",
        "第二季度",
        "第三季度",
        "第四季度"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "1季度",
        "2季度",
        "3季度",
        "4季度"
    };
  }

  @Override
  public String timeFormatFull() {
    return "zzzzah:mm:ss";
  }

  @Override
  public String timeFormatLong() {
    return "ahh:mm:ssz";
  }

  @Override
  public String timeFormatShort() {
    return "ahh:mm";
  }
}
