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
 * Implementation of DateTimeFormatInfo for the "th" locale.
 */
public class DateTimeFormatInfoImpl_th extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "ก่อนเที่ยง",
        "หลังเที่ยง"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEEที่ d MMMM G y";
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
    return "d/M/yyyy";
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
  public String dateTimeShort(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "ปีก่อนคริสต์ศักราช",
        "คริสต์ศักราช"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "ปีก่อน ค.ศ.",
        "ค.ศ."
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
  public String formatYear() {
    return "G y";
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
    return "QQQQ yyyy";
  }

  @Override
  public String formatYearQuarterShort() {
    return "Q yyyy";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "มกราคม",
        "กุมภาพันธ์",
        "มีนาคม",
        "เมษายน",
        "พฤษภาคม",
        "มิถุนายน",
        "กรกฎาคม",
        "สิงหาคม",
        "กันยายน",
        "ตุลาคม",
        "พฤศจิกายน",
        "ธันวาคม"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "ม.ค.",
        "ก.พ.",
        "มี.ค.",
        "เม.ย.",
        "พ.ค.",
        "มิ.ย",
        "ก.ค.",
        "ส.ค.",
        "ก.ย.",
        "ต.ค.",
        "พ.ย.",
        "ธ.ค."
    };
  }

  @Override
  public String[] monthsNarrowStandalone() {
    return new String[] {
        "ม.ค.",
        "ก.พ.",
        "มี.ค.",
        "เม.ย.",
        "พ.ค.",
        "มิ.ย.",
        "ก.ค.",
        "ส.ค.",
        "ก.ย.",
        "ต.ค.",
        "พ.ย.",
        "ธ.ค."
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "ม.ค.",
        "ก.พ.",
        "มี.ค.",
        "เม.ย.",
        "พ.ค.",
        "มิ.ย.",
        "ก.ค.",
        "ส.ค.",
        "ก.ย.",
        "ต.ค.",
        "พ.ย.",
        "ธ.ค."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "ไตรมาส 1",
        "ไตรมาส 2",
        "ไตรมาส 3",
        "ไตรมาส 4"
    };
  }

  @Override
  public String timeFormatFull() {
    return "H นาฬิกา m นาที ss วินาที zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "H นาฬิกา m นาที ss วินาที z";
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
        "วันอาทิตย์",
        "วันจันทร์",
        "วันอังคาร",
        "วันพุธ",
        "วันพฤหัสบดี",
        "วันศุกร์",
        "วันเสาร์"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "อ",
        "จ",
        "อ",
        "พ",
        "พ",
        "ศ",
        "ส"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "อา.",
        "จ.",
        "อ.",
        "พ.",
        "พฤ.",
        "ศ.",
        "ส."
    };
  }
}
