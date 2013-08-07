/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.shared.impl.JsLogger;

/**
 * The implementation of GWT.log() used when Super Dev Mode is on.
 */
public class SuperDevModeLogger extends JsLogger {

  @Override
  public void log(String message, Throwable t) {
    if (!consoleLogEnabled()) {
      return;
    }
    log(message);

    if (t != null) {
      String out = makeSimpleStackTrace(t);
      if (consoleErrorEnabled()) {
        error(out); // also prints the native stack trace leading to GWT.log().
      } else {
        log(out);
      }
    }
  }

  /**
   * Builds a simple stack trace (including chained exceptions) with just the method names.
   */
  private String makeSimpleStackTrace(Throwable first) {
    // TODO: figure out how we can log the original JavaScript exception?

    StringBuilder out = new StringBuilder();
    for (Throwable t = first; t != null; t = t.getCause()) {
      if (t == first) {
        out.append(t.toString() + "\n");
      } else {
        out.append("Caused by: " + t.toString() + "\n");
      }
      for (StackTraceElement element : t.getStackTrace()) {
        out.append("  at " + element.getMethodName() + "\n"); // only the method name is meaningful.
      }
    }
    return out.toString();
  }

  private native boolean consoleLogEnabled() /*-{
    return !!($wnd.console && $wnd.console.log);
  }-*/;

  private native boolean consoleErrorEnabled() /*-{
      return !!($wnd.console && $wnd.console.error);
  }-*/;

  private native void log(String message) /*-{
    $wnd.console.log(message);
  }-*/;

  private native void error(String message) /*-{
    $wnd.console.error(message);
  }-*/;
}
