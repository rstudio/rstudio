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
//  date=$Date: 2014-02-25 22:16:49 +0100 (Tue, 25 Feb 2014) $
//  number=$Revision: 9791 $
//  type=fil

/**
 * Implementation of DateTimeFormatInfo for the "fil" locale.
 */
public class DateTimeFormatInfoImpl_fil extends DateTimeFormatInfoImpl {

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
    return datePattern + " 'ng' " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + " 'ng' " + timePattern;
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
        "BC",
        "AD"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE, MMMM d";
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
    return "M/y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "M/d/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, MMM d, y";
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
        "Enero",
        "Pebrero",
        "Marso",
        "Abril",
        "Mayo",
        "Hunyo",
        "Hulyo",
        "Agosto",
        "Setyembre",
        "Oktubre",
        "Nobyembre",
        "Disyembre"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "E",
        "P",
        "M",
        "A",
        "M",
        "H",
        "H",
        "A",
        "S",
        "O",
        "N",
        "D"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "Ene",
        "Peb",
        "Mar",
        "Abr",
        "May",
        "Hun",
        "Hul",
        "Ago",
        "Set",
        "Okt",
        "Nob",
        "Dis"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "ika-1 quarter",
        "ika-2 quarter",
        "ika-3 quarter",
        "ika-4 na quarter"
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
        "Linggo",
        "Lunes",
        "Martes",
        "Miyerkules",
        "Huwebes",
        "Biyernes",
        "Sabado"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "L",
        "L",
        "M",
        "M",
        "H",
        "B",
        "S"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "Lin",
        "Lun",
        "Mar",
        "Miy",
        "Huw",
        "Biy",
        "Sab"
    };
  }
}
