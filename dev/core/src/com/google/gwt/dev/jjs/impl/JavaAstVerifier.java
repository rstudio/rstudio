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
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Verifies that all the references from AST nodes to AST nodes are reachable from the
 * top of the AST.
 * <p>
 * The purpose fo this pass is to verify the consistency of the AST after a specific pass has
 * run.
 */
public class JavaAstVerifier extends JVisitor {

  private Multimap<JDeclaredType, JNode> membersByType = HashMultimap.create();
  private Set<String> seenTypeNames = Sets.newHashSet();
  private Multimap<JDeclaredType, String> seenMethodsByType = HashMultimap.create();
  private Multimap<JDeclaredType, String> seenFieldsByType = HashMultimap.create();

  JavaAstVerifier(JProgram program) {
    for (JDeclaredType type :program.getModuleDeclaredTypes()) {
      membersByType.putAll(type, type.getMethods());
      membersByType.putAll(type, type.getFields());
    }
  }

  /**
   * Throws an assertion error if the AST for a program is not consistent.
   */
  public static void assertProgramIsConsistent(JProgram program) {
    if (JavaAstVerifier.class.desiredAssertionStatus()) {
      new JavaAstVerifier(program).accept(program);
    }
  }

  @Override
  public void endVisit(JClassType x, Context ctx) {
    assertNotSeenBefore(x);
    assertJsoCorrectness(x);
  }

  @Override
  public void endVisit(JField x, Context ctx) {
    JDeclaredType enclosingType = x.getEnclosingType();
    String fieldName = x.getName();
    assert !seenFieldsByType.containsEntry(enclosingType, fieldName) :
        "Field " + x + " is duplicated.";
    seenFieldsByType.put(enclosingType, fieldName);
  }

  @Override
  public void endVisit(JFieldRef x, Context ctx) {
    assertReferencedFieldIsInAst(x);
  }

  @Override
  public void endVisit(JInterfaceType x, Context ctx) {
    assertNotSeenBefore(x);
  }

  @Override
  public void endVisit(JMethod x, Context ctx) {
    JDeclaredType enclosingType = x.getEnclosingType();
    String methodSignature = x.getSignature();
    assert !seenMethodsByType.containsEntry(enclosingType, methodSignature) :
        "Method " + x + " is duplicated.";
    seenMethodsByType.put(enclosingType, methodSignature);
  }

  @Override
  public void endVisit(JMethodCall x, Context ctx) {
    assertCalledMethodIsInAst(x);
  }

  @Override
  public void endVisit(JsniFieldRef x, Context ctx) {
    assertReferencedFieldIsInAst(x);
  }

  @Override
  public void endVisit(JsniMethodRef x, Context ctx) {
    assertCalledMethodIsInAst(x);
  }

  private void assertCalledMethodIsInAst(JMethodCall x) {
    if (x.getTarget() == JMethod.NULL_METHOD) {
      return;
    }
    assert membersByType.containsEntry(x.getTarget().getEnclosingType(), x.getTarget()) :
      "Method " + x.getTarget() + " is called but is not part of the AST";
  }

  private void assertReferencedFieldIsInAst(JFieldRef x) {
    if (x.getField() == JField.NULL_FIELD) {
      return;
    }
    assert membersByType.containsEntry(x.getField().getEnclosingType(), x.getField()) :
        "Field " + x.getTarget() + " is referenced but is not part of the AST";
  }

  private void assertJsoCorrectness(JClassType x) {
    boolean isJSOorSubclassOfJSO = false;
    for (JClassType current = x; current != null; current = current.getSuperClass()) {
      if (current.getName().equals(JProgram.JAVASCRIPTOBJECT)) {
        isJSOorSubclassOfJSO = true;
        break;
      }
    }
    assert isJSOorSubclassOfJSO == x.isJsoType() : x.isJsoType() ?
        "Type " + x.getName() + " is considered a Jso but is not subclass of " +
            JProgram.JAVASCRIPTOBJECT :
        "Type " + x.getName() + " is subclass of " + JProgram.JAVASCRIPTOBJECT + " but is not " +
            "considered a Jso";
  }

  private void assertNotSeenBefore(JDeclaredType type) {
    assert !seenTypeNames.contains(type.getName()) :
        "Found two types with same name " + type.getName();
    seenTypeNames.add(type.getName());
  }
}
