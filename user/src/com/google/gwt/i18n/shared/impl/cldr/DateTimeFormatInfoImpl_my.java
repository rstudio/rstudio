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
//  date=$Date: 2014-03-04 00:35:05 +0100 (Tue, 04 Mar 2014) $
//  number=$Revision: 9862 $
//  type=my

/**
 * Implementation of DateTimeFormatInfo for the "my" locale.
 */
public class DateTimeFormatInfoImpl_my extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "နံနက်",
        "ညနေ"
    };
  }

  @Override
  public String dateFormatFull() {
    return "EEEE, y MMMM dd";
  }

  @Override
  public String dateFormatShort() {
    return "yy/MM/dd";
  }

  @Override
  public String dateTimeFull(String timePattern, String datePattern) {
    return datePattern + "မှာ " + timePattern;
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "ခရစ်တော် မပေါ်မီကာလ",
        "ခရစ်တော် ပေါ်ထွန်းပြီးကာလ"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "ဘီစီ",
        "အေဒီ"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String formatMonthFullWeekdayDay() {
    return "EEEE, MMMM d";
  }

  @Override
  public String formatMonthNumDay() {
    return "M/d";
  }

  @Override
  public String formatYearMonthNum() {
    return "y/M";
  }

  @Override
  public String formatYearMonthWeekdayDay() {
    return "EEE, y MMM d";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "ဇန်နဝါရီ",
        "ဖေဖော်ဝါရီ",
        "မတ်",
        "ဧပြီ",
        "မေ",
        "ဇွန်",
        "ဇူလိုင်",
        "ဩဂုတ်",
        "စက်တင်ဘာ",
        "အောက်တိုဘာ",
        "နိုဝင်ဘာ",
        "ဒီဇင်ဘာ"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "ဇ",
        "ဖ",
        "မ",
        "ဧ",
        "မ",
        "ဇ",
        "ဇ",
        "ဩ",
        "စ",
        "အ",
        "န",
        "ဒ"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "ဇန်နဝါရီ",
        "ဖေဖော်ဝါရီ",
        "မတ်",
        "ဧပြီ",
        "မေ",
        "ဇွန်",
        "ဇူလိုင်",
        "ဩဂုတ်",
        "စက်တင်ဘာ",
        "အောက်တိုဘာ",
        "နိုဝင်ဘာ",
        "ဒီဇင်ဘာ"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "ပထမ သုံးလပတ်",
        "ဒုတိယ သုံးလပတ်",
        "တတိယ သုံးလပတ်",
        "စတုတ္ထ သုံးလပတ်"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "ပထမ သုံးလပတ်",
        "ဒုတိယ သုံးလပတ်",
        "တတိယ သုံးလပတ်",
        "စတုတ္ထ သုံးလပတ်"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "တနင်္ဂနွေ",
        "တနင်္လာ",
        "အင်္ဂါ",
        "ဗုဒ္ဓဟူး",
        "ကြာသပတေး",
        "သောကြာ",
        "စနေ"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "တ",
        "တ",
        "အ",
        "ဗ",
        "က",
        "သ",
        "စ"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "တနင်္ဂနွေ",
        "တနင်္လာ",
        "အင်္ဂါ",
        "ဗုဒ္ဓဟူး",
        "ကြာသပတေး",
        "သောကြာ",
        "စနေ"
    };
  }
}
