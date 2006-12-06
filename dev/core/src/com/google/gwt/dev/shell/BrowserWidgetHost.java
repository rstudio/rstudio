// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Interface that unifies access to the <code>BrowserWidget</code>, <code>ModuleSpaceHost</code>, and the compiler. 
 */
public interface BrowserWidgetHost {
  TreeLogger getLogger();

  BrowserWidget openNewBrowserWindow() throws UnableToCompleteException;

  String normalizeURL(String whatTheUserTyped);

  // Factor this out if BrowserWidget becomes decoupled from hosted mode
  ModuleSpaceHost createModuleSpaceHost(BrowserWidget widget, String moduleName)
      throws UnableToCompleteException;

  void compile(String[] modules) throws UnableToCompleteException;
}
