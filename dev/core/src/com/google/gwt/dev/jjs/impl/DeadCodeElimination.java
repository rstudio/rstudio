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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;

/**
 * Attempts to remove dead code.
 */
public class DeadCodeElimination {

  /**
   * Eliminates dead or unreachable code when possible.
   */
  public class DeadCodeVisitor extends JModVisitor {

    /**
     * Short circuit boolean AND or OR expressions when possible.
     */
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp() == JBinaryOperator.AND) {
        // simplify short circuit AND expressions
        if (x.getLhs() instanceof JBooleanLiteral) {
          // eg: if (false && isWhatever()) -> if (false)
          // eg: if (true && isWhatever()) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) x.getLhs();
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(x.getRhs());
          } else {
            ctx.replaceMe(x.getLhs());
          }

        } else if (x.getRhs() instanceof JBooleanLiteral) {
          // eg: if (isWhatever() && true) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) x.getRhs();
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(x.getLhs());
          }
        }

      } else if (x.getOp() == JBinaryOperator.OR) {
        // simplify short circuit OR expressions
        if (x.getLhs() instanceof JBooleanLiteral) {
          // eg: if (true || isWhatever()) -> if (true)
          // eg: if (false || isWhatever()) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) x.getLhs();
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(x.getLhs());
          } else {
            ctx.replaceMe(x.getRhs());
          }

        } else if (x.getRhs() instanceof JBooleanLiteral) {
          // eg: if (isWhatever() || false) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) x.getRhs();
          if (!booleanLiteral.getValue()) {
            ctx.replaceMe(x.getLhs());
          }
        }
      }
    }

    /**
     * Prune empty blocks.
     */
    public void endVisit(JBlock x, Context ctx) {
      if (x.statements.size() == 0) {
        if (ctx.canRemove()) {
          ctx.removeMe();
        }
      }
    }

    /**
     * Convert do { } while (false); into a block.
     */
    public void endVisit(JDoStatement x, Context ctx) {
      final JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        final JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, replace do with do's body
        if (!booleanLiteral.getValue()) {
          // Unless it contains break/continue statements
          FindBreakContinueStatementsVisitor visitor = new FindBreakContinueStatementsVisitor();
          visitor.accept(x.getBody());
          if (!visitor.hasBreakContinueStatements()) {
            ctx.replaceMe(x.getBody());
          }
        }
      }
    }

    /**
     * Prune for (X; false; Y) statements, but make sure X is run.
     */
    public void endVisit(JForStatement x, Context ctx) {
      final JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        final JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, replace the for statement with its initializers
        if (!booleanLiteral.getValue()) {
          JBlock block = new JBlock(program, x.getSourceInfo());
          block.statements.addAll(x.getInitializers());
          ctx.replaceMe(block);
        }
      }
    }

    /**
     * Prune "if (false)" statements.
     */
    public void endVisit(JIfStatement x, Context ctx) {
      final JExpression expression = x.getIfExpr();
      if (expression instanceof JBooleanLiteral) {
        final JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        if (booleanLiteral.getValue()) {
          // If true, replace myself with then statement
          ctx.replaceMe(x.getThenStmt());
        } else if (x.getElseStmt() != null) {
          // If false, replace myself with else statement
          ctx.replaceMe(x.getElseStmt());
        } else {
          // just prune me
          removeMe(x, ctx);
        }
      }
    }

    /**
     * Resolve "!true" into "false" and vice versa.
     */
    public void endVisit(JPrefixOperation x, Context ctx) {
      if (x.getOp() == JUnaryOperator.NOT) {
        if (x.getArg() instanceof JBooleanLiteral) {
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) x.getArg();
          ctx.replaceMe(program.getLiteralBoolean(!booleanLiteral.getValue()));
        }
      }
    }

    /**
     * Prune try statements with no body. Hoist up try statements with no
     * catches and an empty finally.
     */
    public void endVisit(JTryStatement x, Context ctx) {
      boolean noTry = x.getTryBlock().statements.isEmpty();
      // TODO: normalize finally block handling
      boolean noFinally = (x.getFinallyBlock() == null)
          || x.getFinallyBlock().statements.isEmpty();
      boolean noCatch = x.getCatchArgs().size() == 0;

      if (noTry) {
        // If the try block is empty, just remove it
        removeMe(x, ctx);
      } else if (noCatch && noFinally) {
        // If there's no catch or finally, there's no point in this even being
        // a try statement, replace myself with the try block
        ctx.replaceMe(x.getTryBlock());
      }
    }

    /**
     * Prune while (false) statements.
     */
    public void endVisit(JWhileStatement x, Context ctx) {
      final JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        final JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, prune the while statement
        if (!booleanLiteral.getValue()) {
          removeMe(x, ctx);
        }
      }
    }

    private void removeMe(JStatement stmt, Context ctx) {
      if (ctx.canRemove()) {
        ctx.removeMe();
      } else {
        // empty block statement
        ctx.replaceMe(new JBlock(program, stmt.getSourceInfo()));
      }
    }
  }

  /**
   * Examines code to find out whether it contains any break or continue
   * statements.
   */
  public static class FindBreakContinueStatementsVisitor extends JVisitor {
    private boolean hasBreakContinueStatements = false;

    public void endVisit(JBreakStatement x, Context ctx) {
      hasBreakContinueStatements = true;
    }

    public void endVisit(JContinueStatement x, Context ctx) {
      hasBreakContinueStatements = true;
    }

    protected boolean hasBreakContinueStatements() {
      return hasBreakContinueStatements;
    }
  }

  public static boolean exec(JProgram program) {
    return new DeadCodeElimination(program).execImpl();
  }

  private final JProgram program;

  public DeadCodeElimination(JProgram program) {
    this.program = program;
  }

  private boolean execImpl() {
    boolean madeChanges = false;
    while (true) {
      DeadCodeVisitor deadCodeVisitor = new DeadCodeVisitor();
      deadCodeVisitor.accept(program);
      if (!deadCodeVisitor.didChange()) {
        break;
      }
      madeChanges = true;
    }
    return madeChanges;
  }
}
