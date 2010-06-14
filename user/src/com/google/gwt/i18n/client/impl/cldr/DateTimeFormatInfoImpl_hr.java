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

// DO NOT EDIT - GENERATED FROM CLDR DATA

/**
 * Implementation of DateTimeFormatInfo for locale "hr".
 */
public class DateTimeFormatInfoImpl_hr extends DateTimeFormatInfoImpl {

  @Override
  public String dateFormatFull() {
    return "EEEE, d. MMMM y.";
  }

  @Override
  public String dateFormatLong() {
    return "d. MMMM y.";
  }

  @Override
  public String dateFormatMedium() {
    return "d.M.yyyy.";
  }

  @Override
  public String dateFormatShort() {
    return "dd.MM.yyyy.";
  }

  @Override
  public String[] erasFull() {
    return new String[] { 
        "Prije Krista",
        "Poslije Krista"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] { 
        "pr.n.e.",
        "AD"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 1;
  }

  @Override
  public String formatHour12Minute() {
    return "hh:mm a";
  }

  @Override
  public String formatHour12MinuteSecond() {
    return "hh:mm:ss a";
  }

  @Override
  public String formatMonthAbbrev() {
    return "LLL.";
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d. MMM";
  }

  @Override
  public String formatMonthFull() {
    return "LLLL.";
  }

  @Override
  public String formatMonthFullDay() {
    return "d. MMMM";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE, d.MMMM.";
  }

  @Override
  public String formatMonthNumDay() {
    return "d.M.";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM.y.";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d. MMM y.";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM.y.";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d. MMMM y.";
  }

  @Override
  public String formatYearMonthNum() {
    return "M.yyyy.";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d. M. y.";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d. MMM y.";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ. yyyy.";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q. yyyy.";
  }

  @Override
  public String[] monthsFull() {
    return new String[] { 
        "siječnja",
        "veljače",
        "ožujka",
        "travnja",
        "svibnja",
        "lipnja",
        "srpnja",
        "kolovoza",
        "rujna",
        "listopada",
        "studenoga",
        "prosinca"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] { 
        "siječanj",
        "veljača",
        "ožujak",
        "travanj",
        "svibanj",
        "lipanj",
        "srpanj",
        "kolovoz",
        "rujan",
        "listopad",
        "studeni",
        "prosinac"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] { 
        "1.",
        "2.",
        "3.",
        "4.",
        "5.",
        "6.",
        "7.",
        "8.",
        "9.",
        "10.",
        "11.",
        "12."
    };
  }

  @Override
  public String[] monthsNarrowStandalone() {
    return monthsNarrow();
  }

  @Override
  public String[] monthsShort() {
    return new String[] { 
        "01.",
        "02.",
        "03.",
        "04.",
        "05.",
        "06.",
        "07.",
        "08.",
        "09.",
        "10.",
        "11.",
        "12."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  @Override
  public String[] quartersFull() {
    return new String[] { 
        "1. kvartal",
        "2. kvartal",
        "3. kvartal",
        "4. kvartal"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] { 
        "1kv",
        "2kv",
        "3kv",
        "4kv"
    };
  }

  @Override
  public String timeFormatFull() {
    return "HH:mm:ss zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "HH:mm:ss z";
  }

  @Override
  public String timeFormatMedium() {
    return "HH:mm:ss";
  }

  @Override
  public String timeFormatShort() {
    return "HH:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] { 
        "nedjelja",
        "ponedjeljak",
        "utorak",
        "srijeda",
        "četvrtak",
        "petak",
        "subota"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return weekdaysFull();
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] { 
        "n",
        "p",
        "u",
        "s",
        "č",
        "p",
        "s"
    };
  }

  @Override
  public String[] weekdaysNarrowStandalone() {
    return weekdaysNarrow();
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] { 
        "ned",
        "pon",
        "uto",
        "sri",
        "čet",
        "pet",
        "sub"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
