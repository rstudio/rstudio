/*
 * Copyright 2010 Google Inc.
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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Creates a type oracle from JDT compiled classes.
 */
public class TypeOracleMediatorFromSource extends TypeOracleMediator {

  /**
   * Adds new units to an existing TypeOracle.
   *
   * @param logger logger to use
   * @param units collection of compilation units to process
   */
  public void addNewUnits(TreeLogger logger, Collection<CompilationUnit> units) {
    Collection<TypeData> classDataList = new ArrayList<TypeData>();

    // Create method args data for types to add
    MethodArgNamesLookup argsLookup = new MethodArgNamesLookup();
    for (CompilationUnit unit : units) {
      argsLookup.mergeFrom(unit.getMethodArgs());
    }

    // Create list including byte code for each type to add
    for (CompilationUnit unit : units) {
      Collection<CompiledClass> compiledClasses = unit.getCompiledClasses();
      for (CompiledClass compiledClass : compiledClasses) {
        classDataList.add(compiledClass.getTypeData());
      }
    }

    // Add the new types to the type oracle build in progress.
    addNewTypes(logger, classDataList, argsLookup);
  }
}
