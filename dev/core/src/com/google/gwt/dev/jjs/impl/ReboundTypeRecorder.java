/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Records all rebinds in an AST that has not been unified.
 */
public class ReboundTypeRecorder {

  private class ReboundTypeVisitor extends JVisitor {
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      String methodSignature = method.getEnclosingType().getName() + '.' + method.getSignature();
      if (GWT_CREATE_METHOD_SIGNATURES.contains(methodSignature)) {
        JExpression classLiteralArgument = x.getArgs().get(0);
        if (!(classLiteralArgument instanceof JClassLiteral)) {
          throw new InternalCompilerException(
              "Only class literals may be used as arguments to GWT.create()");
        }
        JClassLiteral classLiteral = (JClassLiteral) classLiteralArgument;

        JType requestType = classLiteral.getRefType();
        if (!(requestType instanceof JDeclaredType)) {
          throw new InternalCompilerException(
              "Only classes and interfaces may be used as arguments to GWT.create()");
        }

        reboundTypes.add((JDeclaredType) requestType);
      }
    }
  }

  private static final Set<String> GWT_CREATE_METHOD_SIGNATURES =
      Sets.newHashSet(UnifyAst.GWT_CREATE, UnifyAst.OLD_GWT_CREATE);

  /**
   * Walks the AST from the provided node and records seen rebound types into the provided
   * reboundTypes set.
   */
  public static void exec(JNode node, Set<JDeclaredType> reboundTypes) {
    Event recordRebindsEvent = SpeedTracerLogger.start(CompilerEventType.RECORD_REBINDS);
    new ReboundTypeRecorder(node, reboundTypes).execImpl();
    recordRebindsEvent.end();
  }

  private final JNode node;
  private final Set<JDeclaredType> reboundTypes;

  private ReboundTypeRecorder(JNode node, Set<JDeclaredType> reboundTypes) {
    this.node = node;
    this.reboundTypes = reboundTypes;
  }

  private void execImpl() {
    new ReboundTypeVisitor().accept(node);
  }
}
