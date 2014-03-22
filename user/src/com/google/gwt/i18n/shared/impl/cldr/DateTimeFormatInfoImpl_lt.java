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
package com.google.gwt.i18n.shared.impl.cldr;

// DO NOT EDIT - GENERATED FROM CLDR AND ICU DATA
//  cldrVersion=25
//  date=$Date: 2014-03-01 06:57:43 +0100 (Sat, 01 Mar 2014) $
//  number=$Revision: 9852 $
//  type=lt

/**
 * Implementation of DateTimeFormatInfo for the "lt" locale.
 */
public class DateTimeFormatInfoImpl_lt extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "pr.p.",
        "pop."
    };
  }

  @Override
  public String dateFormatFull() {
    return "y 'm'. MMMM d 'd'., EEEE";
  }

  @Override
  public String dateFormatLong() {
    return "y 'm'. MMMM d 'd'.";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "prieš Kristų",
        "po Kristaus"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "pr. Kr.",
        "po Kr."
    };
  }

  @Override
  public String formatDay() {
    return "dd";
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
  public String formatMonthNumDay() {
    return "MM-d";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y 'm'. MMMM d 'd'.";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "sausis",
        "vasaris",
        "kovas",
        "balandis",
        "gegužė",
        "birželis",
        "liepa",
        "rugpjūtis",
        "rugsėjis",
        "spalis",
        "lapkritis",
        "gruodis"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "S",
        "V",
        "K",
        "B",
        "G",
        "B",
        "L",
        "R",
        "R",
        "S",
        "L",
        "G"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "saus.",
        "vas.",
        "kov.",
        "bal.",
        "geg.",
        "birž.",
        "liep.",
        "rugp.",
        "rugs.",
        "spal.",
        "lapkr.",
        "gruod."
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "I ketvirtis",
        "II ketvirtis",
        "III ketvirtis",
        "IV ketvirtis"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "I k.",
        "II k.",
        "III k.",
        "IV k."
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "sekmadienis",
        "pirmadienis",
        "antradienis",
        "trečiadienis",
        "ketvirtadienis",
        "penktadienis",
        "šeštadienis"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "S",
        "P",
        "A",
        "T",
        "K",
        "P",
        "Š"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "sk",
        "pr",
        "an",
        "tr",
        "kt",
        "pn",
        "št"
    };
  }
}
