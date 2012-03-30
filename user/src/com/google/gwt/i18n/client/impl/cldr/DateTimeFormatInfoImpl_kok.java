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
//  date=$Date: 2012-02-07 13:32:35 -0500 (Tue, 07 Feb 2012) $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "kok" locale.
 */
public class DateTimeFormatInfoImpl_kok extends DateTimeFormatInfoImpl {

  @Override
  public String[] ampms() {
    return new String[] {
        "म.पू.",
        "म.नं."
    };
  }

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
    return "dd-MM-yyyy";
  }

  @Override
  public String dateFormatShort() {
    return "d-M-yy";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "क्रिस्तपूर्व",
        "क्रिस्तशखा"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "क्रिस्तपूर्व",
        "क्रिस्तशखा"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "जानेवारी",
        "फेब्रुवारी",
        "मार्च",
        "एप्रिल",
        "मे",
        "जून",
        "जुलै",
        "ओगस्ट",
        "सेप्टेंबर",
        "ओक्टोबर",
        "नोव्हेंबर",
        "डिसेंबर"
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
        "जानेवारी",
        "फेब्रुवारी",
        "मार्च",
        "एप्रिल",
        "मे",
        "जून",
        "जुलै",
        "ओगस्ट",
        "सेप्टेंबर",
        "ओक्टोबर",
        "नोव्हेंबर",
        "डिसेंबर"
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
  public String timeFormatFull() {
    return "h:mm:ss a zzzz";
  }

  @Override
  public String timeFormatLong() {
    return "h:mm:ss a z";
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
        "आदित्यवार",
        "सोमवार",
        "मंगळार",
        "बुधवार",
        "गुरुवार",
        "शुक्रवार",
        "शनिवार"
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
        "रवि",
        "सोम",
        "मंगळ",
        "बुध",
        "गुरु",
        "शुक्र",
        "शनि"
    };
  }

  @Override
  public int weekendStart() {
    return 0;
  }
}
