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
 * Implementation of DateTimeFormatInfo for the "he" locale.
 */
public class DateTimeFormatInfoImpl_he extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "לפנה״צ",
        "אחה״צ"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, d בMMMM y";
  }

  @Override
  public String dateFormatLong() {
    return "d בMMMM y";
  }

  @Override
  public String dateFormatMedium() {
    return "d בMMM yyyy";
  }

  @Override
  public String dateFormatShort() {
    return "dd/MM/yy";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "לפני הספירה",
        "לספירה"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "לפנה״ס",
        "לסה״נ"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d בMMM";
  }

  @Override
  public String formatMonthFullDay() {
    return "d בMMMM";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE, d בMMMM";
  }

  @Override
  public String formatMonthNumDay() {
    return "d/M";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d בMMM y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d בMMMM y";
  }

  @Override
  public String formatYearMonthNum() {
    return "M.yyyy";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d.M.yyyy";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d בMMM y";
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
        "ינואר",
        "פברואר",
        "מרץ",
        "אפריל",
        "מאי",
        "יוני",
        "יולי",
        "אוגוסט",
        "ספטמבר",
        "אוקטובר",
        "נובמבר",
        "דצמבר"
    };
  }

  @Override
  public String[] monthsNarrow() {
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
  public String[] monthsShort() {
    return new String[] {
        "ינו",
        "פבר",
        "מרץ",
        "אפר",
        "מאי",
        "יונ",
        "יול",
        "אוג",
        "ספט",
        "אוק",
        "נוב",
        "דצמ"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "ינו׳",
        "פבר׳",
        "מרץ",
        "אפר׳",
        "מאי",
        "יונ׳",
        "יול׳",
        "אוג׳",
        "ספט׳",
        "אוק׳",
        "נוב׳",
        "דצמ׳"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "רבעון 1",
        "רבעון 2",
        "רבעון 3",
        "רבעון 4"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "רבעון 1",
        "רבעון 2",
        "רבעון 3",
        "רבעון 4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "יום ראשון",
        "יום שני",
        "יום שלישי",
        "יום רביעי",
        "יום חמישי",
        "יום שישי",
        "יום שבת"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "א",
        "ב",
        "ג",
        "ד",
        "ה",
        "ו",
        "ש"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "יום א׳",
        "יום ב׳",
        "יום ג׳",
        "יום ד׳",
        "יום ה׳",
        "יום ו׳",
        "שבת"
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
