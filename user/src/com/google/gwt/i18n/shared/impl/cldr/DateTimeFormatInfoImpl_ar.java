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
//  date=$Date: 2014-03-01 06:57:43 +0100 (Sat, 01 Mar 2014) $
//  number=$Revision: 9852 $
//  type=ar

/**
 * Implementation of DateTimeFormatInfo for the "ar" locale.
 */
public class DateTimeFormatInfoImpl_ar extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "ص",
        "م"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE، d MMMM، y";
  }

  @Override
  public String dateFormatLong() {
    return "d MMMM، y";
  }

  @Override
  public String dateFormatMedium() {
    return "dd‏/MM‏/y";
  }

  @Override
  public String dateFormatShort() {
    return "d‏/M‏/y";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "قبل الميلاد",
        "ميلادي"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "ق.م",
        "م"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 6;
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d MMM";
  }

  @Override
  public String formatMonthFullDay() {
    return "d MMMM";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE، d MMMM";
  }

  @Override
  public String formatMonthNumDay() {
    return "d/‏M";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM، y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM، y";
  }

  @Override
  public String formatYearMonthNum() {
    return "M‏/y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d‏/M‏/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE، d MMM، y";
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
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "ي",
        "ف",
        "م",
        "أ",
        "و",
        "ن",
        "ل",
        "غ",
        "س",
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
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "الربع الأول",
        "الربع الثاني",
        "الربع الثالث",
        "الربع الرابع"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "الربع الأول",
        "الربع الثاني",
        "الربع الثالث",
        "الربع الرابع"
    };
  }

  @Override
  public String timeFormatFull() {
    return "h:mm:ss a zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "h:mm:ss a z";
  }

  @Override
  public String timeFormatMedium() {
    return "h:mm:ss a";
  }

  @Override
  public String timeFormatShort() {
    return "h:mm a";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "الأحد",
        "الاثنين",
        "الثلاثاء",
        "الأربعاء",
        "الخميس",
        "الجمعة",
        "السبت"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "ح",
        "ن",
        "ث",
        "ر",
        "خ",
        "ج",
        "س"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "الأحد",
        "الاثنين",
        "الثلاثاء",
        "الأربعاء",
        "الخميس",
        "الجمعة",
        "السبت"
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
