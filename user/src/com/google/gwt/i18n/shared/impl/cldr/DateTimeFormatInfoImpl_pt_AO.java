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
//  date=$Date: 2013-07-20 19:27:45 +0200 (Sat, 20 Jul 2013) $
//  number=$Revision: 9061 $
//  type=AO

/**
 * Implementation of DateTimeFormatInfo for the "pt_AO" locale.
 */
public class DateTimeFormatInfoImpl_pt_AO extends DateTimeFormatInfoImpl_pt {

  @Override
  public String[] ampms() {
    return new String[] {
        "a.m.",
        "p.m."
    };
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + " 'às' " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + " 'às' " + timePattern;
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
  public int firstDayOfTheWeek() {
    return 1;
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d/MM";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MM/y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d/MM/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d/MM/y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ 'de' y";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q 'de' y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "Janeiro",
        "Fevereiro",
        "Março",
        "Abril",
        "Maio",
        "Junho",
        "Julho",
        "Agosto",
        "Setembro",
        "Outubro",
        "Novembro",
        "Dezembro"
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
        "Ago",
        "Set",
        "Out",
        "Nov",
        "Dez"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1.º trimestre",
        "2.º trimestre",
        "3.º trimestre",
        "4.º trimestre"
    };
  }
}
