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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is used to clean up duplication invocations of clinit function. Whenever there is a
 * possible branch in program flow, the remover will create a new instance of
 * itself to handle the possible branches.
 *
 * We don't look at combining branch choices. This will not produce the most
 * efficient elimination of duplicated calls, but it handles the general case
 * and is simple to verify.
 */
public class DuplicateClinitRemover extends JsModVisitor {
  private static final String NAME = JsInliner.class.getSimpleName();

  /*
   * TODO: Most of the special casing below can be removed if complex
   * statements always use blocks, rather than plain statements.
   */

  /**
   * Retains the functions that we know have been called.
   */
  private final Set<JsFunction> called;
  private final JsProgram program;

  public DuplicateClinitRemover(JsProgram program) {
    this.program = program;
    called = new HashSet<JsFunction>();
  }

  public DuplicateClinitRemover(JsProgram program, Set<JsFunction> alreadyCalled) {
    this.program = program;
    called = new HashSet<JsFunction>(alreadyCalled);
  }

  /**
   * Look for comma expressions that contain duplicate calls and handle the
   * conditional-evaluation case of logical and/or operations.
   */
  @Override
  public boolean visit(JsBinaryOperation x, JsContext ctx) {
    if (x.getOperator() == JsBinaryOperator.COMMA) {

      boolean left = isDuplicateCall(x.getArg1());
      boolean right = isDuplicateCall(x.getArg2());

      if (left && right) {
        /*
         * (clinit(), clinit()) --> delete or null.
         *
         * This construct is very unlikely since the InliningVisitor builds
         * the comma expressions in a right-nested manner.
         */
        if (ctx.canRemove()) {
          ctx.removeMe();
          return false;
        } else {
          // The return value from an XO function is never used
          ctx.replaceMe(JsNullLiteral.INSTANCE);
          return false;
        }

      } else if (left) {
        // (clinit(), xyz) --> xyz
        // This is the common case
        ctx.replaceMe(accept(x.getArg2()));
        return false;

      } else if (right) {
        // (xyz, clinit()) --> xyz
        // Possible if a clinit() were the last element
        ctx.replaceMe(accept(x.getArg1()));
        return false;
      }

    } else if (x.getOperator().equals(JsBinaryOperator.AND)
        || x.getOperator().equals(JsBinaryOperator.OR)) {
      x.setArg1(accept(x.getArg1()));
      // Possibility of conditional evaluation of second parameter
      x.setArg2(branch(x.getArg2()));
      return false;
    }

    return true;
  }

  /**
   * Most of the branching statements (as well as JsFunctions) will visit with
   * a JsBlock, so we don't need to explicitly enumerate all JsStatement
   * subtypes.
   */
  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    branch(x.getStatements());
    return false;
  }

  @Override
  public boolean visit(JsCase x, JsContext ctx) {
    x.setCaseExpr(accept(x.getCaseExpr()));
    branch(x.getStmts());
    return false;
  }

  @Override
  public boolean visit(JsConditional x, JsContext ctx) {
    x.setTestExpression(accept(x.getTestExpression()));
    x.setThenExpression(branch(x.getThenExpression()));
    x.setElseExpression(branch(x.getElseExpression()));
    return false;
  }

  @Override
  public boolean visit(JsDefault x, JsContext ctx) {
    branch(x.getStmts());
    return false;
  }

  @Override
  public boolean visit(JsExprStmt x, JsContext ctx) {
    if (isDuplicateCall(x.getExpression())) {
      if (ctx.canRemove()) {
        ctx.removeMe();
      } else {
        ctx.replaceMe(new JsEmpty(x.getSourceInfo()));
      }
      return false;

    } else {
      return true;
    }
  }

  @Override
  public boolean visit(JsFor x, JsContext ctx) {
    // The JsFor may have an expression xor a variable declaration.
    if (x.getInitExpr() != null) {
      x.setInitExpr(accept(x.getInitExpr()));
    } else if (x.getInitVars() != null) {
      x.setInitVars(accept(x.getInitVars()));
    }

    // The condition is optional
    if (x.getCondition() != null) {
      x.setCondition(accept(x.getCondition()));
    }

    // The increment expression is optional
    if (x.getIncrExpr() != null) {
      x.setIncrExpr(branch(x.getIncrExpr()));
    }

    // The body is not guaranteed to be a JsBlock
    x.setBody(branch(x.getBody()));
    return false;
  }

  @Override
  public boolean visit(JsForIn x, JsContext ctx) {
    if (x.getIterExpr() != null) {
      x.setIterExpr(accept(x.getIterExpr()));
    }

    x.setObjExpr(accept(x.getObjExpr()));

    // The body is not guaranteed to be a JsBlock
    x.setBody(branch(x.getBody()));
    return false;
  }

  @Override
  public boolean visit(JsIf x, JsContext ctx) {
    x.setIfExpr(accept(x.getIfExpr()));

    x.setThenStmt(branch(x.getThenStmt()));
    if (x.getElseStmt() != null) {
      x.setElseStmt(branch(x.getElseStmt()));
    }

    return false;
  }

  /**
   * Possibly record that we've seen a call in the current context.
   */
  @Override
  public boolean visit(JsInvocation x, JsContext ctx) {
    JsFunction func = JsUtils.isExecuteOnce(x);
    while (func != null) {
      called.add(func);
      func = func.getSuperClinit();
    }
    return true;
  }

  @Override
  public boolean visit(JsWhile x, JsContext ctx) {
    x.setCondition(accept(x.getCondition()));

    // The body is not guaranteed to be a JsBlock
    x.setBody(branch(x.getBody()));
    return false;
  }

  /**
   * Static entry point used by JavaToJavaScriptCompiler.
   */
  public static OptimizerStats exec(JsProgram program) {
    Event optimizeJsEvent = SpeedTracerLogger.start(
        CompilerEventType.OPTIMIZE_JS, "duplicateXOremover", NAME);
    OptimizerStats stats = execImpl(program);
    optimizeJsEvent.end("didChange", "" + stats.didChange());
    return stats;
  }


  private static OptimizerStats execImpl(JsProgram program) {
    OptimizerStats stats = new OptimizerStats(NAME);
    DuplicateClinitRemover r = new DuplicateClinitRemover(program);
    r.accept(program);
    if (r.didChange()) {
      stats.recordModified();
    }
    return stats;
  }

  private <T extends JsNode> void branch(List<T> x) {
    DuplicateClinitRemover dup = new DuplicateClinitRemover(program, called);
    dup.acceptWithInsertRemove(x);
    didChange |= dup.didChange();
  }

  private <T extends JsNode> T branch(T x) {
    DuplicateClinitRemover dup = new DuplicateClinitRemover(program, called);
    T toReturn = dup.accept(x);

    if ((toReturn != x) && !dup.didChange()) {
      throw new InternalCompilerException(
          "node replacement should imply didChange()");
    }

    didChange |= dup.didChange();
    return toReturn;
  }

  private boolean isDuplicateCall(JsExpression x) {
    if (!(x instanceof JsInvocation)) {
      return false;
    }

    JsFunction func = JsUtils.isExecuteOnce((JsInvocation) x);
    return (func != null && called.contains(func));
  }
}
