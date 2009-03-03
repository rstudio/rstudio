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
   * Return the list of aliases for this locale.  The current locale is always
   * first on the list.
   * 
   * Language/region codes have changed over time, so some systems continue to
   * use the older codes.  Aliases allow GWT to use the official Unicode CLDR
   * locales while still interoperating with such systems.
   * 
   * @return alias list
   */
  List<GwtLocale> getAliases();
  
  String getAsString();

  List<GwtLocale> getCompleteSearchList();
  
  /**
   * Return a list of locales to search for, in order of preference.  The
   * current locale is always first on the list.  Aliases are not included
   * in the list -- use {@link #getAliases} to expand those.
   * 
   * @return inheritance list
   */
  List<GwtLocale> getInheritanceChain();
  
  String getLanguage();

  String getLanguageNotNull();

  String getRegion();
  
  String getRegionNotNull();

  String getScript();
  
  String getScriptNotNull();
  
  String getVariant();
  
  String getVariantNotNull();

  /**
   * Return true if this locale inherits from the specified locale.  Note that
   * locale.inheritsFrom(locale) is false -- if you want that to be true, you
   * should just use locale.getInheritanceChain().contains(x).
   * 
   * @param parent locale to test against
   * @return true if parent is an ancestor of this locale
   */
  boolean inheritsFrom(GwtLocale parent);

  boolean isDefault();

  String toString();

  /**
   * Checks if this locale uses the same script as another locale.
   * 
   * @param other
   * @return true if the scripts are the same
   */
  boolean usesSameScript(GwtLocale other);
}
