/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.shell.jetty;

import org.mortbay.log.Logger;

/**
 * A Jetty {@link Logger} that suppresses all output.
 */
public class JettyNullLogger implements Logger {

  public void debug(String msg, Throwable th) {
  }

  public void debug(String msg, Object arg0, Object arg1) {
  }

  public Logger getLogger(String name) {
    return this;
  }

  public void info(String msg, Object arg0, Object arg1) {
  }

  public boolean isDebugEnabled() {
    return false;
  }

  public void setDebugEnabled(boolean enabled) {
  }

  public void warn(String msg, Throwable th) {
  }

  public void warn(String msg, Object arg0, Object arg1) {
  }
}
