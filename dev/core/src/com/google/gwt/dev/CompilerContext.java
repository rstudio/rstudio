/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev;

import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.javac.CompilationErrorsIndex;
import com.google.gwt.dev.javac.CompilationErrorsIndexImpl;
import com.google.gwt.dev.javac.MemoryUnitCache;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.resource.ResourceOracle;

/**
 * Contains most global read-only compiler state and makes it easily accessible to the far flung
 * reaches of the compiler call graph without the constant accumulation of more and more function
 * parameters.
 */
public class CompilerContext {

  /**
   * CompilerContext builder.
   */
  public static class Builder {

    private ResourceOracle buildResourceOracle;
    private CompilationErrorsIndex compilationErrorsIndex;
    private MinimalRebuildCache minimalRebuildCache = new MinimalRebuildCache();
    private ModuleDef module;
    private PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();
    private ResourceOracle publicResourceOracle;
    private ResourceOracle sourceResourceOracle;
    private UnitCache unitCache = new MemoryUnitCache();

    public CompilerContext build() {
      if (options != null && module != null) {
        module.getProperties().setProperties(options.getProperties());
      }
      initializeResourceOracles();
      initializeCompilationErrorIndexes();

      CompilerContext compilerContext = new CompilerContext();
      compilerContext.buildResourceOracle = buildResourceOracle;
      compilerContext.minimalRebuildCache = minimalRebuildCache;
      compilerContext.module = module;
      compilerContext.options = options;
      compilerContext.publicResourceOracle = publicResourceOracle;
      compilerContext.sourceResourceOracle = sourceResourceOracle;
      compilerContext.compilationErrorsIndex = compilationErrorsIndex;
      compilerContext.unitCache = unitCache;
      return compilerContext;
    }

    public Builder minimalRebuildCache(MinimalRebuildCache minimalRebuildCache) {
      assert minimalRebuildCache != null;
      this.minimalRebuildCache = minimalRebuildCache;
      return this;
    }

    /**
     * Sets the module and uses it to set resource oracles as well.
     */
    public Builder module(ModuleDef module) {
      this.module = module;
      return this;
    }

    public Builder options(PrecompileTaskOptions options) {
      this.options = options;
      return this;
    }

    public Builder unitCache(UnitCache unitCache) {
      this.unitCache = unitCache;
      return this;
    }

    private void initializeCompilationErrorIndexes() {
      compilationErrorsIndex = new CompilationErrorsIndexImpl();
    }

    /**
     * Initialize source, build, and public resource oracles using the most complete currently
     * available moduleDef.<br />
     *
     * There will likely only be a moduleDef available. That will result in sourcing resource
     * oracles only from it, which is what monolithic compilation expects.
     */
    private void initializeResourceOracles() {
      if (module != null) {
        sourceResourceOracle = module.getSourceResourceOracle();
        buildResourceOracle = module.getBuildResourceOracle();
        publicResourceOracle = module.getPublicResourceOracle();
      } else {
        sourceResourceOracle = null;
        buildResourceOracle = null;
        publicResourceOracle = null;
      }
    }
  }

  private ResourceOracle buildResourceOracle;
  private CompilationErrorsIndex compilationErrorsIndex = new CompilationErrorsIndexImpl();
  private MinimalRebuildCache minimalRebuildCache = new MinimalRebuildCache();
  private ModuleDef module;
  // TODO(stalcup): split this into module parsing, precompilation, compilation, and linking option
  // sets.
  private PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();

  private ResourceOracle publicResourceOracle;
  private ResourceOracle sourceResourceOracle;
  private UnitCache unitCache = new MemoryUnitCache();
  public ResourceOracle getBuildResourceOracle() {
    return buildResourceOracle;
  }

  /**
   * Returns the mutable index of compilation errors for the current compile.
   */
  public CompilationErrorsIndex getCompilationErrorsIndex() {
    return compilationErrorsIndex;
  }

  public MinimalRebuildCache getMinimalRebuildCache() {
    return minimalRebuildCache;
  }

  public ModuleDef getModule() {
    return module;
  }

  public PrecompileTaskOptions getOptions() {
    return options;
  }

  public ResourceOracle getPublicResourceOracle() {
    return publicResourceOracle;
  }

  public ResourceOracle getSourceResourceOracle() {
    return sourceResourceOracle;
  }

  public UnitCache getUnitCache() {
    return unitCache;
  }
}
