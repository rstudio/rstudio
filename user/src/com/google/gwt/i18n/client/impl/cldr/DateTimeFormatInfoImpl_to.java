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
//  type=to

/**
 * Implementation of DateTimeFormatInfo for the "to" locale.
 */
public class DateTimeFormatInfoImpl_to extends DateTimeFormatInfoImpl {

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
  public String dateTimeMedium(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "ki muʻa",
        "taʻu ʻo Sīsū"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "KM",
        "TS"
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
    return "d/M/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE d MMM y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "Sānuali",
        "Fēpueli",
        "Maʻasi",
        "ʻEpeleli",
        "Mē",
        "Sune",
        "Siulai",
        "ʻAokosi",
        "Sepitema",
        "ʻOkatopa",
        "Nōvema",
        "Tīsema"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "S",
        "F",
        "M",
        "E",
        "M",
        "S",
        "S",
        "A",
        "S",
        "O",
        "N",
        "T"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "Sān",
        "Fēp",
        "Maʻa",
        "ʻEpe",
        "Mē",
        "Sun",
        "Siu",
        "ʻAok",
        "Sep",
        "ʻOka",
        "Nōv",
        "Tīs"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "kuata ʻuluaki",
        "kuata ua",
        "kuata tolu",
        "kuata fā"
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
        "Sāpate",
        "Mōnite",
        "Tūsite",
        "Pulelulu",
        "Tuʻapulelulu",
        "Falaite",
        "Tokonaki"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "M",
        "T",
        "P",
        "T",
        "F",
        "T"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "Sāp",
        "Mōn",
        "Tūs",
        "Pul",
        "Tuʻa",
        "Fal",
        "Tok"
    };
  }
}
