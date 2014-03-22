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
package com.google.gwt.i18n.shared.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=25
//  date=$Date: 2013-08-29 04:32:04 +0200 (Thu, 29 Aug 2013) $
//  number=$Revision: 9287 $
//  type=Hant

/**
 * Implementation of DateTimeFormatInfo for the "zh_Hant_HK" locale.
 */
public class DateTimeFormatInfoImpl_zh_Hant_HK extends DateTimeFormatInfoImpl_zh_Hant {

  @Override
  public String dateFormatShort() {
    return "d/M/yy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + " " + timePattern;
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return datePattern + timePattern;
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return datePattern + timePattern;
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "M月d日 (EEEE)";
  }

  @Override
  public String formatMonthNumDay() {
    return "d/M";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "y 年 M 月";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "y 年 M 月 d 日";
  }

  @Override
  public String formatYearMonthFull() {
    return "y 年 M 月";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y 年 M 月 d 日";
  }

  @Override
  public String formatYearMonthNum() {
    return "M/y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d/M/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "y 年 M 月 d 日 (EEE)";
  }

  @Override
  public String timeFormatFull() {
    return "ah:mm:ss [zzzz]";
  }

  @Override
  public String timeFormatLong() {
    return "ah:mm:ss [z]";
  }
}
