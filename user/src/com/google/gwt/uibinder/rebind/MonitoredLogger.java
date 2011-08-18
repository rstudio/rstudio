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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.TreeLogger;

/**
 * Handy for logging a bunch of errors and then dying later. Keeps a hasErrors
 * flag that is set if any errors are logged.
 */
public class MonitoredLogger {
  private boolean hasErrors = false;
  private final MortalLogger logger;

  public MonitoredLogger(MortalLogger mortalLogger) {
    this.logger = mortalLogger;
  }

  /**
   * Post an error. Sets the {@link #hasErrors} flag
   */
  public void error(String message, Object... params) {
    hasErrors = true;
    logger.getTreeLogger().log(TreeLogger.ERROR, String.format(message, params));
  }
  
  public void error(XMLElement context, String message, Object... params) {
    hasErrors = true;
    logger.logLocation(TreeLogger.ERROR, context, String.format(message, params));
  }

  /**
   * Returns true if {@link #error} has ever been called.
   */
  public boolean hasErrors() {
    return hasErrors;
  }
}
