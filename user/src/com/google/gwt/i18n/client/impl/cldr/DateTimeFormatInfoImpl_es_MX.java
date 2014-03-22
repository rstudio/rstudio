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
//  type=MX

/**
 * Implementation of DateTimeFormatInfo for the "es_MX" locale.
 */
public class DateTimeFormatInfoImpl_es_MX extends DateTimeFormatInfoImpl_es_419 {

  @Override
  public String[] ampms() {
    return new String[] {
        "a.m.",
        "p.m."
    };
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
    return datePattern + " " + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + " " + timePattern;
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
    return 0;
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
  public String formatHour24Minute() {
    return "HH:mm";
  }

  @Override
  public String formatHour24MinuteSecond() {
    return "HH:mm:ss";
  }

  @Override
  public String[] monthsFullStandalone() {
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
  public String[] monthsNarrow() {
    return new String[] {
        "E",
        "F",
        "Ma",
        "A",
        "My",
        "Jn",
        "Jl",
        "Ag",
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
        "febr.",
        "mzo.",
        "abr.",
        "my.",
        "jun.",
        "jul.",
        "ag.",
        "set.",
        "oct.",
        "nov.",
        "dic."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "en.",
        "febr.",
        "mzo.",
        "abr.",
        "my.",
        "jun",
        "jul",
        "ag.",
        "set.",
        "oct.",
        "nov.",
        "dic."
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
  public String[] weekdaysFullStandalone() {
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
  public String[] weekdaysShort() {
    return new String[] {
        "dom.",
        "lun.",
        "mar.",
        "miér.",
        "jue.",
        "vier.",
        "sáb"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return new String[] {
        "dom.",
        "lun",
        "mar.",
        "mié.",
        "jue.",
        "vie",
        "sáb."
    };
  }
}
