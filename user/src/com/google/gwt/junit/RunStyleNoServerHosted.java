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
package com.google.gwt.junit;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.shell.BrowserWidget;

/**
 * <p>
 * This run style simulates -noserver hosted mode. It is the same as hosted mode
 * except for two differences:
 * </p>
 * 
 * <ol>
 * <li>The program is compiled for web mode.
 * <li>The embedded server does not do any GWT-specific resource generation.
 * </ol>
 * 
 * <p>
 * In effect, the built-in web server gets used as a dumb web server to serve up
 * the compiled files.
 * </p>
 */
public class RunStyleNoServerHosted extends RunStyleLocalHosted {
  RunStyleNoServerHosted(JUnitShell shell) {
    super(shell);
  }

  @Override
  public void maybeCompileModule(String moduleName)
      throws UnableToCompleteException {
    BrowserWidget browserWindow = getBrowserWindow();
    shell.compileForWebMode(moduleName, browserWindow.getUserAgent());
  }

  @Override
  public boolean shouldAutoGenerateResources() {
    // pretend to be a web server that knows nothing about GWT
    return false;
  }
}
