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
 * Implementation of DateTimeFormatInfo for the "mn" locale.
 */
public class DateTimeFormatInfoImpl_mn extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "ҮӨ",
        "ҮХ"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, y 'оны' MMMM 'сарын' dd";
  }

  @Override
  public String dateFormatLong() {
    return "y 'оны' MMMM 'сарын' d";
  }

  @Override
  public String dateTimeShort(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "манай эриний өмнөх",
        "манай эриний"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "МЭӨ",
        "МЭ"
    };
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE MMMM d";
  }

  @Override
  public String formatMonthNumDay() {
    return "M-d";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y 'оны' MMMM 'сарын' d";
  }

  @Override
  public String formatYearMonthNum() {
    return "y-M";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "y-M-d";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, y MMM d";
  }

  @Override
  public String formatYearQuarterFull() {
    return "y 'оны' QQQQ";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "Нэгдүгээр сар",
        "Хоёрдугаар сар",
        "Гуравдугаар сар",
        "Дөрөвдүгээр сар",
        "Тавдугаар сар",
        "Зургадугаар сар",
        "Долдугаар сар",
        "Наймдугаар сар",
        "Есдүгээр сар",
        "Аравдугаар сар",
        "Арван нэгдүгээр сар",
        "Арван хоёрдугаар сар"
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
        "1-р сар",
        "2-р сар",
        "3-р сар",
        "4-р сар",
        "5-р сар",
        "6-р сар",
        "7-р сар",
        "8-р сар",
        "9-р сар",
        "10-р сар",
        "11-р сар",
        "12-р сар"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1-р улирал",
        "2-р улирал",
        "3-р улирал",
        "4-р улирал"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "У1",
        "У2",
        "У3",
        "У4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "ням",
        "даваа",
        "мягмар",
        "лхагва",
        "пүрэв",
        "баасан",
        "бямба"
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
        "Ня",
        "Да",
        "Мя",
        "Лх",
        "Пү",
        "Ба",
        "Бя"
    };
  }
}
