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
package com.google.gwt.safehtml.shared;

import com.google.gwt.core.client.GwtScriptOnly;

/**
 * Supersource (webonly) version of SafeUrilHostedModeUtils.
 */
@GwtScriptOnly
public class SafeUriHostedModeUtils {

  // Unused in super-source; only defined to avoid compiler warnings
  public static final String FORCE_CHECK_VALID_URI = null;
  static final String HREF_DISCRETE_UCSCHAR = null;

  public static void maybeCheckValidUri(String uri) {
    // This check is a noop in web mode.
  }

  // Unused in super-source; only defined to avoid compiler warnings
  public static boolean isValidUriCharset(String uri) { return true; }
  public static void setForceCheckValidUri(boolean check) { }
  public static void setForceCheckValidUriFromProperty() { }
}
