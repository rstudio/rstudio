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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompilationUnitTypeOracleUpdater;
import com.google.gwt.dev.javac.LibraryCompilationUnitTypeOracleUpdater;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

/**
 * Provides lazy type-related information about a set of types from libraries.
 */
public class LibraryTypeOracle extends TypeOracle {

  /**
   * An exception indicating that the Library flavor of TypeOracle does not support the requested
   * behavior. This runtime exception can be thrown during LibraryTypeOracle access (from
   * Generators) and is expected to be caught in the incremental Generator execution framework in
   * RuleGenerateWith. It would be an error for this exception to ever become visible beyond those
   * bounds.
   */
  public static class UnsupportedTypeOracleAccess extends RuntimeException {

    public UnsupportedTypeOracleAccess(String message) {
      super(message);
    }
  }

  private boolean allLoaded;
  private CompilerContext compilerContext;
  private CompilationUnitTypeOracleUpdater typeOracleUpdater;

  public LibraryTypeOracle(CompilerContext compilerContext) {
    this.compilerContext = compilerContext;
    this.typeOracleUpdater = new LibraryCompilationUnitTypeOracleUpdater(this, compilerContext);
  }

  @Override
  public synchronized void ensureAllLoaded() {
    if (allLoaded) {
      return;
    }
    allLoaded = true;

    for (String typeName : compilerContext.getLibraryGroup().getCompilationUnitTypeSourceNames()) {
      findType(typeName);
    }
    for (String superSourceTypeName :
        compilerContext.getLibraryGroup().getSuperSourceCompilationUnitTypeSourceNames()) {
      findType(superSourceTypeName);
    }
  }

  @Override
  public JPackage findPackage(String pkgName) {
    throw new UnsupportedTypeOracleAccess("Packages can't be lazily loaded from libraries.");
  }

  @Override
  public JClassType findType(String typeSourceName) {
    // If the type is already loaded.
    JClassType type = super.findType(typeSourceName);
    if (type != null) {
      // Then return it.
      return type;
    }

    // Otherwise load its compilation unit.
    CompilationUnit compilationUnit =
        compilerContext.getLibraryGroup().getCompilationUnitByTypeSourceName(typeSourceName);
    if (compilationUnit != null) {
      // Cache the compilation unit.
      compilerContext.getUnitCache().add(compilationUnit);
      // Transform the compilation unit into a type.
      typeOracleUpdater.addNewUnits(TreeLogger.NULL, Lists.newArrayList(compilationUnit));
      // And return it.
      return super.findType(typeSourceName);
    }

    return null;
  }

  @Override
  public JClassType findType(String pkgName, String shortName) {
    return findType(pkgName + "." + shortName);
  }

  @Override
  public JPackage getPackage(String pkgName) throws NotFoundException {
    throw new UnsupportedTypeOracleAccess("Packages can't be lazily loaded from libraries.");
  }

  @Override
  public JPackage[] getPackages() {
    throw new UnsupportedTypeOracleAccess("Packages can't be lazily loaded from libraries.");
  }

  public CompilationUnitTypeOracleUpdater getTypeOracleUpdater() {
    return typeOracleUpdater;
  }
}
