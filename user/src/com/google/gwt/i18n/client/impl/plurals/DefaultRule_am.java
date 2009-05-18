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
 * Plural forms for Amharic are 1 and n, with 0 treated as singular.
 */
public class DefaultRule_am extends DefaultRule {

  @Override
  public PluralForm[] pluralForms() {
    return DefaultRule_01_n.pluralForms();
  }

  @Override
  public int select(int n) {
    return DefaultRule_01_n.select(n);
  }
}
