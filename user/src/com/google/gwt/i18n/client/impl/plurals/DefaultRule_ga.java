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
 * Plural forms for Celtic (Scottish Gaelic).
 * 
 * See https://developer.mozilla.org/en/Localization_and_Plurals#Plural_rule_.234_%284_forms%29
 */
public class DefaultRule_ga extends DefaultRule {

  @Override
  public PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
        new PluralForm("one", "Count is 1 or 11"),
        new PluralForm("two", "Count is 2 or 12"),
        new PluralForm("few", "Count is 3-10 or 13-19"),
    };
  }

  @Override
  public int select(int n) {
    return (n == 1 || n == 11) ? 1 : (n == 2 || n == 12) ? 2
        : (n >= 3 && n <= 19) ? 3 : 0;
  }
}
