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

import com.google.gwt.dev.jjs.ast.CanBeAbstract;
import com.google.gwt.dev.jjs.ast.CanBeFinal;
import com.google.gwt.dev.jjs.ast.CanBeNative;
import com.google.gwt.dev.jjs.ast.CanBeStatic;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.HasType;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayLength;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JAssertStatement;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLabel;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNameOf;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JSeedIdOf;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsCastMap.JsQueryType;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.util.TextOutput;

import java.util.Iterator;

/**
 * Implements a reasonable toString() for all JNodes. The goal is to print a
 * recognizable declaration for large constructs (classes, methods) for easy use
 * in a debugger. Expressions and Statements should look like Java code
 * fragments.
 */
public class ToStringGenerationVisitor extends TextOutputVisitor {

  protected static final char[] CHARS_ABSTRACT = "abstract ".toCharArray();
  protected static final char[] CHARS_ASSERT = "assert ".toCharArray();
  protected static final char[] CHARS_BREAK = "break".toCharArray();
  protected static final char[] CHARS_CASE = "case ".toCharArray();
  protected static final char[] CHARS_CATCH = " catch ".toCharArray();
  protected static final char[] CHARS_CLASS = "class ".toCharArray();
  protected static final char[] CHARS_COMMA = ", ".toCharArray();
  protected static final char[] CHARS_CONTINUE = "continue".toCharArray();
  protected static final char[] CHARS_DEFAULT = "default".toCharArray();
  protected static final char[] CHARS_DO = "do".toCharArray();
  protected static final char[] CHARS_DOTCLASS = ".class".toCharArray();
  protected static final char[] CHARS_ELSE = "else".toCharArray();
  protected static final char[] CHARS_EMPTYDIMS = "[]".toCharArray();
  protected static final char[] CHARS_EXTENDS = "extends ".toCharArray();
  protected static final char[] CHARS_FALSE = "false".toCharArray();
  protected static final char[] CHARS_FINAL = "final ".toCharArray();
  protected static final char[] CHARS_FINALLY = " finally ".toCharArray();
  protected static final char[] CHARS_FOR = "for ".toCharArray();
  protected static final char[] CHARS_IF = "if ".toCharArray();
  protected static final char[] CHARS_IMPLEMENTS = "implements ".toCharArray();
  protected static final char[] CHARS_INSTANCEOF = " instanceof ".toCharArray();
  protected static final char[] CHARS_INTERFACE = "interface ".toCharArray();
  protected static final char[] CHARS_NAMEOF = " JNameOf ".toCharArray();
  protected static final char[] CHARS_NATIVE = "native ".toCharArray();
  protected static final char[] CHARS_NEW = "new ".toCharArray();
  protected static final char[] CHARS_NULL = "null".toCharArray();
  protected static final char[] CHARS_PRIVATE = "private ".toCharArray();
  protected static final char[] CHARS_PROTECTED = "protected ".toCharArray();
  protected static final char[] CHARS_PUBLIC = "public ".toCharArray();
  protected static final char[] CHARS_RETURN = "return".toCharArray();
  protected static final char[] CHARS_SEEDIDOF = " JSeedIdOf ".toCharArray();
  protected static final char[] CHARS_SLASHSTAR = "/*".toCharArray();
  protected static final char[] CHARS_STARSLASH = "*/".toCharArray();
  protected static final char[] CHARS_STATIC = "static ".toCharArray();
  protected static final char[] CHARS_SUPER = "super".toCharArray();
  protected static final char[] CHARS_SWITCH = "switch ".toCharArray();
  protected static final char[] CHARS_THIS = "this".toCharArray();
  protected static final char[] CHARS_THROW = "throw".toCharArray();
  protected static final char[] CHARS_THROWS = " throws ".toCharArray();
  protected static final char[] CHARS_TRUE = "true".toCharArray();
  protected static final char[] CHARS_TRY = "try ".toCharArray();
  protected static final char[] CHARS_WHILE = "while ".toCharArray();

  private boolean needSemi = true;

  private boolean suppressType = false;

  public ToStringGenerationVisitor(TextOutput textOutput) {
    super(textOutput);
  }

  @Override
  public boolean visit(JAbsentArrayDimension x, Context ctx) {
    // nothing to print, parent prints []
    return false;
  }

  @Override
  public boolean visit(JArrayLength x, Context ctx) {
    JExpression instance = x.getInstance();
    parenPush(x, instance);
    accept(instance);
    parenPop(x, instance);
    print(".length");
    return false;
  }

