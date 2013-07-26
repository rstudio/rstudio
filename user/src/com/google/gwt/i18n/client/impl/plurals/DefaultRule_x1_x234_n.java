/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.i18n.client.impl.plurals;

import com.google.gwt.i18n.client.PluralRule.PluralForm;

/**
 * Common plural rule for languages that have singular and two plural forms,
 * based on the units and tens digits. Some Slavic languages use this form.
 * 
 * @see DefaultRule_0_1_2_n
 * @see DefaultRule_0_1_n
 * @see DefaultRule_01_n
 * @see DefaultRule_1_0n
 * @see DefaultRule_1_2_n
 * @see DefaultRule_1_234_n for the plural forms used in other Slavic languages
 * @see DefaultRule_1_paucal_n
 */
public class DefaultRule_x1_x234_n {

  /*
   * Returns the list of plural forms, indicating the "many" form is optional.
   *
   * The "many" form is marked "noWarn" to supress warning messages for
   * older translations that only have 3 forms: "one", "few", and "other".
   * Old translations should be updated by moving "other" to "many" and adding
   * a new "other" form for fractional values (as defined below).
   *
   * New translations should have all 4 forms including the "other" form
   * for fractional values (as defined below).
   *
   * TODO: After a transition time, remove the "noWarn" flag from "many".
   */
  public static PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
        new PluralForm("one", "Count ends in 1 but not 11"),
        new PluralForm("few", "Count ends in 2-4 but not 12-14"),
        new PluralForm("many", "Count ends in 0 or 5-9 or 11-15", true),
    };
  }

  /*
   * Returns 1, 2, or 3 ("one", "few", or "many").
   * Will never return 0 because "other" is the fractional plural form.
   */
  public static int select(int n) {
    return n % 10 == 1 && n % 100 != 11 ? 1 : n % 10 >= 2 && n % 10 <= 4
        && (n % 100 < 10 || n % 100 >= 20) ? 2 : 3;
  }
}
