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
package com.google.gwt.i18n.client;

/**
 * Create a custom localized date/time format at compile time. All methods on
 * subtypes of this interface must take no parameters and return DateTimeFormat
 * (which will be an initialized instance).
 * 
 * deprecated use {@link com.google.gwt.i18n.shared.CustomDateTimeFormat} instead
 */
// Temporarily remove deprecation to keep from breaking teams that don't allow
// deprecated references.
// @Deprecated
public interface CustomDateTimeFormat extends com.google.gwt.i18n.shared.CustomDateTimeFormat {

  /**
   * Annotation containing the pattern skeleton.
   * 
   * <p>The order of pattern characters and any literals don't matter, just
   * which pattern characters are present and their counts.
   * 
   * deprecated use {@link com.google.gwt.i18n.shared.CustomDateTimeFormat.Pattern} instead
   */
  // Temporarily remove deprecation to keep from breaking teams that don't allow
  // deprecated references.
  // @Deprecated
  public @interface Pattern {

    /**
     * The pattern skeleton for which to generate a localized pattern.  Note
     * that the order of pattern characters don't matter, as the generated
     * pattern will be derived from a localized pattern that conveys the same
     * information.
     * 
     * @return the pattern skeleton
     */
    String value();
  }
}
