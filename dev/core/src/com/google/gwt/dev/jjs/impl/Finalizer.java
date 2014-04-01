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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.CanBeSetFinal;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds all items are effectively final. That is, methods that are never
 * overridden, classes that are never subclassed, and variables that are never
 * reassigned. Mark all such methods and classes as final, since it helps us
 * optimize.
 */
public class Finalizer {
  /**
   * Any items that weren't marked during MarkVisitor can be set final.
   *
   * Open question: What does it mean if an interface/abstract method becomes
   * final? Is this possible after Pruning? I guess it means that someone tried
   * to make a call to method that wasn't actually implemented anywhere in the
   * program. But if it wasn't implemented, then the enclosing class should have
   * come up as not instantiated and been culled. So I think it's not possible.
   */
  private class FinalizeVisitor extends JModVisitor {

    @Override
    public void endVisit(JClassType x, Context ctx) {
      if (!x.isFinal() && !isSubclassed.contains(x)) {
        setFinal(x);
      }
    }

    @Override
    public void endVisit(JConstructor x, Context ctx) {
      // Not applicable.
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      if (!x.isVolatile()) {
        maybeFinalize(x);
      }
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      maybeFinalize(x);
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (!x.isFinal() && !isOverridden.contains(x)) {
        setFinal(x);
      }
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      maybeFinalize(x);
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // Don't visit external types, because we can't change their final
      // specifiers.
      return !x.isExternal();
    }

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      for (JLocal local : x.getLocals()) {
        maybeFinalize(local);
      }
      return false;
    }

    private void maybeFinalize(JVariable x) {
      if (!x.isFinal() && !isReassigned.contains(x)) {
        setFinal(x);
      }
    }

    private void setFinal(CanBeSetFinal x) {
      x.setFinal();
      assert x.isFinal();
      madeChanges();
    }
  }

  /**
   * Find all items that ARE overridden/subclassed/reassigned.
   */
  private class MarkVisitor extends JVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp().isAssignment()) {
        recordAssignment(x.getLhs());
      }
    }

    @Override
    public void endVisit(JClassType x, Context ctx) {
      if (x.getSuperClass() != null) {
        isSubclassed.add(x.getSuperClass());
      }
    }

    @Override
    public void endVisit(JConstructor x, Context ctx) {
      // Never overridden.
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      // This is not a reassignment, the target may still be final.
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      for (int i = 0; i < x.getOverriddenMethods().size(); ++i) {
        JMethod it = x.getOverriddenMethods().get(i);
        isOverridden.add(it);
      }
    }

    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        recordAssignment(x.getArg());
      }
    }

    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        recordAssignment(x.getArg());
      }
    }

    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      if (x.isLvalue()) {
        recordAssignment(x);
      }
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // Don't visit external types, because we can't change their final
      // specifiers.
      return !x.isExternal();
    }

    private void recordAssignment(JExpression lhs) {
      if (lhs instanceof JVariableRef) {
        JVariableRef variableRef = (JVariableRef) lhs;
        isReassigned.add(variableRef.getTarget());
      }
    }
  }

  private static final String NAME = Finalizer.class.getSimpleName();

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new Finalizer().execImpl(program);
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private final Set<JMethod> isOverridden = new HashSet<JMethod>();

  private final Set<JVariable> isReassigned = new HashSet<JVariable>();

  private final Set<JClassType> isSubclassed = new HashSet<JClassType>();

  private Finalizer() {
  }

  private OptimizerStats execImpl(JProgram program) {
    MarkVisitor marker = new MarkVisitor();
    marker.accept(program);

    FinalizeVisitor finalizer = new FinalizeVisitor();
    finalizer.accept(program);

    return new OptimizerStats(NAME).recordModified(finalizer.getNumMods());
  }
}
