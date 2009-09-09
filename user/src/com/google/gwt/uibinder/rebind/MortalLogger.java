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
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Wraps a {@link TreeLogger} with handy {@link String#format} style methods and
 * can be told to die. Perhaps we should instead add die(), warn(), etc. to
 * Treelogger.
 */
public class MortalLogger {
  private final TreeLogger logger;

  MortalLogger(TreeLogger logger) {
    this.logger = logger;
  }

  /**
   * Post an error message and halt processing. This method always throws an
   * {@link UnableToCompleteException}.
   */
  public void die(String message, Object... params)
      throws UnableToCompleteException {
    logger.log(TreeLogger.ERROR, String.format(message, params));
    throw new UnableToCompleteException();
  }

  public TreeLogger getTreeLogger() {
    return logger;
  }

  /**
   * Post a warning message.
   */
  public void warn(String message, Object... params) {
    logger.log(TreeLogger.WARN, String.format(message, params));
  }
}
