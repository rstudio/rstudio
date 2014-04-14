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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.ModuleHandle;

/**
 * Interface that unifies access to the <code>BrowserWidget</code>,
 * <code>ModuleSpaceHost</code>, and the compiler.
 */
public interface BrowserWidgetHost {

  /**
   * Create a logger for a module instance.
   *
   * @param moduleName
   * @param userAgent
   * @param url URL of top-level window, may be null for old browser plugins
   * @param tabKey opaque key for the tab, may be empty string if the plugin
   *     can't distinguish tabs or null if using an old browser plugin
   * @param sessionKey unique session key, may be null for old browser plugins
   * @param serverChannel connection from the client
   * @param userAgentIcon byte array containing an icon (which fits in 24x24)
   *     for this user agent or null if unavailable
   * @return ModuleHandle instance -- caller is responsible for calling
   *     {@link ModuleHandle#unload()} on it when done
   */
  ModuleHandle createModuleLogger(String moduleName, String userAgent,
      String url, String tabKey, String sessionKey,
      BrowserChannelServer serverChannel, byte[] userAgentIcon);

  /**
   * Create a ModuleSpaceHost for the specified module.
   *
   * @param module ModuleHandle returned from a previous createModuleLogger
   *     call.
   * @param moduleName name of module to create
   * @return ModuleSpaceHost instance
   * @throws UnableToCompleteException
   */
  ModuleSpaceHost createModuleSpaceHost(ModuleHandle module, String moduleName)
      throws UnableToCompleteException;
}
