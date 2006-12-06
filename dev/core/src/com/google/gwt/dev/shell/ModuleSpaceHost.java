// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.RebindOracle;

public interface ModuleSpaceHost extends RebindOracle {
  String[] getEntryPointTypeNames();

  CompilingClassLoader getClassLoader();

  TreeLogger getLogger();

  void onModuleReady(ModuleSpace space) throws UnableToCompleteException;
}
