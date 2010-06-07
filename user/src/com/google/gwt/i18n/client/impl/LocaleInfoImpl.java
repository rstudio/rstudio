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
package com.google.gwt.i18n.client.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.constants.DateTimeConstants;
import com.google.gwt.i18n.client.constants.DateTimeConstantsImpl;
import com.google.gwt.i18n.client.constants.NumberConstants;
import com.google.gwt.i18n.client.constants.NumberConstantsImpl;

/**
 * Implementation detail of LocaleInfo -- not a public API and subject to
 * change.
 * 
 * Generated interface for locale information.  The default implementation
 * returns null, which is used if the i18n module is not imported.
 * 
 * @see com.google.gwt.i18n.client.LocaleInfo
 */
public class LocaleInfoImpl {

  /**
   * @return the runtime locale (note that this requires the i18n locale
   *     property provider's assistance)
   */
  static native String getRuntimeLocale() /*-{
    return $wnd['__gwt_Locale'];
  }-*/;

  /**
   * @return an array of available locale names
   */
  public String[] getAvailableLocaleNames() {
    return null;
  }
  
  /**
   * Create a DateTimeConstants instance appropriate for this locale.
   * 
   * Note that the caller takes care of any caching so subclasses need not
   * bother.
   * 
   * @return a DateTimeConstants instance
   */
  public DateTimeConstants getDateTimeConstants() {
    return GWT.create(DateTimeConstantsImpl.class);
  }

  /**
   * @return the current locale name, such as "default, "en_US", etc.
   */
  public String getLocaleName() {
    return null;
  }

  /**
   * Return the display name of the requested locale in its native locale, if
   * possible. If no native localization is available, the English name will
   * be returned, or as a last resort just the locale name will be returned.  If
   * the locale name is unknown (including user overrides), null is returned.
   * 
   * @param localeName the name of the locale to lookup.
   * @return the name of the locale in its native locale
   */
  public String getLocaleNativeDisplayName(String localeName) {
    return null;
  }
  
  /**
   * @return a NumberConstants instance appropriate for this locale.
   */
  public NumberConstants getNumberConstants() {
    return GWT.create(NumberConstantsImpl.class);
  }
  
  /**
   * @return true if any locale supported by this build of the app is RTL.
   */
  public boolean hasAnyRTL() {
    return false;
  }
}
