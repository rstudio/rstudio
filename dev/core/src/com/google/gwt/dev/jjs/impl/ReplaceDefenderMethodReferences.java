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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JThisRef;

/**
 * Java8 defender methods are implemented by creating a forwarding method on each class that
 * inherits the implementation. For a concrete type C inheriting I.m(), there will be an
 * implementation <code>C.m() { return I.super.m(); }</code>.
 *
 * References to I.super.m() are replaced by creating a static version of this method on the
 * interface, and then delegating to it instead.
 */
public class ReplaceDefenderMethodReferences extends JModVisitor {

  private final MakeCallsStatic.CreateStaticImplsVisitor staticImplCreator;
  private JProgram program;

  public static void exec(JProgram program) {
    ReplaceDefenderMethodReferences visitor =
        new ReplaceDefenderMethodReferences(program);
    visitor.accept(program);
  }

  private ReplaceDefenderMethodReferences(JProgram program) {
    this.program = program;
    this.staticImplCreator = new MakeCallsStatic.CreateStaticImplsVisitor(program);
  }

  @Override
  public void endVisit(JMethodCall x, Context ctx) {
    JMethod targetMethod = x.getTarget();
    if (targetMethod.isDefaultMethod() && x.isStaticDispatchOnly()) {
      assert x.getInstance() instanceof JThisRef;

      JMethod staticMethod = staticImplCreator.getOrCreateStaticImpl(program, targetMethod);
      // Cannot use setStaticDispatchOnly() here because interfaces don't have prototypes
      JMethodCall callStaticMethod = new JMethodCall(x.getSourceInfo(), null, staticMethod);
      // add 'this' as first parameter
      callStaticMethod.addArg(new JThisRef(x.getSourceInfo(), targetMethod.getEnclosingType()));
      callStaticMethod.addArgs(x.getArgs());
      ctx.replaceMe(callStaticMethod);
    }
  }
}
