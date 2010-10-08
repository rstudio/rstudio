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

import com.google.gwt.i18n.client.Localizable;

/**
 * Implementation detail of LocaleInfo -- not a public API and subject to
 * change.
 *
 * Locale data from CLDR.
 *
 * Subclasses of this are currently hand-written, but will eventually be
 * generated directly from the CLDR data and make available most of the
 * information present in CLDR.
 */
public class CldrImpl implements Localizable {
  /*
   * This class is separate from LocaleInfoImpl because it will be generated
   * from CLDR data rather than at compile time by a generator.
   */

  /**
   * Returns true if the current locale is right-to-left rather than
   * left-to-right.
   *
   * Most languages are left-to-right, so the default is false.
   */
  public boolean isRTL() {
    return false;
  }
}
