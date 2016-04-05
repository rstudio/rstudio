/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.SourceInfo;
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
import com.google.gwt.dev.js.ast.JsDebugger;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsNumericEntry;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsPositionMarker;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
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
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.js.ast.NodeKind;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.javascript.jscomp.AbstractCompiler;
import com.google.gwt.thirdparty.javascript.jscomp.AstValidator;
import com.google.gwt.thirdparty.javascript.rhino.IR;
import com.google.gwt.thirdparty.javascript.rhino.InputId;
import com.google.gwt.thirdparty.javascript.rhino.Node;
import com.google.gwt.thirdparty.javascript.rhino.SimpleSourceFile;
import com.google.gwt.thirdparty.javascript.rhino.StaticSourceFile;
import com.google.gwt.thirdparty.javascript.rhino.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Translate a GWT JS AST to a Closure Compiler AST.
 */
public class ClosureJsAstTranslator {
  private static String getStringValue(double value) {
    long longValue = (long) value;

    // Return "1" instead of "1.0"
    if (longValue == value) {
      return Long.toString(longValue);
    } else {
      return Double.toString(value);
    }
  }

  private final Map<String, StaticSourceFile> sourceCache = new HashMap<>();

  private final boolean validate;
  private final Set<String> globalVars = Sets.newHashSet();
  private final Set<String> externalProperties = Sets.newHashSet();

  private final Set<String> externalVars = Sets.newHashSet();

  private final JsProgram program;
  private final AbstractCompiler compiler;

  ClosureJsAstTranslator(boolean validate, JsProgram program, AbstractCompiler compiler) {
    this.program = program;
    this.validate = validate;
    this.compiler = compiler;
  }

  public Node translate(JsProgramFragment fragment, InputId inputId, String source) {
    Node script = IR.script();
    script.putBooleanProp(Node.SYNTHETIC_BLOCK_PROP, true);
    script.setInputId(inputId);
    script.setStaticSourceFile(getClosureSourceFile(source));
    for (JsStatement s : fragment.getGlobalBlock().getStatements()) {
      script.addChildToBack(transform(s));
    }
    // Validate the structural integrity of the AST.
    if (validate) {
      new AstValidator(compiler).validateScript(script);
    }
    return script;
  }

  Set<String> getExternalPropertyReferences() {
    return externalProperties;
  }

  Set<String> getExternalVariableReferences() {
    return externalVars;
  }

  Set<String> getGlobalVariableNames() {
    return globalVars;
  }

  private Node applyOriginalName(Node n, JsNode x) {
    /*
     * if (x instanceof HasSymbol) { Symbol symbol = ((HasSymbol)x).getSymbol(); if (symbol != null)
     * { String originalName = symbol.getOriginalSymbolName(); n.putProp(Node.ORIGINALNAME_PROP,
     * originalName); } }
     */
    return n;
  }

  private Node applySourceInfo(Node n, HasSourceInfo srcNode) {
    if (n != null && srcNode != null) {
      SourceInfo info = srcNode.getSourceInfo();
      if (info != null && info.getFileName() != null) {
        n.setStaticSourceFile(getClosureSourceFile(info.getFileName()));
        n.setLineno(info.getStartLine());
        n.setCharno(0);
      }
    }
    return n;
  }

  private StaticSourceFile getClosureSourceFile(String source) {
    StaticSourceFile closureSourceFile = sourceCache.get(source);
    if (closureSourceFile == null) {
      closureSourceFile = new SimpleSourceFile(source, false);
      sourceCache.put(source, closureSourceFile);
    }
    return closureSourceFile;
  }

  private String getName(JsName name) {
    return name.getShortIdent();
  }

  private String getName(JsNameRef name) {
    return name.getShortIdent();
  }

  private Node getNameNodeFor(HasName hasName) {
    Node n = IR.name(getName(hasName.getName()));
    applyOriginalName(n, (JsNode) hasName);
    return applySourceInfo(n, (HasSourceInfo) hasName);
  }