  @Override
  public boolean visit(JArrayRef x, Context ctx) {
    JExpression instance = x.getInstance();
    parenPush(x, instance);
    accept(instance);
    parenPop(x, instance);
    print('[');
    accept(x.getIndexExpr());
    print(']');
    return false;
  }

  @Override
  public boolean visit(JArrayType x, Context ctx) {
    accept(x.getElementType());
    print("[]");
    return false;
  }

  @Override
  public boolean visit(JAssertStatement x, Context ctx) {
    print(CHARS_ASSERT);
    accept(x.getTestExpr());
    if (x.getArg() != null) {
      print(" : ");
      accept(x.getArg());
    }
    return false;
  }

  @Override
  public boolean visit(JBinaryOperation x, Context ctx) {
    // TODO(later): associativity
    JExpression arg1 = x.getLhs();
    parenPush(x, arg1);
    accept(arg1);
    parenPop(x, arg1);

    space();
    print(x.getOp().getSymbol());
    space();

    JExpression arg2 = x.getRhs();
    parenPush(x, arg2);
    accept(arg2);
    parenPop(x, arg2);

    return false;
  }

  @Override
  public boolean visit(JBlock x, Context ctx) {
    openBlock();
    for (int i = 0; i < x.getStatements().size(); ++i) {
      JStatement statement = x.getStatements().get(i);
      needSemi = true;
      accept(statement);
      if (needSemi) {
        semi();
      }
      newline();
    }
    closeBlock();
    needSemi = false;
    return false;
  }

  @Override
  public boolean visit(JBooleanLiteral x, Context ctx) {
    printBooleanLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JBreakStatement x, Context ctx) {
    print(CHARS_BREAK);
    if (x.getLabel() != null) {
      space();
      accept(x.getLabel());
    }
    return false;
  }

  @Override
  public boolean visit(JCaseStatement x, Context ctx) {
    if (x.getExpr() != null) {
      print(CHARS_CASE);
      accept(x.getExpr());
    } else {
      print(CHARS_DEFAULT);
    }
    print(':');
    space();
    needSemi = false;
    return false;
  }

  @Override
  public boolean visit(JCastOperation x, Context ctx) {
    lparen();
    printType(x);
    rparen();
    space();

    JExpression expr = x.getExpr();
    parenPush(x, expr);
    accept(expr);
    parenPop(x, expr);
    return false;
  }

  @Override
  public boolean visit(JCharLiteral x, Context ctx) {
    printCharLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JClassLiteral x, Context ctx) {
    printTypeName(x.getRefType());
    print(CHARS_DOTCLASS);
    return false;
  }

  @Override
  public boolean visit(JClassType x, Context ctx) {
    printAbstractFlag(x);
    printFinalFlag(x);
    print(CHARS_CLASS);
    printTypeName(x);
    space();
    if (x.getSuperClass() != null) {
      print(CHARS_EXTENDS);
      printTypeName(x.getSuperClass());
      space();
    }

    if (x.getImplements().size() > 0) {
      print(CHARS_IMPLEMENTS);
      for (int i = 0, c = x.getImplements().size(); i < c; ++i) {
        if (i > 0) {
          print(CHARS_COMMA);
        }
        printTypeName(x.getImplements().get(i));
      }
      space();
    }

    return false;
  }

  @Override
  public boolean visit(JConditional x, Context ctx) {
    // TODO(later): associativity
    JExpression ifTest = x.getIfTest();
    parenPush(x, ifTest);
    accept(ifTest);
    parenPop(x, ifTest);

    print(" ? ");

    JExpression thenExpr = x.getThenExpr();
    parenPush(x, thenExpr);
    accept(thenExpr);
    parenPop(x, thenExpr);

    print(" : ");

    JExpression elseExpr = x.getElseExpr();
    parenPush(x, elseExpr);
    accept(elseExpr);
    parenPop(x, elseExpr);

    return false;
  }

  @Override
  public boolean visit(JConstructor x, Context ctx) {
    // Modifiers
    if (x.isPrivate()) {
      print(CHARS_PRIVATE);
    } else {
      print(CHARS_PUBLIC);
    }
    printName(x);

    // Parameters
    printParameterList(x);

    if (x.isAbstract() || !shouldPrintMethodBody()) {
      semi();
      newlineOpt();
    } else {
      accept(x.getBody());
    }

    return false;
  }

