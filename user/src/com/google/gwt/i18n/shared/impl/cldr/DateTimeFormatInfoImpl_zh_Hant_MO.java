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
//  number=$Revision: 6255 $
//  type=MO
//  date=$Date: 2011-11-24 04:07:30 -0500 (Thu, 24 Nov 2011) $

/**
 * Implementation of DateTimeFormatInfo for the "zh_Hant_MO" locale.
 */
public class DateTimeFormatInfoImpl_zh_Hant_MO extends DateTimeFormatInfoImpl_zh_Hant {

  @Override
  public String dateFormatFull() {
    return "y年MM月dd日EEEE";
  }

  @Override
  public String dateFormatLong() {
    return "y年MM月dd日";
  }

  @Override
  public String dateFormatMedium() {
    return "y年M月d日";
  }

  @Override
  public String dateFormatShort() {
    return "yy年M月d日";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + " " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + " " + timePattern;
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "ahh:mm:ss";
  }

  @Override
  public String formatMonthNumDay() {
    return "d-M";
  }

  @Override
  public String timeFormatFull() {
    return "ah:mm:ss [zzzz]";
  }

  @Override
  public String timeFormatLong() {
    return "ah:mm:ss [z]";
  }

  @Override
  public String timeFormatMedium() {
    return "ahh:mm:ss";
  }
}
