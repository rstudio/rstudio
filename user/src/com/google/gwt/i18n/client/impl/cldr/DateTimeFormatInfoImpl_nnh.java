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
//  date=$Date: 2013-08-31 06:19:50 +0200 (Sat, 31 Aug 2013) $
//  number=$Revision: 9297 $
//  type=nnh

/**
 * Implementation of DateTimeFormatInfo for the "nnh" locale.
 */
public class DateTimeFormatInfoImpl_nnh extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "mbaʼámbaʼ",
        "ncwònzém"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE , 'lyɛ'̌ʼ d 'na' MMMM, y";
  }

  @Override
  public String dateFormatLong() {
    return "'lyɛ'̌ʼ d 'na' MMMM, y";
  }

  @Override
  public String dateFormatMedium() {
    return "d MMM, y";
  }

  @Override
  public String dateFormatShort() {
    return "dd/MM/yy";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + "," + timePattern;
  }

  @Override
  public String dateTimeLong(String timePattern, String datePattern) {
    return datePattern + ", " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "mé zyé Yěsô",
        "mé gÿo ńzyé Yěsô"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "m.z.Y.",
        "m.g.n.Y."
    };
  }

  @Override
  public String formatYearMonthAbbrev() {
    return "MMM y";
  }

  @Override
  public String formatYearMonthAbbrevDay() {
    return "'lyɛ'̌ʼ d 'na' MMMM, y";
  }

  @Override
  public String formatYearMonthFull() {
    return "MMMM y";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "'lyɛ'̌ʼ d 'na' MMMM, y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d/M/y";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE , 'lyɛ'̌ʼ d 'na' MMM, y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "saŋ tsetsɛ̀ɛ lùm",
        "saŋ kàg ngwóŋ",
        "saŋ lepyè shúm",
        "saŋ cÿó",
        "saŋ tsɛ̀ɛ cÿó",
        "saŋ njÿoláʼ",
        "saŋ tyɛ̀b tyɛ̀b mbʉ̀",
        "saŋ mbʉ̀ŋ",
        "saŋ ngwɔ̀ʼ mbÿɛ",
        "saŋ tàŋa tsetsáʼ",
        "saŋ mejwoŋó",
        "saŋ lùm"
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
        "saŋ tsetsɛ̀ɛ lùm",
        "saŋ kàg ngwóŋ",
        "saŋ lepyè shúm",
        "saŋ cÿó",
        "saŋ tsɛ̀ɛ cÿó",
        "saŋ njÿoláʼ",
        "saŋ tyɛ̀b tyɛ̀b mbʉ̀",
        "saŋ mbʉ̀ŋ",
        "saŋ ngwɔ̀ʼ mbÿɛ",
        "saŋ tàŋa tsetsáʼ",
        "saŋ mejwoŋó",
        "saŋ lùm"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "Q1",
        "Q2",
        "Q3",
        "Q4"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "lyɛʼɛ́ sẅíŋtè",
        "mvfò lyɛ̌ʼ",
        "mbɔ́ɔntè mvfò lyɛ̌ʼ",
        "tsètsɛ̀ɛ lyɛ̌ʼ",
        "mbɔ́ɔntè tsetsɛ̀ɛ lyɛ̌ʼ",
        "mvfò màga lyɛ̌ʼ",
        "màga lyɛ̌ʼ"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "lyɛʼɛ́ sẅíŋtè",
        "mvfò lyɛ̌ʼ",
        "mbɔ́ɔntè mvfò lyɛ̌ʼ",
        "tsètsɛ̀ɛ lyɛ̌ʼ",
        "mbɔ́ɔntè tsetsɛ̀ɛ lyɛ̌ʼ",
        "mvfò màga lyɛ̌ʼ",
        "màga lyɛ̌ʼ"
    };
  }
}
