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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JCastMap;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.Map;

/**
 * Builds exhaustive cast maps for class types defined in and array types used in this library.
 */
public class ComputeExhaustiveCastabilityInformation {

  public static void exec(JProgram program) {
    new ComputeExhaustiveCastabilityInformation(program).execImpl();
  }

  private final Map<JReferenceType, JCastMap> castMaps = Maps.newIdentityHashMap();
  private final JProgram program;

  private ComputeExhaustiveCastabilityInformation(JProgram program) {
    this.program = program;
  }

  private void createCompleteCastMap(JReferenceType sourceType) {
    castMaps.put(sourceType, new JCastMap(SourceOrigin.UNKNOWN, program.getTypeJavaLangObject(),
        program.typeOracle.getCastableDestinationTypes(sourceType)));
  }

  private void execImpl() {
    // Builds cast maps for all types declared in this library.
    for (JDeclaredType declaredType : program.getModuleDeclaredTypes()) {
      createCompleteCastMap(declaredType);
    }

    // Builds cast maps for all array types used in this library.
    for (JArrayType arrayType : program.getAllArrayTypes()) {
      if (program.typeOracle.isInstantiatedType(arrayType)) {
        createCompleteCastMap(arrayType);
      }
    }

    // Saves the constructed cast maps.
    program.initTypeInfo(castMaps);
  }
}
