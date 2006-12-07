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
import com.google.gwt.dev.shell.BrowserWidget;

/**
 * Runs locally in hosted mode.
 */
class RunStyleLocalHosted extends RunStyle {

  /**
   * The containing shell.
   */
  protected final JUnitShell shell;

  /**
   * A browser window to host local tests.
   */
  private BrowserWidget browserWindow;

  /**
   * @param shell the containing shell
   */
  RunStyleLocalHosted(JUnitShell shell) {
    this.shell = shell;
  }

  public void maybeLaunchModule(String moduleName, boolean forceLaunch)
      throws UnableToCompleteException {
    if (forceLaunch) {
      launchUrl(moduleName + "/");
    }
  }

  /**
   * Launches a URL in the browser window.
   * 
   * @param url The URL to launch.
   * @throws UnableToCompleteException
   */
  protected void launchUrl(String url) throws UnableToCompleteException {
    if (browserWindow == null) {
      browserWindow = shell.openNewBrowserWindow();
    }
    browserWindow.go(url);
  }
}
