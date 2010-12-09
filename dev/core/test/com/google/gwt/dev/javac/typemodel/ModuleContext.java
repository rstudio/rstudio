/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for loading modules from the classpath.
 */
class ModuleContext {
  private static final Map<String, TypeOracle> typeOracleMap = new HashMap<String, TypeOracle>();

  private static TypeOracle getTypeOracleFor(TreeLogger logger,
      String moduleName) throws UnableToCompleteException {
    TypeOracle oracle;
    synchronized (typeOracleMap) {
      oracle = typeOracleMap.get(moduleName);
      if (oracle == null) {
        ModuleDef moduleDef = ModuleDefLoader.loadFromClassPath(logger,
            moduleName);
        oracle = moduleDef.getCompilationState(logger).getTypeOracle();
        typeOracleMap.put(moduleName, oracle);
      }
    }
    return oracle;
  }

  private final TypeOracle oracle;

  ModuleContext(TreeLogger logger, String moduleName)
      throws UnableToCompleteException {
    this.oracle = getTypeOracleFor(logger, moduleName);
  }

  public TypeOracle getOracle() {
    return oracle;
  }
}