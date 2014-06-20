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
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Rewrite instantiations of Boolean, Double, and String to use static helper methods which return
 * unboxed versions.
 *
 */
public class RewriteConstructorCallsForUnboxedTypes extends JModVisitor {

  public static final String NATIVE_TYPE_CREATEMETHOD_PREFIX = "$create";
  private JProgram program;
  private Map<String, JMethod> jsniSig2Method = new HashMap<>();

  public RewriteConstructorCallsForUnboxedTypes(JProgram program) {
    this.program = program;
    for (JDeclaredType unboxedType : program.getRepresentedAsNativeTypes()) {
      for (JMethod method : unboxedType.getMethods()) {
        if (method.getName().startsWith(NATIVE_TYPE_CREATEMETHOD_PREFIX)) {
          jsniSig2Method.put(method.getJsniSignature(false, false), method);
        }
      }
    }
  }

  @Override
  public void endVisit(JNewInstance x, Context ctx) {
    JMethod createMethod;
    JConstructor ctor = x.getTarget();

    if (!program.isRepresentedAsNativeJsPrimitive(ctor.getEnclosingType())) {
      return;
    }

    // map BoxedType(args) -> BoxedType.$createBoxedType(args)
    createMethod = jsniSig2Method.get(NATIVE_TYPE_CREATEMETHOD_PREFIX
        + ctor.getJsniSignature(false, false));
    assert createMethod != null;

    JMethodCall createCall = new JMethodCall(x.getSourceInfo(), null, createMethod);
    createCall.addArgs(x.getArgs());
    ctx.replaceMe(createCall);
  }

  @Override
  public void endVisit(JsniMethodRef x, Context ctx) {
    if (x.getTarget().isConstructor()
        && program.isRepresentedAsNativeJsPrimitive(x.getTarget().getEnclosingType())) {
      JConstructor ctor = (JConstructor) x.getTarget();
      // map BoxedType(args) -> BoxedType.$createBoxedType(args)
      JMethod createMethod = jsniSig2Method.get(NATIVE_TYPE_CREATEMETHOD_PREFIX
          + ctor.getJsniSignature(false, false));
      assert createMethod != null;

      JsniMethodRef newJsniMethodRef = new JsniMethodRef(x.getSourceInfo(),
          x.getIdent(), createMethod, program.getJavaScriptObject());
      ctx.replaceMe(newJsniMethodRef);
    }
  }

  private static final String NAME = RewriteConstructorCallsForUnboxedTypes.class
      .getSimpleName();

  private OptimizerStats execImpl() {
    OptimizerStats stats = new OptimizerStats(NAME);
    accept(program);
    return stats;
  }

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger
        .start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new RewriteConstructorCallsForUnboxedTypes(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }
}
