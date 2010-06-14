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
 * Implementation of DateTimeFormatInfo for locale "zu".
 */
public class DateTimeFormatInfoImpl_zu extends DateTimeFormatInfoImpl {

  @Override
  public String dateFormatFull() {
    return "EEEE dd MMMM y";
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
    return "yyyy-MM-dd";
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
    return 1;
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE d MMM y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] { 
        "Januwari",
        "Februwari",
        "Mashi",
        "Apreli",
        "Meyi",
        "Juni",
        "Julayi",
        "Agasti",
        "Septhemba",
        "Okthoba",
        "Novemba",
        "Disemba"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] { 
        "uJanuwari",
        "uFebruwari",
        "uMashi",
        "u-Apreli",
        "uMeyi",
        "uJuni",
        "uJulayi",
        "uAgasti",
        "uSepthemba",
        "u-Okthoba",
        "uNovemba",
        "uDisemba"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] { 
        "Jan",
        "Feb",
        "Mas",
        "Apr",
        "Mey",
        "Jun",
        "Jul",
        "Aga",
        "Sep",
        "Okt",
        "Nov",
        "Dis"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  @Override
  public String[] quartersFull() {
    return new String[] { 
        "Q1",
        "Q2",
        "Q3",
        "Q4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] { 
        "Sonto",
        "Msombuluko",
        "Lwesibili",
        "Lwesithathu",
        "uLwesine",
        "Lwesihlanu",
        "Mgqibelo"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return weekdaysFull();
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] { 
        "S",
        "M",
        "B",
        "T",
        "S",
        "H",
        "M"
    };
  }

  @Override
  public String[] weekdaysNarrowStandalone() {
    return weekdaysNarrow();
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] { 
        "Son",
        "Mso",
        "Bil",
        "Tha",
        "Sin",
        "Hla",
        "Mgq"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
