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
 * Plural forms for Lithuanian are x1 (except x11), x2-x9 (except x12-x19), and n.
 */
public class DefaultRule_lt extends DefaultRule {

  @Override
  public PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
        new PluralForm("one", "Count ends in 1 but not 11"),
        new PluralForm("few", "Count ends in 2-9 but not 12-19"),
    };
  }

  @Override
  public int select(int n) {
    return n % 10 == 1 && n % 100 != 11 ? 1
        : n % 10 >= 2 && (n % 100 < 10 || n % 100 >= 20) ? 2
        : 0;
  }
}
