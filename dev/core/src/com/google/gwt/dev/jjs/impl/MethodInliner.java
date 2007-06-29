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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Inline methods that can be inlined.
 * 
 * TODO(later): more aggressive inlining
 */
public class MethodInliner {

  /**
   * Flattens <code>JMultiExpressions</code> where possible.
   * 
   * TODO: make this a JModVisitor
   */
  public class FlattenMultiVisitor extends JVisitor {

    private boolean didChange = false;

    public boolean didChange() {
      return didChange;
    }

    // @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      ArrayList exprs = x.exprs;

      /*
       * Add the contents of all nested multis into the top multi, in place. We
       * are in fact iterating over nodes we've just added, but that should be
       * okay as the children will already be flattened.
       */
      for (int i = 0; i < exprs.size(); ++i) {
        JExpression expr = (JExpression) exprs.get(i);
        if (expr instanceof JMultiExpression) {
          JMultiExpression sub = (JMultiExpression) expr;
          exprs.addAll(i + 1, sub.exprs);
          didChange = true;
        }
      }

      // now remove the old multis
      for (Iterator it = exprs.iterator(); it.hasNext();) {
        JExpression expr = (JExpression) it.next();
        if (expr instanceof JMultiExpression) {
          it.remove();
          didChange = true;
        }
      }
    }
  }
  /**
   * Method inlining visitor.
   */
  public class InliningVisitor extends JModVisitor {
    /**
     * Resets with each new visitor, which is good since things that couldn't be
     * inlined before might become inlineable.
     */
    Set/* <JMethod> */cannotInline = new HashSet/* <JMethod> */();

    public void endVisit(JMethod x, Context ctx) {
      currentMethod = null;
    }

    // @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      // The method call must be known statically
      if (!method.isStatic() || method.isNative()) {
        return;
      }

      if (cannotInline.contains(method)) {
        return;
      }

      JMethodBody body = (JMethodBody) method.getBody();
      List/* <JStatement> */stmts = body.getStatements();
      boolean possibleToInline = false;
      if (stmts.isEmpty()) {
        tryInlineEmptyMethodCall(x, ctx);
        possibleToInline = true;
      } else if (stmts.size() == 1) {
        JStatement stmt = (JStatement) stmts.get(0);
        if (stmt instanceof JReturnStatement) {
          possibleToInline = tryInlineExpression(x, ctx,
              ((JReturnStatement) stmt).getExpr());
        } else if (stmt instanceof JExpressionStatement) {
          possibleToInline = tryInlineExpression(x, ctx,
              ((JExpressionStatement) stmt).getExpr());
        }
      }

      if (!possibleToInline) {
        cannotInline.add(method);
      }
    }

    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      return true;
    }

    /**
     * This complicated method has a simple purpose: see if the expression part
     * of a return statement is inlinable. The trickiness comes from the fact
     * that we'd like to be able to do this recursively in certain cases. For
     * example, the accessor method
     * 
     * <pre>
     * $getFoo(this$static) {
     *  return this$static.foo
     * }
     * </pre>
     * 
     * should be inlinable, but we have to first examine the field reference and
     * then recursively determine that the qualifier is inlinable.
     */
    private JExpression canInlineExpression(SourceInfo info,
        JExpression targetExpr, List/* <JParameter> */params, ArrayList args,
        int[] magicArg) {
      if (targetExpr instanceof JLiteral) {
        // just reference the same JLiteral
        /*
         * hackish: pretend there is an arg that is returned which comes after
         * all the real args; this allows the evaluation order check in
         * tryInlineSimpleMethodCall to succeed
         */
        magicArg[0] = args.size();
        return targetExpr;
      } else if (targetExpr instanceof JParameterRef) {
        // translate the param ref into the appropriate arg
        int i = params.indexOf(((JParameterRef) targetExpr).getTarget());
        assert (i >= 0);
        magicArg[0] = i;
        return (JExpression) args.get(i);
      } else if (targetExpr instanceof JFieldRef) {
        JFieldRef oldFieldRef = (JFieldRef) targetExpr;
        JField field = oldFieldRef.getField();
        JExpression instance = oldFieldRef.getInstance();
        if (instance != null) {
          // If an instance field, we have to be able to inline the qualifier
          instance = canInlineExpression(info, instance, params, args, magicArg);
          if (instance == null) {
            return null;
          }
        }
        JFieldRef newFieldRef = new JFieldRef(program, info, instance, field,
            currentMethod.getEnclosingType());
        return newFieldRef;
      } else {
        /*
         * For now, only inline REALLY trivial stuff since we have no way of
         * cloning arbitrary expressions.
         */
        return null;
      }
    }

    /**
     * Returns <code>true</code> if inlining the target expression would
     * eliminate a necessary clinit.
     */
    private boolean checkClinitViolation(JMethodCall x,
        JExpression resultExpression) {
      JReferenceType targetEnclosingType = x.getTarget().getEnclosingType();
      if (!program.typeOracle.checkClinit(currentMethod.getEnclosingType(), targetEnclosingType)) {
        // Access from this class to the target class won't trigger a clinit
        return false;
      }
      if (program.isStaticImpl(x.getTarget())) {
        // No clinit needed; target is really an instance method.
        return false;
      }

      /*
       * Potential clinit violation! We can only allow this if the result is
       * itself a static field ref into the target class, which would trigger
       * the clinit itself.
       */

      // resultExpression might be null, which behaves correctly here.
      if (!(resultExpression instanceof JFieldRef)) {
        return true;
      }
      JFieldRef fieldRefResult = (JFieldRef) resultExpression;
      JField fieldResult = fieldRefResult.getField();
      if (!fieldResult.isStatic()) {
        // A nonstatic field reference won't trigger the clinit we need.
        return true;
      }
      if (fieldResult.getEnclosingType() != targetEnclosingType) {
        // We have a static field reference, but it's to the wrong class (not
        // the class whose clinit we must trigger).
        return true;
      }
      // The correct cross-class static field reference will trigger the clinit.
      return false;
    }

    /**
     * Inlines a call to an empty method.
     */
    private void tryInlineEmptyMethodCall(JMethodCall x, Context ctx) {
      if (checkClinitViolation(x, null)) {
        return;
      }
      JMultiExpression multi = new JMultiExpression(program, x.getSourceInfo());
      JExpression instance = x.getInstance();
      if (instance != null && instance.hasSideEffects()) {
        multi.exprs.add(x.getInstance());
      }
      for (int i = 0, c = x.getArgs().size(); i < c; ++i) {
        if (((JExpression) x.getArgs().get(i)).hasSideEffects()) {
          multi.exprs.add(x.getArgs().get(i));
        }
      }
      ctx.replaceMe(multi);
    }

    /**
     * Inline a call to a method that contains only a return statement.
     */
    private boolean tryInlineExpression(JMethodCall x, Context ctx,
        JExpression targetExpr) {
      List/* <JParameter> */params = x.getTarget().params;
      ArrayList args = x.getArgs();

      // the expression returned by the inlined method, if any
      JExpression resultExpression;
      // the argument that is returned by the inlined method, if any
      int magicArg[] = new int[1];

      resultExpression = canInlineExpression(x.getSourceInfo(), targetExpr,
          params, args, magicArg);

      if (resultExpression == null) {
        return false; // cannot inline
      }

      // The expression is inlinable.

      if (checkClinitViolation(x, resultExpression)) {
        /*
         * Inlining here would cause a clinit to not fire, so we can't. But
         * return true, because this method could still be inlined from within
         * the same class.
         */
        return true;
      }

      // the argument that is returned by the inlined method
      int iMagicArg = magicArg[0];

      JMultiExpression multi = new JMultiExpression(program, x.getSourceInfo());

      // Evaluate the instance argument (we can have one even with static calls)
      JExpression instance = x.getInstance();
      if (instance != null && instance.hasSideEffects()) {
        multi.exprs.add(x.getInstance());
      }

      // Now evaluate any side-effect args that aren't the magic arg.
      for (int i = 0; i < params.size(); ++i) {
        if (((JExpression) args.get(i)).hasSideEffects()) {
          if (i < iMagicArg) {
            // evaluate this arg inside of the multi
            multi.exprs.add(args.get(i));
          } else if (i == iMagicArg) {
            // skip this arg, we'll do it below as the final one
          } else {
            assert (i > iMagicArg);
            /*
             * ABORT ABORT ABORT! This would cause an out-of-order evalutation.
             * Due to the way we construct multis, the magic arg must come last.
             * However, we've encountered a case where an argument coming after
             * the magic arg must be evaluated. Just bail.
             * 
             * However, we return true because this call might be inlinable at
             * other call sites.
             */
            return true;
          }
        }
      }

      // add in the result expression as the last item in the multi
      multi.exprs.add(resultExpression);
      ctx.replaceMe(multi);
      return true;
    }
  }

  /**
   * Reduces <code>JMultiExpression</code> where possible.
   */
  public class ReduceMultiVisitor extends JModVisitor {

    // @Override
    public void endVisit(JBlock x, Context ctx) {
      for (Iterator it = x.statements.iterator(); it.hasNext();) {
        JStatement stmt = (JStatement) it.next();
        // If we're a JExprStmt with no side effects, just remove me
        if (stmt instanceof JExpressionStatement) {
          JExpression expr = ((JExpressionStatement) stmt).getExpr();
          if (!expr.hasSideEffects()) {
            it.remove();
            didChange = true;
          }
        }
      }
    }

    // @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      ArrayList exprs = x.exprs;

      final int c = exprs.size();
      if (c == 0) {
        return;
      }

      int countSideEffectsBeforeLast = 0;
      for (int i = 0; i < c - 1; ++i) {
        JExpression expr = (JExpression) exprs.get(i);
        if (expr.hasSideEffects()) {
          ++countSideEffectsBeforeLast;
        }
      }

      if (countSideEffectsBeforeLast == 0) {
        ctx.replaceMe((JExpression) x.exprs.get(c - 1));
      } else {
        JMultiExpression newMulti = new JMultiExpression(program,
            x.getSourceInfo());
        for (int i = 0; i < c - 1; ++i) {
          JExpression expr = (JExpression) exprs.get(i);
          if (expr.hasSideEffects()) {
            newMulti.exprs.add(expr);
          }
        }
        newMulti.exprs.add(x.exprs.get(c - 1));
        if (newMulti.exprs.size() < x.exprs.size()) {
          ctx.replaceMe(newMulti);
        }
      }
    }
  }

  public static boolean exec(JProgram program) {
    return new MethodInliner(program).execImpl();
  }

  private JMethod currentMethod;
  private final JProgram program;

  private MethodInliner(JProgram program) {
    this.program = program;
  }

  private boolean execImpl() {
    boolean madeChanges = false;
    while (true) {
      InliningVisitor inliner = new InliningVisitor();
      inliner.accept(program);
      if (!inliner.didChange()) {
        break;
      }
      madeChanges = true;

      FlattenMultiVisitor flattener = new FlattenMultiVisitor();
      flattener.accept(program);

      ReduceMultiVisitor reducer = new ReduceMultiVisitor();
      reducer.accept(program);
    }
    return madeChanges;
  }

}
