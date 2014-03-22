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
//  date=$Date: 2014-03-06 06:14:25 +0100 (Thu, 06 Mar 2014) $
//  number=$Revision: 9876 $
//  type=root

/**
 * Implementation of DateTimeFormatInfo for the "ml" locale.
 */
public class DateTimeFormatInfoImpl_ml extends DateTimeFormatInfoImpl {

  @Override
  public String dateFormatFull() {
    return "y, MMMM d, EEEE";
  }

  @Override
  public String dateFormatLong() {
    return "y, MMMM d";
  }

  @Override
  public String dateFormatMedium() {
    return "y, MMM d";
  }

  @Override
  public String dateFormatShort() {
    return "dd/MM/yy";
  }

  @Override
  public String[] erasFull() {
    return new String[] {
        "ക്രിസ്തുവിനു് മുമ്പ്‌",
        "ക്രിസ്തുവിന് പിൻപ്"
    };
  }

  @Override
  public String[] erasShort() {
    return new String[] {
        "ക്രി.മൂ",
        "എഡി"
    };
  }

  @Override
  public int firstDayOfTheWeek() {
    return 0;
  }

  @Override
  public String formatMonthNumDay() {
    return "d/M";
  }

  @Override
  public String formatYearMonthFullDay() {
    return "y, MMMM d";
  }

  @Override
  public String formatYearMonthNum() {
    return "M-y";
  }

  @Override
  public String formatYearMonthNumDay() {
    return "d/M/y";
  }

  @Override
  public String[] monthsFull() {
    return new String[] {
        "ജനുവരി",
        "ഫെബ്രുവരി",
        "മാർച്ച്",
        "ഏപ്രിൽ",
        "മേയ്",
        "ജൂൺ",
        "ജൂലൈ",
        "ആഗസ്റ്റ്",
        "സെപ്റ്റംബർ",
        "ഒക്‌ടോബർ",
        "നവംബർ",
        "ഡിസംബർ"
    };
  }

  @Override
  public String[] monthsNarrow() {
    return new String[] {
        "ജ",
        "ഫെ",
        "മാ",
        "ഏ",
        "മേ",
        "ജൂ",
        "ജൂ",
        "ഓ",
        "സെ",
        "ഒ",
        "ന",
        "ഡി"
    };
  }

  @Override
  public String[] monthsShort() {
    return new String[] {
        "ജനു",
        "ഫെബ്രു",
        "മാർ",
        "ഏപ്രി",
        "മേയ്",
        "ജൂൺ",
        "ജൂലൈ",
        "ഓഗ",
        "സെപ്റ്റം",
        "ഒക്ടോ",
        "നവം",
        "ഡിസം"
    };
  }

  @Override
  public String[] quartersFull() {
    return new String[] {
        "ഒന്നാം പാദം",
        "രണ്ടാം പാദം",
        "മൂന്നാം പാദം",
        "നാലാം പാദം"
    };
  }

  @Override
  public String[] quartersShort() {
    return new String[] {
        "ഒന്നാം പാദം",
        "രണ്ടാം പാദം",
        "മൂന്നാം പാദം",
        "നാലാം പാദം"
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
        "ഞായറാഴ്‌ച",
        "തിങ്കളാഴ്‌ച",
        "ചൊവ്വാഴ്ച",
        "ബുധനാഴ്‌ച",
        "വ്യാഴാഴ്‌ച",
        "വെള്ളിയാഴ്‌ച",
        "ശനിയാഴ്‌ച"
    };
  }

  @Override
  public String[] weekdaysFullStandalone() {
    return new String[] {
        "ഞായറാഴ്‌ച",
        "തിങ്കളാഴ്‌ച",
        "ചൊവ്വാഴ്‌ച",
        "ബുധനാഴ്‌ച",
        "വ്യാഴാഴ്‌ച",
        "വെള്ളിയാഴ്‌ച",
        "ശനിയാഴ്‌ച"
    };
  }

  @Override
  public String[] weekdaysNarrow() {
    return new String[] {
        "ഞാ",
        "തി",
        "ചൊ",
        "ബു",
        "വ്യാ",
        "വെ",
        "ശ"
    };
  }

  @Override
  public String[] weekdaysShort() {
    return new String[] {
        "ഞായർ",
        "തിങ്കൾ",
        "ചൊവ്വ",
        "ബുധൻ",
        "വ്യാഴം",
        "വെള്ളി",
        "ശനി"
    };
  }

  @Override
  public int weekendStart() {
    return 0;
  }
}