  private int getTokenForOp(JsBinaryOperator op) {
    switch (op) {
      case MUL:
        return Token.MUL;
      case DIV:
        return Token.DIV;
      case MOD:
        return Token.MOD;
      case ADD:
        return Token.ADD;
      case SUB:
        return Token.SUB;
      case SHL:
        return Token.LSH;
      case SHR:
        return Token.RSH;
      case SHRU:
        return Token.URSH;
      case LT:
        return Token.LT;
      case LTE:
        return Token.LE;
      case GT:
        return Token.GT;
      case GTE:
        return Token.GE;
      case INSTANCEOF:
        return Token.INSTANCEOF;
      case INOP:
        return Token.IN;
      case EQ:
        return Token.EQ;
      case NEQ:
        return Token.NE;
      case REF_EQ:
        return Token.SHEQ;
      case REF_NEQ:
        return Token.SHNE;
      case BIT_AND:
        return Token.BITAND;
      case BIT_XOR:
        return Token.BITXOR;
      case BIT_OR:
        return Token.BITOR;
      case AND:
        return Token.AND;
      case OR:
        return Token.OR;
      case ASG:
        return Token.ASSIGN;
      case ASG_ADD:
        return Token.ASSIGN_ADD;
      case ASG_SUB:
        return Token.ASSIGN_SUB;
      case ASG_MUL:
        return Token.ASSIGN_MUL;
      case ASG_DIV:
        return Token.ASSIGN_DIV;
      case ASG_MOD:
        return Token.ASSIGN_MOD;
      case ASG_SHL:
        return Token.ASSIGN_LSH;
      case ASG_SHR:
        return Token.ASSIGN_RSH;
      case ASG_SHRU:
        return Token.ASSIGN_URSH;
      case ASG_BIT_AND:
        return Token.ASSIGN_BITAND;
      case ASG_BIT_OR:
        return Token.ASSIGN_BITOR;
      case ASG_BIT_XOR:
        return Token.ASSIGN_BITXOR;
      case COMMA:
        return Token.COMMA;
    }
    return 0;
  }

  private int getTokenForOp(JsUnaryOperator op) {
    switch (op) {
      case BIT_NOT:
        return Token.BITNOT;
      case DEC:
        return Token.DEC;
      case DELETE:
        return Token.DELPROP;
      case INC:
        return Token.INC;
      case NEG:
        return Token.NEG;
      case POS:
        return Token.POS;
      case NOT:
        return Token.NOT;
      case TYPEOF:
        return Token.TYPEOF;
      case VOID:
        return Token.VOID;
    }
    throw new IllegalStateException();
  }

  private Node transform(JsArrayAccess x) {
    Node n = IR.getelem(transform(x.getArrayExpr()), transform(x.getIndexExpr()));
    return applySourceInfo(n, x);
  }

