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
 * Plural forms for Welsh.
 * 
 * See http://www.saltcymru.org/wordpress/?p=99&lang=en
 */
public class DefaultRule_cy extends DefaultRule {

  @Override
  public PluralForm[] pluralForms() {
    return new PluralForm[] {
        new PluralForm("other", "Default plural form"),
        new PluralForm("none", "Count is 0"),
        new PluralForm("one", "Count is 1"),
        new PluralForm("two", "Count is 2"),
        new PluralForm("three", "Count is 3"),
        new PluralForm("six", "Count is 6"),
    };
  }

  @Override
  public int select(int n) {
    return n == 0 ? 1 : n == 1 ? 2 : n == 2 ? 3 : n == 3 ? 4
        : n == 6 ? 5 : 0;
  }
}
