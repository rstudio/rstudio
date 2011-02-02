/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Removes JsFunctions that are never referenced in the program.
 */
public class JsUnusedFunctionRemover {
  public static final String NAME = JsUnusedFunctionRemover.class.getSimpleName();

  /**
   * Finds all functions in the program.
   */
  private class JsFunctionVisitor extends JsVisitor {

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      // Anonymous function, ignore it
      if (x.getName() != null && !x.isArtificiallyRescued()) {
        toRemove.put(x.getName(), x);
      }
    }
  }

  /**
   * Finds all function references in the program.
   */
  private class JsNameRefVisitor extends JsVisitor {

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      toRemove.remove(x.getName());
    }
  }

  private class RemovalVisitor extends JsModVisitor {

    @Override
    public void endVisit(JsExprStmt x, JsContext ctx) {
      if (!(x.getExpression() instanceof JsFunction)) {
        return;
      }

      JsFunction f = (JsFunction) x.getExpression();
      JsName name = f.getName();

      if (toRemove.containsKey(name)) {
        // Removing a static initializer indicates a problem in
        // JsInliner.
        if (name.getIdent().equals("$clinit")) {
          throw new InternalCompilerException("Tried to remove clinit "
              + name.getStaticRef().toSource());
        }

        if (!name.isObfuscatable()) {
          // This is intended to be used externally (e.g. gwtOnLoad)
          return;
        }

        // Remove the statement
        ctx.removeMe();
      }
    }
  }

  public static OptimizerStats exec(JsProgram program) {
    Event optimizeJsEvent = SpeedTracerLogger.start(
        CompilerEventType.OPTIMIZE_JS, "optimizer", NAME);
    OptimizerStats stats = new JsUnusedFunctionRemover(program).execImpl();
    optimizeJsEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private final Map<JsName, JsFunction> toRemove = new HashMap<JsName, JsFunction>();
  private final JsProgram program;

  public JsUnusedFunctionRemover(JsProgram program) {
    this.program = program;
  }

  public OptimizerStats execImpl() {
    OptimizerStats stats = new OptimizerStats(NAME);

    // Find all functions
    (new JsFunctionVisitor()).accept(program);

    // Remove the functions that are referenced from the hit list
    (new JsNameRefVisitor()).accept(program);

    // Remove the unused functions from the JsProgram
    RemovalVisitor removalVisitor = new RemovalVisitor();
    removalVisitor.accept(program);

    if (removalVisitor.didChange()) {
      stats.recordModified();
    }
    return stats;
  }
}
