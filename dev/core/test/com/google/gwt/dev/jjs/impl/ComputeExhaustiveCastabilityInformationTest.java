/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JCastMap;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Tests {@code ComputeExhaustiveCastabilityInformation}.
 */
public class ComputeExhaustiveCastabilityInformationTest extends JJSTestBase {

  public void testArrayClassCastabilityIsExhaustive() throws UnableToCompleteException {
    registerCompilableResources();

    // Compiles and gets a reference to the String[] type.
    JProgram program = compileSnippet("void", "", "", false);
    ComputeExhaustiveCastabilityInformation.exec(program, false);
    JDeclaredType stringType = program.getIndexedType("String");
    JArrayType stringArrayType = program.getTypeArray(stringType);

    // Verifies that String[] casts to the exhaustive list of related array types and Object.
    assertSourceCastsToTargets(program, stringArrayType, Sets.newHashSet("java.lang.String[]",
        "java.lang.CharSequence[]", "java.lang.Comparable[]", "java.lang.Object[]",
        "java.io.Serializable[]", "java.lang.Object"));
  }

  public void testRegularClassCastabilityIsExhaustive() throws UnableToCompleteException {
    registerCompilableResources();

    // Compiles and verifies that the String type casts to the exhaustive list of related types.
    assertSourceCastsToTargets("String", Sets.newHashSet("java.lang.String",
        "java.lang.CharSequence", "java.lang.Comparable", "java.lang.Object",
        "java.io.Serializable"));
  }

  private void assertSourceCastsToTargets(JProgram program, JReferenceType sourceType,
      Set<String> expectedTargetTypeNames) {
    // Gets the castmap for the given source type.
    JCastMap castMap = program.getCastMap(sourceType);

    // Converts the castmap entries to runtime type references.
    List<JRuntimeTypeReference> runtimeTypeReferences = Lists.newArrayList(
        Iterables.filter(castMap.getCanCastToTypes(), JRuntimeTypeReference.class));

    // Makes a set of the names of the runtime type references.
    Set<String> actualTargetTypeNames = Sets.newHashSet(
        Lists.transform(runtimeTypeReferences, new Function<JRuntimeTypeReference, String>() {
            @Override
          public String apply(JRuntimeTypeReference runtimeTypeReference) {
            return runtimeTypeReference.getReferredType().getName();
          }
        }));

    assertEquals(expectedTargetTypeNames, actualTargetTypeNames);
  }

  private void assertSourceCastsToTargets(String sourceTypeName,
      Set<String> expectedTargetTypeNames) throws UnableToCompleteException {
    JProgram program = compileSnippet("void", "", "", false);
    ComputeExhaustiveCastabilityInformation.exec(program, false);
    JDeclaredType sourceType = program.getIndexedType(sourceTypeName);
    assertSourceCastsToTargets(program, sourceType, expectedTargetTypeNames);
  }

  private void registerCompilableResources() {
    sourceOracle.addOrReplace(JavaResourceBase.OBJECT);
    sourceOracle.addOrReplace(JavaResourceBase.STRING);
    sourceOracle.addOrReplace(JavaResourceBase.COMPARABLE);
    sourceOracle.addOrReplace(JavaResourceBase.CHAR_SEQUENCE);
    sourceOracle.addOrReplace(JavaResourceBase.SERIALIZABLE);
    sourceOracle.addOrReplace(JavaResourceBase.SUPPRESS_WARNINGS);
  }
}
