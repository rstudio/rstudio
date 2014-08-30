/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.Iterator;
import java.util.Map;

/**
 * An in-memory directory of all the modules available on this code server. The {@link WebServer}
 * uses this directory to find the {@link ModuleState} associated with a URL and to list all the
 * modules on the front page.
 */
class Modules implements Iterable<String> {
  private final Options options;
  private final Map<String, ModuleState> moduleStateMap = Maps.newHashMap();

  public Modules(Options options) {
    this.options = options;
  }

  /**
   * Adds a {@link ModuleState} to the map.
   * @param state the module state to map
   */
  public void addModuleState(ModuleState state) {
    moduleStateMap.put(state.getModuleName(), state);
  }

  /**
   * Retrieves a {@link ModuleState} corresponding to a given module name.
   * This should be the module name after renaming.
   */
  public ModuleState get(String moduleName) {
    // TODO: maybe this lookup should also succeed if passed the module name before renaming?
    // (I believe currently everything breaks if you change how a module is renamed, requiring
    // a restart.)
    return moduleStateMap.get(moduleName);
  }

  /**
   * Iterates over the list of modules.
   */
  @Override
  public Iterator<String> iterator() {
    return moduleStateMap.keySet().iterator();
  }

  void defaultCompileAll(boolean noPrecompile, TreeLogger logger) throws UnableToCompleteException {
    for (ModuleState m: moduleStateMap.values()) {
      m.maybePrecompile(noPrecompile, logger);
    }
  }

  /**
   * Returns a configuration object containing the names of all the modules
   * and warnings to display to the user.
   */
  JsonObject getConfig() {
    JsonObject config = JsonObject.create();
    JsonArray moduleNames = new JsonArray();
    for (String module : this) {
      moduleNames.add(module);
    }
    config.put("moduleNames", moduleNames);
    config.put("warnings", options.getWarningsAsJson());
    return config;
  }
}
