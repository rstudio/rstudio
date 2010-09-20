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
package com.google.gwt.safehtml.shared;

import com.google.gwt.core.client.GwtScriptOnly;

// This is the super-source peer of this class.
@GwtScriptOnly
public class SafeHtmlHostedModeUtils {

  // Unused in super-source; only defined to avoid compiler warnings
  public static final String FORCE_CHECK_COMPLETE_HTML = null;
  
  public static void maybeCheckCompleteHtml(String html) {
    // This check is a noop in web mode.
  }
  
  // Unused in super-source; only defined to avoid compiler warnings
  public static void setForceCheckCompleteHtml(boolean check) { }
  static void setForceCheckCompleteHtmlFromProperty() { }
}
