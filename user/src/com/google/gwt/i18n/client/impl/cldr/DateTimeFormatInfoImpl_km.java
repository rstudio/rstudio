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
 * Implementation of DateTimeFormatInfo for the "km" locale.
 */
public class DateTimeFormatInfoImpl_km extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "ព្រឹក",
        "ល្ងាច"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE ថ្ងៃ d ខែ MMMM ឆ្នាំ y";
  }

  @Override
  public String dateFormatLong() {
    return "d ខែ MMMM ឆ្នាំ y";
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
        "មុន​គ្រិស្តសករាជ",
        "គ្រិស្តសករាជ"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "មុន​គ.ស.",
        "គ.ស."
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
  public String[] monthsFull() {
    return new String[] {
        "មករា",
        "កុម្ភៈ",
        "មិនា",
        "មេសា",
        "ឧសភា",
        "មិថុនា",
        "កក្កដា",
        "សីហា",
        "កញ្ញា",
        "តុលា",
        "វិច្ឆិកា",
        "ធ្នូ"
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
        "១",
        "២",
        "៣",
        "៤",
        "៥",
        "៦",
        "៧",
        "៨",
        "៩",
        "១០",
        "១១",
        "១២"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "ត្រីមាសទី១",
        "ត្រីមាសទី២",
        "ត្រីមាសទី៣",
        "ត្រីមាសទី៤"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "ត្រី១",
        "ត្រី២",
        "ត្រី៣",
        "ត្រី៤"
    };
  }

  @Override
  public String timeFormatFull() {
    return "H ម៉ោង m នាទី ss វិនាទី​ zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "H ម៉ោង m នាទី ss វិនាទី​z";
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
        "ថ្ងៃអាទិត្យ",
        "​ថ្ងៃច័ន្ទ",
        "ថ្ងៃអង្គារ",
        "ថ្ងៃពុធ",
        "ថ្ងៃព្រហស្បតិ៍",
        "ថ្ងៃសុក្រ",
        "ថ្ងៃសៅរ៍"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "1",
        "2",
        "3",
        "4",
        "5",
        "6",
        "7"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "អា",
        "ច",
        "អ",
        "ពុ",
        "ព្រ",
        "សុ",
        "ស"
    };
  }
}
