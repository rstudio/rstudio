/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.typemodel.JRealClassType;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

/**
 * Lazily builds or rebuilds a {@link com.google.gwt.core.ext.typeinfo.TypeOracle} from a set of
 * compilation units and a LibraryGroup.
 */
public class LibraryCompilationUnitTypeOracleUpdater extends CompilationUnitTypeOracleUpdater {

  private CompilerContext compilerContext;
  private TypeOracleBuildContext context;

  public LibraryCompilationUnitTypeOracleUpdater(TypeOracle typeOracle,
      CompilerContext compilerContext) {
    super(typeOracle);
    this.compilerContext = compilerContext;
  }

  /**
   * Lazily returns the type corresponding to the given internal name.
   */
  @Override
  protected JRealClassType findByInternalName(String internalName) {
    if (super.findByInternalName(internalName) == null) {
      CompilationUnit compilationUnit = compilerContext.getLibraryGroup()
          .getCompilationUnitByTypeSourceName(InternalName.toSourceName(internalName));
      if (compilationUnit != null) {
        compilerContext.getUnitCache().add(compilationUnit);
        // We're already executing within the recursive call tree resulting from a addNewUnits()
        // invocation. That invocation will index all new types at the end. We avoid indexing here
        // for both performance and correctness (to avoid indexing types that have already arrived
        // in the TypeOracle.recentTypes list but which have not yet finished resolving).
        addNewTypesDontIndex(TreeLogger.NULL, Lists.newArrayList(compilationUnit));
      }
    }
    return super.findByInternalName(internalName);
  }

  @Override
  protected void finish() {
    super.finish();
    // Reset the build context after each externally invoked addNewTypes() invocation.
    context = null;
  }

  /**
   * Returns the same build context across multiple (recursive) addNewTypesDontIndex() invocations.
   */
  @Override
  protected TypeOracleBuildContext getContext(MethodArgNamesLookup argsLookup) {
    if (context == null) {
      context = new TypeOracleBuildContext(argsLookup);
    } else {
      context.allMethodArgs.mergeFrom(argsLookup);
    }
    return context;
  }
}
