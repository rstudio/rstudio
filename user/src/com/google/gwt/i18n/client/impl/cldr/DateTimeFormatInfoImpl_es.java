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
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "es" locale.
 */
public class DateTimeFormatInfoImpl_es extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "a. m.",
        "p. m."
    };
  }

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
    return "d/M/y";
  }

  @Override
  public String dateFormatShort() {
    return "d/M/yy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "antes de Cristo",
        "anno Dómini"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "a. C.",
        "d. C."
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String formatHour24Minute() {
    return "H:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "H:mm:ss";
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
    return "EEEE d 'de' MMMM";
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
    return "M/y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d/M/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d 'de' MMMM 'de' y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ 'de' y";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "enero",
        "febrero",
        "marzo",
        "abril",
        "mayo",
        "junio",
        "julio",
        "agosto",
        "septiembre",
        "octubre",
        "noviembre",
        "diciembre"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "Enero",
        "Febrero",
        "Marzo",
        "Abril",
        "Mayo",
        "Junio",
        "Julio",
        "Agosto",
        "Septiembre",
        "Octubre",
        "Noviembre",
        "Diciembre"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "E",
        "F",
        "M",
        "A",
        "M",
        "J",
        "J",
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
        "ene.",
        "feb.",
        "mar.",
        "abr.",
        "may.",
        "jun.",
        "jul.",
        "ago.",
        "sept.",
        "oct.",
        "nov.",
        "dic."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "Ene.",
        "Feb.",
        "Mar.",
        "Abr.",
        "May.",
        "Jun.",
        "Jul.",
        "Ago.",
        "Sept.",
        "Oct.",
        "Nov.",
        "Dic."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1.er trimestre",
        "2.º trimestre",
        "3.er trimestre",
        "4.º trimestre"
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
    return "H:mm:ss (zzzz)";
  }

  @Override
  public String timeFormatLong() {
    return "H:mm:ss z";
  }

  @Override
  public String timeFormatMedium() {
    return "H:mm:ss";
  }

  @Override
  public String timeFormatShort() {
    return "H:mm";
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "domingo",
        "lunes",
        "martes",
        "miércoles",
        "jueves",
        "viernes",
        "sábado"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return new String[] {
        "Domingo",
        "Lunes",
        "Martes",
        "Miércoles",
        "Jueves",
        "Viernes",
        "Sábado"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "D",
        "L",
        "M",
        "X",
        "J",
        "V",
        "S"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "dom.",
        "lun.",
        "mar.",
        "mié.",
        "jue.",
        "vie.",
        "sáb."
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return new String[] {
        "Dom.",
        "Lun.",
        "Mar.",
        "Mié.",
        "Jue.",
        "Vie.",
        "Sáb."
    };
  }
}