  @Override
  public boolean visit(JContinueStatement x, Context ctx) {
    print(CHARS_CONTINUE);
    if (x.getLabel() != null) {
      space();
      accept(x.getLabel());
    }
    return false;
  }

  @Override
  public boolean visit(JDeclarationStatement x, Context ctx) {
    if (!suppressType) {
      accept(x.getVariableRef().getTarget());
    } else {
      accept(x.getVariableRef());
    }
    JExpression initializer = x.getInitializer();
    if (initializer != null) {
      print(" = ");
      accept(initializer);
    }
    return false;
  }

  @Override
  public boolean visit(JDoStatement x, Context ctx) {
    print(CHARS_DO);
    if (x.getBody() != null) {
      nestedStatementPush(x.getBody());
      accept(x.getBody());
      nestedStatementPop(x.getBody());
    }
    if (needSemi) {
      semi();
      newline();
    } else {
      space();
      needSemi = true;
    }
    print(CHARS_WHILE);
    lparen();
    accept(x.getTestExpr());
    rparen();
    return false;
  }

  @Override
  public boolean visit(JDoubleLiteral x, Context ctx) {
    printDoubleLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JExpressionStatement x, Context ctx) {
    accept(x.getExpr());
    return false;
  }

  @Override
  public boolean visit(JField x, Context ctx) {
    printFinalFlag(x);
    printStaticFlag(x);
    printType(x);
    space();
    printName(x);
    return false;
  }

  @Override
  public boolean visit(JFieldRef x, Context ctx) {
    JExpression instance = x.getInstance();
    if (instance != null) {
      parenPush(x, instance);
      accept(instance);
      parenPop(x, instance);
    } else {
      printTypeName(x.getField().getEnclosingType());
    }
    print('.');
    printName(x.getField());
    return false;
  }

  @Override
  public boolean visit(JFloatLiteral x, Context ctx) {
    printFloatLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JForStatement x, Context ctx) {
    print(CHARS_FOR);
    lparen();

    Iterator<JStatement> iter = x.getInitializers().iterator();
    if (iter.hasNext()) {
      JStatement stmt = iter.next();
      accept(stmt);
    }
    suppressType = true;
    while (iter.hasNext()) {
      print(CHARS_COMMA);
      JStatement stmt = iter.next();
      accept(stmt);
    }
    suppressType = false;

    semi();
    space();
    if (x.getTestExpr() != null) {
      accept(x.getTestExpr());
    }

    semi();
    space();
    visitCollectionWithCommas(x.getIncrements().iterator());
    rparen();

    if (x.getBody() != null) {
      nestedStatementPush(x.getBody());
      accept(x.getBody());
      nestedStatementPop(x.getBody());
    }
    return false;
  }

  @Override
  public boolean visit(JGwtCreate x, Context ctx) {
    print("GWT.create(");
    print(x.getSourceType());
    print(".class)");
    return false;
  }

  @Override
  public boolean visit(JIfStatement x, Context ctx) {
    print(CHARS_IF);
    lparen();
    accept(x.getIfExpr());
    rparen();

    if (x.getThenStmt() != null) {
      nestedStatementPush(x.getThenStmt());
      accept(x.getThenStmt());
      nestedStatementPop(x.getThenStmt());
    }

    if (x.getElseStmt() != null) {
      if (needSemi) {
        semi();
        newline();
      } else {
        space();
        needSemi = true;
      }
      print(CHARS_ELSE);
      boolean elseIf = x.getElseStmt() instanceof JIfStatement;
      if (!elseIf) {
        nestedStatementPush(x.getElseStmt());
      } else {
        space();
      }
      accept(x.getElseStmt());
      if (!elseIf) {
        nestedStatementPop(x.getElseStmt());
      }
    }

    return false;
  }

  @Override
  public boolean visit(JInstanceOf x, Context ctx) {
    JExpression expr = x.getExpr();
    parenPush(x, expr);
    accept(expr);
    parenPop(x, expr);
    print(CHARS_INSTANCEOF);
    printTypeName(x.getTestType());
    return false;
  }

  @Override
  public boolean visit(JInterfaceType x, Context ctx) {
    print(CHARS_INTERFACE);
    printTypeName(x);
    space();

    if (x.getImplements().size() > 0) {
      print(CHARS_EXTENDS);
      for (int i = 0, c = x.getImplements().size(); i < c; ++i) {
        if (i > 0) {
          print(CHARS_COMMA);
        }
        printTypeName(x.getImplements().get(i));
      }
      space();
    }

    return false;
  }

