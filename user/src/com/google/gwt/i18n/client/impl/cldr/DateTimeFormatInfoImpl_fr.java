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
//  number=$Revision: 6546 Google $
//  type=root
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $

/**
 * Implementation of DateTimeFormatInfo for the "fr" locale.
 */
public class DateTimeFormatInfoImpl_fr extends DateTimeFormatInfoImpl {

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
    return "dd/MM/yy";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "avant Jésus-Christ",
        "après Jésus-Christ"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "av. J.-C.",
        "ap. J.-C."
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
    return "M/yyyy";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d/M/yyyy";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE d MMM y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "'T'QQQQ y";
  }

  @Override
  public String formatYearQuarterShort() {
    return "'T'Q y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "janvier",
        "février",
        "mars",
        "avril",
        "mai",
        "juin",
        "juillet",
        "août",
        "septembre",
        "octobre",
        "novembre",
        "décembre"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "janv.",
        "févr.",
        "mars",
        "avr.",
        "mai",
        "juin",
        "juil.",
        "août",
        "sept.",
        "oct.",
        "nov.",
        "déc."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1er trimestre",
        "2e trimestre",
        "3e trimestre",
        "4e trimestre"
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
        "dimanche",
        "lundi",
        "mardi",
        "mercredi",
        "jeudi",
        "vendredi",
        "samedi"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "D",
        "L",
        "M",
        "M",
        "J",
        "V",
        "S"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "dim.",
        "lun.",
        "mar.",
        "mer.",
        "jeu.",
        "ven.",
        "sam."
    };
  }
}
