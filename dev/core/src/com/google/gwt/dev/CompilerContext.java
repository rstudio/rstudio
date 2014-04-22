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

import com.google.gwt.dev.cfg.CombinedResourceOracle;
import com.google.gwt.dev.cfg.ImmutableLibraryGroup;
import com.google.gwt.dev.cfg.LibraryGroup;
import com.google.gwt.dev.cfg.LibraryGroupBuildResourceOracle;
import com.google.gwt.dev.cfg.LibraryGroupPublicResourceOracle;
import com.google.gwt.dev.cfg.LibraryWriter;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.NullLibraryWriter;
import com.google.gwt.dev.javac.MemoryUnitCache;
import com.google.gwt.dev.javac.UnitCache;
import com.google.gwt.dev.resource.ResourceOracle;
import com.google.gwt.dev.util.TinyCompileSummary;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

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
    private boolean compileMonolithic = true;
    private LibraryGroup libraryGroup = new ImmutableLibraryGroup();
    private LibraryWriter libraryWriter = new NullLibraryWriter();
    private ModuleDef module;
    private PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();
    private ResourceOracle publicResourceOracle;
    private ResourceOracle sourceResourceOracle;
    private UnitCache unitCache = new MemoryUnitCache();

    public CompilerContext build() {
      initializeResourceOracles();

      CompilerContext compilerContext = new CompilerContext();
      compilerContext.buildResourceOracle = buildResourceOracle;
      compilerContext.libraryWriter = libraryWriter;
      compilerContext.libraryGroup = libraryGroup;
      compilerContext.module = module;
      compilerContext.compileMonolithic = compileMonolithic;
      compilerContext.options = options;
      compilerContext.publicResourceOracle = publicResourceOracle;
      compilerContext.sourceResourceOracle = sourceResourceOracle;
      compilerContext.unitCache = unitCache;
      return compilerContext;
    }

    /**
     * Sets whether compilation should proceed monolithically or separately.
     */
    public Builder compileMonolithic(boolean compileMonolithic) {
      this.compileMonolithic = compileMonolithic;
      return this;
    }

    /**
     * Sets the libraryGroup and uses it to set resource oracles as well.
     */
    public Builder libraryGroup(LibraryGroup libraryGroup) {
      this.libraryGroup = libraryGroup;
      return this;
    }

    public Builder libraryWriter(LibraryWriter libraryWriter) {
      this.libraryWriter = libraryWriter;
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

    /**
     * Initialize source, build, and public resource oracles using the most complete currently
     * available combination of moduleDef and libraryGroup.<br />
     *
     * When executing as part of a monolithic compilation there will likely only be a moduleDef
     * available. That will result in sourcing resource oracles only from it, which is what
     * monolithic compilation expects.<br />
     *
     * When executing as part of a separate compilation there will likely be both a moduleDef and
     * libraryGroup available. That will result in sourcing resource oracles from a mixed
     * combination, which is what separate compilation expects.
     */
    private void initializeResourceOracles() {
      if (libraryGroup != null) {
        if (module != null) {
          sourceResourceOracle = module.getSourceResourceOracle();
          buildResourceOracle = new CombinedResourceOracle(
              module.getBuildResourceOracle(), new LibraryGroupBuildResourceOracle(libraryGroup));
          publicResourceOracle = new CombinedResourceOracle(
              module.getPublicResourceOracle(), new LibraryGroupPublicResourceOracle(libraryGroup));
        } else {
          sourceResourceOracle = null;
          buildResourceOracle = new LibraryGroupBuildResourceOracle(libraryGroup);
          publicResourceOracle = new LibraryGroupPublicResourceOracle(libraryGroup);
        }
      } else {
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
  }

  private ResourceOracle buildResourceOracle;
  /**
   * Whether compilation should proceed monolithically or separately. It is an example of a
   * configuration property that is not assignable by command line args. If more of these accumulate
   * they should be grouped together instead of floating free here.
   */
  private boolean compileMonolithic = true;
  private LibraryGroup libraryGroup = new ImmutableLibraryGroup();
  private LibraryWriter libraryWriter = new NullLibraryWriter();
  private ModuleDef module;
  private TinyCompileSummary tinyCompileSummary = new TinyCompileSummary();

  // TODO(stalcup): split this into module parsing, precompilation, compilation, and linking option
  // sets.
  private PrecompileTaskOptions options = new PrecompileTaskOptionsImpl();
  private ResourceOracle publicResourceOracle;
  private ResourceOracle sourceResourceOracle;
  private UnitCache unitCache = new MemoryUnitCache();

  public ResourceOracle getBuildResourceOracle() {
    return buildResourceOracle;
  }

  public LibraryGroup getLibraryGroup() {
    return libraryGroup;
  }

  public LibraryWriter getLibraryWriter() {
    return libraryWriter;
  }

  public ModuleDef getModule() {
    return module;
  }

  public PrecompileTaskOptions getOptions() {
    return options;
  }

  /**
   * Returns the set of source names of rebound types that have been processed by the given
   * Generator.
   */
  public Set<String> getProcessedReboundTypeSourceNames(String generatorName) {
    Set<String> processedReboundTypeSourceNames = Sets.newHashSet();
    processedReboundTypeSourceNames.addAll(
        getLibraryWriter().getProcessedReboundTypeSourceNames(generatorName));
    processedReboundTypeSourceNames.addAll(
        getLibraryGroup().getProcessedReboundTypeSourceNames(generatorName));
    return processedReboundTypeSourceNames;
  }

  public ResourceOracle getPublicResourceOracle() {
    return publicResourceOracle;
  }

  /**
   * Returns the set of source names of types for which GWT.create() rebind has been requested. The
   * types may or may not yet have been processed by some Generators.
   */
  public Set<String> getReboundTypeSourceNames() {
    Set<String> reboundTypeSourceNames = Sets.newHashSet();
    reboundTypeSourceNames.addAll(getLibraryWriter().getReboundTypeSourceNames());
    reboundTypeSourceNames.addAll(getLibraryGroup().getReboundTypeSourceNames());
    return reboundTypeSourceNames;
  }

  public ResourceOracle getSourceResourceOracle() {
    return sourceResourceOracle;
  }

  public UnitCache getUnitCache() {
    return unitCache;
  }

  public boolean shouldCompileMonolithic() {
    return compileMonolithic;
  }

  public TinyCompileSummary getTinyCompileSummary() {
    return tinyCompileSummary;
  }
}
