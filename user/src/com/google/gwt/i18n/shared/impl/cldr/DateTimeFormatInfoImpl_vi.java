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
package com.google.gwt.i18n.shared.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=21.0
//  number=$Revision: 6546 Google $
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "vi" locale.
 */
public class DateTimeFormatInfoImpl_vi extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "SA",
        "CH"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, 'ngày' dd MMMM 'năm' y";
  }

  @Override
  public String dateFormatLong() {
    return "'Ngày' dd 'tháng' M 'năm' y";
  }

  @Override
  public String dateFormatMedium() {
    return "dd-MM-yyyy";
  }

  @Override
  public String dateFormatShort() {
    return "dd/MM/yyyy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return timePattern + " " + datePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return timePattern + " " + datePattern;
  }

  @Override
  public String dateTimeMedium(String timePattern, String datePattern) {
    return timePattern + " " + datePattern;
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return timePattern + " " + datePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "tr. CN",
        "sau CN"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "tr. CN",
        "sau CN"
    };
  }

  @Override
  public String formatDay() {
    return "'Ngày' d";
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
    return "d-M";
  }

  @Override
  public String formatYear() {
    return "'Năm' y";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM, y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM, y";
  }

  @Override
  public String formatYearMonthNum() {
    return "M/yyyy";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d/M/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d MMM y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQQ yyyy";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q yyyy";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "tháng một",
        "tháng hai",
        "tháng ba",
        "tháng tư",
        "tháng năm",
        "tháng sáu",
        "tháng bảy",
        "tháng tám",
        "tháng chín",
        "tháng mười",
        "tháng mười một",
        "tháng mười hai"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "10",
        "11",
        "12"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "thg 1",
        "thg 2",
        "thg 3",
        "thg 4",
        "thg 5",
        "thg 6",
        "thg 7",
        "thg 8",
        "thg 9",
        "thg 10",
        "thg 11",
        "thg 12"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "Quý 1",
        "Quý 2",
        "Quý 3",
        "Quý 4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "Chủ nhật",
        "Thứ hai",
        "Thứ ba",
        "Thứ tư",
        "Thứ năm",
        "Thứ sáu",
        "Thứ bảy"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "CN",
        "T2",
        "T3",
        "T4",
        "T5",
        "T6",
        "T7"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "CN",
        "Th 2",
        "Th 3",
        "Th 4",
        "Th 5",
        "Th 6",
        "Th 7"
    };
  }
}
