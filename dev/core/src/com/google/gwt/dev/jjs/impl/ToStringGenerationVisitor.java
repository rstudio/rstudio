// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.CanBeAbstract;
import com.google.gwt.dev.jjs.ast.CanBeFinal;
import com.google.gwt.dev.jjs.ast.CanBeNative;
import com.google.gwt.dev.jjs.ast.CanBeStatic;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.HasType;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
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
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLabel;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
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
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;
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
  protected static final char[] CHARS_NATIVE = "native ".toCharArray();
  protected static final char[] CHARS_NEW = "new ".toCharArray();
  protected static final char[] CHARS_NULL = "null".toCharArray();
  protected static final char[] CHARS_PRIVATE = "private ".toCharArray();
  protected static final char[] CHARS_PROTECTED = "protected ".toCharArray();
  protected static final char[] CHARS_PUBLIC = "public ".toCharArray();
  protected static final char[] CHARS_RETURN = "return".toCharArray();
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

  public ToStringGenerationVisitor(TextOutput textOutput) {
    super(textOutput);
  }

  // @Override
  public boolean visit(JAbsentArrayDimension x, Mutator h) {
    // nothing to print, parent prints []
    return false;
  }

  // @Override
  public boolean visit(JArrayRef x, Mutator h) {
    JExpression instance = x.getInstance();
    parenPush(x, instance);
    instance.traverse(this);
    parenPop(x, instance);
    print('[');
    x.getIndexExpr().traverse(this);
    print(']');
    return false;
  }

  // @Override
  public boolean visit(JArrayType x) {
    x.leafType.traverse(this);
    for (int i = 0, c = x.dims; i < c; ++i) {
      print("[]");
    }
    return false;
  }

  // @Override
  public boolean visit(JAssertStatement x) {
    print(CHARS_ASSERT);
    x.getTestExpr().traverse(this);
    if (x.getArg() != null) {
      print(" : ");
      x.getArg().traverse(this);
    }
    return false;
  }

  // @Override
  public boolean visit(JBinaryOperation x, Mutator h) {
    // TODO(later): associativity
    JExpression arg1 = x.getLhs();
    parenPush(x, arg1);
    arg1.traverse(this);
    parenPop(x, arg1);

    space();
    print(x.op.getSymbol());
    space();

    JExpression arg2 = x.getRhs();
    parenPush(x, arg2);
    arg2.traverse(this);
    parenPop(x, arg2);

    return false;
  }

  // @Override
  public boolean visit(JBlock x) {
    openBlock();
    for (int i = 0; i < x.statements.size(); ++i) {
      JStatement statement = (JStatement) x.statements.get(i);
      fNeedSemi = true;
      statement.traverse(this);
      if (fNeedSemi) {
        semi();
      }
      newline();
    }
    closeBlock();
    fNeedSemi = false;
    return false;
  }

  // @Override
  public boolean visit(JBooleanLiteral x, Mutator h) {
    printBooleanLiteral(x.value);
    return false;
  }

  // @Override
  public boolean visit(JBreakStatement x) {
    print(CHARS_BREAK);
    if (x.label != null) {
      space();
      x.label.traverse(this);
    }
    return false;
  }

  // @Override
  public boolean visit(JCaseStatement x) {
    if (x.getExpression() != null) {
      print(CHARS_CASE);
      x.getExpression().traverse(this);
    } else {
      print(CHARS_DEFAULT);
    }
    print(':');
    space();
    fNeedSemi = false;
    return false;
  }

  // @Override
  public boolean visit(JCastOperation x, Mutator h) {
    lparen();
    printType(x);
    rparen();
    space();

    JExpression expr = x.getExpression();
    parenPush(x, expr);
    expr.traverse(this);
    parenPop(x, expr);
    return false;
  }

  // @Override
  public boolean visit(JCharLiteral x, Mutator h) {
    printCharLiteral(x.value);
    return false;
  }

  // @Override
  public boolean visit(JClassLiteral x, Mutator h) {
    printTypeName(x.refType);
    print(CHARS_DOTCLASS);
    return false;
  }

  // @Override
  public boolean visit(JClassType x) {
    printAbstractFlag(x);
    printFinalFlag(x);
    print(CHARS_CLASS);
    printTypeName(x);
    space();
    if (x.extnds != null) {
      print(CHARS_EXTENDS);
      printTypeName(x.extnds);
      space();
    }

    if (x.implments.size() > 0) {
      print(CHARS_IMPLEMENTS);
      for (int i = 0, c = x.implments.size(); i < c; ++i) {
        if (i > 0) {
          print(CHARS_COMMA);
        }
        printTypeName((JType) x.implments.get(i));
      }
      space();
    }

    return false;
  }

  // @Override
  public boolean visit(JConditional x, Mutator h) {
    // TODO(later): associativity
    JExpression ifTest = x.getIfTest();
    parenPush(x, ifTest);
    ifTest.traverse(this);
    parenPop(x, ifTest);

    print(" ? ");

    JExpression thenExpr = x.getThenExpr();
    parenPush(x, thenExpr);
    thenExpr.traverse(this);
    parenPop(x, thenExpr);

    print(" : ");

    JExpression elseExpr = x.getElseExpr();
    parenPush(x, elseExpr);
    elseExpr.traverse(this);
    parenPop(x, elseExpr);

    return false;
  }

  // @Override
  public boolean visit(JContinueStatement x) {
    print(CHARS_CONTINUE);
    if (x.label != null) {
      space();
      x.label.traverse(this);
    }
    return false;
  }

  // @Override
  public boolean visit(JDoStatement x) {
    print(CHARS_DO);
    if (x.body != null) {
      nestedStatementPush(x.body);
      x.body.traverse(this);
      nestedStatementPop(x.body);
    }
    if (fNeedSemi) {
      semi();
      newline();
    } else {
      space();
      fNeedSemi = true;
    }
    print(CHARS_WHILE);
    lparen();
    x.getTestExpr().traverse(this);
    rparen();
    return false;
  }

  // @Override
  public boolean visit(JDoubleLiteral x, Mutator h) {
    printDoubleLiteral(x.value);
    return false;
  }

  // @Override
  public boolean visit(JExpressionStatement x) {
    x.getExpression().traverse(this);
    return false;
  }

  // @Override
  public boolean visit(JsniFieldRef x) {
    return visit(x.getField());
  }

  // @Override
  public boolean visit(JField x) {
    // Due to our wacky construction model, only constant fields may be final
    // when generating source
    if (x.constInitializer != null) {
      printFinalFlag(x);
    } else {
      printMemberFinalFlag(x);
    }
    
    printStaticFlag(x);
    printType(x);
    space();
    printUniqueName(x);
    return false;
  }

  // @Override
  public boolean visit(JFieldRef x, Mutator h) {
    JExpression instance = x.getInstance();
    if (instance != null) {
      parenPush(x, instance);
      instance.traverse(this);
      parenPop(x, instance);
    } else {
      printTypeName(x.field.enclosingType);
    }
    print('.');
    printUniqueName(x.field);
    return false;
  }

  // @Override
  public boolean visit(JFloatLiteral x, Mutator h) {
    printFloatLiteral(x.value);
    return false;
  }

  // @Override
  public boolean visit(JForStatement x) {
    print(CHARS_FOR);
    lparen();

    Iterator/* <JStatement> */iter = x.getInitializers().iterator();
    if (iter.hasNext()) {
      JStatement stmt = (JStatement) iter.next();
      stmt.traverse(this);
    }
    fSuppressType = true;
    while (iter.hasNext()) {
      print(CHARS_COMMA);
      JStatement stmt = (JStatement) iter.next();
      stmt.traverse(this);
    }
    fSuppressType = false;

    semi();
    space();
    if (x.getTestExpr() != null) {
      x.getTestExpr().traverse(this);
    }
    
    semi();
    space();
    visitCollectionWithCommas(x.getIncrements().iterator());
    rparen();

    if (x.body != null) {
      nestedStatementPush(x.body);
      x.body.traverse(this);
      nestedStatementPop(x.body);
    }
    return false;
  }

  // @Override
  public boolean visit(JIfStatement x) {
    print(CHARS_IF);
    lparen();
    x.getIfExpr().traverse(this);
    rparen();

    if (x.thenStmt != null) {
      nestedStatementPush(x.thenStmt);
      x.thenStmt.traverse(this);
      nestedStatementPop(x.thenStmt);
    }

    if (x.elseStmt != null) {
      if (fNeedSemi) {
        semi();
        newline();
      } else {
        space();
        fNeedSemi = true;
      }
      print(CHARS_ELSE);
      boolean elseIf = x.elseStmt instanceof JIfStatement;
      if (!elseIf) {
        nestedStatementPush(x.elseStmt);
      } else {
        space();
      }
      x.elseStmt.traverse(this);
      if (!elseIf) {
        nestedStatementPop(x.elseStmt);
      }
    }

    return false;
  }

  // @Override
  public boolean visit(JInstanceOf x, Mutator h) {
    JExpression expr = x.getExpression();
    parenPush(x, expr);
    expr.traverse(this);
    parenPop(x, expr);
    print(CHARS_INSTANCEOF);
    printTypeName(x.testType);
    return false;
  }

  // @Override
  public boolean visit(JInterfaceType x) {
    print(CHARS_INTERFACE);
    printTypeName(x);
    space();

    if (x.implments.size() > 0) {
      print(CHARS_EXTENDS);
      for (int i = 0, c = x.implments.size(); i < c; ++i) {
        if (i > 0) {
          print(CHARS_COMMA);
        }
        printTypeName((JType) x.implments.get(i));
      }
      space();
    }

    return false;
  }

  // @Override
  public boolean visit(JIntLiteral x, Mutator h) {
    print(Integer.toString(x.value).toCharArray());
    return false;
  }

  // @Override
  public boolean visit(JLabel x) {
    printName(x);
    return false;
  }

  // @Override
  public boolean visit(JLabeledStatement x) {
    x.label.traverse(this);
    print(" : ");
    x.body.traverse(this);
    return false;
  }

  // @Override
  public boolean visit(JLocal x) {
    printFinalFlag(x);
    printType(x);
    space();
    printName(x);
    return false;
  }

  // @Override
  public boolean visit(JLocalDeclarationStatement x) {
    if (!fSuppressType) {
      x.getLocalRef().getTarget().traverse(this);
    } else {
      x.getLocalRef().traverse(this);
    }
    JExpression initializer = x.getInitializer();
    if (initializer != null) {
      print(" = ");
      initializer.traverse(this);
    }
    return false;
  }

  // @Override
  public boolean visit(JLocalRef x, Mutator h) {
    printName(x.local);
    return false;
  }

  // @Override
  public boolean visit(JLongLiteral x, Mutator h) {
    printLongLiteral(x.value);
    return false;
  }

  // @Override
  public boolean visit(JMethod x) {
    return printMethodHeader(x);
  }

  // @Override
  public boolean visit(JMethodCall x, Mutator h) {
    JExpression instance = x.getInstance();
    JMethod target = x.getTarget();
    if (instance != null) {
      parenPush(x, instance);
      instance.traverse(this);
      parenPop(x, instance);
    } else {
      printTypeName(target.getEnclosingType());
    }
    print('.');
    if (target.isStatic()) {
      printUniqueName(target);
    } else {
      printName(target);
    }
    lparen();
    visitCollectionWithCommas(x.args.iterator());
    rparen();
    return false;
  }

  // @Override
  public boolean visit(JMultiExpression x, Mutator m) {
    lparen();
    visitCollectionWithCommas(x.exprs.iterator());
    rparen();
    return false;
  }

  // @Override
  public boolean visit(JsniMethod x) {
    return printMethodHeader(x);
  }

  // @Override
  public boolean visit(JsniMethodRef x) {
    return printMethodHeader(x.getTarget());
  }

  // @Override
  public boolean visit(JNewArray x, Mutator h) {
    print(CHARS_NEW);
    printTypeName(x.getArrayType().leafType);
    if (x.initializers != null) {
      print('{');
      visitCollectionWithCommas(x.initializers.iterator());
      print('}');
    } else {
      for (int i = 0; i < x.dims.size(); ++i) {
        JExpression expr = x.dims.getExpr(i);
        print('[');
        expr.traverse(this);
        print(']');
      }
    }
    return false;
  }

  // @Override
  public boolean visit(JNewInstance x, Mutator h) {
    print(CHARS_NEW);
    printType(x);
    lparen();
    rparen();
    return false;
  }

  // @Override
  public boolean visit(JNullLiteral x, Mutator h) {
    print(CHARS_NULL);
    return false;
  }

  // @Override
  public boolean visit(JNullType x) {
    printTypeName(x);
    return false;
  }

  // @Override
  public boolean visit(JParameter x) {
    printType(x);
    space();
    printName(x);
    return false;
  }

  // @Override
  public boolean visit(JParameterRef x, Mutator h) {
    printName(x.getTarget());
    return false;
  }

  // @Override
  public boolean visit(JPostfixOperation x, Mutator h) {
    // TODO(later): associativity
    JExpression arg = x.getArg();
    parenPush(x, arg);
    arg.traverse(this);
    parenPop(x, arg);
    print(x.op.getSymbol());
    return false;
  }

  // @Override
  public boolean visit(JPrefixOperation x, Mutator h) {
    // TODO(later): associativity
    print(x.op.getSymbol());
    JExpression arg = x.getArg();
    parenPush(x, arg);
    arg.traverse(this);
    parenPop(x, arg);
    return false;
  }

  // @Override
  public boolean visit(JPrimitiveType x) {
    printTypeName(x);
    return false;
  }

  // @Override
  public boolean visit(JProgram x) {
    print("<JProgram>");
    return false;
  }

  // @Override
  public boolean visit(JReturnStatement x) {
    print(CHARS_RETURN);
    if (x.getExpression() != null) {
      space();
      x.getExpression().traverse(this);
    }
    return false;
  }

  // @Override
  public boolean visit(JsonArray x, Mutator m) {
    print('[');
    visitCollectionWithCommas(x.exprs.iterator());
    print(']');
    return false;
  }

  // @Override
  public boolean visit(JsonObject x, Mutator m) {
    print('{');
    visitCollectionWithCommas(x.propInits.iterator());
    print('}');
    return false;
  }

  // @Override
  public boolean visit(JsonPropInit x) {
    x.labelExpr.traverse(this);
    print(':');
    x.valueExpr.traverse(this);
    return false;
  }

  // @Override
  public boolean visit(JStringLiteral x, Mutator h) {
    printStringLiteral(x.value);
    return false;
  }

  // @Override
  public boolean visit(JSwitchStatement x) {
    print(CHARS_SWITCH);
    lparen();
    x.getExpression().traverse(this);
    rparen();
    space();
    nestedStatementPush(x.body);
    x.body.traverse(this);
    nestedStatementPop(x.body);
    return false;
  }

  // @Override
  public boolean visit(JThisRef x, Mutator h) {
    print(CHARS_THIS);
    return false;
  }

  // @Override
  public boolean visit(JThrowStatement x) {
    print(CHARS_THROW);
    if (x.getExpression() != null) {
      space();
      x.getExpression().traverse(this);
    }
    return false;
  }

  // @Override
  public boolean visit(JTryStatement x) {
    print(CHARS_TRY);
    x.tryBlock.traverse(this);
    for (int i = 0, c = x.catchArgs.size(); i < c; ++i) {
      print(CHARS_CATCH);
      lparen();
      JLocalRef localRef = (JLocalRef) x.catchArgs.get(i);
      localRef.getTarget().traverse(this);
      rparen();
      space();
      JBlock block = (JBlock) x.catchBlocks.get(i);
      block.traverse(this);
    }
    if (x.finallyBlock != null) {
      print(CHARS_FINALLY);
      x.finallyBlock.traverse(this);
    }
    return false;
  }

  // @Override
  public boolean visit(JWhileStatement x) {
    print(CHARS_WHILE);
    lparen();
    x.getTestExpr().traverse(this);
    rparen();
    if (x.body != null) {
      nestedStatementPush(x.body);
      x.body.traverse(this);
      nestedStatementPop(x.body);
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

  protected void printMemberFinalFlag(CanBeFinal x) {
    if (x.isFinal()) {
      print(CHARS_FINAL);
    }
  }

  protected boolean printMethodHeader(JMethod x) {
    // Modifiers
    if (x.isPrivate()) {
      print(CHARS_PRIVATE);
    } else {
      print(CHARS_PUBLIC);
    }
    printStaticFlag(x);
    printAbstractFlag(x);
    printNativeFlag(x);
    printMemberFinalFlag(x);
    printType(x);
    space();
    if (x.isStatic()) {
      printUniqueName(x);
    } else {
      printName(x);
    }
    
    // Parameters
    printParameterList(x);

    if (x.thrownExceptions.size() > 0) {
      print(CHARS_THROWS);
      Iterator/* <JClassType> */iter = x.thrownExceptions.iterator();
      if (iter.hasNext()) {
        printTypeName((JType) iter.next());
      }
      while (iter.hasNext()) {
        print(CHARS_COMMA);
        printTypeName((JType) iter.next());
      }
    }
    return false;
  }

  protected void printFloatLiteral(float value) {
    print(Float.toString(value));
    print('f');
  }

  protected void printLongLiteral(long value) {
    print(Long.toString(value));
    print('L');
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
    visitCollectionWithCommas(x.params.iterator());
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

  protected void rparen() {
    print(')');
  }

  protected void semi() {
    print(';');
  }

  protected void space() {
    print(' ');
  }

  protected void visitCollectionWithCommas(Iterator/* <? extends JNode> */iter) {
    if (iter.hasNext()) {
      JNode node = (JNode) iter.next();
      node.traverse(this);
    }
    while (iter.hasNext()) {
      print(CHARS_COMMA);
      JNode node = (JNode) iter.next();
      node.traverse(this);
    }
  }

  protected void printTypeName(JType type) {
    if (type instanceof JReferenceType) {
      print(((JReferenceType) type).getShortName());
    } else {
      print(type.getName());
    }
  }

  private void printUniqueName(HasName x) {
    print(x.getName());
  }

  private boolean fNeedSemi = true;

  private boolean fSuppressType = false;

}