  private Node transform(JsArrayLiteral x) {
    Node n = IR.arraylit();
    for (Object element : x.getExpressions()) {
      JsExpression arg = (JsExpression) element;
      n.addChildToBack(transform(arg));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsBinaryOperation x) {
    JsBinaryOperator op = x.getOperator();
    Node n = new Node(getTokenForOp(op), transform(x.getArg1()), transform(x.getArg2()));
    return applySourceInfo(n, x);
  }

  private Node transform(JsBlock x) {
    Node n = IR.block();
    for (JsStatement s : x.getStatements()) {
      n.addChildToBack(transform(s));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsBooleanLiteral x) {
    Node n = x.getValue() ? IR.trueNode() : IR.falseNode();
    return applySourceInfo(n, x);
  }

  private Node transform(JsBreak x) {
    Node n;
    JsNameRef label = x.getLabel();
    if (label == null) {
      n = IR.breakNode();
    } else {
      n = IR.breakNode(transformLabel(label));
    }

    return applySourceInfo(n, x);
  }

  private Node transform(JsCase x) {
    Node expr = transform(x.getCaseExpr());
    Node body = IR.block();
    body.putBooleanProp(Node.SYNTHETIC_BLOCK_PROP, true);
    applySourceInfo(body, x);

    for (Object element : x.getStmts()) {
      JsStatement stmt = (JsStatement) element;
      body.addChildToBack(transform(stmt));
    }

    Node n = IR.caseNode(expr, body);
    return applySourceInfo(n, x);
  }

  private Node transform(JsCatch x) {
    Node n = IR.catchNode(transformName(x.getParameter().getName()), transform(x.getBody()));
    Preconditions.checkState(x.getCondition() == null);
    return applySourceInfo(n, x);
  }

  private Node transform(JsConditional x) {
    Node n = IR.hook(transform(x.getTestExpression()), transform(x.getThenExpression()),
        transform(x.getElseExpression()));
    return applySourceInfo(n, x);
  }

  private Node transform(JsContinue x) {
    Node n;
    JsNameRef label = x.getLabel();
    if (label == null) {
      n = IR.continueNode();
    } else {
      n = IR.continueNode(transformLabel(label));
    }

    return applySourceInfo(n, x);
  }

  private Node transform(JsDebugger x) {
    Node n = new Node(Token.DEBUGGER);
    return applySourceInfo(n, x);
  }

  private Node transform(JsDefault x) {
    Node body = IR.block();
    body.putBooleanProp(Node.SYNTHETIC_BLOCK_PROP, true);
    applySourceInfo(body, x);

    for (Object element : x.getStmts()) {
      JsStatement stmt = (JsStatement) element;
      body.addChildToBack(transform(stmt));
    }
    Node n = IR.defaultCase(body);
    return applySourceInfo(n, x);
  }

  private Node transform(JsDoWhile x) {
    Node n = IR.doNode(transformBody(x.getBody(), x), transform(x.getCondition()));
    return applySourceInfo(n, x);
  }

  private Node transform(JsEmpty x) {
    return IR.empty();
  }

  private Node transform(JsExpression x) {
    assert x != null;
    switch (x.getKind()) {
      case ARRAY:
        return transform((JsArrayLiteral) x);
      case ARRAY_ACCESS:
        return transform((JsArrayAccess) x);
      case BINARY_OP:
        return transform((JsBinaryOperation) x);
      case CONDITIONAL:
        return transform((JsConditional) x);
      case INVOKE:
        return transform((JsInvocation) x);
      case FUNCTION:
        return transform((JsFunction) x);
      case OBJECT:
        return transform((JsObjectLiteral) x);
      case BOOLEAN:
        return transform((JsBooleanLiteral) x);
      case NULL:
        return transform((JsNullLiteral) x);
      case NUMBER:
        if (x instanceof JsNumericEntry) {
          return transform((JsNumericEntry) x);
        }
        return transform((JsNumberLiteral) x);
      case REGEXP:
        return transform((JsRegExp) x);
      case STRING:
        return transform((JsStringLiteral) x);
      case THIS:
        return transform((JsThisRef) x);
      case NAME_OF:
        return transform((JsNameOf) x);
      case NAME_REF:
        return transform((JsNameRef) x);
      case NEW:
        return transform((JsNew) x);
      case POSTFIX_OP:
        return transform((JsPostfixOperation) x);
      case PREFIX_OP:
        return transform((JsPrefixOperation) x);
      default:
        throw new IllegalStateException("Unexpected expression type: "
            + x.getClass().getSimpleName());
    }
  }

  private Node transform(JsExprStmt x) {
    // The GWT JS AST doesn't produce function declarations, instead
    // they are expressions statements:
    Node expr = transform(x.getExpression());
    if (!expr.isFunction()) {
      return IR.exprResult(expr);
    } else {
      return expr;
    }
  }

  private Node transform(JsFor x) {
    // The init expressions or var decl.
    //
    Node init;
    if (x.getInitExpr() != null) {
      init = transform(x.getInitExpr());
    } else if (x.getInitVars() != null) {
      init = transform(x.getInitVars());
    } else {
      init = IR.empty();
    }

    // The loop test.
    //
    Node cond;
    if (x.getCondition() != null) {
      cond = transform(x.getCondition());
    } else {
      cond = IR.empty();
    }

    // The incr expression.
    //
    Node incr;
    if (x.getIncrExpr() != null) {
      incr = transform(x.getIncrExpr());
    } else {
      incr = IR.empty();
    }

    Node body = transformBody(x.getBody(), x);
    Node n = IR.forNode(init, cond, incr, body);
    return applySourceInfo(n, x);
  }

  private Node transform(JsForIn x) {
    Node valueExpr;
    if (x.getIterVarName() != null) {
      valueExpr = new Node(Token.VAR, transformName(x.getIterVarName()));
    } else {
      // Just a name ref.
      //
      valueExpr = transform(x.getIterExpr());
    }

    Node n = IR.forIn(valueExpr, transform(x.getObjExpr()), transformBody(x.getBody(), x));
    return applySourceInfo(n, x);
  }

  private Node transform(JsFunction x) {
    Node name;
    if (x.getName() != null) {
      name = getNameNodeFor(x);
    } else {
      name = IR.name("");
    }
    applySourceInfo(name, x);

    Node params = IR.paramList();
    for (Object element : x.getParameters()) {
      JsParameter param = (JsParameter) element;
      params.addChildToBack(transform(param));
    }
    applySourceInfo(params, x);

    Node n = IR.function(name, params, transform(x.getBody()));
    if (name.getString().isEmpty()) {
      n.putProp(Node.ORIGINALNAME_PROP, "");
    } else {
      applyOriginalName(n, x);
    }

    /*
     * if (x.isConstructor()) { JSDocInfoBuilder builder = new JSDocInfoBuilder(false);
     * builder.recordConstructor(); n.setJSDocInfo(builder.build(n)); }
     */

    return applySourceInfo(n, x);
  }

  private Node transform(JsIf x) {
    Node n = IR.ifNode(transform(x.getIfExpr()), transformBody(x.getThenStmt(), x));
    if (x.getElseStmt() != null) {
      n.addChildToBack(transformBody(x.getElseStmt(), x));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsInvocation x) {
    Node n = IR.call(transform(x.getQualifier()));
    for (Object element : x.getArguments()) {
      JsExpression arg = (JsExpression) element;
      n.addChildToBack(transform(arg));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsLabel x) {
    Node n = IR.label(transformLabel(x.getName()), transform(x.getStmt()));

    return applySourceInfo(n, x);
  }

  private Node transform(JsNameOf x) {
    Node n = transformNameAsString(x.getName().getShortIdent(), x);
    applyOriginalName(n, x);
    return applySourceInfo(n, x);
  }

  private Node transform(JsNameRef x) {
    Node n;
    JsName name = x.getName();
    boolean isExternal = name == null || !name.isObfuscatable();
    if (x.getQualifier() != null) {
      n = IR.getprop(transform(x.getQualifier()), transformNameAsString(x.getShortIdent(), x));
      if (isExternal) {
        this.externalProperties.add(x.getShortIdent());
      }
    } else {
      n = transformName(x.getShortIdent(), x);
      if (isExternal) {
        this.externalVars.add(x.getShortIdent());
      } else if (name.getEnclosing() == program.getScope()) {
        this.globalVars.add(x.getShortIdent());
      }
    }
    applyOriginalName(n, x);
    return applySourceInfo(n, x);
  }

  private Node transform(JsNew x) {
    Node n = IR.newNode(transform(x.getConstructorExpression()));
    for (Object element : x.getArguments()) {
      JsExpression arg = (JsExpression) element;
      n.addChildToBack(transform(arg));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsNullLiteral x) {
    return IR.nullNode();
  }

  private Node transform(JsNumericEntry x) {
    return IR.number(x.getValue());
  }

  private Node transform(JsNumberLiteral x) {
    return IR.number(x.getValue());
  }

  private Node transform(JsObjectLiteral x) {
    Node n = IR.objectlit();

    for (JsPropertyInitializer element : x.getPropertyInitializers()) {
      Node key;
      if (element.getLabelExpr().getKind() == NodeKind.NUMBER) {
        key = transformNumberAsString((JsNumberLiteral) element.getLabelExpr());
        key.putBooleanProp(Node.QUOTED_PROP, true);
      } else if (element.getLabelExpr().getKind() == NodeKind.NAME_REF) {
        key = transformNameAsString(((JsNameRef) element.getLabelExpr()).getShortIdent(),
            element.getLabelExpr());
      } else {
        key = transform(element.getLabelExpr());
      }
      Preconditions.checkState(key.isString(), key);
      key.setType(Token.STRING_KEY);
      // Set as quoted as the rhino version we use does not distinguish one from the other.
      // Closure assumes unquoted property names are obfuscatable, but since there is no way to
      // distinguish between them at this point they have to be assumed quoted, hence not
      // obfuscatable.
      // TODO(rluble): Make sure this is handled correctly once rhino is upgraded.
      key.putBooleanProp(Node.QUOTED_PROP, true);
      n.addChildToBack(IR.propdef(key, transform(element.getValueExpr())));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsParameter x) {
    return getNameNodeFor(x);
  }

  private Node transform(JsPositionMarker x) {
    return IR.empty();
  }

  private Node transform(JsPostfixOperation x) {
    Node n = new Node(getTokenForOp(x.getOperator()), transform(x.getArg()));
    n.putBooleanProp(Node.INCRDECR_PROP, true);
    return applySourceInfo(n, x);
  }

  private Node transform(JsPrefixOperation x) {
    Node n = new Node(getTokenForOp(x.getOperator()), transform(x.getArg()));
    return applySourceInfo(n, x);
  }

  private Node transform(JsRegExp x) {
    String flags = x.getFlags();
    Node n = IR.regexp(Node.newString(x.getPattern()),
        Node.newString(flags != null ? x.getFlags() : ""));
    return applySourceInfo(n, x);
  }

  private Node transform(JsReturn x) {
    Node n = IR.returnNode();
    JsExpression result = x.getExpr();
    if (result != null) {
      n.addChildToBack(transform(x.getExpr()));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsStatement x) {
    switch (x.getKind()) {
      case BLOCK:
        return transform((JsBlock) x);
      case BREAK:
        return transform((JsBreak) x);
      case CONTINUE:
        return transform((JsContinue) x);
      case DEBUGGER:
        return transform((JsDebugger) x);
      case DO:
        return transform((JsDoWhile) x);
      case EMPTY:
        return transform((JsEmpty) x);
      case EXPR_STMT:
        return transform((JsExprStmt) x);
      case FOR:
        return transform((JsFor) x);
      case FOR_IN:
        return transform((JsForIn) x);
      case IF:
        return transform((JsIf) x);
      case LABEL:
        return transform((JsLabel) x);
      case POSITION_MARKER:
        return transform((JsPositionMarker) x);
      case RETURN:
        return transform((JsReturn) x);
      case SWITCH:
        return transform((JsSwitch) x);
      case THROW:
        return transform((JsThrow) x);
      case TRY:
        return transform((JsTry) x);
      case VARS:
        return transform((JsVars) x);
      case WHILE:
        return transform((JsWhile) x);
      default:
        throw new IllegalStateException("Unexpected statement type: "
            + x.getClass().getSimpleName());
    }
  }

  private Node transform(JsStringLiteral x) {
    return IR.string(x.getValue());
  }

  private Node transform(JsSwitch x) {
    Node n = IR.switchNode(transform(x.getExpr()));
    for (JsSwitchMember member : x.getCases()) {
      n.addChildToBack(transform(member));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsSwitchMember x) {
    switch (x.getKind()) {
      case CASE:
        return transform((JsCase) x);
      case DEFAULT:
        return transform((JsDefault) x);
      default:
        throw new IllegalStateException("Unexpected switch member type: "
            + x.getClass().getSimpleName());
    }
  }

  private Node transform(JsThisRef x) {
    Node n = new Node(Token.THIS);
    return applySourceInfo(n, x);
  }

  private Node transform(JsThrow x) {
    Node n = IR.throwNode(transform(x.getExpr()));
    return applySourceInfo(n, x);
  }

  private Node transform(JsTry x) {
    Node n = new Node(Token.TRY, transform(x.getTryBlock()));

    Node catches = new Node(Token.BLOCK);
    for (JsCatch catchBlock : x.getCatches()) {
      catches.addChildToBack(transform(catchBlock));
    }
    n.addChildToBack(catches);

    JsBlock finallyBlock = x.getFinallyBlock();
    if (finallyBlock != null) {
      n.addChildToBack(transform(finallyBlock));
    }

    return applySourceInfo(n, x);
  }

  private Node transform(JsVar x) {
    Node n = getNameNodeFor(x);
    JsExpression initExpr = x.getInitExpr();
    if (initExpr != null) {
      n.addChildToBack(transform(initExpr));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsVars x) {
    Node n = new Node(Token.VAR);
    for (JsVar var : x) {
      n.addChildToBack(transform(var));
    }
    return applySourceInfo(n, x);
  }

  private Node transform(JsWhile x) {
    Node n =
        IR.forNode(IR.empty(), transform(x.getCondition()), IR.empty(), transformBody(x.getBody(),
            x));
    return applySourceInfo(n, x);
  }

  private Node transformBody(JsStatement x, HasSourceInfo parent) {
    Node n = transform(x);
    if (!n.isBlock()) {
      Node stmt = n;
      n = IR.block();
      if (!stmt.isEmpty()) {
        n.addChildToBack(stmt);
      }
      applySourceInfo(n, parent);
    }
    return n;
  }

  private Node transformLabel(JsName label) {
    Node n = IR.labelName(getName(label));
    return applySourceInfo(n, label.getStaticRef());
  }

  private Node transformLabel(JsNameRef label) {
    Node n = IR.labelName(getName(label));
    return applySourceInfo(n, label);
  }

  private Node transformName(JsName name) {
    Node n = IR.name(getName(name));
    return applySourceInfo(n, name.getStaticRef());
  }

  private Node transformName(String name, HasSourceInfo info) {
    Node n = IR.name(name);
    return applySourceInfo(n, info);
  }

  private Node transformNameAsString(String name, HasSourceInfo info) {
    Node n = IR.string(name);
    return applySourceInfo(n, info);
  }

  private Node transformNumberAsString(JsNumberLiteral literalNode) {
    Node irNode = Node.newString(getStringValue(literalNode.getValue()));
    return irNode;
  }
}
