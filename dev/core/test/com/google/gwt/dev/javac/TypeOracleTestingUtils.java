/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.impl.JavaResourceBase;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for tests that build a type oracle and watch for errors.
 * 
 */
public class TypeOracleTestingUtils {

  public static TypeOracle buildStandardTypeOracleWith(TreeLogger logger,
      CompilationUnit... extraUnits) {
    Set<CompilationUnit> extraUnitSet = new HashSet<CompilationUnit>();
    Collections.addAll(extraUnitSet, extraUnits);
    return buildStandardTypeOracleWith(logger, extraUnitSet);
  }

  public static TypeOracle buildStandardTypeOracleWith(TreeLogger logger,
      Set<CompilationUnit> extraUnits) {
    Set<CompilationUnit> unitSet = new HashSet<CompilationUnit>();
    addStandardCups(unitSet);
    for (CompilationUnit extraUnit : extraUnits) {
      unitSet.add(extraUnit);
    }
    return buildTypeOracle(logger, unitSet);
  }

  public static TypeOracle buildTypeOracle(TreeLogger logger,
      Set<CompilationUnit> units) {
    Set<String> validBinaryTypeNames = new HashSet<String>();
    for (CompilationUnit unit : units) {
      Set<CompiledClass> compiledClasses = unit.getCompiledClasses();
      if (compiledClasses != null) {
        for (CompiledClass compiledClass : compiledClasses) {
          validBinaryTypeNames.add(compiledClass.getInternalName());
        }
      }
    }
    JdtCompiler.compile(units);
    CompilationUnitInvalidator.InvalidatorState state = new CompilationUnitInvalidator.InvalidatorState();
    CompilationUnitInvalidator.validateCompilationUnits(state, units);
    if (CompilationUnitInvalidator.invalidateUnitsWithErrors(logger, units)) {
      CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(logger, units);
    }
    TypeOracleMediator mediator = new TypeOracleMediator();
    mediator.refresh(logger, units);
    return mediator.getTypeOracle();
  }

  /**
   * Add compilation units for basic classes like Object and String.
   */
  private static void addStandardCups(Set<CompilationUnit> units) {
    for (MockJavaResource resource : JavaResourceBase.getStandardResources()) {
      units.add(new SourceFileCompilationUnit(resource));
    }
  }
}
