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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Provides access to the statistics collector function as an intrinsic for use
 * by the compiler. The typical use case is:
 * 
 * <pre>
 *   isStatsAvailable() && stats()
 * </pre>
 */
final class Stats {

  static native String getModuleName() /*-{
    return $moduleName;
  }-*/;

  static native boolean isStatsAvailable() /*-{
    return !!$stats;
  }-*/;

  static native JavaScriptObject makeTimeStat() /*-{
    return {millis : (new Date()).getTime()};
  }-*/;

  static native boolean stats(String moduleName, String system, String event,
      JavaScriptObject data) /*-{
    return $stats(moduleName, system, event, data);
  }-*/;
}
