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

import java.util.List;

/**
 * Class representing GWT locales and conversion to/from other formats.
 *
 * These locales correspond to BCP47.
 */
public interface GwtLocale extends Comparable<GwtLocale> {

  String DEFAULT_LOCALE = "default";

  /**
   * The default comparison is a lexical ordering.
   */
  int compareTo(GwtLocale o);

  /**
   * Returns the list of aliases for this locale.  The canonical form of the
   * current locale is always first on the list.
   *
   * Language/region codes have changed over time, so some systems continue to
   * use the older codes.  Aliases allow GWT to use the official Unicode CLDR
   * locales while still interoperating with such systems.
   *
   * @return alias list
   */
  List<GwtLocale> getAliases();

  /**
   * Returns the locale as a fixed-format string suitable for use in searching
   * for localized resources.  The format is language_Script_REGION_VARIANT,
   * where language is a 2-8 letter code (possibly with 3-letter extensions),
   * script is a 4-letter code with an initial capital letter, region is a
   * 2-character country code or a 3-digit region code, and variant is a 5-8
   * character (may be 4 if the first character is numeric) code.  If a
   * component is missing, its preceding _ is also omitted.  If this is the
   * default locale, the empty string will be returned.
   *
   * @return String representing locale
   */
  String getAsString();

  /**
   * Returns this locale in canonical form.
   * <ul>
   *  <li>Deprecated language/region tags are replaced with official versions
   *  <li>Default scripts are removed (including region-specific defaults for
   *      Chinese)
   *  <li>no/nb/nn are normalized
   *  <li>Default region for zh_Hans and zh_Hant if none specified
   * </ul>
   *
   * @return GwtLocale instance
   */
  GwtLocale getCanonicalForm();

  /**
   * Returns the complete list of locales to search for the current locale.
   * This list will always start with the canonical form of this locale, and
   * end with "default", and include all appropriate aliases along the way.
   *
   * @return search list
   */
  List<GwtLocale> getCompleteSearchList();

  /**
   * Returns a list of locales to search for, in order of preference.  The
   * current locale is always first on the list.  Aliases are not included
   * in the list -- use {@link #getAliases} to expand those.
   *
   * @return inheritance list
   */
  List<GwtLocale> getInheritanceChain();

  /**
   * Returns the language portion of the locale, or null if none.
   */
  String getLanguage();

  /**
   * Returns the language portion of the locale, or the empty string if none.
   */
  String getLanguageNotNull();

  /**
   * Returns the region portion of the locale, or null if none.
   */
  String getRegion();

  /**
   * Returns the region portion of the locale, or the empty string if none.
   */
  String getRegionNotNull();

  /**
   * Returns the script portion of the locale, or null if none.
   */
  String getScript();

  /**
   * Returns the script portion of the locale, or the empty string if none.
   */
  String getScriptNotNull();

  /**
   * Returns the variant portion of the locale, or null if none.
   */
  String getVariant();

  /**
   * Returns the variant portion of the locale, or the empty string if none.
   */
  String getVariantNotNull();

  /**
   * Returns true if this locale inherits from the specified locale.  Note that
   * locale.inheritsFrom(locale) is false -- if you want that to be true, you
   * should just use locale.getInheritanceChain().contains(x).
   *
   * @param parent locale to test against
   * @return true if parent is an ancestor of this locale
   */
  boolean inheritsFrom(GwtLocale parent);

  /**
   * Returns true if this is the default or root locale.
   */
  boolean isDefault();

  /**
   * Returns a human readable string -- "default" or the same as getAsString().
   */
  String toString();

  /**
   * Checks if this locale uses the same script as another locale.
   *
   * @param other
   * @return true if the scripts are the same
   */
  boolean usesSameScript(GwtLocale other);
}
