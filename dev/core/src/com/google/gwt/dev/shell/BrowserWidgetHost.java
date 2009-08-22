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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Interface that unifies access to the <code>BrowserWidget</code>,
 * <code>ModuleSpaceHost</code>, and the compiler.
 */
public interface BrowserWidgetHost {
  /**
   * Perform a web-mode compile on the user-specified set of modules. Used in
   * non-legacy mode.
   * 
   * @throws UnableToCompleteException
   */
  void compile() throws UnableToCompleteException;

  /**
   * Compile the specified set of modules, used in legacy mode.
   * 
   * @param modules the names of the modules to compile
   * @throws UnableToCompleteException
   * @deprecated Will be removed when legacy shell mode is removed
   */
  @Deprecated
  void compile(String[] modules) throws UnableToCompleteException;

  /**
   * For SWT.
   */
  ModuleSpaceHost createModuleSpaceHost(TreeLogger logger,
      BrowserWidget widget, String moduleName) throws UnableToCompleteException;

  /**
   * For OOPHM.
   * 
   * @param logger
   * @param moduleName
   * @param userAgent
   * @param url URL of top-level window, may be null for old browser plugins
   * @param tabKey opaque key for the tab, may be empty string if the plugin
   *     can't distinguish tabs or null if using an old browser plugin 
   * @param sessionKey unique session key, may be null for old browser plugins
   * @param remoteEndpoint
   * 
   * TODO(jat): change remoteEndpoint to be a BrowserChannelServer instance
   *    when we remove the SWT implementation
   */
  ModuleSpaceHost createModuleSpaceHost(TreeLogger logger, String moduleName,
      String userAgent, String url, String tabKey, String sessionKey,
      String remoteEndpoint) throws UnableToCompleteException;

  TreeLogger getLogger();

  /**
   * Called from a selection script as it begins to load in hosted mode. This
   * triggers a hosted mode link, which might actually update the running
   * selection script.
   * 
   * @param moduleName the module to link
   * @return <code>true</code> if the selection script was overwritten; this
   *         will trigger a full-page refresh by the calling (out of date)
   *         selection script
   */
  boolean initModule(String moduleName);

  /**
   * Returns <code>true</code> if running in legacy mode.
   * 
   * @deprecated Will be removed when legacy shell mode is removed
   */
  @Deprecated
  boolean isLegacyMode();

  String normalizeURL(String whatTheUserTyped);

  /**
   * For SWT.
   */
  BrowserWidget openNewBrowserWindow() throws UnableToCompleteException;

  /**
   * For OOPHM.
   */
  void unloadModule(ModuleSpaceHost moduleSpaceHost);
}
