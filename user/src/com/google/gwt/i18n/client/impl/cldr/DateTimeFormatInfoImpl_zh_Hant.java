/*
 * Copyright 2014 Google Inc.
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
//  cldrVersion=25
//  date=$Date: 2014-03-01 06:57:43 +0100 (Sat, 01 Mar 2014) $
//  number=$Revision: 9852 $
//  type=Hant

/**
 * Implementation of DateTimeFormatInfo for the "zh_Hant" locale.
 */
public class DateTimeFormatInfoImpl_zh_Hant extends DateTimeFormatInfoImpl_zh {

  @Override
  public String dateFormatShort() {
    return "y/M/d";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + timePattern;
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
  public String formatYearQuarterFull() {
    return "y年QQQQ";
  }

  @Override
  public String formatYearQuarterShort() {
    return "y年Q";
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
  public String[] quartersFull() {
    return new String[] {
        "第1季",
        "第2季",
        "第3季",
        "第4季"
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
