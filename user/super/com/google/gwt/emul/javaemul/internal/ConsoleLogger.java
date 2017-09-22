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

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A helper to print log messages to console.
 * <p> Note that, this is not a public API and can change/disappear in any release.
 */
public class ConsoleLogger {
  public static ConsoleLogger createIfSupported() {
    return JsUtils.typeOf(getConsole()).equals("undefined") ? null : new ConsoleLogger();
  }

  public void log(String level, String message) {
    LogFn logFn = JsUtils.getProperty(getConsole(), level);
    // Older Firefox versions expect console instance as 'this'
    logFn.call(getConsole(), message);
  }

  public void log(String level, Throwable t) {
    log(level, t, "Exception: ", true);
  }

  private void log(String level, Throwable t, String label, boolean expanded) {
    groupStart(label + t.toString(), expanded);
    log(level, getBackingErrorStack(t));
    Throwable cause = t.getCause();
    if (cause != null) {
      log(level, cause, "Caused by: ", false);
    }
    for (Throwable suppressed : t.getSuppressed()) {
      log(level, suppressed, "Suppressed: ", false);
    }
    groupEnd();
  }

  private void groupStart(String msg, boolean expanded) {
    // Older Firefox versions expect console instance as 'this'
    getGroupStartFn(expanded).call(getConsole(), msg);
  }

  private LogFn getGroupStartFn(boolean expanded) {
    // Not all browsers support grouping:
    if (!expanded && Console.groupCollapsed != null) {
      return Console.groupCollapsed;
    } else if (Console.group != null) {
      return Console.group;
    } else {
      return Console.log;
    }
  }

  private void groupEnd() {
    if (Console.groupEnd != null) {
      // Older Firefox versions expect console instance as 'this'
      Console.groupEnd.call(getConsole());
    }
  }

  @SuppressWarnings("unusable-by-js")
  private static native String getBackingErrorStack(Throwable t) /*-{
    var backingError = t.@Throwable::backingJsObject;

    // Converts CollectorLegacy (IE8/IE9/Safari5) function stack to something readable.
    function stringify(fnStack) {
      if (!fnStack || fnStack.length == 0) {
        return "";
      }
      return "\t" + fnStack.join("\n\t");
    }

    return backingError && (backingError.stack || stringify(t["fnStack"]));
  }-*/;

  @JsType(isNative = true, namespace = "<window>", name = "Function")
  private interface LogFn {
    void call(Object objThis, Object... args);
  }

  @JsType(isNative = true, namespace = "<window>", name = "console")
  private static class Console {
    public static LogFn log;
    public static LogFn group;
    public static LogFn groupCollapsed;
    public static LogFn groupEnd;
  }

  // Using <window> due to https://code.google.com/archive/p/fbug/issues/2914 but probably not
  // necessary anymore.
  @JsProperty(namespace = "<window>", name = "console")
  private static native Object getConsole();
}
