/*
 * Copyright 2008 Google Inc.
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
 * Common plural rule for languages that have 0, 1, 2, and other forms.
 * 
 * @see DefaultRule_0_1_n
 * @see DefaultRule_01_n
 * @see DefaultRule_1_0n
 * @see DefaultRule_1_2_n
 * @see DefaultRule_1_234_n
 * @see DefaultRule_1_paucal_n
 * @see DefaultRule_x1_x234_n
 */
public class DefaultRule_0_1_2_n {

  public static PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
        new PluralForm("none", "Count is 0"),
        new PluralForm("one", "Count is 1"),
        new PluralForm("two", "Count is 2"),
    };
  }

  public static int select(int n) {
    return n >= 0 && n <= 2 ? n + 1 : 0;
  }
}
