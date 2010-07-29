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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.junit.JUnitShell.Strategy;

/**
 * Strategy that compiles only one module at a time. Optimizes memory usage.
 */
class SimpleCompileStrategy extends CompileStrategy {
  public SimpleCompileStrategy(JUnitShell junitShell) {
    super(junitShell);
  }

  @Override
  public ModuleDef maybeCompileModule(String moduleName,
      String syntheticModuleName, Strategy strategy,
      BatchingStrategy batchingStrategy, TreeLogger treeLogger)
      throws UnableToCompleteException {
    return maybeCompileModuleImpl(moduleName, syntheticModuleName, strategy,
        batchingStrategy, treeLogger);
  }
}
