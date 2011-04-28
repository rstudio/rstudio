/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects when same literal is passed as parameter value, and uses literal
 * instead of parameter. The unused parameter will be removed by other analyses.
 */
// TODO: this optimization can mistakenly act on methods such as LongLib.fromInt
// since only one call is seen in LongLib itself.
public class SameParameterValueOptimizer {
  /**
   * Fill parameterValues map.
   */
  private class AnalysisVisitor extends JVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.isAssignment() && x.getLhs() instanceof JParameterRef) {
        parameterValues.put(((JParameterRef) x.getLhs()).getParameter(), null);
      }
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      if (x.canBePolymorphic() || rescuedMethods.contains(method)) {
        return;
      }

      List<JExpression> args = x.getArgs();
      List<JParameter> params = method.getParams();

      for (int i = 0; i < args.size() && i < params.size(); i++) {
        JParameter param = params.get(i);
        JExpression arg = args.get(i);

        if (!(arg instanceof JValueLiteral)) {
          parameterValues.put(param, null);
          continue;
        }

        if (!parameterValues.containsKey(param)) {
          parameterValues.put(param, (JValueLiteral) arg);
          continue;
        }

        JValueLiteral commonParamValue = parameterValues.get(param);
        if (commonParamValue == null) {
          continue;
        }

        if (!equalLiterals(commonParamValue, (JValueLiteral) arg)) {
          parameterValues.put(param, null);
        }
      }
    }

    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      if (x.getArg() instanceof JParameterRef) {
        parameterValues.put(((JParameterRef) x.getArg()).getParameter(), null);
      }
    }

    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      if (x.getArg() instanceof JParameterRef) {
        parameterValues.put(((JParameterRef) x.getArg()).getParameter(), null);
      }
    }

    @Override
    public void endVisit(JsniMethodBody x, Context ctx) {
      for (JsniMethodRef methodRef : x.getJsniMethodRefs()) {
        rescuedMethods.add(methodRef.getTarget());
      }
    }

    @Override
    public boolean visit(JConstructor x, Context ctx) {
      // Cannot be overridden or staticified.
      return true;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      Set<JMethod> overrides = program.typeOracle.getAllOverrides(x);
      if (!overrides.isEmpty()) {
        for (JMethod m : overrides) {
          rescuedMethods.add(m);
        }
        rescuedMethods.add(x);
      }
      return true;
    }

    private boolean equalLiterals(JValueLiteral l1, JValueLiteral l2) {
      Object v1 = l1.getValueObj();
      Object v2 = l2.getValueObj();

      if (v1 == v2) {
        return true;
      }

      if (v1 == null || v2 == null) {
        return false;
      }

      return v1.equals(v2);
    }
  }

  /**
   * Substitute all parameter references with expression.
   */
  private class SubstituteParameterVisitor extends JModVisitor {
    private final CloneExpressionVisitor cloner;
    private final JExpression expression;
    private final JParameter parameter;

    public SubstituteParameterVisitor(JParameter parameter, JExpression expression) {
      this.parameter = parameter;
      this.expression = expression;
      cloner = new CloneExpressionVisitor();
    }

    @Override
    public void endVisit(JParameterRef x, Context ctx) {
      if (x.getParameter() == parameter) {
        ctx.replaceMe(cloner.cloneExpression(expression));
      }
    }
  }

  private static final String NAME = SameParameterValueOptimizer.class.getSimpleName();

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new SameParameterValueOptimizer(program).execImpl(program);
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  /**
   * Parameter values.
   * 
   * If doesn't contain a parameter, then its value is unknown. If contains
   * parameter, and value is null - the parameter's value is not the same across
   * all calls. If value is not null - the parameter's value is the same across
   * all calls.
   */
  private final Map<JParameter, JValueLiteral> parameterValues =
      new IdentityHashMap<JParameter, JValueLiteral>();
  private final JProgram program;

  /**
   * These methods should not be tried to optimized due to their polymorphic
   * nature.
   * 
   * TODO: support polymorphic calls properly.
   */
  private final Set<JMethod> rescuedMethods = new HashSet<JMethod>();
  private final Simplifier simplifier;

  private SameParameterValueOptimizer(JProgram program) {
    this.program = program;
    simplifier = new Simplifier(program);
  }

  private OptimizerStats execImpl(JNode node) {
    OptimizerStats stats = new OptimizerStats(NAME);
    AnalysisVisitor analysisVisitor = new AnalysisVisitor();
    analysisVisitor.accept(node);

    for (JParameter parameter : parameterValues.keySet()) {
      if (rescuedMethods.contains(parameter.getEnclosingMethod())) {
        continue;
      }
      JValueLiteral valueLiteral = parameterValues.get(parameter);
      if (valueLiteral != null) {
        SubstituteParameterVisitor substituteParameterVisitor =
            new SubstituteParameterVisitor(parameter, simplifier.cast(parameter.getType(),
                valueLiteral));
        substituteParameterVisitor.accept(parameter.getEnclosingMethod());
        stats.recordModified(substituteParameterVisitor.getNumMods());
      }
    }
    return stats;
  }
}
