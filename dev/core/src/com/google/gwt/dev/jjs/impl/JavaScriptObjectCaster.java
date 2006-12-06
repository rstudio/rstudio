// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JLocalDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;

/**
 * Replace cast and instanceof operations with calls to the Cast class.
 */
public class JavaScriptObjectCaster {

  private class AssignmentVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
      "Synthesize casts from JavaScriptObjects to trigger wrapping.");

    public ChangeList getChangeList() {
      return changeList;
    }

    private void checkAndReplaceJso(Mutator arg, JType targetType) {
      JType argType = arg.get().getType();
      if (argType == targetType) {
        return;
      }
      
      if (!(targetType instanceof JReferenceType)) {
        return;
      }
      
      if (!program.isJavaScriptObject(argType)) {
        return;
      }
      JCastOperation cast = new JCastOperation(program, targetType, program
        .getLiteralNull());
      ChangeList myChangeList = new ChangeList("Synthesize a cast from '"
        + argType + "' to '" + targetType + "'.");
      myChangeList.replaceExpression(cast.expr, arg);
      myChangeList.replaceExpression(arg, cast);
      changeList.add(myChangeList);
    }

    private JMethod fCurrentMethod;

    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      if (x.isAssignment()) {
        checkAndReplaceJso(x.rhs, x.getLhs().getType());
      }
    }

    // @Override
    public void endVisit(JConditional x, Mutator m) {
      checkAndReplaceJso(x.thenExpr, x.getType());
      checkAndReplaceJso(x.elseExpr, x.getType());
    }

    // @Override
    public void endVisit(JLocalDeclarationStatement x) {
      JExpression initializer = x.getInitializer();
      if (initializer != null) {
        checkAndReplaceJso(x.initializer, x.getLocalRef().getType());
      }
    }

    // @Override
    public void endVisit(JMethod x) {
      fCurrentMethod = null;
    }

    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      if (!x.getTarget().isStatic()) {
        // for polymorphic calls, force wrapping
        checkAndReplaceJso(x.instance, program.getTypeJavaLangObject());
      }
      for (int i = 0; i < x.getTarget().params.size(); ++i) {
        JParameter param = (JParameter) x.getTarget().params.get(i);
        checkAndReplaceJso(x.args.getMutator(i), param.getType());
      }
    }

    // @Override
    public void endVisit(JReturnStatement x) {
      if (x.getExpression() != null) {
        checkAndReplaceJso(x.expr, fCurrentMethod.getType());
      }
    }

    // @Override
    public boolean visit(JMethod x) {
      fCurrentMethod = x;
      return true;
    }
  }

  private void execImpl() {
    {
      AssignmentVisitor visitor = new AssignmentVisitor();
      program.traverse(visitor);
      ChangeList changes = visitor.getChangeList();
      if (!changes.empty()) {
        changes.apply();
      }
    }
  }

  private final JProgram program;

  private JavaScriptObjectCaster(JProgram program) {
    this.program = program;
  }

  public static void exec(JProgram program) {
    new JavaScriptObjectCaster(program).execImpl();
  }

}
