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
  public void log(String message, Throwable e) {
    if (!consoleEnabled()) {
      return;
    }
    log(message);
    if (e != null) {
      // TODO: figure out how to log stack traces.
      log(e.toString());
    }
  }

  private native boolean consoleEnabled() /*-{
    return !!$wnd.console;
  }-*/;

  private native void log(String message) /*-{
    $wnd.console.log(message);
  }-*/;
}
