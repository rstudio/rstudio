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
import com.google.gwt.dev.jdt.RebindOracle;

/**
 * Defines the contract necessary to host a module space. "Hosting a module
 * space" refers to supporting an isolated logical arena that can load a GWT
 * module in hosted mode, execute JavaScript, and so on. The primary exmaple of
 * this is the GWT shell.
 */
public interface ModuleSpaceHost extends RebindOracle {
  String[] getEntryPointTypeNames();

  CompilingClassLoader getClassLoader();

  TreeLogger getLogger();

  void onModuleReady(ModuleSpace space) throws UnableToCompleteException;
}
