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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.DevModeBase.HostedModeBaseOptions;
import com.google.gwt.dev.ui.DevModeUI;

/**
 * UI used in headless operation.
 */
public class HeadlessUI extends DevModeUI {

  /**
   * @param options unused
   */
  public HeadlessUI(HostedModeBaseOptions options) {
  }

  @Override
  public TreeLogger getWebServerLogger(String serverName, byte[] serverIcon) {
    return getConsoleLogger();
  }

  @Override
  public ModuleHandle getModuleLogger(String userAgent, String remoteSocket,
      String url, String tabKey, String moduleName, String sessionKey,
      String agentTag, byte[] agentIcon, Type logLevel) {
    return new ModuleHandle() {
      @Override
      public TreeLogger getLogger() {
        return getConsoleLogger();
      }

      @Override
      public void unload() {
      }
    };
  }
}
