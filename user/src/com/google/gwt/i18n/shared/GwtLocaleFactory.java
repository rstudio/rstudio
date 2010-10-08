/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.i18n.shared;

/**
 * Factories that know how to create GwtLocale instances.
 */
public interface GwtLocaleFactory {

  /**
   * Construct a GWT locale from its component parts.
   *
   * Null or empty strings are accepted for parts not present.
   *
   * @param language
   * @param script
   * @param region
   * @param variant
   * @return GwtLocale instance, unique for a given set of values
   */
  GwtLocale fromComponents(String language, String script, String region,
      String variant);

  /**
   * Get a GWT locale from a string conforming to a subset of BCP47
   * (specifically assuming extension tags are not present, at most
   * one variant is present, and grandfathered tags are not supported;
   * also private-use tags are only supported for the entire tag).
   * Only minimal validation of BCP47 tags is performed, and will continue
   * with what it is able to parse if unexpected input is encountered.
   *
   * A null or empty string is treated as the default locale.
   *
   * @param localeName
   * @return a locale instance, always the same one for a given localeName
   */
  GwtLocale fromString(String localeName);

  /**
   * Returns an instance of the default locale.
   */
  GwtLocale getDefault();
}
