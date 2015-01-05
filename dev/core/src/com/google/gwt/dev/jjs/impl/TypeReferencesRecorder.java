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

import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;

import java.util.List;

/**
 * Records Type->Type references.
 */
public class TypeReferencesRecorder extends JVisitor {

  public static void exec(JProgram program, MinimalRebuildCache minimalRebuildCache,
      boolean onlyUpdate) {
    new TypeReferencesRecorder(minimalRebuildCache, onlyUpdate).execImpl(program);
  }

  private String fromTypeName;
  private final MinimalRebuildCache minimalRebuildCache;
  private final boolean onlyUpdate;

  public TypeReferencesRecorder(MinimalRebuildCache minimalRebuildCache, boolean onlyUpdate) {
    this.onlyUpdate = onlyUpdate;
    this.minimalRebuildCache = minimalRebuildCache;
  }

  @Override
  public void endVisit(JCastOperation x, Context ctx) {
    // Gather (Foo) casts.
    maybeRecordTypeRef(x.getCastType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JClassLiteral x, Context ctx) {
    // Gather Foo.class literal references.
    maybeRecordTypeRef(x.getRefType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JFieldRef x, Context ctx) {
    // Gather Foo.someField static references.
    processJFieldRef(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JInstanceOf x, Context ctx) {
    // Gather instanceof Foo references.
    maybeRecordTypeRef(x.getTestType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JMethod x, Context ctx) {
    // Gather return types of method definitions.
    maybeRecordTypeRef(x.getType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JMethodCall x, Context ctx) {
    // Gather Foo.doSomething() static method calls.
    processMethodCall(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JNewInstance x, Context ctx) {
    // Gather the type when new objects are created.
    JClassType enclosingType = x.getTarget().getEnclosingType();
    maybeRecordTypeRef(enclosingType);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JsniFieldRef x, Context ctx) {
    // Gather Foo.someField static references.
    processJFieldRef(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JsniMethodRef x, Context ctx) {
    // Gather Foo.doSomething() static method calls.
    // System.out.println("JsniMethodRef " + x);
    processMethodCall(x);
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JValueLiteral x, Context ctx) {
    // Gather types whose constructor function is effectively called in value literal definitions.
    maybeRecordTypeRef(x.getType());
    super.endVisit(x, ctx);
  }

  @Override
  public void endVisit(JVariable x, Context ctx) {
    // Gather declared types of local variables, class fields and method parameters.
    maybeRecordTypeRef(x.getType());
    super.endVisit(x, ctx);
  }

  @Override
  public boolean visit(JDeclaredType x, Context ctx) {
    fromTypeName = x.getName();
    if (!onlyUpdate) {
      minimalRebuildCache.removeReferencesFrom(fromTypeName);
    }

    // Gather superclass and implemented interface types.
    maybeRecordTypeRef(x.getSuperClass());
    maybeRecordTypeRefs(x.getImplements());
    return super.visit(x, ctx);
  }

  private void execImpl(JProgram program) {
    accept(program);
  }

  private void maybeRecordTypeRef(JType referencedType) {
    if (referencedType instanceof JArrayType) {
      JArrayType toArrayType = (JArrayType) referencedType;
      maybeRecordTypeRef(toArrayType.getLeafType());
    }

    if (!(referencedType instanceof JDeclaredType)) {
      return;
    }

    JDeclaredType toType = (JDeclaredType) referencedType;
    maybeRecordTypeRef(fromTypeName, toType.getName());
  }

  private void maybeRecordTypeRef(String fromTypeName, String toTypeName) {
    minimalRebuildCache.addTypeReference(fromTypeName, toTypeName);
  }

  private void maybeRecordTypeRefs(List<? extends JDeclaredType> toTypes) {
    for (JDeclaredType toType : toTypes) {
      maybeRecordTypeRef(toType);
    }
  }

  private void processJFieldRef(JFieldRef x) {
    if (x.getTarget() instanceof JField) {
      JField field = (JField) x.getTarget();
      if (field.isStatic()) {
        maybeRecordTypeRef(field.getEnclosingType());
      }
    }
  }

  private void processMethodCall(JMethodCall x) {
    if (x.getTarget().isStatic() || x.getTarget().isConstructor()) {
      maybeRecordTypeRef(x.getTarget().getEnclosingType());
    }
  }
}
