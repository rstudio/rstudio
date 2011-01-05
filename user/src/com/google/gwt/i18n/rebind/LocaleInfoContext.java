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

import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.SelectionProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * A LocaleUtils specific context for caching.
 */
public class LocaleInfoContext {

  /**
   * A key for lookup of computed values in a cache.
   */
  private static class CacheKey {
    private final SelectionProperty localeProperty;
    private final ConfigurationProperty runtimeLocaleProperty;
    private ConfigurationProperty queryParamProperty;
    private ConfigurationProperty cookieProperty;

    /**
     * Create a key for cache lookup.
     * 
     * @param localeProperty "locale" property, must not be null
     * @param runtimeLocaleProperty "runtime.locales" property, must not be null
     * @param cookieProperty "locale.queryparam" property, must not be null
     * @param queryParamProperty "locale.cookie" property, must not be null
     */
    public CacheKey(SelectionProperty localeProperty,
        ConfigurationProperty runtimeLocaleProperty,
        ConfigurationProperty queryParamProperty,
        ConfigurationProperty cookieProperty) {
      this.localeProperty = localeProperty;
      this.runtimeLocaleProperty = runtimeLocaleProperty;
      this.queryParamProperty = queryParamProperty;
      this.cookieProperty = cookieProperty;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      CacheKey other = (CacheKey) obj;
      return localeProperty.equals(other.localeProperty)
          && runtimeLocaleProperty.equals(other.runtimeLocaleProperty)
          && queryParamProperty.equals(other.queryParamProperty)
          && cookieProperty.equals(other.cookieProperty);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + localeProperty.hashCode();
      result = prime * result + runtimeLocaleProperty.hashCode();
      result = prime * result + queryParamProperty.hashCode();
      result = prime * result + cookieProperty.hashCode();
      return result;
    }
  }

  private final Map<CacheKey, LocaleUtils> localeUtilsCache = new HashMap<
      CacheKey, LocaleUtils>();

  public LocaleUtils getLocaleUtils(SelectionProperty localeProperty,
      ConfigurationProperty runtimeLocaleProperty,
      ConfigurationProperty queryParamProp, ConfigurationProperty cookieProp) {
    CacheKey key = new CacheKey(localeProperty, runtimeLocaleProperty,
        queryParamProp, cookieProp);
    return localeUtilsCache.get(key);
  }

  public void putLocaleUtils(SelectionProperty localeProperty,
      ConfigurationProperty runtimeLocaleProperty, ConfigurationProperty queryParamProp,
      ConfigurationProperty cookieProp, LocaleUtils localeUtils) {
    CacheKey key = new CacheKey(localeProperty, runtimeLocaleProperty,
        queryParamProp, cookieProp);
    localeUtilsCache.put(key, localeUtils);
  }
}
