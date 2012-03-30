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
//  type=root
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $

/**
 * Implementation of DateTimeFormatInfo for the "seh" locale.
 */
public class DateTimeFormatInfoImpl_seh extends DateTimeFormatInfoImpl {

  @Override
  public String dateFormatFull() {
    return "EEEE, d 'de' MMMM 'de' y";
  }

  @Override
  public String dateFormatLong() {
    return "d 'de' MMMM 'de' y";
  }

  @Override
  public String dateFormatMedium() {
    return "d 'de' MMM 'de' y";
  }

  @Override
  public String dateFormatShort() {
    return "d/M/yyyy";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "Antes de Cristo",
        "Anno Domini"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "AC",
        "AD"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
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
    return "EEEE, d MMMM";
  }

  @Override
  public String formatMonthNumDay() {
    return "d/M";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM 'de' y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d 'de' MMM 'de' y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM 'de' y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d 'de' MMMM 'de' y";
  }

  @Override
  public String formatYearMonthNum() {
    return "MM/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d 'de' MMM 'de' y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "Janeiro",
        "Fevreiro",
        "Marco",
        "Abril",
        "Maio",
        "Junho",
        "Julho",
        "Augusto",
        "Setembro",
        "Otubro",
        "Novembro",
        "Decembro"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "Jan",
        "Fev",
        "Mar",
        "Abr",
        "Mai",
        "Jun",
        "Jul",
        "Aug",
        "Set",
        "Otu",
        "Nov",
        "Dec"
    };
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
        "Dimingu",
        "Chiposi",
        "Chipiri",
        "Chitatu",
        "Chinai",
        "Chishanu",
        "Sabudu"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "D",
        "P",
        "C",
        "T",
        "N",
        "S",
        "S"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "Dim",
        "Pos",
        "Pir",
        "Tat",
        "Nai",
        "Sha",
        "Sab"
    };
  }
}