  @Override
  public boolean visit(JIntLiteral x, Context ctx) {
    print(Integer.toString(x.getValue()));
    return false;
  }

  @Override
  public boolean visit(JLabel x, Context ctx) {
    printName(x);
    return false;
  }

  @Override
  public boolean visit(JLabeledStatement x, Context ctx) {
    accept(x.getLabel());
    print(" : ");
    accept(x.getBody());
    return false;
  }

  @Override
  public boolean visit(JLocal x, Context ctx) {
    printFinalFlag(x);
    printType(x);
    space();
    printName(x);
    return false;
  }

  @Override
  public boolean visit(JLocalRef x, Context ctx) {
    printName(x.getLocal());
    return false;
  }

  @Override
  public boolean visit(JLongLiteral x, Context ctx) {
    printLongLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JMethod x, Context ctx) {
    printMethodHeader(x);

    if (x.isAbstract() || !shouldPrintMethodBody()) {
      semi();
      newlineOpt();
    } else {
      accept(x.getBody());
    }

    return false;
  }

  @Override
  public boolean visit(JMethodBody x, Context ctx) {
    accept(x.getBlock());
    return false;
  }

  @Override
  public boolean visit(JMethodCall x, Context ctx) {
    JExpression instance = x.getInstance();
    JMethod target = x.getTarget();
    if (instance == null) {
      // Static call.
      printTypeName(target.getEnclosingType());
      print('.');
      printName(target);
    } else if (x.isStaticDispatchOnly()) {
      // super() or this() call.
      JReferenceType thisType = (JReferenceType) x.getInstance().getType();
      thisType = thisType.getUnderlyingType();
      if (thisType == target.getEnclosingType()) {
        print(CHARS_THIS);
      } else {
        print(CHARS_SUPER);
      }
    } else {
      // Instance call.
      parenPush(x, instance);
      accept(instance);
      parenPop(x, instance);
      print('.');
      printName(target);
    }
    lparen();
    visitCollectionWithCommas(x.getArgs().iterator());
    rparen();
    return false;
  }

  @Override
  public boolean visit(JMultiExpression x, Context ctx) {
    lparen();
    visitCollectionWithCommas(x.exprs.iterator());
    rparen();
    return false;
  }

  @Override
  public boolean visit(JNameOf x, Context ctx) {
    print(CHARS_SLASHSTAR);
    print(x instanceof JSeedIdOf ? CHARS_SEEDIDOF : CHARS_NAMEOF);
    print(CHARS_STARSLASH);
    printStringLiteral(x.getNode().getName());
    return false;
  }

  @Override
  public boolean visit(JsQueryType x, Context ctx) {
    print(CHARS_SLASHSTAR);
    printTypeName(x.getQueryType());
    print(CHARS_STARSLASH);
    return super.visit(x, ctx);
  }

  @Override
  public boolean visit(JNewArray x, Context ctx) {
    print(CHARS_NEW);
    printTypeName(x.getArrayType());
    if (x.initializers != null) {
      print(" {");
      visitCollectionWithCommas(x.initializers.iterator());
      print('}');
    } else {
      for (int i = 0; i < x.dims.size(); ++i) {
        JExpression expr = x.dims.get(i);
        print('[');
        accept(expr);
        print(']');
      }
    }
    return false;
  }

  @Override
  public boolean visit(JNewInstance x, Context ctx) {
    print(CHARS_NEW);
    JConstructor target = x.getTarget();
    printName(target);
    lparen();
    visitCollectionWithCommas(x.getArgs().iterator());
    rparen();
    return false;
  }

  @Override
  public boolean visit(JNullLiteral x, Context ctx) {
    print(CHARS_NULL);
    return false;
  }

  @Override
  public boolean visit(JNullType x, Context ctx) {
    printTypeName(x);
    return false;
  }

  @Override
  public boolean visit(JParameter x, Context ctx) {
    printType(x);
    space();
    printName(x);
    return false;
  }

  @Override
  public boolean visit(JParameterRef x, Context ctx) {
    printName(x.getTarget());
    return false;
  }

  @Override
  public boolean visit(JPostfixOperation x, Context ctx) {
    // TODO(later): associativity
    JExpression arg = x.getArg();
    parenPush(x, arg);
    accept(arg);
    parenPop(x, arg);
    print(x.getOp().getSymbol());
    return false;
  }

