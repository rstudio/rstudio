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
 * Implementation of DateTimeFormatInfo for the "bg" locale.
 */
public class DateTimeFormatInfoImpl_bg extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "пр. об.",
        "сл. об."
    };
  }

  @Override
  public String dateFormatFull() {
    return "dd MMMM y, EEEE";
  }

  @Override
  public String dateFormatLong() {
    return "dd MMMM y";
  }

  @Override
  public String dateFormatMedium() {
    return "dd.MM.yyyy";
  }

  @Override
  public String dateFormatShort() {
    return "dd.MM.yy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "пр.Хр.",
        "сл.Хр."
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "пр. н. е.",
        "от н. е."
    };
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
    return "d MMMM, EEEE";
  }

  @Override
  public String formatMonthNumDay() {
    return "d.M";
  }

  @Override
  public String formatYear() {
    return "y 'г'.";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM y 'г'.";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "dd MMM y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y 'г'.";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM y";
  }

  @Override
  public String formatYearMonthNum() {
    return "M.y 'г'.";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "dd.MM.yy";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d MMM y 'г'.";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ y 'г'.";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q y 'г'.";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "януари",
        "февруари",
        "март",
        "април",
        "май",
        "юни",
        "юли",
        "август",
        "септември",
        "октомври",
        "ноември",
        "декември"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "я",
        "ф",
        "м",
        "а",
        "м",
        "ю",
        "ю",
        "а",
        "с",
        "о",
        "н",
        "д"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "ян.",
        "февр.",
        "март",
        "апр.",
        "май",
        "юни",
        "юли",
        "авг.",
        "септ.",
        "окт.",
        "ноем.",
        "дек."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1-во тримесечие",
        "2-ро тримесечие",
        "3-то тримесечие",
        "4-то тримесечие"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "I трим.",
        "II трим.",
        "III трим.",
        "IV трим."
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "неделя",
        "понеделник",
        "вторник",
        "сряда",
        "четвъртък",
        "петък",
        "събота"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "н",
        "п",
        "в",
        "с",
        "ч",
        "п",
        "с"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "нд",
        "пн",
        "вт",
        "ср",
        "чт",
        "пт",
        "сб"
    };
  }
}
