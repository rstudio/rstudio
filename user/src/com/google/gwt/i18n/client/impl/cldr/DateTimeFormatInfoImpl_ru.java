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
//  number=$Revision: 6546 Google $
//  type=root
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $

/**
 * Implementation of DateTimeFormatInfo for the "ru" locale.
 */
public class DateTimeFormatInfoImpl_ru extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "до полудня",
        "после полудня"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, d MMMM y 'г'.";
  }

  @Override
  public String dateFormatLong() {
    return "d MMMM y 'г'.";
  }

  @Override
  public String dateFormatMedium() {
    return "dd.MM.yyyy";
  }

  @Override
  public String dateFormatShort() {
    return "dd.MM.yy";
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
        "до н.э.",
        "н.э."
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "до н.э.",
        "н.э."
    };
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
    return "cccc, d MMMM";
  }

  @Override
  public String formatMonthNumDay() {
    return "d.M";
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "LLL y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "d MMM y 'г'.";
  }

  @Override
  public String formatYearMonthFull() {
    return "LLLL y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "d MMMM y 'г'.";
  }

  @Override
  public String formatYearMonthNum() {
    return "M.y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d.M.y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, d MMM y";
  }

  @Override
  public String formatYearQuarterFull() {
    return "QQQ y 'г'.";
  }

  @Override
  public String formatYearQuarterShort() {
    return "QQQ y 'г'.";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "января",
        "февраля",
        "марта",
        "апреля",
        "мая",
        "июня",
        "июля",
        "августа",
        "сентября",
        "октября",
        "ноября",
        "декабря"
    };
  }

  @Override
  public String[] monthsFullStandalone() {
    return new String[] {
        "Январь",
        "Февраль",
        "Март",
        "Апрель",
        "Май",
        "Июнь",
        "Июль",
        "Август",
        "Сентябрь",
        "Октябрь",
        "Ноябрь",
        "Декабрь"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "Я",
        "Ф",
        "М",
        "А",
        "М",
        "И",
        "И",
        "А",
        "С",
        "О",
        "Н",
        "Д"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "янв.",
        "февр.",
        "марта",
        "апр.",
        "мая",
        "июня",
        "июля",
        "авг.",
        "сент.",
        "окт.",
        "нояб.",
        "дек."
    };
  }

  @Override
  public String[] monthsShortStandalone() {
    return new String[] {
        "Янв.",
        "Февр.",
        "Март",
        "Апр.",
        "Май",
        "Июнь",
        "Июль",
        "Авг.",
        "Сент.",
        "Окт.",
        "Нояб.",
        "Дек."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "1-й квартал",
        "2-й квартал",
        "3-й квартал",
        "4-й квартал"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "1-й кв.",
        "2-й кв.",
        "3-й кв.",
        "4-й кв."
    };
  }

  @Override
  public String timeFormatFull() {
    return "H:mm:ss zzzz";
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
        "воскресенье",
        "понедельник",
        "вторник",
        "среда",
        "четверг",
        "пятница",
        "суббота"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return new String[] {
        "Воскресенье",
        "Понедельник",
        "Вторник",
        "Среда",
        "Четверг",
        "Пятница",
        "Суббота"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "В",
        "Пн",
        "Вт",
        "С",
        "Ч",
        "П",
        "С"
    };
  }

  @Override
  public String[] weekdaysNarrowStandalone() {
    return new String[] {
        "В",
        "П",
        "В",
        "С",
        "Ч",
        "П",
        "С"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "вс",
        "пн",
        "вт",
        "ср",
        "чт",
        "пт",
        "сб"
    };
  }

  @Override
  public String[] weekdaysShortStandalone() {
    return new String[] {
        "Вс",
        "Пн",
        "Вт",
        "Ср",
        "Чт",
        "Пт",
        "Сб"
    };
  }
}
