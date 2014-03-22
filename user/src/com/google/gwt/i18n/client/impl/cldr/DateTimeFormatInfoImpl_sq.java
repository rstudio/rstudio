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
package com.google.gwt.i18n.client.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=25
//  date=$Date: 2014-03-01 06:57:43 +0100 (Sat, 01 Mar 2014) $
//  number=$Revision: 9852 $
//  type=sq

/**
 * Implementation of DateTimeFormatInfo for the "sq" locale.
 */
public class DateTimeFormatInfoImpl_sq extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "paradite",
        "pasdite"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, dd MMMM y";
  }

  @Override
  public String dateFormatLong() {
    return "dd MMMM y";
  }

  @Override
  public String dateFormatMedium() {
    return "dd/MM/y";
  }

  @Override
  public String dateFormatShort() {
    return "dd/MM/yy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + " 'në' " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + " 'në' " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "para erës së re",
        "erës së re"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "p.e.r.",
        "e.r."
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
    return "EEEE d MMMM";
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
    return "M/y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "dd/MM/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d MMM y";
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
        "janar",
        "shkurt",
        "mars",
        "prill",
        "maj",
        "qershor",
        "korrik",
        "gusht",
        "shtator",
        "tetor",
        "nëntor",
        "dhjetor"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "J",
        "S",
        "M",
        "P",
        "M",
        "Q",
        "K",
        "G",
        "S",
        "T",
        "N",
        "D"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "Jan",
        "Shk",
        "Mar",
        "Pri",
        "Maj",
        "Qer",
        "Kor",
        "Gsh",
        "Sht",
        "Tet",
        "Nën",
        "Dhj"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "tremujori i parë",
        "tremujori i dytë",
        "tremujori i tretë",
        "tremujori i katërt"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "T1",
        "T2",
        "T3",
        "T4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "e diel",
        "e hënë",
        "e martë",
        "e mërkurë",
        "e enjte",
        "e premte",
        "e shtunë"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "D",
        "H",
        "M",
        "M",
        "E",
        "P",
        "S"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "Die",
        "Hën",
        "Mar",
        "Mër",
        "Enj",
        "Pre",
        "Sht"
    };
  }
}
