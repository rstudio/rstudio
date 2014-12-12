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
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JCastMap;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;
import com.google.gwt.thirdparty.guava.common.collect.Multisets;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Assigns and replaces JRuntimeTypeReference nodes with a type id literal.
 */
public class ResolveRuntimeTypeReferences {

  /**
   * Identifies a way of sorting types when generating ids.
   */
  public enum TypeOrder {
    ALPHABETICAL, FREQUENCY, NONE
  }

  /**
   * Maps a type into a type id literal.
   */
  public interface TypeMapper<T> {
    T getOrCreateTypeId(JType type);

    void copyFrom(TypeMapper<T> typeMapper);

    T get(JType type);
  }

  /**
   * Sequentially creates int type ids for types.
   */
  public static class IntTypeMapper implements Serializable, TypeMapper<Integer> {

    // NOTE: DO NOT STORE ANY AST REFERENCE. Objects of this type persist across compiles.
    private final Map<String, Integer> typeIdByTypeName = Maps.newHashMap();
    private int nextAvailableId =  0;

    @Override
    public void copyFrom(TypeMapper<Integer> that) {
      if (!(that instanceof  IntTypeMapper)) {
        throw new IllegalArgumentException("Can only copy from IntTypeMapper");
      }

      IntTypeMapper from = (IntTypeMapper) that;
      this.nextAvailableId = from.nextAvailableId;
      this.typeIdByTypeName.clear();
      this.typeIdByTypeName.putAll(from.typeIdByTypeName);
    }

    @VisibleForTesting
    public boolean hasSameContent(IntTypeMapper that) {
      return Objects.equal(this.typeIdByTypeName, that.typeIdByTypeName)
          && Objects.equal(this.nextAvailableId, that.nextAvailableId);
    }

    @Override
    public Integer get(JType type) {
      return typeIdByTypeName.get(type.getName());
    }

    @Override
    public Integer getOrCreateTypeId(JType type) {
      String typeName = type.getName();
      if (typeIdByTypeName.containsKey(typeName)) {
        return typeIdByTypeName.get(typeName);
      }

      int nextId = nextAvailableId++;
      typeIdByTypeName.put(typeName, nextId);
      return nextId;
    }
  }

  /**
   * Predictably creates String type id literals for castable and instantiable types.
   */
  public static class StringTypeMapper implements TypeMapper<String> {

    @Override
    public void copyFrom(TypeMapper<String> that) {
      if (!(that instanceof  StringTypeMapper)) {
        throw new IllegalArgumentException("Can only copy from StringTypeMapper");
      }
    }

    @Override
    public String getOrCreateTypeId(JType type) {
      return get(type);
    }

    @Override
    public String get(JType type) {
      return type.getName();
    }
  }

  /**
   * Collects all types that need an id at runtime.
   */
  // TODO(rluble): Maybe this pass should insert the defineClass in Java.
  private class RuntimeTypeCollectorVisitor extends JVisitor {

    private final Multiset<JReferenceType> typesRequiringRuntimeIds = LinkedHashMultiset.create();

    @Override
    public void endVisit(JRuntimeTypeReference x, Context ctx) {
      // Collects types in cast maps.
      typesRequiringRuntimeIds.add(x.getReferredType());
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // Calls through super and X.this need a runtime type id.
      if (!x.isStaticDispatchOnly() || x.getTarget().isStatic()) {
        return;
      }
      typesRequiringRuntimeIds.add(x.getTarget().getEnclosingType());
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
      ctx.replaceMe(getTypeIdLiteral(x.getReferredType()));
    }
  }

  private final JProgram program;

  private TypeMapper<?> typeMapper;

  private TypeOrder typeOrder;

  private ResolveRuntimeTypeReferences(JProgram program, TypeMapper<?> typeMapper,
      TypeOrder typeOrder) {
    this.program = program;
    this.typeMapper = typeMapper;
    this.typeOrder = typeOrder;
  }

  private void assignTypes(Multiset<JReferenceType> typesWithReferenceCounts) {
    // TODO(rluble): remove the need for special ids
    typeMapper.getOrCreateTypeId(program.getJavaScriptObject());
    typeMapper.getOrCreateTypeId(program.getTypeJavaLangObject());
    typeMapper.getOrCreateTypeId(program.getTypeJavaLangString());

    Iterable<JReferenceType> types = null;
    switch (typeOrder) {
      case FREQUENCY:
        types = Multisets.copyHighestCountFirst(typesWithReferenceCounts).elementSet();
        break;
      case ALPHABETICAL:
        types = Lists.newArrayList(typesWithReferenceCounts.elementSet());
        Collections.sort((List<JReferenceType>) types, HasName.BY_NAME_COMPARATOR);
        break;
      case NONE:
        types = typesWithReferenceCounts.elementSet();
        break;
    }

    for (JType type :types) {
      typeMapper.getOrCreateTypeId(type);
    }
  }

  private void execImpl() {
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
  }

  private JLiteral getTypeIdLiteral(JType type) {
    return program.getLiteral(typeMapper.getOrCreateTypeId(type));
  }

  public static void exec(JProgram program, TypeMapper<?> typeMapper, TypeOrder typeOrder) {
    new ResolveRuntimeTypeReferences(program, typeMapper, typeOrder).execImpl();
  }

}
