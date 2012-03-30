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
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "fa" locale.
 */
public class DateTimeFormatInfoImpl_fa extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "قبل‌ازظهر",
        "بعدازظهر"
    };
  }

  @Override
  public String dateFormat() {
    return dateFormatLong();
  }

  @Override
  public String dateFormatFull() {
    return "EEEE d MMMM y";
  }

  @Override
  public String dateFormatLong() {
    return "d MMMM y";
  }

  @Override
  public String dateFormatMedium() {
    return "d MMM y";
  }

  @Override
  public String dateFormatShort() {
    return "yyyy/M/d";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + "، ساعت " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + "، ساعت " + timePattern;
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return datePattern + "،‏ " + timePattern;
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return datePattern + "،‏ " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "قبل از میلاد",
        "میلادی"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "ق.م.",
        "م."
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 6;
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
    return "d LLL";
  }

  @Override
  public String formatMonthFullDay() {
    return "d LLLL";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE d LLLL";
  }

  @Override
  public String formatMonthNumDay() {
    return "M/d";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM y";
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
  public String formatYearMonthWeekdayDay() {
    return "EEE d MMM y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ y";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "ژانویهٔ",
        "فوریهٔ",
        "مارس",
        "آوریل",
        "مهٔ",
        "ژوئن",
        "ژوئیهٔ",
        "اوت",
        "سپتامبر",
        "اکتبر",
        "نوامبر",
        "دسامبر"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "ژانویه",
        "فوریه",
        "مارس",
        "آوریل",
        "مه",
        "ژوئن",
        "ژوئیه",
        "اوت",
        "سپتامبر",
        "اکتبر",
        "نوامبر",
        "دسامبر"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "ژ",
        "ف",
        "م",
        "آ",
        "م",
        "ژ",
        "ژ",
        "ا",
        "س",
        "ا",
        "ن",
        "د"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "ژانویهٔ",
        "فوریهٔ",
        "مارس",
        "آوریل",
        "مهٔ",
        "ژوئن",
        "ژوئیهٔ",
        "اوت",
        "سپتامبر",
        "اکتبر",
        "نوامبر",
        "دسامبر"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "ژانویه",
        "فوریه",
        "مارس",
        "آوریل",
        "مه",
        "ژوئن",
        "ژوئیه",
        "اوت",
        "سپتامبر",
        "اکتبر",
        "نوامبر",
        "دسامبر"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "سه‌ماههٔ اول",
        "سه‌ماههٔ دوم",
        "سه‌ماههٔ سوم",
        "سه‌ماههٔ چهارم"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "س‌م۱",
        "س‌م۲",
        "س‌م۳",
        "س‌م۴"
    };
  }

  @Override
  public String timeFormatFull() {
    return "H:mm:ss (zzzz)";
  }

  @Override
  public String timeFormatLong() {
    return "H:mm:ss (z)";
  }

  @Override
  public String timeFormatMedium() {
    return "H:mm:ss";
  }

  @Override
  public String timeFormatShort() {
    return "H:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "یکشنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنجشنبه",
        "جمعه",
        "شنبه"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "ی",
        "د",
        "س",
        "چ",
        "پ",
        "ج",
        "ش"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "یکشنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنجشنبه",
        "جمعه",
        "شنبه"
    };
  }

  @Override
  public int weekendEnd() {
    return 5;
  }

  @Override
  public int weekendStart() {
    return 4;
  }
}