  @Override
  public boolean visit(JPrefixOperation x, Context ctx) {
    // TODO(later): associativity
    print(x.getOp().getSymbol());
    JExpression arg = x.getArg();
    parenPush(x, arg);
    accept(arg);
    parenPop(x, arg);
    return false;
  }

  @Override
  public boolean visit(JPrimitiveType x, Context ctx) {
    printTypeName(x);
    return false;
  }

  @Override
  public boolean visit(JProgram x, Context ctx) {
    print("<JProgram>");
    return false;
  }

  @Override
  public boolean visit(JReboundEntryPoint x, Context ctx) {
    print("<JReboundEntryPoint>");
    print(x.getSourceType());
    return false;
  }

  @Override
  public boolean visit(JReturnStatement x, Context ctx) {
    print(CHARS_RETURN);
    if (x.getExpr() != null) {
      space();
      accept(x.getExpr());
    }
    return false;
  }

  @Override
  public boolean visit(JsniFieldRef x, Context ctx) {
    return visit(x.getField(), ctx);
  }

  @Override
  public boolean visit(final JsniMethodBody x, Context ctx) {
    print(" /*-");
    new JsSourceGenerationVisitor(this) {
      {
        printJsBlock(x.getFunc().getBody(), false, false);
      }
    };
    print("-*/");
    semi();
    return false;
  }

  @Override
  public boolean visit(JsniMethodRef x, Context ctx) {
    printMethodHeader(x.getTarget());
    return false;
  }

  @Override
  public boolean visit(JsonArray x, Context ctx) {
    print('[');
    visitCollectionWithCommas(x.getExprs().iterator());
    print(']');
    return false;
  }

  @Override
  public boolean visit(JsonObject x, Context ctx) {
    print('{');
    visitCollectionWithCommas(x.propInits.iterator());
    print('}');
    return false;
  }

  @Override
  public boolean visit(JsonPropInit x, Context ctx) {
    accept(x.labelExpr);
    print(':');
    accept(x.valueExpr);
    return false;
  }

  @Override
  public boolean visit(JStringLiteral x, Context ctx) {
    printStringLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JSwitchStatement x, Context ctx) {
    print(CHARS_SWITCH);
    lparen();
    accept(x.getExpr());
    rparen();
    space();
    nestedStatementPush(x.getBody());
    accept(x.getBody());
    nestedStatementPop(x.getBody());
    return false;
  }

  @Override
  public boolean visit(JThisRef x, Context ctx) {
    print(CHARS_THIS);
    return false;
  }

  @Override
  public boolean visit(JThrowStatement x, Context ctx) {
    print(CHARS_THROW);
    if (x.getExpr() != null) {
      space();
      accept(x.getExpr());
    }
    return false;
  }

  @Override
  public boolean visit(JTryStatement x, Context ctx) {
    print(CHARS_TRY);
    accept(x.getTryBlock());
    for (int i = 0, c = x.getCatchArgs().size(); i < c; ++i) {
      print(CHARS_CATCH);
      lparen();
      JLocalRef localRef = x.getCatchArgs().get(i);
      accept(localRef.getTarget());
      rparen();
      space();
      JBlock block = x.getCatchBlocks().get(i);
      accept(block);
    }
    if (x.getFinallyBlock() != null) {
      print(CHARS_FINALLY);
      accept(x.getFinallyBlock());
    }
    return false;
  }

  @Override
  public boolean visit(JWhileStatement x, Context ctx) {
    print(CHARS_WHILE);
    lparen();
    accept(x.getTestExpr());
    rparen();
    if (x.getBody() != null) {
      nestedStatementPush(x.getBody());
      accept(x.getBody());
      nestedStatementPop(x.getBody());
    }
    return false;
  }

  protected void closeBlock() {
    indentOut();
    print('}');
  }

  protected void lparen() {
    print('(');
  }

  protected boolean nestedStatementPop(JStatement statement) {
    boolean pop = !(statement instanceof JBlock);
    if (pop) {
      indentOut();
    }
    return pop;
  }

  protected boolean nestedStatementPush(JStatement statement) {
    boolean push = !(statement instanceof JBlock);
    if (push) {
      indentIn();
      newline();
    } else {
      space();
    }
    return push;
  }

  protected void openBlock() {
    print('{');
    indentIn();
    newline();
  }

