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
//  date=$Date: 2014-03-06 06:14:25 +0100 (Thu, 06 Mar 2014) $
//  number=$Revision: 9876 $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "ne" locale.
 */
public class DateTimeFormatInfoImpl_ne extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "पूर्व मध्यान्ह",
        "उत्तर मध्यान्ह"
    };
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
        "ईसा पूर्व",
        "सन्"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "ईसा पूर्व",
        "सन्"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "जनवरी",
        "फेब्रुअरी",
        "मार्च",
        "अप्रिल",
        "मे",
        "जुन",
        "जुलाई",
        "अगस्ट",
        "सेप्टेम्बर",
        "अक्टोबर",
        "नोभेम्बर",
        "डिसेम्बर"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "१",
        "२",
        "३",
        "४",
        "५",
        "६",
        "७",
        "८",
        "९",
        "१०",
        "११",
        "१२"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "जनवरी",
        "फेब्रुअरी",
        "मार्च",
        "अप्रिल",
        "मे",
        "जुन",
        "जुलाई",
        "अगस्ट",
        "सेप्टेम्बर",
        "अक्टोबर",
        "नोभेम्बर",
        "डिसेम्बर"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "पहिलो सत्र",
        "दोस्रो सत्र",
        "तेस्रो सत्र",
        "चौथो सत्र"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "पहिलो सत्र",
        "दोस्रो सत्र",
        "तेस्रो सत्र",
        "चौथो सत्र"
    };
  }

  @Override
  public String[] weekdaysFull() {
    return new String[] {
        "आइतबार",
        "सोमबार",
        "मङ्गलबार",
        "बुधबार",
        "बिहीबार",
        "शुक्रबार",
        "शनिबार"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "आ",
        "सो",
        "म",
        "बु",
        "बि",
        "शु",
        "श"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "आइत",
        "सोम",
        "मङ्गल",
        "बुध",
        "बिही",
        "शुक्र",
        "शनि"
    };
  }
}
