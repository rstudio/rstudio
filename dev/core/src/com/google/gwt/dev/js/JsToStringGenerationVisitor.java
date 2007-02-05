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
import com.google.gwt.dev.js.ast.JsContinue;
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
import com.google.gwt.dev.js.ast.JsParameters;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.TextOutput;

import java.util.Iterator;

/**
 * Produces text output from a JavaScript AST.
 */
public class JsToStringGenerationVisitor extends JsAbstractVisitorWithEndVisits {

  private static final char[] CHARS_BREAK = "break".toCharArray();
  private static final char[] CHARS_CASE = "case".toCharArray();
  private static final char[] CHARS_CATCH = "catch".toCharArray();
  private static final char[] CHARS_CONTINUE = "continue".toCharArray();
  private static final char[] CHARS_DEFAULT = "default".toCharArray();
  private static final char[] CHARS_DELETE = "delete".toCharArray();
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

  protected boolean needSemi = true;

  private final NamingStrategy namer;

  private final TextOutput p;

  public JsToStringGenerationVisitor(TextOutput out, NamingStrategy namer) {
    this.p = out;
    this.namer = namer;
  }

  public boolean visit(JsArrayAccess x) {
    JsExpression arrayExpr = x.getArrayExpr();
    _parenPush(x, arrayExpr, false);
    arrayExpr.traverse(this);
    _parenPop(x, arrayExpr, false);
    _lsquare();
    x.getIndexExpr().traverse(this);
    _rsquare();
    return false;
  }

  public boolean visit(JsArrayLiteral x) {
    _lsquare();
    boolean sep = false;
    for (Iterator iter = x.getExpressions().iterator(); iter.hasNext();) {
      JsExpression arg = (JsExpression) iter.next();
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      arg.traverse(this);
      _parenPopIfCommaExpr(arg);
    }
    _rsquare();
    return false;
  }

  public boolean visit(JsBinaryOperation x) {
    JsBinaryOperator op = x.getOperator();
    JsExpression arg1 = x.getArg1();
    _parenPush(x, arg1, !op.isLeftAssociative());
    arg1.traverse(this);
    boolean needSpace = op.isKeyword() || arg1 instanceof JsPostfixOperation;
    if (needSpace) {
      _parenPopOrSpace(x, arg1, !op.isLeftAssociative());
    } else {
      _parenPop(x, arg1, !op.isLeftAssociative());
    }
    p.print(op.getSymbol());
    JsExpression arg2 = x.getArg2();
    needSpace = op.isKeyword() || arg2 instanceof JsPrefixOperation;
    if (needSpace) {
      _parenPushOrSpace(x, arg2, op.isLeftAssociative());
    } else {
      _parenPush(x, arg2, op.isLeftAssociative());
    }
    arg2.traverse(this);
    _parenPop(x, arg2, op.isLeftAssociative());
    return false;
  }

