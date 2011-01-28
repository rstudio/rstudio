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
 * Implementation of DateTimeFormatInfo for the "fa" locale.
 */
public class DateTimeFormatInfoImpl_fa extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "قبل از ظهر",
        "بعد از ظهر"
    };
  }

  @Override
  public String dateFormat() {
    return dateFormatLong();
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, MMMM d, y";
  }

  @Override
  public String dateFormatLong() {
    return "MMMM d, y";
  }

  @Override
  public String dateFormatMedium() {
    return "MMM d, y";
  }

  @Override
  public String dateFormatShort() {
    return "M/d/yy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append("، ساعت ").append(timePattern).toString();
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append("، ساعت ").append(timePattern).toString();
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append("،‏ ").append(timePattern).toString();
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return new java.lang.StringBuffer().append(datePattern).append("،‏ ").append(timePattern).toString();
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
        "ب. م."
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
    return "MMM d, y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "MMMM d, y";
  }

  @Override
  public String formatYearMonthNum() {
    return "yyyy/M";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "M/d/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "E d MMM y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "yyyy QQQQ";
  }

  @Override
  public String formatYearQuarterShort() {
    return "yyyy Q";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "ژانویهٔ",
        "فوریهٔ",
        "مارس",
        "آوریل",
        "می",
        "جون",
        "جولای",
        "آگوست",
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
        "می",
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
        "می",
        "جون",
        "جولای",
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
