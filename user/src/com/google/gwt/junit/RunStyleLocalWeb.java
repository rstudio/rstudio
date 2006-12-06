/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.junit;

import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Runs locally in web mode.
 */
class RunStyleLocalWeb extends RunStyleLocalHosted {

  /**
   * This query parameter forces gwt.js to run web mode.
   */
  private static final String PROP_GWT_HYBRID_MODE = "gwt.hybrid";

  /**
   * @param shell the containing shell
   */
  RunStyleLocalWeb(JUnitShell shell) {
    super(shell);
  }

  public void maybeLaunchModule(String moduleName, boolean forceLaunch)
      throws UnableToCompleteException {
    if (forceLaunch) {
      shell.compileForWebMode(moduleName);
      launchUrl(moduleName + "/?" + PROP_GWT_HYBRID_MODE);
    }
  }

}
