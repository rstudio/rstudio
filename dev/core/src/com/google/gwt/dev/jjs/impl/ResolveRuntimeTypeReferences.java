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
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.HashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;
import com.google.gwt.thirdparty.guava.common.collect.Multisets;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Assigns and replaces JRuntimeTypeReference nodes with a type id literal.
 */
public abstract class ResolveRuntimeTypeReferences {

  /**
   * Sequentially creates int type ids for castable and instantiable types.
   */
  public static class IntoIntLiterals extends ResolveRuntimeTypeReferences {

    private void assignId(JType type) {
      if (typeIdLiteralsByType.containsKey(type)) {
        return;
      }
      int id = nextAvailableId++;
      assert (id != 0 || type == program.getJavaScriptObject());
      assert (id != 1 || type == program.getTypeJavaLangObject());
      assert (id != 2 || type == program.getTypeJavaLangString());

      typeIdLiteralsByType.put(type, JIntLiteral.get(id));
    }

    public static Map<JType, JLiteral> exec(JProgram program) {
      return new ResolveRuntimeTypeReferences.IntoIntLiterals(program).execImpl();
    }

    private int nextAvailableId =  0;

    private IntoIntLiterals(JProgram program) {
      super(program);
    }

    @Override
    protected void assignTypes(Multiset<JReferenceType> typesWithReferenceCounts) {
      // TODO(rluble): remove the need for special ids
      assignId(program.getJavaScriptObject());
      assignId(program.getTypeJavaLangObject());
      assignId(program.getTypeJavaLangString());

      ImmutableMultiset<JReferenceType> typesOrderedByFrequency =
          Multisets.copyHighestCountFirst(typesWithReferenceCounts);
      for (JType type : typesOrderedByFrequency.elementSet()) {
        assignId(type);
      }
    }
  }

  /**
   * Predictably creates String type ids for castable and instantiable types.
   */
  public static class IntoStringLiterals extends ResolveRuntimeTypeReferences {

    private void assignId(JType type) {
      JStringLiteral stringLiteral = program.getStringLiteral(type.getSourceInfo(), type.getName());
      typeIdLiteralsByType.put(type, stringLiteral);
    }

    public static Map<JType, JLiteral> exec(JProgram program) {
      return new ResolveRuntimeTypeReferences.IntoStringLiterals(program).execImpl();
    }

    @Override
    protected void assignTypes(Multiset<JReferenceType> typesWithReferenceCounts) {
      for (JType type : typesWithReferenceCounts.elementSet()) {
        assignId(type);
      }
    }

    private IntoStringLiterals(JProgram program) {
      super(program);
    }
  }

  /**
   * Collects all types that need an id at runtime.
   */
  // TODO(rluble): Maybe this pass should insert the defineClass in Java.
  private class RuntimeTypeCollectorVisitor extends JVisitor {

    private final Multiset<JReferenceType> typesRequiringRuntimeIds = HashMultiset.create();

    @Override
    public void endVisit(JRuntimeTypeReference x, Context ctx) {
      // Collects types in cast maps.
      typesRequiringRuntimeIds.add(x.getReferredType());
    }

    @Override
    public void endVisit(JReferenceType x, Context ctx) {
      // Collects types that need a runtime type id for defineClass().
      if (program.typeOracle.isInstantiatedType(x)) {
        typesRequiringRuntimeIds.add(x);
      }
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

  protected final JProgram program;

  protected final Map<JType, JLiteral> typeIdLiteralsByType = Maps.newIdentityHashMap();

  protected ResolveRuntimeTypeReferences(JProgram program) {
    this.program = program;
  }

  protected abstract void assignTypes(Multiset<JReferenceType> typesWithReferenceCounts);

  protected Map<JType, JLiteral> execImpl() {
    RuntimeTypeCollectorVisitor runtimeTypeCollector = new RuntimeTypeCollectorVisitor();
    // Collects runtime type references visible from types in the program that are part of the
    // current compile.
    runtimeTypeCollector.accept(program);
    // Collects runtime type references that are missed (inside of annotations) in a normal AST
    // traversal.
    runtimeTypeCollector.accept(Lists.newArrayList(program.getCastMap().values()));
    // Collects runtime type references in the ClassLiteralHolder even if the ClassLiteralHolder
    // isn't part of the current compile.
    runtimeTypeCollector.accept(program.getIndexedType("ClassLiteralHolder"));
    // TODO(stalcup): each module should have it's own ClassLiteralHolder or some agreed upon
    // location that is default accessible to all.

    assignTypes(runtimeTypeCollector.typesRequiringRuntimeIds);

    ReplaceRuntimeTypeReferencesVisitor replaceTypeIdsVisitor = new ReplaceRuntimeTypeReferencesVisitor();
    replaceTypeIdsVisitor.accept(program);
    replaceTypeIdsVisitor.accept(program.getIndexedType("ClassLiteralHolder"));
    // TODO(rluble): Improve the code so that things are not scattered all over; here cast maps
    // that appear as parameters to soon to be generated
    // {@link JavaClassHierarchySetup::defineClass()} are NOT traversed when traversing the program.
    for (Entry<JReferenceType, JCastMap> entry : program.getCastMap().entrySet()) {
      JCastMap castMap = entry.getValue();
      replaceTypeIdsVisitor.accept(castMap);
    }

    return typeIdLiteralsByType;
  }
}
