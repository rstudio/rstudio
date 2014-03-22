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
//  date=$Date: 2014-01-09 06:53:23 +0100 (Thu, 09 Jan 2014) $
//  number=$Revision: 9625 $
//  type=ar

/**
 * Implementation of DateTimeFormatInfo for the "ar_MA" locale.
 */
public class DateTimeFormatInfoImpl_ar_MA extends DateTimeFormatInfoImpl_ar_001 {

  @Override
  public String dateFormatMedium() {
    return "y/MM/dd";
  }

  @Override
  public String dateFormatShort() {
    return "y/M/d";
  }

  @Override
  public int firstDayOfTheWeek() {
    return 6;
  }

  @Override
  public String formatMonthNumDay() {
    return "M/d";
  }

  @Override
  public String formatYearMonthNum() {
    return "y/M";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "y/M/d";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "ماي",
        "يونيو",
        "يوليوز",
        "غشت",
        "شتنبر",
        "أكتوبر",
        "نونبر",
        "دجنبر"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "ي",
        "ف",
        "م",
        "أ",
        "م",
        "ن",
        "ل",
        "غ",
        "ش",
        "ك",
        "ب",
        "د"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "ماي",
        "يونيو",
        "يوليوز",
        "غشت",
        "شتنبر",
        "أكتوبر",
        "نونبر",
        "دجنبر"
    };
  }

  @Override
  public int weekendEnd() {
    return 6;
  }

  @Override
  public int weekendStart() {
    return 5;
  }
}
