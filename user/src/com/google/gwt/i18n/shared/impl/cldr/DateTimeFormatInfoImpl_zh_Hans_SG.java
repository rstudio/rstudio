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
//  type=Hans

/**
 * Implementation of DateTimeFormatInfo for the "zh_Hans_SG" locale.
 */
public class DateTimeFormatInfoImpl_zh_Hans_SG extends DateTimeFormatInfoImpl_zh_Hans {

  @Override
  public String dateFormatShort() {
    return "dd/MM/yy";
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
  public String formatHour12Minute() {
    return "ahh:mm";
  }

  @Override
  public String formatMonthAbbrev() {
    return "M月";
  }

  @Override
  public String formatMonthFull() {
    return "M月";
  }

  @Override
  public String formatMonthNumDay() {
    return "M-d";
  }

  @Override
  public String formatYearMonthNum() {
    return "y年M月";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "y年M月d日";
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