  public boolean visit(JsBlock block) {
    boolean needBraces = !block.isGlobalBlock();

    if (needBraces) {
      // Open braces.
      //
      _blockOpen();
    }

    for (Iterator iter = block.getStatements().iterator(); iter.hasNext();) {
      JsStatement stmt = (JsStatement) iter.next();
      needSemi = true;
      stmt.traverse(this);
      if (needSemi) {
        /*
         * Special treatment of function decls: function decls always set
         * fNeedSemi back to true. But if they are the only item in a statement
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
    return false;
  }

  public boolean visit(JsBooleanLiteral x) {
    if (x.getValue()) {
      _true();
    } else {
      _false();
    }
    return false;
  }

  public boolean visit(JsBreak x) {
    _break();

    JsNameRef label = x.getLabel();
    if (label != null) {
      _space();
      _nameOf(label);
    }

    return false;
  }

  public boolean visit(JsCase x) {
    _case();
    _space();
    x.getCaseExpr().traverse(this);
    _colon();
    _newlineOpt();

    indent();
    for (Iterator iter = x.getStmts().iterator(); iter.hasNext();) {
      JsStatement stmt = (JsStatement) iter.next();
      needSemi = true;
      stmt.traverse(this);
      if (needSemi) {
        _semi();
      }
      _newlineOpt();
    }
    outdent();
    needSemi = false;
    return false;
  }

  public boolean visit(JsCatch x) {
    _spaceOpt();
    _catch();
    _spaceOpt();
    _lparen();
    _nameDef(x.getName());

    // Optional catch condition.
    //
    JsExpression catchCond = x.getCondition();
    if (catchCond != null) {
      _space();
      _if();
      _space();
      catchCond.traverse(this);
    }

    _rparen();
    _spaceOpt();
    x.getBody().traverse(this);

    return false;
  }

  public boolean visit(JsConditional x) {
    // right associative
    {
      JsExpression testExpression = x.getTestExpression();
      _parenPush(x, testExpression, true);
      testExpression.traverse(this);
      _parenPop(x, testExpression, true);
    }
    _questionMark();
    {
      JsExpression thenExpression = x.getThenExpression();
      _parenPush(x, thenExpression, true);
      thenExpression.traverse(this);
      _parenPop(x, thenExpression, true);
    }
    _colon();
    {
      JsExpression elseExpression = x.getElseExpression();
      _parenPush(x, elseExpression, false);
      elseExpression.traverse(this);
      _parenPop(x, elseExpression, false);
    }
    return false;
  }

  public boolean visit(JsContinue x) {
    _continue();

    JsNameRef label = x.getLabel();
    if (label != null) {
      _space();
      _nameOf(label);
    }

    return false;
  }

  public boolean visit(JsDecimalLiteral x) {
    String s = x.getValue();
    // TODO: optimize this to only the cases that need it
    if (s.startsWith("-")) {
      _space();
    }
    p.print(s);
    return false;
  }

  public boolean visit(JsDefault x) {
    _default();
    _colon();

    indent();
    for (Iterator iter = x.getStmts().iterator(); iter.hasNext();) {
      JsStatement stmt = (JsStatement) iter.next();
      needSemi = true;
      stmt.traverse(this);
      if (needSemi) {
        _semi();
      }
      _newlineOpt();
    }
    outdent();
    needSemi = false;
    return false;
  }

  public boolean visit(JsDoWhile x) {
    _do();
    _nestedPush(x.getBody(), true);
    x.getBody().traverse(this);
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
    x.getCondition().traverse(this);
    _rparen();
    return false;
  }

  public boolean visit(JsEmpty x) {
    return false;
  }

  public boolean visit(JsExprStmt x) {
    final JsExpression expr = x.getExpression();
    expr.traverse(this);
    return false;
  }

  public boolean visit(JsFor x) {
    _for();
    _spaceOpt();
    _lparen();

    // The init expressions or var decl.
    //
    if (x.getInitExpr() != null) {
      x.getInitExpr().traverse(this);
    } else if (x.getInitVars() != null) {
      x.getInitVars().traverse(this);
    }

    _semi();

    // The loop test.
    //
    if (x.getCondition() != null) {
      _spaceOpt();
      x.getCondition().traverse(this);
    }

    _semi();

    // The incr expression.
    //
    if (x.getIncrExpr() != null) {
      _spaceOpt();
      x.getIncrExpr().traverse(this);
    }

    _rparen();
    _nestedPush(x.getBody(), false);
    x.getBody().traverse(this);
    _nestedPop(x.getBody());
    return false;
  }

  public boolean visit(JsForIn x) {
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
        x.getIterExpr().traverse(this);
      }
    } else {
      // Just a name ref.
      //
      x.getIterExpr().traverse(this);
    }

    _space();
    _in();
    _space();
    x.getObjExpr().traverse(this);

    _rparen();
    _nestedPush(x.getBody(), false);
    x.getBody().traverse(this);
    _nestedPop(x.getBody());
    return false;
  }

  // function foo(a, b) {
  // stmts...
  // }
  //
  public boolean visit(JsFunction x) {
    _function();

    // Functions can be anonymous.
    //
    if (x.getName() != null) {
      _space();
      _nameOf(x);
    }

    x.getParameters().traverse(this);

    // suppress body text in ToString, let subclass provide

    return false;
  }

  public boolean visit(JsIf x) {
    _if();
    _spaceOpt();
    _lparen();
    x.getIfExpr().traverse(this);
    _rparen();
    JsStatement thenStmt = x.getThenStmt();
    _nestedPush(thenStmt, false);
    thenStmt.traverse(this);
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
      elseStmt.traverse(this);
      if (!elseIf) {
        _nestedPop(elseStmt);
      }
    }
    return false;
  }

  public boolean visit(JsIntegralLiteral x) {
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

  public boolean visit(JsInvocation x) {
    x.getQualifier().traverse(this);

    _lparen();
    boolean sep = false;
    for (Iterator iter = x.getArguments().iterator(); iter.hasNext();) {
      JsExpression arg = (JsExpression) iter.next();
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      arg.traverse(this);
      _parenPopIfCommaExpr(arg);
    }
    _rparen();
    return false;
  }

  public boolean visit(JsLabel x) {
    _nameOf(x);
    _colon();
    _spaceOpt();
    x.getStmt().traverse(this);
    return false;
  }

  public boolean visit(JsNameRef x) {
    JsExpression q = x.getQualifier();
    if (q != null) {
      _parenPush(x, q, false);
      q.traverse(this);
      _parenPop(x, q, false);
      _dot();
    }
    _nameOf(x);
    return false;
  }

  public boolean visit(JsNew x) {
    _new();
    _space();

    JsExpression ctorExpr = x.getConstructorExpression();
    _parenPush(x, ctorExpr, true);
    ctorExpr.traverse(this);
    _parenPop(x, ctorExpr, true);

    _lparen();
    boolean sep = false;
    for (Iterator iter = x.getArguments().iterator(); iter.hasNext();) {
      JsExpression arg = (JsExpression) iter.next();
      sep = _sepCommaOptSpace(sep);
      _parenPushIfCommaExpr(arg);
      arg.traverse(this);
      _parenPopIfCommaExpr(arg);
    }
    _rparen();

    return false;
  }

  public boolean visit(JsNullLiteral x) {
    _null();
    return false;
  }

  public boolean visit(JsObjectLiteral x) {
    _lbrace();
    boolean sep = false;
    for (Iterator iter = x.getPropertyInitializers().iterator(); iter.hasNext();) {
      sep = _sepCommaOptSpace(sep);
      JsPropertyInitializer propInit = (JsPropertyInitializer) iter.next();
      JsExpression labelExpr = propInit.getLabelExpr();
      _parenPushIfConditional(labelExpr);
      labelExpr.traverse(this);
      _parenPopIfConditional(labelExpr);
      _colon();
      JsExpression valueExpr = propInit.getValueExpr();
      _parenPushIfConditional(valueExpr);
      valueExpr.traverse(this);
      _parenPopIfConditional(valueExpr);
    }
    _rbrace();
    return false;
  }

  public boolean visit(JsParameter x) {
    _nameOf(x);
    return false;
  }

  public boolean visit(JsParameters x) {
    _lparen();
    boolean sep = false;
    for (Iterator iter = x.iterator(); iter.hasNext();) {
      JsParameter param = (JsParameter) iter.next();
      sep = _sepCommaOptSpace(sep);
      param.traverse(this);
    }
    _rparen();
    return false;
  }

  public boolean visit(JsPostfixOperation x) {
    JsUnaryOperator op = x.getOperator();
    JsExpression arg = x.getArg();
    // unary operators always associate correctly (I think)
    _parenPush(x, arg, true);
    arg.traverse(this);
    _parenPop(x, arg, true);
    p.print(op.getSymbol());
    return false;
  }

  public boolean visit(JsPrefixOperation x) {
    JsUnaryOperator op = x.getOperator();
    p.print(op.getSymbol());
    if (op.isKeyword()) {
      _space();
    }
    JsExpression arg = x.getArg();
    // unary operators always associate correctly (I think)
    _parenPush(x, arg, true);
    arg.traverse(this);
    _parenPop(x, arg, true);
    return false;
  }

  public boolean visit(JsProgram x) {
    p.print("<JsProgram>");
    return false;
  }

  public boolean visit(JsPropertyInitializer x) {
    // Since there are separators, we actually print the property init
    // in visit(JsObjectLiteral).
    //
    return false;
  }

  public boolean visit(JsRegExp x) {
    _slash();
    p.print(x.getPattern());
    _slash();
    String flags = x.getFlags();
    if (flags != null) {
      p.print(flags);
    }
    return false;
  }

  public boolean visit(JsReturn x) {
    _return();
    JsExpression expr = x.getExpr();
    if (expr != null) {
      _space();
      expr.traverse(this);
    }
    return false;
  }

  public boolean visit(JsStringLiteral x) {
    printStringLiteral(x.getValue());
    return false;
  }

  public boolean visit(JsSwitch x) {
    _switch();
    _spaceOpt();
    _lparen();
    x.getExpr().traverse(this);
    _rparen();
    _spaceOpt();
    _blockOpen();
    x.getCases().traverse(this);
    _blockClose();
    return false;
  }

  public boolean visit(JsThisRef x) {
    _this();
    return false;
  }

  public boolean visit(JsThrow x) {
    _throw();
    _space();
    x.getExpr().traverse(this);
    return false;
  }

  public boolean visit(JsTry x) {
    _try();
    _spaceOpt();
    x.getTryBlock().traverse(this);

    x.getCatches().traverse(this);

    JsBlock finallyBlock = x.getFinallyBlock();
    if (finallyBlock != null) {
      _spaceOpt();
      _finally();
      _spaceOpt();
      finallyBlock.traverse(this);
    }

    return false;
  }

  public boolean visit(JsVars x) {
    _var();
    _space();
    boolean sep = false;
    for (Iterator iter = x.iterator(); iter.hasNext();) {
      sep = _sepCommaOptSpace(sep);
      JsVars.JsVar var = (JsVars.JsVar) iter.next();
      _nameOf(var);
      JsExpression initExpr = var.getInitExpr();
      if (initExpr != null) {
        _spaceOpt();
        _assignment();
        _spaceOpt();
        _parenPushIfCommaExpr(initExpr);
        initExpr.traverse(this);
        _parenPopIfCommaExpr(initExpr);
      }
    }
    return false;
  }

  public boolean visit(JsWhile x) {
    _while();
    _spaceOpt();
    _lparen();
    x.getCondition().traverse(this);
    _rparen();
    _nestedPush(x.getBody(), false);
    x.getBody().traverse(this);
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

  private void _default() {
    p.print(CHARS_DEFAULT);
  }

  private void _delete() {
    p.print(CHARS_DELETE);
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
    String ident = namer.getIdent(name);
    p.print(ident);
  }

  private void _nameOf(HasName hasName) {
    String ident = namer.getIdent(hasName.getName());
    p.print(ident);
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

  // TODO(later): we often leave a space when we don't need to
  private boolean _parenPopOrSpace(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPop = _parenCalc(parent, child, wrongAssoc);
    if (doPop) {
      _rparen();
    } else {
      // if (!child.isLeaf())
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

  // TODO(later): we often leave a space when we don't need to
  private boolean _parenPushOrSpace(JsExpression parent, JsExpression child,
      boolean wrongAssoc) {
    boolean doPush = _parenCalc(parent, child, wrongAssoc);
    if (doPush) {
      _lparen();
    } else {
      // if (!child.isLeaf())
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

  private void indent() {
    p.indentIn();
  }

  private void outdent() {
    p.indentOut();
  }

  /**
   * Adapted from
   * {@link com.google.javascript.jscomp.rhino.ScriptRuntime#escapeString(String)}.
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

    char quoteChar = (quoteCount < aposCount) ? '"' : '\'';
    p.print(quoteChar);
    for (int i = 0; i < n; ++i) {
      char c = chars[i];

      if (' ' <= c && c <= '~' && c != quoteChar && c != '\\') {
        // an ordinary print character (like C isprint())
        p.print(c);
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
        case 0xb:
          escape = 'v';
          break; // Java lacks \v.
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
        p.print('\\');
        p.print((char) escape);
      } else {
        int hexSize;
        if (c < 256) {
          // 2-digit hex
          p.print("\\x");
          hexSize = 2;
        } else {
          // Unicode.
          p.print("\\u");
          hexSize = 4;
        }
        // append hexadecimal form of ch left-padded with 0
        for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
          int digit = 0xf & (c >> shift);
          p.print(HEX_DIGITS[digit]);
        }
      }
    }
    p.print(quoteChar);
  }
}
