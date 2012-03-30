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
//  number=$Revision: 6546 Google $
//  type=root
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $

/**
 * Implementation of DateTimeFormatInfo for the "zh_Hant" locale.
 */
public class DateTimeFormatInfoImpl_zh_Hant extends DateTimeFormatInfoImpl_zh {

  @Override
  public String dateFormatMedium() {
    return "yyyy/M/d";
  }

  @Override
  public String dateFormatShort() {
    return "y/M/d";
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
  public String formatMonthNumDay() {
    return "M/d";
  }

  @Override
  public String formatYearMonthNum() {
    return "yyyy/M";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "yyyy/M/d";
  }

  @Override
  public String[] monthsFullStandalone() {
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
