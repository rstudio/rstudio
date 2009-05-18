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
 * Usually, there are no plural forms in Chinese.  When they are used,
 * plural forms are 1 and n, with 0 treated as plural.
 * 
 * Plural forms are not required to be marked, but it is optional so
 * a plural form is provided here.  No warning will be issued if it
 * is not included, as it is acceptable to use the same form.
 * 
 * Also, note that this does not address the issue of using different
 * symbols for the same digits depending on what is being counted.
 */
public class DefaultRule_zh extends DefaultRule {

  @Override
  public PluralForm[] pluralForms() {
    return DefaultRule_1_0n.pluralFormsOptional();
  }

  @Override
  public int select(int n) {
    return DefaultRule_1_0n.select(n);
  }
}
