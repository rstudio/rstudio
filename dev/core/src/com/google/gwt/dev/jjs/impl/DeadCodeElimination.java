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
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Attempts to remove dead code.
 */
public class DeadCodeElimination {

  /**
   * Eliminates dead or unreachable code when possible.
   */
  public class DeadCodeVisitor extends JModVisitor {

    /**
     * Short circuit binary operations.
     */
    public void endVisit(JBinaryOperation x, Context ctx) {
      JBinaryOperator op = x.getOp();
      JExpression lhs = x.getLhs();
      JExpression rhs = x.getRhs();
      if (op == JBinaryOperator.AND) {
        // simplify short circuit AND expressions
        if (lhs instanceof JBooleanLiteral) {
          // eg: if (false && isWhatever()) -> if (false)
          // eg: if (true && isWhatever()) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) lhs;
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(rhs);
          } else {
            ctx.replaceMe(lhs);
          }

        } else if (rhs instanceof JBooleanLiteral) {
          // eg: if (isWhatever() && true) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) rhs;
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(lhs);
          }
        }

      } else if (op == JBinaryOperator.OR) {
        // simplify short circuit OR expressions
        if (lhs instanceof JBooleanLiteral) {
          // eg: if (true || isWhatever()) -> if (true)
          // eg: if (false || isWhatever()) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) lhs;
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(lhs);
          } else {
            ctx.replaceMe(rhs);
          }

        } else if (rhs instanceof JBooleanLiteral) {
          // eg: if (isWhatever() || false) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) rhs;
          if (!booleanLiteral.getValue()) {
            ctx.replaceMe(lhs);
          }
        }
      } else if (op == JBinaryOperator.EQ) {
        // simplify: null == null -> true
        if (lhs.getType() == program.getTypeNull()
            && rhs.getType() == program.getTypeNull()) {
          ctx.replaceMe(program.getLiteralBoolean(true));
        }
      } else if (op == JBinaryOperator.NEQ) {
        // simplify: null != null -> false
        if (lhs.getType() == program.getTypeNull()
            && rhs.getType() == program.getTypeNull()) {
          ctx.replaceMe(program.getLiteralBoolean(false));
        }
      } else if (op == JBinaryOperator.ADD
          && x.getType() == program.getTypeJavaLangString()) {
        // try to statically evaluate concatentation
        if (lhs instanceof JValueLiteral && rhs instanceof JValueLiteral) {
          Object lhsObj = ((JValueLiteral) lhs).getValueObj();
          Object rhsObj = ((JValueLiteral) rhs).getValueObj();
          ctx.replaceMe(program.getLiteralString(String.valueOf(lhsObj)
              + String.valueOf(rhsObj)));
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

    public void endVisit(JConditional x, Context ctx) {
      JExpression expression = x.getIfTest();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        if (booleanLiteral.getValue()) {
          // If true, replace myself with then expression
          ctx.replaceMe(x.getThenExpr());
        } else {
          // If false, replace myself with else expression
          ctx.replaceMe(x.getElseExpr());
        }
      }
    }

    /**
     * Convert do { } while (false); into a block.
     */
    public void endVisit(JDoStatement x, Context ctx) {
      JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

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
      JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

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
      JExpression expression = x.getIfExpr();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

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
     * Resolve method calls that can be computed statically.
     */
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (method.getEnclosingType() == program.getTypeJavaLangString()) {
        tryOptimizeStringCall(x, ctx, method);
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
     * 1) Remove catch blocks whose exception type is not instantiable. 2) Prune
     * try statements with no body. 3) Hoist up try statements with no catches
     * and an empty finally.
     */
    public void endVisit(JTryStatement x, Context ctx) {
      // 1) Remove catch blocks whose exception type is not instantiable.
      List catchArgs = x.getCatchArgs();
      List catchBlocks = x.getCatchBlocks();
      for (Iterator itA = catchArgs.iterator(), itB = catchBlocks.iterator(); itA.hasNext();) {
        JLocalRef localRef = (JLocalRef) itA.next();
        itB.next();
        JReferenceType type = (JReferenceType) localRef.getType();
        if (!program.typeOracle.isInstantiatedType(type)
            || type == program.getTypeNull()) {
          itA.remove();
          itB.remove();
        }
      }

      // Compute properties regarding the state of this try statement
      boolean noTry = x.getTryBlock().statements.isEmpty();
      // TODO: normalize finally block handling
      boolean noFinally = (x.getFinallyBlock() == null)
          || x.getFinallyBlock().statements.isEmpty();
      boolean noCatch = catchArgs.size() == 0;

      if (noTry) {
        // 2) Prune try statements with no body.
        removeMe(x, ctx);
      } else if (noCatch && noFinally) {
        // 3) Hoist up try statements with no catches and an empty finally.
        // If there's no catch or finally, there's no point in this even being
        // a try statement, replace myself with the try block
        ctx.replaceMe(x.getTryBlock());
      }
    }

    /**
     * Prune while (false) statements.
     */
    public void endVisit(JWhileStatement x, Context ctx) {
      JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, prune the while statement
        if (!booleanLiteral.getValue()) {
          removeMe(x, ctx);
        }
      }
    }

    private Class mapType(JType type) {
      return (Class) typeClassMap.get(type);
    }

    private void removeMe(JStatement stmt, Context ctx) {
      if (ctx.canRemove()) {
        ctx.removeMe();
      } else {
        // empty block statement
        ctx.replaceMe(new JBlock(program, stmt.getSourceInfo()));
      }
    }

    /**
     * Replace String methods having literal args with the static result.
     */
    private void tryOptimizeStringCall(JMethodCall x, Context ctx,
        JMethod method) {

      if (method.getType() == program.getTypeVoid()) {
        return;
      }

      int skip = 0;
      Object instance;
      if (program.isStaticImpl(method)) {
        // is it static implementation for instance method?
        method = program.staticImplFor(method);
        instance = tryTranslateLiteral((JExpression) x.getArgs().get(0),
            String.class);
        skip = 1;
      } else {
        // instance may be null
        instance = tryTranslateLiteral(x.getInstance(), String.class);
      }

      if (instance == null && !method.isStatic()) {
        return;
      }

      List params = method.getOriginalParamTypes();
      Class paramTypes[] = new Class[params.size()];
      Object paramValues[] = new Object[params.size()];
      ArrayList args = x.getArgs();
      for (int i = 0; i != params.size(); ++i) {
        paramTypes[i] = mapType((JType) params.get(i));
        if (paramTypes[i] == null) {
          return;
        }
        paramValues[i] = tryTranslateLiteral((JExpression) args.get(i + skip),
            paramTypes[i]);
        if (paramValues[i] == null) {
          return;
        }
      }

      try {
        Method actual = String.class.getMethod(method.getName(), paramTypes);
        if (actual == null) {
          return;
        }
        Object result = actual.invoke(instance, paramValues);
        if (result instanceof String) {
          ctx.replaceMe(program.getLiteralString((String) result));
        } else if (result instanceof Boolean) {
          ctx.replaceMe(program.getLiteralBoolean(((Boolean) result).booleanValue()));
        } else if (result instanceof Character) {
          ctx.replaceMe(program.getLiteralChar(((Character) result).charValue()));
        } else if (result instanceof Integer) {
          ctx.replaceMe(program.getLiteralInt(((Integer) result).intValue()));
        } else {
          boolean stopHere = true;
        }
      } catch (Exception e) {
        // If the call threw an exception, just don't optimize
        boolean stopHere = true;
      }
    }

    private Object tryTranslateLiteral(JExpression maybeLit, Class type) {
      if (!(maybeLit instanceof JValueLiteral)) {
        return null;
      }
      // TODO: make this way better by a mile
      if (type == boolean.class && maybeLit instanceof JBooleanLiteral) {
        return Boolean.valueOf(((JBooleanLiteral) maybeLit).getValue());
      }
      if (type == char.class && maybeLit instanceof JCharLiteral) {
        return new Character(((JCharLiteral) maybeLit).getValue());
      }
      if (type == double.class && maybeLit instanceof JDoubleLiteral) {
        return new Double(((JDoubleLiteral) maybeLit).getValue());
      }
      if (type == float.class && maybeLit instanceof JIntLiteral) {
        return new Float(((JIntLiteral) maybeLit).getValue());
      }
      if (type == int.class && maybeLit instanceof JIntLiteral) {
        return new Integer(((JIntLiteral) maybeLit).getValue());
      }
      if (type == long.class && maybeLit instanceof JLongLiteral) {
        return new Long(((JLongLiteral) maybeLit).getValue());
      }
      if (type == String.class && maybeLit instanceof JStringLiteral) {
        return ((JStringLiteral) maybeLit).getValue();
      }
      if (type == Object.class && maybeLit instanceof JValueLiteral) {
        return ((JValueLiteral) maybeLit).getValueObj();
      }
      return null;
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

  private final Map typeClassMap = new IdentityHashMap();

  public DeadCodeElimination(JProgram program) {
    this.program = program;
    typeClassMap.put(program.getTypeJavaLangObject(), Object.class);
    typeClassMap.put(program.getTypeJavaLangString(), String.class);
    typeClassMap.put(program.getTypePrimitiveBoolean(), boolean.class);
    typeClassMap.put(program.getTypePrimitiveByte(), byte.class);
    typeClassMap.put(program.getTypePrimitiveChar(), char.class);
    typeClassMap.put(program.getTypePrimitiveDouble(), double.class);
    typeClassMap.put(program.getTypePrimitiveFloat(), float.class);
    typeClassMap.put(program.getTypePrimitiveInt(), int.class);
    typeClassMap.put(program.getTypePrimitiveLong(), long.class);
    typeClassMap.put(program.getTypePrimitiveShort(), short.class);
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
