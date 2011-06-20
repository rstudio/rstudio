/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.safecss.shared;

import com.google.gwt.core.client.GwtScriptOnly;

// This is the super-source peer of this class.
@GwtScriptOnly
public class SafeStylesHostedModeUtils {

  // Unused in super-source; only defined to avoid compiler warnings
  public static final String FORCE_CHECK_VALID_STYLES = null;

  // Unused in super-source; only defined to avoid compiler warnings
  public static String isValidStyleName(String name) {
    return null;
  }

  // Unused in super-source; only defined to avoid compiler warnings
  public static String isValidStyleValue(String value) {
    return null;
  }

  public static void maybeCheckValidStyleName(String html) {
    // This check is a noop in web mode.
  }

  public static void maybeCheckValidStyleValue(String html) {
    // This check is a noop in web mode.
  }

  // Unused in super-source; only defined to avoid compiler warnings
  public static void setForceCheckValidStyle(boolean check) {
  }

  static void setForceCheckCompleteHtmlFromProperty() {
  }
}
