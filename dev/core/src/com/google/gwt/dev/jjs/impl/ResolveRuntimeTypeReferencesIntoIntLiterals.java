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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JCastMap;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.HashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;
import com.google.gwt.thirdparty.guava.common.collect.Multisets;

import java.util.Map;

/**
 * Assigns and replaces JRuntimeTypeReference nodes with an int type id literal.<br />
 *
 * Ints are assigned sequentially under the assumption that all types in the application are known.
 */
public class ResolveRuntimeTypeReferencesIntoIntLiterals {
  // TODO(rluble): this pass should insert the defineClass in Java.

  /**
   * Collects all types that need an Id at runtime.
   */
  private class RuntimeTypeCollectorVisitor extends JVisitor {

    private final Multiset<JReferenceType> typesRequiringRuntimeIds = HashMultiset.create();

    @Override
    public void endVisit(JRuntimeTypeReference x, Context ctx) {
      typesRequiringRuntimeIds.add(x.getReferredType());
    }

    public void endVisit(JReferenceType x, Context ctx) {
      // All reference types retained will need an id.
      typesRequiringRuntimeIds.add(x);
    }
  }

  /**
   * Replaces JRuntimeTypeReference nodes with the corresponding JLiteral.
   */
  private class ReplaceRuntimeTypeReferencesVisitor extends JModVisitor {
    @Override
    public void endVisit(JRuntimeTypeReference x, Context ctx) {
      ctx.replaceMe(typeIdLiteralsByType.get(x.getReferredType()));
    }
  }

  public static Map<JType, JLiteral>  exec(JProgram program) {
    return new ResolveRuntimeTypeReferencesIntoIntLiterals(program).execImpl();
  }

  private final JProgram program;

  private final Map<JType, JLiteral> typeIdLiteralsByType = Maps.newIdentityHashMap();

  private int nextAvailableId =  0;

  private ResolveRuntimeTypeReferencesIntoIntLiterals(JProgram program) {
    this.program = program;
  }

  private void assignNextId(JType type) {
    if (typeIdLiteralsByType.containsKey(type)) {
      return;
    }
    int id = nextAvailableId++;
    assert (id != 0 || type == program.getJavaScriptObject());
    assert (id != 1 || type == program.getTypeJavaLangObject());
    assert (id != 2 || type == program.getTypeJavaLangString());

    typeIdLiteralsByType.put(type, JIntLiteral.get(id));
  }

  private Map<JType, JLiteral> execImpl() {
    RuntimeTypeCollectorVisitor visitor = new RuntimeTypeCollectorVisitor();
    visitor.accept(program);

    // TODO(rluble): remove the need for special ids
    // JavaScriptObject should get 0
    assignNextId(program.getJavaScriptObject());
    // java.lang.Object should get 1
    assignNextId(program.getTypeJavaLangObject());
    // java.lang.String should get 2
    assignNextId(program.getTypeJavaLangString());

    ImmutableMultiset<JReferenceType> typesByFrequency =
        Multisets.copyHighestCountFirst(visitor.typesRequiringRuntimeIds);
    for (JType type : typesByFrequency) {
      assignNextId(type);
    }

    ReplaceRuntimeTypeReferencesVisitor replaceTypeIdsVisitor = new ReplaceRuntimeTypeReferencesVisitor();
    replaceTypeIdsVisitor.accept(program);
    // Fix stored cast maps.
    // TODO(rluble): Improve the code so that things are not scattered all over.
    for (JCastMap castMap : program.getCastMaps()) {
      replaceTypeIdsVisitor.accept(castMap);
    }

    return typeIdLiteralsByType;
  }
}
