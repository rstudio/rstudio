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
package com.google.gwt.i18n.server;

import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Creates server-side GwtLocale instances.  Thread-safe.
 */
public class GwtLocaleFactoryImpl implements GwtLocaleFactory {

  private static boolean isAlpha(String str, int min, int max) {
    return matches(str, min, max, true);
  }

  private static boolean isDigit(String str, int min, int max) {
    return matches(str, min, max, false);
  }

  /**
   * Check if the supplied string matches length and composition requirements.
   * 
   * @param str string to check
   * @param min minimum length
   * @param max maximum length
   * @param lettersNotDigits true if all characters should be letters, false if
   *     all characters should be digits
   * @return true if the string is of a proper length and contains only the
   *     specified characters 
   */
  private static boolean matches(String str, int min, int max,
      boolean lettersNotDigits) {
    int len = str.length();
    if (len < min || len > max) {
      return false;
    }
    for (int i = 0; i < len; ++i) {
      if ((lettersNotDigits && !Character.isLetter(str.charAt(i)))
          || (!lettersNotDigits && !Character.isDigit(str.charAt(i)))) {
        return false;
      }
    }
    return true;
  }

  private static String titleCase(String str) {
    if (str.length() < 2) {
      return str.toUpperCase(Locale.ENGLISH);
    }
    return String.valueOf(Character.toTitleCase(str.charAt(0))) +
        str.substring(1).toLowerCase(Locale.ENGLISH);
  }

  private final Object instanceCacheLock = new Object[0];
  
  // Locales are stored pointing at themselves. A new instance is created,
  // which is pretty cheap, then looked up here. If it exists, the old
  // one is used instead to preserved cached data structures.
  private Map<GwtLocaleImpl, GwtLocaleImpl> instanceCache
      = new HashMap<GwtLocaleImpl, GwtLocaleImpl>();

  /**
   * Clear an embedded cache of instances when they are no longer needed.
   * <p>
   * Note that GwtLocale instances constructed after this is called will not
   * maintain identity with instances constructed before this call.
   */
  public void clear() {
    synchronized (instanceCacheLock) {
      instanceCache.clear();     
    }
  }

  public GwtLocale fromComponents(String language, String script,
      String region, String variant) {
    if (language != null && language.length() == 0) {
      language = null;
    }
    if (language != null) {
      language = language.toLowerCase(Locale.ENGLISH);
    }
    if (script != null && script.length() == 0) {
      script = null;
    }
    if (script != null) {
      script = titleCase(script);
    }
    if (region != null && region.length() == 0) {
      region = null;
    }
    if (region != null) {
      region = region.toUpperCase(Locale.ENGLISH);
    }
    if (variant != null && variant.length() == 0) {
      variant = null;
    }
    if (variant != null) {
      variant = variant.toUpperCase(Locale.ENGLISH);
    }
    GwtLocaleImpl locale = new GwtLocaleImpl(this, language, region, script,
        variant);
    synchronized (instanceCacheLock) {
      if (instanceCache.containsKey(locale)) {
        return instanceCache.get(locale);
      }
      instanceCache.put(locale, locale);
    }
    return locale;
  }

  /**
   * @throws IllegalArgumentException if the supplied locale does not match
   *     BCP47 structural requirements.
   */
  public GwtLocale fromString(String localeName) {
    String language = null;
    String script = null;
    String region = null;
    String variant = null;
    if (localeName != null && !GwtLocale.DEFAULT_LOCALE.equals(localeName)) {
      // split into component parts
      ArrayList<String> localeParts = new ArrayList<String>();
      String[] parts = localeName.split("[-_]");
      for (int i = 0; i < parts.length; ++i) {
        if (parts[i].length() == 1 && i + 1 < parts.length) {
          localeParts.add(parts[i] + '-' + parts[++i]);
        } else {
          localeParts.add(parts[i]);
        }
      }
      
      // figure out the role of each part
      int partIdx = 1;
      int numParts = localeParts.size();
      if (numParts > 0) {
        // Treat an initial private-use tag as the language tag
        // Otherwise, language tags are 2-3 characters plus up to three
        // 3-letter extensions, or 4-8 characters.
        language = localeParts.get(0);
        int len = language.length();
        // TODO: verify language tag length
        // See if we have extended language tags
        if ((len == 2 || len == 3) && partIdx < numParts) {
          String part = localeParts.get(partIdx);
          while (partIdx < numParts && partIdx < 4
              && isAlpha(part, 3, 3)) {
            language += '-' + part;
            if (++partIdx >= numParts) {
              break;
            }
            part = localeParts.get(partIdx);
          }
        }
      }
      if (numParts > partIdx
          && isAlpha(localeParts.get(partIdx), 4, 4)) {
        // Scripts are exactly 4 letters
        script = localeParts.get(partIdx++);
      }
      if (partIdx < numParts) {
        // Regions may be 2 letters or 3 digits
        String part = localeParts.get(partIdx);
        if (isAlpha(part, 2, 2) || isDigit(part, 3, 3)) {
          region = part;
          ++partIdx;
        }
      }
      if (partIdx < numParts) {
        // Variants are 5-8 alphanum, or 4 alphanum if first is digit
        String part = localeParts.get(partIdx);
        int len = part.length();
        if ((len >= 5 && len <= 8) || (len == 4
            && Character.isDigit(part.charAt(0)))) {
          variant = part;
          ++partIdx;
        }
      }
      if (partIdx < numParts) {
        throw new IllegalArgumentException("Unrecognized locale format: "
            + localeName);
      }
    }
    return fromComponents(language, script, region, variant);
  }

  public GwtLocale getDefault() {
    return fromComponents(null, null, null, null);
  }
}
