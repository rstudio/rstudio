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
package com.google.gwt.i18n.client.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=21.0
//  number=$Revision: 6546 $
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "nd" locale.
 */
public class DateTimeFormatInfoImpl_nd extends DateTimeFormatInfoImpl {

  @Override
  public String dateFormatFull() {
    return "EEEE, d MMMM y";
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
    return "dd/MM/yyyy";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "UKristo angakabuyi",
        "Ukristo ebuyile"
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
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM y";
  }

  @Override
  public String formatYearMonthNum() {
    return "M/y";
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
        "Zibandlela",
        "Nhlolanja",
        "Mbimbitho",
        "Mabasa",
        "Nkwenkwezi",
        "Nhlangula",
        "Ntulikazi",
        "Ncwabakazi",
        "Mpandula",
        "Mfumfu",
        "Lwezi",
        "Mpalakazi"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "Z",
        "N",
        "M",
        "M",
        "N",
        "N",
        "N",
        "N",
        "M",
        "M",
        "L",
        "M"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "Zib",
        "Nhlo",
        "Mbi",
        "Mab",
        "Nkw",
        "Nhla",
        "Ntu",
        "Ncw",
        "Mpan",
        "Mfu",
        "Lwe",
        "Mpal"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "Kota 1",
        "Kota 2",
        "Kota 3",
        "Kota 4"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "K1",
        "K2",
        "K3",
        "K4"
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
        "Sonto",
        "Mvulo",
        "Sibili",
        "Sithathu",
        "Sine",
        "Sihlanu",
        "Mgqibelo"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "M",
        "S",
        "S",
        "S",
        "S",
        "M"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "Son",
        "Mvu",
        "Sib",
        "Sit",
        "Sin",
        "Sih",
        "Mgq"
    };
  }
}
