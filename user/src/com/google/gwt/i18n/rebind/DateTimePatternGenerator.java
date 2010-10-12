/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.i18n.rebind;

import com.google.gwt.i18n.shared.GwtLocale;

import com.ibm.icu.util.ULocale;

/**
 * Helper class to create a localized date/time pattern based on a pattern
 * skeleton.
 */
public class DateTimePatternGenerator {
  // TODO(jat): Currently uses ICU4J's DateTimePatternGenerator, but should
  // probably be rewritten to avoid that dependency.

  private final com.ibm.icu.text.DateTimePatternGenerator dtpg;

  /**
   * Construct a DateTimePatternGenerator for a given locale.
   * 
   * @param gwtLocale
   */
  public DateTimePatternGenerator(GwtLocale gwtLocale) {
    String localeName = gwtLocale.getAsString();
    localeName = ULocale.canonicalize(localeName);
    ULocale locale = new ULocale(localeName);
    dtpg = com.ibm.icu.text.DateTimePatternGenerator.getInstance(locale);
  }

  /**
   * Get the best matching localized pattern for the requested skeleton
   * pattern.
   * 
   * @param skeleton a skeleton pattern consisting of groups of pattern
   *     characters - spaces and punctuation are ignored
   * @return a localized pattern suitable for use with
   *     {@link com.google.gwt.i18n.client.DateTimeFormat}.
   */
  public String getBestPattern(String skeleton) {
    return dtpg.getBestPattern(skeleton);
  }
}
