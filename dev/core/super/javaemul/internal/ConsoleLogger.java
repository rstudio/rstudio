/*
 * Copyright 2015 Google Inc.
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
package javaemul.internal;

/**
 * A helper to print log messages to console.
 * <p> Note that, this is not a public API and can change/disappear in any release.
 */
public class ConsoleLogger {
  public static ConsoleLogger createIfSupported() {
    return isSupported() ? new ConsoleLogger() : null;
  }

  private static native boolean isSupported() /*-{
    return !!window.console;
  }-*/;

  public native void log(String level, String message) /*-{
    console[level](message);
  }-*/;

  public void log(String level, Throwable t) {
    log(level, t, "Exception: ", true);
  }

  private void log(String level, Throwable t, String label, boolean expanded) {
    groupStart(label + t.toString(), expanded);
    log(level, getBackingError(t));
    Throwable cause = t.getCause();
    if (cause != null) {
      log(level, cause, "Caused by: ", false);
    }
    for (Throwable suppressed : t.getSuppressed()) {
      log(level, suppressed, "Suppressed: ", false);
    }
    groupEnd();
  }

  private native void groupStart(String msg, boolean expanded) /*-{
    // Not all browsers support grouping:
    var groupStart = (!expanded && console.groupCollapsed) || console.group || console.log;
    groupStart.call(console, msg);
  }-*/;

  private native void groupEnd() /*-{
    var groupEnd = console.groupEnd || function(){};
    groupEnd.call(console);
  }-*/;

  private native String getBackingError(Throwable t) /*-{
    // Converts CollectorLegacy (IE8/IE9/Safari5) function stack to something readable.
    function stringify(fnStack) {
      if (!fnStack || fnStack.length == 0) {
        return "";
      }
      return "\t" + fnStack.join("\n\t");
    }
    var backingError = t.backingJsObject;
    return backingError && (backingError.stack || stringify(t["fnStack"]));
  }-*/;
}
