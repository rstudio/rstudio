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
 * Implementation of DateTimeFormatInfo for locale "pt".
 */
public class DateTimeFormatInfoImpl_pt extends DateTimeFormatInfoImpl {

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
    return "dd/MM/yyyy";
  }

  @Override
  public String dateFormatShort() {
    return "dd/MM/yy";
  }

  @Override
  public String[] erasFull() {
    return new String[] { 
        "Antes de Cristo",
        "Ano do Senhor"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] { 
        "a.C.",
        "d.C."
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 1;
  }

  @Override
  public String formatMinuteSecond() {
    return "mm'min'ss's'";
  }

  @Override
  public String formatMonthAbbrevDay() {
    return "d 'de' MMM";
  }

  @Override
  public String formatMonthFullDay() {
    return "d 'de' MMMM";
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE, d 'de' MMMM";
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
    return "MM/yyyy";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d/M/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d 'de' MMM 'de' y";
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
        "janeiro",
        "fevereiro",
        "março",
        "abril",
        "maio",
        "junho",
        "julho",
        "agosto",
        "setembro",
        "outubro",
        "novembro",
        "dezembro"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return monthsFull();
  }

  @Override
  public String[] monthsShort() {
    return new String[] { 
        "jan",
        "fev",
        "mar",
        "abr",
        "mai",
        "jun",
        "jul",
        "ago",
        "set",
        "out",
        "nov",
        "dez"
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return monthsShort();
  }

  @Override
  public String[] quartersFull() {
    return new String[] { 
        "1º trimestre",
        "2º trimestre",
        "3º trimestre",
        "4º trimestre"
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
  public String timeFormatFull() {
    return "HH'h'mm'min'ss's' zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "HH'h'mm'min'ss's' z";
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
        "domingo",
        "segunda-feira",
        "terça-feira",
        "quarta-feira",
        "quinta-feira",
        "sexta-feira",
        "sábado"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return weekdaysFull();
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] { 
        "D",
        "S",
        "T",
        "Q",
        "Q",
        "S",
        "S"
    };
  }

  @Override
  public String[] weekdaysNarrowStandalone() {
    return weekdaysNarrow();
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] { 
        "dom",
        "seg",
        "ter",
        "qua",
        "qui",
        "sex",
        "sáb"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return weekdaysShort();
  }
}
