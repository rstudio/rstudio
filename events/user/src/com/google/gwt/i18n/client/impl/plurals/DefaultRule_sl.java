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
 * Plural forms for Slovenian are x01, x02, x03-x04, and n.
 */
public class DefaultRule_sl extends DefaultRule {

  @Override
  public PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
        new PluralForm("one", "Count ends in 01"),
        new PluralForm("two", "Count ends in 02"),
        new PluralForm("few", "Count ends in 03 or 04"),
    };
  }

  @Override
  public int select(int n) {
    return n % 100 == 1 ? 1 : n % 100 == 2 ? 2
        : n % 100 == 3 || n % 100 == 4 ? 3 : 0;
  }
}
