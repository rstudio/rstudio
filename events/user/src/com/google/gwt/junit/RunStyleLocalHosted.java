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
package com.google.gwt.junit;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.shell.BrowserWidget;

/**
 * Runs locally in hosted mode.
 */
class RunStyleLocalHosted extends RunStyle {

  /**
   * A browser window to host local tests.
   */
  private BrowserWidget browserWindow;

  RunStyleLocalHosted(JUnitShell shell) {
    super(shell);
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  @Override
  public void launchModule(String moduleName) throws UnableToCompleteException {
    launchUrl(getUrlSuffix(moduleName));
  }

  @Override
  public void maybeCompileModule(String moduleName)
      throws UnableToCompleteException {
    // nothing to do
  }

  protected BrowserWidget getBrowserWindow() throws UnableToCompleteException {
    if (browserWindow == null) {
      browserWindow = shell.openNewBrowserWindow();
    }
    return browserWindow;
  }

  /**
   * Launches a URL in the browser window.
   * 
   * @param url The URL to launch.
   * @throws UnableToCompleteException
   */
  protected void launchUrl(String url) throws UnableToCompleteException {
    getBrowserWindow().go(url);
  }
}
