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
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

/**
 * Verifies that all the references from AST nodes to AST nodes are reachable from the
 * top of the AST.
 * <p>
 * The purpose fo this pass is to verify the consistency of the AST after a specific pass has
 * run.
 */
public class JavaAstVerifier extends JVisitor {

  private Multimap<JDeclaredType, JNode> membersByType = HashMultimap.create();

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
  public void endVisit(JFieldRef x, Context ctx) {
    assertReferencedFieldIsInAst(x);
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
    assert membersByType.containsEntry(x.getTarget().getEnclosingType(), x.getTarget());
  }

  private void assertReferencedFieldIsInAst(JFieldRef x) {
    if (x.getField() == JField.NULL_FIELD) {
      return;
    }
    assert membersByType.containsEntry(x.getField().getEnclosingType(), x.getField());
  }
}
