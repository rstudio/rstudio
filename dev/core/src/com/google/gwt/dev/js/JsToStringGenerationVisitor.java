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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.HasName;
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDebugger;
import com.google.gwt.dev.js.ast.JsDecimalLiteral;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsIntegralLiteral;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsSwitchMember;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.util.TextOutput;

import java.util.Iterator;

/**
 * Produces text output from a JavaScript AST.
 */
public class JsToStringGenerationVisitor extends JsVisitor {

  private static final char[] CHARS_BREAK = "break".toCharArray();
  private static final char[] CHARS_CASE = "case".toCharArray();
  private static final char[] CHARS_CATCH = "catch".toCharArray();
  private static final char[] CHARS_CONTINUE = "continue".toCharArray();
  private static final char[] CHARS_DEBUGGER = "debugger".toCharArray();
  private static final char[] CHARS_DEFAULT = "default".toCharArray();
  private static final char[] CHARS_DO = "do".toCharArray();
  private static final char[] CHARS_ELSE = "else".toCharArray();
  private static final char[] CHARS_FALSE = "false".toCharArray();
  private static final char[] CHARS_FINALLY = "finally".toCharArray();
  private static final char[] CHARS_FOR = "for".toCharArray();
  private static final char[] CHARS_FUNCTION = "function".toCharArray();
  private static final char[] CHARS_IF = "if".toCharArray();
  private static final char[] CHARS_IN = "in".toCharArray();
  private static final char[] CHARS_NEW = "new".toCharArray();
  private static final char[] CHARS_NULL = "null".toCharArray();
  private static final char[] CHARS_RETURN = "return".toCharArray();
  private static final char[] CHARS_SWITCH = "switch".toCharArray();
  private static final char[] CHARS_THIS = "this".toCharArray();
  private static final char[] CHARS_THROW = "throw".toCharArray();
  private static final char[] CHARS_TRUE = "true".toCharArray();
  private static final char[] CHARS_TRY = "try".toCharArray();
  private static final char[] CHARS_VAR = "var".toCharArray();
  private static final char[] CHARS_WHILE = "while".toCharArray();
  private static final char[] HEX_DIGITS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
      'E', 'F'};

  /**
   * How many lines of code to print inside of a JsBlock when printing terse.
   */
  private static final int JSBLOCK_LINES_TO_PRINT = 3;

  protected boolean needSemi = true;
  private final TextOutput p;

  public JsToStringGenerationVisitor(TextOutput out) {
    this.p = out;
  }

  @Override
  public boolean visit(JsArrayAccess x, JsContext<JsExpression> ctx) {
    JsExpression arrayExpr = x.getArrayExpr();
    _parenPush(x, arrayExpr, false);
    accept(arrayExpr);
    _parenPop(x, arrayExpr, false);
    _lsquare();
    accept(x.getIndexExpr());
    _rsquare();
    return false;
  }

  @Override
  public boolean visit(JsArrayLiteral x, JsContext<JsExpression> ctx) {
    _lsquare();
    boolean sep = false;
    for (Object element : x.getExpressions()) {
      JsExpression arg = (JsExpression) element;
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      accept(arg);
      _parenPopIfCommaExpr(arg);
    }
    _rsquare();
    return false;
  }

  @Override
  public boolean visit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
    JsBinaryOperator op = x.getOperator();
    JsExpression arg1 = x.getArg1();
    _parenPush(x, arg1, !op.isLeftAssociative());
    accept(arg1);
    boolean needSpace = op.isKeyword() || arg1 instanceof JsPostfixOperation;
    if (needSpace) {
      _parenPopOrSpace(x, arg1, !op.isLeftAssociative());
    } else {
      _parenPop(x, arg1, !op.isLeftAssociative());
      _spaceOpt();
    }
    p.print(op.getSymbol());
    JsExpression arg2 = x.getArg2();
    needSpace = op.isKeyword() || arg2 instanceof JsPrefixOperation;
    if (needSpace) {
      _parenPushOrSpace(x, arg2, op.isLeftAssociative());
    } else {
      _spaceOpt();
      _parenPush(x, arg2, op.isLeftAssociative());
    }
    accept(arg2);
    _parenPop(x, arg2, op.isLeftAssociative());
    return false;
  }

  @Override
  public boolean visit(JsBlock x, JsContext<JsStatement> ctx) {
    printJsBlockOptionalTruncate(x, true);
    return false;
  }

  @Override
  public boolean visit(JsBooleanLiteral x, JsContext<JsExpression> ctx) {
    if (x.getValue()) {
      _true();
    } else {
      _false();
    }
    return false;
  }

  @Override
  public boolean visit(JsBreak x, JsContext<JsStatement> ctx) {
    _break();

    JsNameRef label = x.getLabel();
    if (label != null) {
      _space();
      _nameRef(label);
    }

    return false;
  }

  @Override
  public boolean visit(JsCase x, JsContext<JsSwitchMember> ctx) {
    _case();
    _space();
    accept(x.getCaseExpr());
    _colon();
    _newlineOpt();

    indent();
    for (Object element : x.getStmts()) {
      JsStatement stmt = (JsStatement) element;
      needSemi = true;
      accept(stmt);
      if (needSemi) {
        _semi();
      }
      _newlineOpt();
    }
    outdent();
    needSemi = false;
    return false;
  }

  @Override
  public boolean visit(JsCatch x, JsContext<JsCatch> ctx) {
    _spaceOpt();
    _catch();
    _spaceOpt();
    _lparen();
    _nameDef(x.getParameter().getName());

    // Optional catch condition.
    //
    JsExpression catchCond = x.getCondition();
    if (catchCond != null) {
      _space();
      _if();
      _space();
      accept(catchCond);
    }

    _rparen();
    _spaceOpt();
    accept(x.getBody());

    return false;
  }

  @Override
  public boolean visit(JsConditional x, JsContext<JsExpression> ctx) {
    // right associative
    {
      JsExpression testExpression = x.getTestExpression();
      _parenPush(x, testExpression, true);
      accept(testExpression);
      _parenPop(x, testExpression, true);
    }
    _questionMark();
    {
      JsExpression thenExpression = x.getThenExpression();
      _parenPush(x, thenExpression, true);
      accept(thenExpression);
      _parenPop(x, thenExpression, true);
    }
    _colon();
    {
      JsExpression elseExpression = x.getElseExpression();
      _parenPush(x, elseExpression, false);
      accept(elseExpression);
      _parenPop(x, elseExpression, false);
    }
    return false;
  }

  @Override
  public boolean visit(JsContinue x, JsContext<JsStatement> ctx) {
    _continue();

    JsNameRef label = x.getLabel();
    if (label != null) {
      _space();
      _nameRef(label);
    }

    return false;
  }

  @Override
  public boolean visit(JsDebugger x, JsContext<JsStatement> ctx) {
    _debugger();
    return false;
  }

  @Override
  public boolean visit(JsDecimalLiteral x, JsContext<JsExpression> ctx) {
    String s = x.getValue();
    // TODO: optimize this to only the cases that need it
    if (s.startsWith("-")) {
      _space();
    }
    p.print(s);
    return false;
  }

  @Override
  public boolean visit(JsDefault x, JsContext<JsSwitchMember> ctx) {
    _default();
    _colon();

    indent();
    for (Object element : x.getStmts()) {
      JsStatement stmt = (JsStatement) element;
      needSemi = true;
      accept(stmt);
      if (needSemi) {
        _semi();
      }
      _newlineOpt();
    }
    outdent();
    needSemi = false;
    return false;
  }

  @Override
  public boolean visit(JsDoWhile x, JsContext<JsStatement> ctx) {
    _do();
    _nestedPush(x.getBody(), true);
    accept(x.getBody());
    _nestedPop(x.getBody());
    if (needSemi) {
      _semi();
      _newlineOpt();
    } else {
      _spaceOpt();
      needSemi = true;
    }
    _while();
    _spaceOpt();
    _lparen();
    accept(x.getCondition());
    _rparen();
    return false;
  }

  @Override
  public boolean visit(JsEmpty x, JsContext<JsStatement> ctx) {
    return false;
  }

  @Override
  public boolean visit(JsExprStmt x, JsContext<JsStatement> ctx) {
    final JsExpression expr = x.getExpression();
    accept(expr);
    return false;
  }

  @Override
  public boolean visit(JsFor x, JsContext<JsStatement> ctx) {
    _for();
    _spaceOpt();
    _lparen();

    // The init expressions or var decl.
    //
    if (x.getInitExpr() != null) {
      accept(x.getInitExpr());
    } else if (x.getInitVars() != null) {
      accept(x.getInitVars());
    }

    _semi();

    // The loop test.
    //
    if (x.getCondition() != null) {
      _spaceOpt();
      accept(x.getCondition());
    }

    _semi();

    // The incr expression.
    //
    if (x.getIncrExpr() != null) {
      _spaceOpt();
      accept(x.getIncrExpr());
    }

    _rparen();
    _nestedPush(x.getBody(), false);
    accept(x.getBody());
    _nestedPop(x.getBody());
    return false;
  }

  @Override
  public boolean visit(JsForIn x, JsContext<JsStatement> ctx) {
    _for();
    _spaceOpt();
    _lparen();

    if (x.getIterVarName() != null) {
      _var();
      _space();
      _nameDef(x.getIterVarName());

      if (x.getIterExpr() != null) {
        _spaceOpt();
        _assignment();
        _spaceOpt();
        accept(x.getIterExpr());
      }
    } else {
      // Just a name ref.
      //
      accept(x.getIterExpr());
    }

    _space();
    _in();
    _space();
    accept(x.getObjExpr());

    _rparen();
    _nestedPush(x.getBody(), false);
    accept(x.getBody());
    _nestedPop(x.getBody());
    return false;
  }

  // function foo(a, b) {
  // stmts...
  // }
  //
  @Override
  public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
    _function();

    // Functions can be anonymous.
    //
    if (x.getName() != null) {
      _space();
      _nameOf(x);
    }

    _lparen();
    boolean sep = false;
    for (Object element : x.getParameters()) {
      JsParameter param = (JsParameter) element;
      sep = _sepCommaOptSpace(sep);
      accept(param);
    }
    _rparen();

    accept(x.getBody());
    needSemi = true;
    return false;
  }

  @Override
  public boolean visit(JsIf x, JsContext<JsStatement> ctx) {
    _if();
    _spaceOpt();
    _lparen();
    accept(x.getIfExpr());
    _rparen();
    JsStatement thenStmt = x.getThenStmt();
    _nestedPush(thenStmt, false);
    accept(thenStmt);
    _nestedPop(thenStmt);
    JsStatement elseStmt = x.getElseStmt();
    if (elseStmt != null) {
      if (needSemi) {
        _semi();
        _newlineOpt();
      } else {
        _spaceOpt();
        needSemi = true;
      }
      _else();
      boolean elseIf = elseStmt instanceof JsIf;
      if (!elseIf) {
        _nestedPush(elseStmt, true);
      } else {
        _space();
      }
      accept(elseStmt);
      if (!elseIf) {
        _nestedPop(elseStmt);
      }
    }
    return false;
  }

  @Override
  public boolean visit(JsIntegralLiteral x, JsContext<JsExpression> ctx) {
    String s = x.getValue().toString();
    boolean needParens = s.startsWith("-");
    if (needParens) {
      _lparen();
    }
    p.print(s);
    if (needParens) {
      _rparen();
    }
    return false;
  }

  @Override
  public boolean visit(JsInvocation x, JsContext<JsExpression> ctx) {
    accept(x.getQualifier());

    _lparen();
    boolean sep = false;
    for (Object element : x.getArguments()) {
      JsExpression arg = (JsExpression) element;
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      accept(arg);
      _parenPopIfCommaExpr(arg);
    }
    _rparen();
    return false;
  }

  @Override
  public boolean visit(JsLabel x, JsContext<JsStatement> ctx) {
    _nameOf(x);
    _colon();
    _spaceOpt();
    accept(x.getStmt());
    return false;
  }

  @Override
  public boolean visit(JsNameRef x, JsContext<JsExpression> ctx) {
    JsExpression q = x.getQualifier();
    if (q != null) {
      _parenPush(x, q, false);
      accept(q);
      _parenPop(x, q, false);
      _dot();
    }
    _nameRef(x);
    return false;
  }

  @Override
  public boolean visit(JsNew x, JsContext<JsExpression> ctx) {
    _new();
    _space();

    JsExpression ctorExpr = x.getConstructorExpression();
    _parenPush(x, ctorExpr, true);
    accept(ctorExpr);
    _parenPop(x, ctorExpr, true);

    _lparen();
    boolean sep = false;
    for (Object element : x.getArguments()) {
      JsExpression arg = (JsExpression) element;
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      accept(arg);
      _parenPopIfCommaExpr(arg);
    }
    _rparen();

    return false;
  }

  @Override
  public boolean visit(JsNullLiteral x, JsContext<JsExpression> ctx) {
    _null();
    return false;
  }

  @Override
  public boolean visit(JsObjectLiteral x, JsContext<JsExpression> ctx) {
    _lbrace();
    boolean sep = false;
    for (Object element : x.getPropertyInitializers()) {
      sep = _sepCommaOptSpace(sep);
      JsPropertyInitializer propInit = (JsPropertyInitializer) element;
      JsExpression labelExpr = propInit.getLabelExpr();
      _parenPushIfConditional(labelExpr);
      accept(labelExpr);
      _parenPopIfConditional(labelExpr);
      _colon();
      JsExpression valueExpr = propInit.getValueExpr();
      _parenPushIfConditional(valueExpr);
      accept(valueExpr);
      _parenPopIfConditional(valueExpr);
    }
    _rbrace();
    return false;
  }

  @Override
  public boolean visit(JsParameter x, JsContext<JsParameter> ctx) {
    _nameOf(x);
    return false;
  }

  @Override
  public boolean visit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
    JsUnaryOperator op = x.getOperator();
    JsExpression arg = x.getArg();
    // unary operators always associate correctly (I think)
    _parenPush(x, arg, true);
    accept(arg);
    _parenPop(x, arg, true);
    p.print(op.getSymbol());
    return false;
  }

  @Override
  public boolean visit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
    JsUnaryOperator op = x.getOperator();
    p.print(op.getSymbol());
    if (op.isKeyword()) {
      _space();
    }
    JsExpression arg = x.getArg();
    // unary operators always associate correctly (I think)
    _parenPush(x, arg, true);
    accept(arg);
    _parenPop(x, arg, true);
    return false;
  }

  @Override
  public boolean visit(JsProgram x, JsContext<JsProgram> ctx) {
    p.print("<JsProgram>");
    return false;
  }

  @Override
  public boolean visit(JsPropertyInitializer x, JsContext<JsPropertyInitializer> ctx) {
    // Since there are separators, we actually print the property init
    // in visit(JsObjectLiteral).
    //
    return false;
  }

  @Override
  public boolean visit(JsRegExp x, JsContext<JsExpression> ctx) {
    _slash();
    p.print(x.getPattern());
    _slash();
    String flags = x.getFlags();
    if (flags != null) {
      p.print(flags);
    }
    return false;
  }

  @Override
  public boolean visit(JsReturn x, JsContext<JsStatement> ctx) {
    _return();
    JsExpression expr = x.getExpr();
    if (expr != null) {
      _space();
      accept(expr);
    }
    return false;
  }

  @Override
  public boolean visit(JsStringLiteral x, JsContext<JsExpression> ctx) {
    printStringLiteral(x.getValue());
    return false;
  }

  @Override
  public boolean visit(JsSwitch x, JsContext<JsStatement> ctx) {
    _switch();
    _spaceOpt();
    _lparen();
    accept(x.getExpr());
    _rparen();
    _spaceOpt();
    _blockOpen();
    acceptList(x.getCases());
    _blockClose();
    return false;
  }

  @Override
  public boolean visit(JsThisRef x, JsContext<JsExpression> ctx) {
    _this();
    return false;
  }

  @Override
  public boolean visit(JsThrow x, JsContext<JsStatement> ctx) {
    _throw();
    _space();
    accept(x.getExpr());
    return false;
  }

  @Override
  public boolean visit(JsTry x, JsContext<JsStatement> ctx) {
    _try();
    _spaceOpt();
    accept(x.getTryBlock());

    acceptList(x.getCatches());

    JsBlock finallyBlock = x.getFinallyBlock();
    if (finallyBlock != null) {
      _spaceOpt();
      _finally();
      _spaceOpt();
      accept(finallyBlock);
    }

    return false;
  }

  @Override
  public boolean visit(JsVar x, JsContext<JsVar> ctx) {
    _nameOf(x);
    JsExpression initExpr = x.getInitExpr();
    if (initExpr != null) {
      _spaceOpt();
      _assignment();
      _spaceOpt();
      _parenPushIfCommaExpr(initExpr);
      accept(initExpr);
      _parenPopIfCommaExpr(initExpr);
    }
    return false;
  }

  @Override
  public boolean visit(JsVars x, JsContext<JsStatement> ctx) {
    _var();
    _space();
    boolean sep = false;
    for (Iterator<JsVar> iter = x.iterator(); iter.hasNext();) {
      sep = _sepCommaOptSpace(sep);
      JsVars.JsVar var = iter.next();
      accept(var);
    }
    return false;
  }

  @Override
  public boolean visit(JsWhile x, JsContext<JsStatement> ctx) {
    _while();
    _spaceOpt();
    _lparen();
    accept(x.getCondition());
    _rparen();
    _nestedPush(x.getBody(), false);
    accept(x.getBody());
    _nestedPop(x.getBody());
    return false;
  }

  // CHECKSTYLE_NAMING_OFF
  protected void _newline() {
    p.newline();
  }

  protected void _newlineOpt() {
    p.newlineOpt();
  }

  protected void printJsBlockOptionalTruncate(JsBlock x, boolean truncate) {
    boolean needBraces = !x.isGlobalBlock();

    if (needBraces) {
      // Open braces.
      //
      _blockOpen();
    }

    int count = 0;
    for (Iterator<JsStatement> iter = x.getStatements().iterator(); iter.hasNext(); ++count) {
      if (truncate && count > JSBLOCK_LINES_TO_PRINT) {
        p.print("[...]");
        _newlineOpt();
        break;
      }
      JsStatement stmt = iter.next();
      needSemi = true;
      accept(stmt);
      if (needSemi) {
        /*
         * Special treatment of function decls: function decls always set
         * needSemi back to true. But if they are the only item in a statement
         * (i.e. not part of an assignment operation), just give them a newline
         * instead of a semi since it makes obfuscated code so much "nicer"
         * (sic).
         */
        if (stmt instanceof JsExprStmt
            && ((JsExprStmt) stmt).getExpression() instanceof JsFunction) {
          _newline();
        } else {
          _semi();
          _newlineOpt();
        }
      }
    }

    if (needBraces) {
      // Close braces.
      //
      _blockClose();
    }
    needSemi = false;
  }

  private void _assignment() {
    p.print('=');
  }

  private void _blockClose() {
    p.indentOut();
    p.print('}');
    _newlineOpt();
  }

  private void _blockOpen() {
    p.print('{');
    p.indentIn();
    _newlineOpt();
  }

  private void _break() {
    p.print(CHARS_BREAK);
  }

  private void _case() {
    p.print(CHARS_CASE);
  }

  private void _catch() {
    p.print(CHARS_CATCH);
  }

  private void _colon() {
    p.print(':');
  }

  private void _continue() {
    p.print(CHARS_CONTINUE);
  }

  private void _debugger() {
    p.print(CHARS_DEBUGGER);
  }

  private void _default() {
    p.print(CHARS_DEFAULT);
  }

  private void _do() {
    p.print(CHARS_DO);
  }

  private void _dot() {
    p.print('.');
  }

  private void _else() {
    p.print(CHARS_ELSE);
  }

  private void _false() {
    p.print(CHARS_FALSE);
  }

  private void _finally() {
    p.print(CHARS_FINALLY);
  }

  private void _for() {
    p.print(CHARS_FOR);
  }

  private void _function() {
    p.print(CHARS_FUNCTION);
  }

  private void _if() {
    p.print(CHARS_IF);
  }

  private void _in() {
    p.print(CHARS_IN);
  }

  private void _lbrace() {
    p.print('{');
  }

  private void _lparen() {
    p.print('(');
  }

  private void _lsquare() {
    p.print('[');
  }

  private void _nameDef(JsName name) {
    p.print(name.getShortIdent());
  }

  private void _nameOf(HasName hasName) {
    _nameDef(hasName.getName());
  }

  private void _nameRef(JsNameRef nameRef) {
    p.print(nameRef.getShortIdent());
  }

  private boolean _nestedPop(JsStatement statement) {
    boolean pop = !(statement instanceof JsBlock);
    if (pop) {
      p.indentOut();
    }
    return pop;
  }

  private boolean _nestedPush(JsStatement statement, boolean needSpace) {
    boolean push = !(statement instanceof JsBlock);
    if (push) {
      if (needSpace) {
        _space();
      }
      p.indentIn();
      _newlineOpt();
    } else {
      _spaceOpt();
    }
    return push;
  }

  private void _new() {
    p.print(CHARS_NEW);
  }

  private void _null() {
    p.print(CHARS_NULL);
  }

  private boolean _parenCalc(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    int parentPrec = JsPrecedenceVisitor.exec(parent);
    int childPrec = JsPrecedenceVisitor.exec(child);
    return (parentPrec > childPrec || (parentPrec == childPrec && wrongAssoc));
  }

  private boolean _parenPop(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPop = _parenCalc(parent, child, wrongAssoc);
    if (doPop) {
      _rparen();
    }
    return doPop;
  }

  private boolean _parenPopIfCommaExpr(JsExpression x) {
    boolean doPop = x instanceof JsBinaryOperation
        && ((JsBinaryOperation) x).getOperator() == JsBinaryOperator.COMMA;
    if (doPop) {
      _rparen();
    }
    return doPop;
  }

  private boolean _parenPopIfConditional(JsExpression x) {
    boolean doPop = x instanceof JsConditional;
    if (doPop) {
      _rparen();
    }
    return doPop;
  }

  private boolean _parenPopOrSpace(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPop = _parenCalc(parent, child, wrongAssoc);
    if (doPop) {
      _rparen();
    } else {
      _space();
    }
    return doPop;
  }

  private boolean _parenPush(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPush = _parenCalc(parent, child, wrongAssoc);
    if (doPush) {
      _lparen();
    }
    return doPush;
  }

  private boolean _parenPushIfCommaExpr(JsExpression x) {
    boolean doPush = x instanceof JsBinaryOperation
        && ((JsBinaryOperation) x).getOperator() == JsBinaryOperator.COMMA;
    if (doPush) {
      _lparen();
    }
    return doPush;
  }

  private boolean _parenPushIfConditional(JsExpression x) {
    boolean doPush = x instanceof JsConditional;
    if (doPush) {
      _lparen();
    }
    return doPush;
  }

  private boolean _parenPushOrSpace(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPush = _parenCalc(parent, child, wrongAssoc);
    if (doPush) {
      _lparen();
    } else {
      _space();
    }
    return doPush;
  }

  private void _questionMark() {
    p.print('?');
  }

  private void _rbrace() {
    p.print('}');
  }

  private void _return() {
    p.print(CHARS_RETURN);
  }

  private void _rparen() {
    p.print(')');
  }

  private void _rsquare() {
    p.print(']');
  }

  private void _semi() {
    p.print(';');
  }

  private boolean _sepCommaOptSpace(boolean sep) {
    if (sep) {
      p.print(',');
      _spaceOpt();
    }
    return true;
  }

  private void _slash() {
    p.print('/');
  }

  private void _space() {
    p.print(' ');
  }

  private void _spaceOpt() {
    p.printOpt(' ');
  }

  private void _switch() {
    p.print(CHARS_SWITCH);
  }

  private void _this() {
    p.print(CHARS_THIS);
  }

  private void _throw() {
    p.print(CHARS_THROW);
  }

  private void _true() {
    p.print(CHARS_TRUE);
  }

  private void _try() {
    p.print(CHARS_TRY);
  }

  private void _var() {
    p.print(CHARS_VAR);
  }

  private void _while() {
    p.print(CHARS_WHILE);
  }

  // CHECKSTYLE_NAMING_ON

  /**
   * Escapes any closing XML tags embedded in <code>str</code>, which could
   * potentially cause a parse failure in a browser, for example, embedding a
   * closing <code>&lt;script&gt;</code> tag.
   * 
   * @param str an unescaped literal; May be null
   */
  private void escapeClosingTags(StringBuffer str) {
    if (str == null) {
      return;
    }

    int index = 0;

    while ((index = str.indexOf("</", index)) != -1) {
      str.insert(index + 1, '\\');
    }
  }

  private void indent() {
    p.indentIn();
  }

  private void outdent() {
    p.indentOut();
  }

  /**
   * Adapted from
   * {@link com.google.gwt.dev.js.rhino.ScriptRuntime#escapeString(String)}.
   * The difference is that we quote with either &quot; or &apos; depending on
   * which one is used less inside the string.
   */
  private void printStringLiteral(String value) {

    char[] chars = value.toCharArray();
    final int n = chars.length;
    int quoteCount = 0;
    int aposCount = 0;
    for (int i = 0; i < n; ++i) {
      switch (chars[i]) {
        case '"':
          ++quoteCount;
          break;
        case '\'':
          ++aposCount;
          break;
      }
    }

    StringBuffer result = new StringBuffer(value.length() + 16);

    char quoteChar = (quoteCount < aposCount) ? '"' : '\'';
    p.print(quoteChar);

    for (int i = 0; i < n; ++i) {
      char c = chars[i];

      if (' ' <= c && c <= '~' && c != quoteChar && c != '\\') {
        // an ordinary print character (like C isprint())
        result.append(c);
        continue;
      }

      int escape = -1;
      switch (c) {
        case 0:
          escape = '0';
          break;
        case '\b':
          escape = 'b';
          break;
        case '\f':
          escape = 'f';
          break;
        case '\n':
          escape = 'n';
          break;
        case '\r':
          escape = 'r';
          break;
        case '\t':
          escape = 't';
          break;
        case '"':
          escape = '"';
          break; // only reach here if == quoteChar
        case '\'':
          escape = '\'';
          break; // only reach here if == quoteChar
        case '\\':
          escape = '\\';
          break;
      }

      if (escape >= 0) {
        // an \escaped sort of character
        result.append('\\');
        result.append((char) escape);
      } else {
        int hexSize;
        if (c < 256) {
          // 2-digit hex
          result.append("\\x");
          hexSize = 2;
        } else {
          // Unicode.
          result.append("\\u");
          hexSize = 4;
        }
        // append hexadecimal form of ch left-padded with 0
        for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
          int digit = 0xf & (c >> shift);
          result.append(HEX_DIGITS[digit]);
        }
      }
    }
    result.append(quoteChar);
    escapeClosingTags(result);
    p.print(result.toString());
  }
}
