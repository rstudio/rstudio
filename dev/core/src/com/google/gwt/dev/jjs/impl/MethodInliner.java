/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.jjs.ast.Holder;
import com.google.gwt.dev.jjs.ast.HolderList;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.util.HashSet;
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
   */
  public class FlattenMultiVisitor extends JVisitor {
    private final ChangeList changeList = new ChangeList(
        "Flatten multis where possible.");

    // @Override
    public void endVisit(JMultiExpression x, Mutator m) {
      HolderList exprs = x.exprs;

      // do all adds FIRST or the indices will be wrong
      for (int i = 0; i < exprs.size(); ++i) {
        JExpression expr = exprs.getExpr(i);
        if (expr instanceof JMultiExpression) {
          JMultiExpression sub = (JMultiExpression) expr;
          changeList.addAll(sub.exprs, i, exprs);
        }
      }

      // now remove the old multi
      for (int i = 0; i < exprs.size(); ++i) {
        JExpression expr = exprs.getExpr(i);
        if (expr instanceof JMultiExpression) {
          changeList.removeNode(exprs.getMutator(i), exprs);
        }
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }
  }
  /**
   * Method inlining visitor.
   */
  public class InliningVisitor extends JVisitor {
    /**
     * Resets with each new visitor, which is good since things that couldn't be
     * inlined before might become inlineable.
     */
    Set/* <JMethod> */cannotInline = new HashSet/* <JMethod> */();

    private final ChangeList changeList = new ChangeList("Inline methods.");

    public void endVisit(JMethod x) {
      currentMethod = null;
    }

    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      JMethod method = x.getTarget();

      // The method call must be known statically
      if (!method.isStatic() || method.isNative()) {
        return;
      }

      if (cannotInline.contains(method)) {
        return;
      }

      List/* <JStatement> */stmts = method.body.statements;
      boolean possibleToInline = false;
      if (stmts.isEmpty()) {
        possibleToInline = inlineEmptyMethodCall(x, m, method);
      } else if (stmts.size() == 1) {
        JStatement stmt = (JStatement) stmts.get(0);
        if (stmt instanceof JReturnStatement) {
          possibleToInline = tryInlineSimpleMethodCall(x, m, method,
              (JReturnStatement) stmt);
        }
      }

      if (!possibleToInline) {
        cannotInline.add(method);
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }

    public boolean visit(JMethod x) {
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
    private Mutator canInlineResultExpression(JExpression targetReturnExpr,
        List/* <JParameter> */params, HolderList args, int[] magicArg,
        ChangeList changes) {
      if (targetReturnExpr instanceof JLiteral) {
        // just reference the same JLiteral
        /*
         * hackish: pretend there is an arg that is returned which comes after
         * all the real args; this allows the evaluation order check in
         * tryInlineSimpleMethodCall to succeed
         */
        magicArg[0] = args.size();
        return new Holder(targetReturnExpr);
      } else if (targetReturnExpr instanceof JParameterRef) {
        // translate the param ref into the appropriate arg
        int i = params.indexOf(((JParameterRef) targetReturnExpr).getTarget());
        assert (i >= 0);
        magicArg[0] = i;
        return args.getMutator(i);
      } else if (targetReturnExpr instanceof JFieldRef) {
        JFieldRef oldFieldRef = (JFieldRef) targetReturnExpr;
        JField field = oldFieldRef.getField();
        JExpression instance = oldFieldRef.getInstance();
        JFieldRef newFieldRef = new JFieldRef(program, null, field,
            currentMethod.getEnclosingType());
        if (instance != null) {
          // If an instance field, we have to be able to inline the qualifier
          Mutator instanceMutator = canInlineResultExpression(instance, params,
              args, magicArg, changes);
          if (instanceMutator == null) {
            return null;
          }
          changes.replaceExpression(newFieldRef.instance, instanceMutator);
        }
        return new Holder(newFieldRef);
      } else {
        /*
         * For now, only inline REALLY trivial stuff since we have no way of
         * cloning arbitrary expressions.
         */
        return null;
      }
    }

    private boolean inlineEmptyMethodCall(JMethodCall x, Mutator m,
        JMethod method) {
      ChangeList changes = new ChangeList("Inline a call to empty method '"
          + method + "'");
      JMultiExpression multi = new JMultiExpression(program);
      JExpression instance = x.getInstance();
      if (instance != null && instance.hasSideEffects()) {
        changes.addExpression(x.instance, multi.exprs);
      }
      for (int i = 0, c = x.args.size(); i < c; ++i) {
        if (x.args.getExpr(i).hasSideEffects()) {
          changes.addExpression(x.args.getMutator(i), multi.exprs);
        }
      }

      changes.replaceExpression(m, multi);
      changeList.add(changes);
      return true;
    }

    private boolean tryInlineSimpleMethodCall(JMethodCall x, Mutator m,
        JMethod method, JReturnStatement returnStmt) {
      List/* <JParameter> */params = method.params;
      HolderList args = x.args;

      ChangeList changes = new ChangeList("Inline a call to simple method '"
          + method + "'");

      // the expression returned by the inlined method, if any
      Mutator resultExpression;
      // the argument that is returned by the inlined method, if any
      int magicArg[] = new int[1];

      JExpression targetReturnExpr = returnStmt.getExpression();
      resultExpression = canInlineResultExpression(targetReturnExpr, params,
          args, magicArg, changes);

      if (resultExpression == null) {
        return false; // cannot inline
      }

      // the argument that is returned by the inlined method
      int iMagicArg = magicArg[0];

      JMultiExpression multi = new JMultiExpression(program);

      // Evaluate the instance argument (we can have one even with static calls)
      JExpression instance = x.getInstance();
      if (instance != null && instance.hasSideEffects()) {
        changes.addExpression(x.instance, multi.exprs);
      }

      // Now evaluate any side-effect args that aren't the magic arg.
      for (int i = 0; i < params.size(); ++i) {
        if (args.getExpr(i).hasSideEffects()) {
          if (i < iMagicArg) {
            // evaluate this arg inside of the multi
            changes.addExpression(args.getMutator(i), multi.exprs);
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
      changes.addExpression(resultExpression, multi.exprs);
      changes.replaceExpression(m, multi);
      changeList.add(changes);
      return true;
    }
  }

  /**
   * Reduces <code>JMultiExpression</code> where possible.
   */
  public class ReduceMultiVisitor extends JVisitor {
    private final ChangeList changeList = new ChangeList(
        "Reduce multis where possible.");

    // @Override
    public void endVisit(JMultiExpression x, Mutator m) {
      HolderList exprs = x.exprs;

      final int c = exprs.size();
      if (c == 0) {
        return;
      }

      int countSideEffectsBeforeLast = 0;
      for (int i = 0; i < c - 1; ++i) {
        JExpression expr = exprs.getExpr(i);
        if (expr.hasSideEffects()) {
          ++countSideEffectsBeforeLast;
        }
      }

      if (countSideEffectsBeforeLast == 0) {
        changeList.replaceExpression(m, x.exprs.getMutator(c - 1));
      } else {
        for (int i = 0; i < c - 1; ++i) {
          JExpression expr = exprs.getExpr(i);
          if (!expr.hasSideEffects()) {
            changeList.removeNode(x.exprs.getMutator(i), exprs);
          }
        }
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JBlock x) {
      for (int i = 0; i < x.statements.size(); ++i) {
        JStatement stmt = (JStatement) x.statements.get(i);
        stmt.traverse(this);
        // If we're a JExprStmt with no side effects, just remove me
        if (stmt instanceof JExpressionStatement) {
          JExpression expr = ((JExpressionStatement) stmt).getExpression();
          if (!expr.hasSideEffects()) {
            changeList.removeNode(stmt, x.statements);
          }
        }
      }
      return false;
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
      {
        InliningVisitor inliner = new InliningVisitor();
        program.traverse(inliner);
        ChangeList changes = inliner.getChangeList();
        if (changes.empty()) {
          break;
        }
        changes.apply();
        madeChanges = true;
      }

      {
        FlattenMultiVisitor flattener = new FlattenMultiVisitor();
        program.traverse(flattener);
        ChangeList changes = flattener.getChangeList();
        if (!changes.empty()) {
          changes.apply();
        }
      }

      {
        ReduceMultiVisitor reducer = new ReduceMultiVisitor();
        program.traverse(reducer);
        ChangeList changes = reducer.getChangeList();
        if (!changes.empty()) {
          changes.apply();
        }
      }
    }
    return madeChanges;
  }

}
