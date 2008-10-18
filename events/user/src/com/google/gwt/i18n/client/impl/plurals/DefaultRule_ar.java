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

/**
 * Plural forms for Arabic are 0, 1, 2, x03-x10, x11-x99.
 */
public class DefaultRule_ar extends DefaultRule {

  @Override
  public PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
        new PluralForm("none", "Count is 0"),
        new PluralForm("one", "Count is 1"),
        new PluralForm("two", "Count is 2"),
        new PluralForm("few", "Count is between x03 and x10"),
        new PluralForm("many", "Count is between x11 and x99"),
    };
  }

  @Override
  public int select(int n) {
    return n == 0 ? 1 : n == 1 ? 2 : n == 2 ? 3 : n % 100 >= 3 && n % 100 <= 10 ? 4 : n % 100 >= 11
        && n % 100 <= 99 ? 5 : 0;
  }
}