  protected boolean parenPop(int parentPrec, JExpression child) {
    int childPrec = JavaPrecedenceVisitor.exec(child);
    if (parentPrec < childPrec) {
      rparen();
      return true;
    } else {
      return false;
    }
  }

  protected boolean parenPop(JExpression parent, JExpression child) {
    return parenPop(JavaPrecedenceVisitor.exec(parent), child);
  }

  protected boolean parenPush(int parentPrec, JExpression child) {
    int childPrec = JavaPrecedenceVisitor.exec(child);
    if (parentPrec < childPrec) {
      lparen();
      return true;
    } else {
      return false;
    }
  }

  protected boolean parenPush(JExpression parent, JExpression child) {
    return parenPush(JavaPrecedenceVisitor.exec(parent), child);
  }

  protected void printAbstractFlag(CanBeAbstract x) {
    if (x.isAbstract()) {
      print(CHARS_ABSTRACT);
    }
  }

  protected void printBooleanLiteral(boolean value) {
    print(value ? CHARS_TRUE : CHARS_FALSE);
  }

  protected void printChar(char c) {
    switch (c) {
      case '\b':
        print("\\b");
        break;
      case '\t':
        print("\\t");
        break;
      case '\n':
        print("\\n");
        break;
      case '\f':
        print("\\f");
        break;
      case '\r':
        print("\\r");
        break;
      case '\"':
        print("\\\"");
        break;
      case '\'':
        print("\\'");
        break;
      case '\\':
        print("\\\\");
        break;
      default:
        if (Character.isISOControl(c)) {
          print("\\u");
          if (c < 0x1000) {
            print('0');
          }

          if (c < 0x100) {
            print('0');
          }

          if (c < 0x10) {
            print('0');
          }
          print(Integer.toHexString(c));
        } else {
          print(c);
        }
    }
  }

  protected void printCharLiteral(char value) {
    print('\'');
    printChar(value);
    print('\'');
  }

  protected void printDoubleLiteral(double value) {
    print(Double.toString(value));
  }

  protected void printFinalFlag(CanBeFinal x) {
    if (x.isFinal()) {
      print(CHARS_FINAL);
    }
  }

  protected void printFloatLiteral(float value) {
    print(Float.toString(value));
    print('f');
  }

  protected void printLongLiteral(long value) {
    print(Long.toString(value));
    print('L');
  }

  protected void printMethodHeader(JMethod x) {
    // Modifiers
    switch (x.getAccess()) {
      case PUBLIC:
        print(CHARS_PUBLIC);
        break;
      case PROTECTED:
        print(CHARS_PROTECTED);
        break;
      case PRIVATE:
        print(CHARS_PRIVATE);
        break;
      case DEFAULT:
        break;
    }
    printStaticFlag(x);
    printAbstractFlag(x);
    printNativeFlag(x);
    printFinalFlag(x);
    printType(x);
    space();
    printName(x);

    // Parameters
    printParameterList(x);
  }

  protected void printName(HasName x) {
    print(x.getName());
  }

  protected void printNativeFlag(CanBeNative x) {
    if (x.isNative()) {
      print(CHARS_NATIVE);
    }
  }

  protected void printParameterList(JMethod x) {
    lparen();
    visitCollectionWithCommas(x.getParams().iterator());
    rparen();
  }

  protected void printStaticFlag(CanBeStatic x) {
    if (x.isStatic()) {
      print(CHARS_STATIC);
    }
  }

  protected void printStringLiteral(String string) {
    char[] s = string.toCharArray();
    print('\"');
    for (int i = 0; i < s.length; ++i) {
      printChar(s[i]);
    }
    print('\"');
  }

  protected void printType(HasType hasType) {
    printTypeName(hasType.getType());
  }

  protected void printTypeName(JType type) {
    if (type instanceof JReferenceType) {
      print(((JReferenceType) type).getShortName());
    } else {
      print(type.getName());
    }
  }

  protected void rparen() {
    print(')');
  }

  protected void semi() {
    print(';');
  }

  protected boolean shouldPrintMethodBody() {
    return false;
  }

  protected void space() {
    print(' ');
  }

  protected void visitCollectionWithCommas(Iterator<? extends JNode> iter) {
    if (iter.hasNext()) {
      accept(iter.next());
    }
    while (iter.hasNext()) {
      print(CHARS_COMMA);
      accept(iter.next());
    }
  }
}
