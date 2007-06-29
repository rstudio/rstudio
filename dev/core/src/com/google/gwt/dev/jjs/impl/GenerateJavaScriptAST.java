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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JAssertStatement;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
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
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
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
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsCollection;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsIntegralLiteral;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsParameters;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStatements;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsSwitchMember;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsUnaryOperation;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * Creates a JavaScript AST from a <code>JProgram</code> node.
 */
public class GenerateJavaScriptAST {

  private class CreateNamesAndScopesVisitor extends JVisitor {

    private final Stack/* <JsScope> */scopeStack = new Stack();

    // @Override
    public void endVisit(JClassType x, Context ctx) {
      pop();
    }

    // @Override
    public void endVisit(JField x, Context ctx) {
      String name = x.getName();
      String mangleName = mangleName(x);
      if (x.isStatic()) {
        names.put(x, topScope.declareName(mangleName, name));
      } else {
        names.put(x, peek().declareName(mangleName, name));
      }
    }

    // @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      pop();
    }

    // @Override
    public void endVisit(JLabel x, Context ctx) {
      if (getName(x) != null) {
        return;
      }
      names.put(x, peek().declareName(x.getName()));
    }

    // @Override
    public void endVisit(JLocal x, Context ctx) {
      // locals can conflict, that's okay just reuse the same variable
      JsScope scope = peek();
      JsName jsName = scope.declareName(x.getName());
      names.put(x, jsName);
    }

    // @Override
    public void endVisit(JMethod x, Context ctx) {
      pop();
    }

    // @Override
    public void endVisit(JParameter x, Context ctx) {
      names.put(x, peek().declareName(x.getName()));
    }

    // @Override
    public void endVisit(JProgram x, Context ctx) {
      // visit special things that may have been culled
      JField field = x.getSpecialField("Object.typeId");
      names.put(field, objectScope.declareName(mangleName(field),
          field.getName()));

      field = x.getSpecialField("Object.typeName");
      names.put(field, objectScope.declareName(mangleName(field),
          field.getName()));

      field = x.getSpecialField("Cast.typeIdArray");
      names.put(field, topScope.declareName(mangleName(field), field.getName()));

      /*
       * put the null method and field into objectScope since they can be
       * referenced as instance on null-types (as determined by type flow)
       */
      JMethod nullMethod = x.getNullMethod();
      polymorphicNames.put(nullMethod,
          objectScope.declareName(nullMethod.getName()));
      JField nullField = x.getNullField();
      JsName nullFieldName = objectScope.declareName(nullField.getName());
      polymorphicNames.put(nullField, nullFieldName);
      names.put(nullField, nullFieldName);

      /*
       * put nullMethod in the global scope, too; it's the replacer for clinits
       */
      nullMethodName = topScope.declareName(nullMethod.getName());
      names.put(nullMethod, nullMethodName);
    }

    // @Override
    public boolean visit(JClassType x, Context ctx) {
      // have I already been visited as a super type?
      JsScope myScope = (JsScope) classScopes.get(x);
      if (myScope != null) {
        push(myScope);
        return false;
      }

      // My seed function name
      names.put(x, topScope.declareName(getNameString(x), x.getShortName()));

      // My class scope
      if (x.extnds == null) {
        myScope = objectScope;
      } else {
        JsScope parentScope = (JsScope) classScopes.get(x.extnds);
        // Run my superclass first!
        if (parentScope == null) {
          accept(x.extnds);
        }
        parentScope = (JsScope) classScopes.get(x.extnds);
        assert (parentScope != null);
        /*
         * WEIRD: we wedge the global interface scope in between object and all
         * of its subclasses; this ensures that interface method names trump all
         * (except Object method names)
         */
        if (parentScope == objectScope) {
          parentScope = interfaceScope;
        }
        myScope = new JsScope(parentScope, "class " + x.getShortName());
      }
      classScopes.put(x, myScope);

      // Make a name for my package if it doesn't already exist
      String packageName = getPackageName(x.getName());
      if (packageName != null && packageName.length() > 0) {
        if (packageNames.get(packageName) == null) {
          String ident = "package_" + packageName.replace('.', '_');
          JsName jsName = topScope.declareName(ident);
          packageNames.put(packageName, jsName);
        }
      }

      push(myScope);
      return true;
    }

    // @Override
    public boolean visit(JInterfaceType x, Context ctx) {
      // interfaces have no name at run time
      push(interfaceScope);
      return true;
    }

    // @Override
    public boolean visit(JMethod x, Context ctx) {
      // my polymorphic name
      String name = x.getName();
      if (!x.isStatic()) {
        if (getPolyName(x) == null) {
          String mangleName = mangleNameForPoly(x);
          JsName polyName = interfaceScope.declareName(mangleName, name);
          polymorphicNames.put(x, polyName);
        }
      }

      if (x.isAbstract()) {
        // just push a dummy scope that we can pop in endVisit
        push(null);
        return false;
      }

      // my global name
      JsName globalName;
      if (x.getEnclosingType() == null) {
        globalName = topScope.declareName(name);
      } else {
        String mangleName = mangleNameForGlobal(x);
        globalName = topScope.declareName(mangleName, name);
      }
      names.put(x, globalName);

      JsFunction jsFunction;
      if (x.isNative()) {
        // set the global name of the JSNI peer
        JsniMethodBody body = (JsniMethodBody) x.getBody();
        jsFunction = body.getFunc();
        jsFunction.setName(globalName);
      } else {
        // create a new peer JsFunction
        jsFunction = new JsFunction(topScope, globalName);
        methodBodyMap.put(x.getBody(), jsFunction);
      }
      push(jsFunction.getScope());
      return true;
    }
    
    public boolean visit(JTryStatement x, Context ctx) {
      accept(x.getTryBlock());

      List catchArgs = x.getCatchArgs();
      List catchBlocks = x.getCatchBlocks();
      for (int i = 0, c = catchArgs.size(); i < c; ++i) {
        JLocalRef arg = (JLocalRef) catchArgs.get(i);
        JBlock catchBlock = (JBlock) catchBlocks.get(i);
        JsCatch jsCatch = new JsCatch(peek(), arg.getTarget().getName());
        JsParameter jsParam = jsCatch.getParameter();
        names.put(arg.getTarget(), jsParam.getName());
        catchMap.put(catchBlock, jsCatch);

        push(jsCatch.getScope());
        accept(catchBlock);
        pop();
      }

      // TODO: normalize this so it's never null?
      if (x.getFinallyBlock() != null) {
        accept(x.getFinallyBlock());
      }
      return false;
    }

    private JsScope peek() {
      return (JsScope) scopeStack.peek();
    }

    private void pop() {
      scopeStack.pop();
    }

    private void push(JsScope scope) {
      scopeStack.push(scope);
    }
  }

  private class GenerateJavaScriptVisitor extends JVisitor {

    private final Set/* <JClassType> */alreadyRan = new HashSet/* <JClassType> */();

    private JMethod currentMethod = null;

    private final JsName globalTemp = topScope.declareName("_");

    private final Stack/* <JsNode> */nodeStack = new Stack/* <JsNode> */();

    private final JsName prototype = objectScope.declareName("prototype");

    {
      globalTemp.setObfuscatable(false);
      prototype.setObfuscatable(false);
    }

    // @Override
    public void endVisit(JAbsentArrayDimension x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JArrayRef x, Context ctx) {
      JsArrayAccess jsArrayAccess = new JsArrayAccess();
      jsArrayAccess.setIndexExpr((JsExpression) pop());
      jsArrayAccess.setArrayExpr((JsExpression) pop());
      push(jsArrayAccess);
    }

    // @Override
    public void endVisit(JAssertStatement x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JsExpression rhs = (JsExpression) pop(); // rhs
      JsExpression lhs = (JsExpression) pop(); // lhs
      JsBinaryOperator myOp = JavaToJsOperatorMap.get(x.getOp());

      /*
       * Use === and !== on reference types, or else you can get wrong answers
       * when Object.toString() == 'some string'.
       */
      if (myOp == JsBinaryOperator.EQ
          && x.getLhs().getType() instanceof JReferenceType
          && x.getRhs().getType() instanceof JReferenceType) {
        myOp = JsBinaryOperator.REF_EQ;
      } else if (myOp == JsBinaryOperator.NEQ
          && x.getLhs().getType() instanceof JReferenceType
          && x.getRhs().getType() instanceof JReferenceType) {
        myOp = JsBinaryOperator.REF_NEQ;
      }

      push(new JsBinaryOperation(myOp, lhs, rhs));
    }

    // @Override
    public void endVisit(JBlock x, Context ctx) {
      JsBlock jsBlock = new JsBlock();
      JsStatements stmts = jsBlock.getStatements();
      popList(stmts, x.statements.size()); // stmts
      Iterator iterator = stmts.iterator();
      while (iterator.hasNext()) {
        JsStatement stmt = (JsStatement) iterator.next();
        if (stmt == jsProgram.getEmptyStmt()) {
          iterator.remove();
        }
      }
      push(jsBlock);
    }

    // @Override
    public void endVisit(JBooleanLiteral x, Context ctx) {
      push(x.getValue() ? jsProgram.getTrueLiteral()
          : jsProgram.getFalseLiteral());
    }

    // @Override
    public void endVisit(JBreakStatement x, Context ctx) {
      JsNameRef labelRef = null;
      if (x.getLabel() != null) {
        JsLabel label = (JsLabel) pop(); // label
        labelRef = label.getName().makeRef();
      }
      push(new JsBreak(labelRef));
    }

    // @Override
    public void endVisit(JCaseStatement x, Context ctx) {
      if (x.getExpr() == null) {
        push(new JsDefault());
      } else {
        JsCase jsCase = new JsCase();
        jsCase.setCaseExpr((JsExpression) pop()); // expr
        push(jsCase);
      }
    }

    // @Override
    public void endVisit(JCastOperation x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JCharLiteral x, Context ctx) {
      push(jsProgram.getIntegralLiteral(BigInteger.valueOf(x.getValue())));
    }

    // @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      // My seed function name
      String nameString = x.getRefType().getJavahSignatureName() + "_classlit";
      JsName classLit = topScope.declareName(nameString);
      classLits.put(x.getRefType(), classLit);
      push(classLit.makeRef());
    }

    // @Override
    public void endVisit(JClassSeed x, Context ctx) {
      push(getName(x.getRefType()).makeRef());
    }

    // @Override
    public void endVisit(JClassType x, Context ctx) {
      if (alreadyRan.contains(x)) {
        return;
      }

      alreadyRan.add(x);

      List/* <JsFunction> */jsFuncs = popList(x.methods.size()); // methods
      List/* <JsStatement> */jsFields = popList(x.fields.size()); // fields

      if (typeOracle.hasClinit(x)) {
        handleClinit((JsFunction) jsFuncs.get(0));
      } else {
        jsFuncs.set(0, null);
      }

      JsStatements globalStmts = jsProgram.getGlobalBlock().getStatements();

      // declare all methods into the global scope
      for (int i = 0; i < jsFuncs.size(); ++i) {
        JsFunction func = (JsFunction) jsFuncs.get(i);
        if (func != null) {
          globalStmts.add(func.makeStmt());
        }
      }

      if (typeOracle.isInstantiatedType(x)) {
        generateClassSetup(x, globalStmts);
      }

      // setup fields
      JsVars vars = new JsVars();
      for (int i = 0; i < jsFields.size(); ++i) {
        JsNode node = (JsNode) jsFields.get(i);
        if (node instanceof JsVar) {
          vars.add((JsVar) node);
        } else {
          assert (node instanceof JsStatement);
          JsStatement stmt = (JsStatement) jsFields.get(i);
          globalStmts.add(stmt);
        }
      }
      if (!vars.isEmpty()) {
        globalStmts.add(vars);
      }
    }

    // @Override
    public void endVisit(JConditional x, Context ctx) {
      JsExpression elseExpr = (JsExpression) pop(); // elseExpr
      JsExpression thenExpr = (JsExpression) pop(); // thenExpr
      JsExpression ifTest = (JsExpression) pop(); // ifTest
      push(new JsConditional(ifTest, thenExpr, elseExpr));
    }

    // @Override
    public void endVisit(JContinueStatement x, Context ctx) {
      JsNameRef labelRef = null;
      if (x.getLabel() != null) {
        JsLabel label = (JsLabel) pop(); // label
        labelRef = label.getName().makeRef();
      }
      push(new JsContinue(labelRef));
    }

    // @Override
    public void endVisit(JDoStatement x, Context ctx) {
      JsDoWhile stmt = new JsDoWhile();
      if (x.getBody() != null) {
        stmt.setBody((JsStatement) pop()); // body
      } else {
        stmt.setBody(jsProgram.getEmptyStmt());
      }
      stmt.setCondition((JsExpression) pop()); // testExpr
      push(stmt);
    }

    // @Override
    public void endVisit(JDoubleLiteral x, Context ctx) {
      push(jsProgram.getDecimalLiteral(String.valueOf(x.getValue())));
    }

    // @Override
    public void endVisit(JExpressionStatement x, Context ctx) {
      JsExpression expr = (JsExpression) pop(); // expr
      push(expr.makeStmt());
    }

    // @Override
    public void endVisit(JField x, Context ctx) {
      // if we need an initial value, create an assignment
      if (x.constInitializer != null) {
        // setup the constant value
        accept(x.constInitializer);
      } else if (!x.hasInitializer()) {
        // setup a default value
        accept(x.getType().getDefaultValue());
      } else {
        // the variable is setup during clinit, no need to initialize here
        push(null);
      }
      JsExpression rhs = (JsExpression) pop();
      JsName name = getName(x);

      if (x.isStatic()) {
        // setup a var for the static
        JsVar var = new JsVar(name);
        var.setInitExpr(rhs);
        push(var);
      } else {
        // for non-statics, only setup an assignment if needed
        if (rhs != null) {
          JsNameRef fieldRef = name.makeRef();
          fieldRef.setQualifier(globalTemp.makeRef());
          JsExpression asg = createAssignment(fieldRef, rhs);
          push(new JsExprStmt(asg));
        } else {
          push(null);
        }
      }
    }

    // @Override
    public void endVisit(JFieldRef x, Context ctx) {
      JField field = x.getField();
      JsName jsFieldName = getName(field);
      JsNameRef nameRef = jsFieldName.makeRef();
      JsExpression curExpr = nameRef;

      /*
       * Note: the comma expressions here would cause an illegal tree state if
       * the result expression ended up on the lhs of an assignment. A hack in
       * in endVisit(JBinaryOperation) rectifies the situation.
       */

      // See if we need a clinit
      JsInvocation jsInvocation = maybeCreateClinitCall(field);
      if (jsInvocation != null) {
        curExpr = createCommaExpression(jsInvocation, curExpr);
      }

      if (x.getInstance() != null) {
        JsExpression qualifier = (JsExpression) pop();
        if (field.isStatic()) {
          // unnecessary qualifier, create a comma expression
          curExpr = createCommaExpression(qualifier, curExpr);
        } else {
          // necessary qualifier, qualify the name ref
          nameRef.setQualifier(qualifier);
        }
      }

      push(curExpr);
    }

    // @Override
    public void endVisit(JFloatLiteral x, Context ctx) {
      push(jsProgram.getDecimalLiteral(String.valueOf(x.getValue())));
    }

    // @Override
    public void endVisit(JForStatement x, Context ctx) {
      JsFor jsFor = new JsFor();

      // body
      if (x.getBody() != null) {
        jsFor.setBody((JsStatement) pop());
      } else {
        jsFor.setBody(jsProgram.getEmptyStmt());
      }

      // increments
      {
        JsExpression incrExpr = null;
        List/* <JsExprStmt> */exprStmts = popList(x.getIncrements().size());
        for (int i = 0; i < exprStmts.size(); ++i) {
          JsExprStmt exprStmt = (JsExprStmt) exprStmts.get(i);
          incrExpr = createCommaExpression(incrExpr, exprStmt.getExpression());
        }
        jsFor.setIncrExpr(incrExpr);
      }

      // condition
      if (x.getTestExpr() != null) {
        jsFor.setCondition((JsExpression) pop());
      }

      // initializers
      JsExpression initExpr = null;
      List/* <JsExprStmt> */initStmts = popList(x.getInitializers().size());
      for (int i = 0; i < initStmts.size(); ++i) {
        JsExprStmt initStmt = (JsExprStmt) initStmts.get(i);
        if (initStmt != null) {
          initExpr = createCommaExpression(initExpr, initStmt.getExpression());
        }
      }
      jsFor.setInitExpr(initExpr);

      push(jsFor);
    }

    // @Override
    public void endVisit(JIfStatement x, Context ctx) {
      JsIf stmt = new JsIf();

      if (x.getElseStmt() != null) {
        stmt.setElseStmt((JsStatement) pop()); // elseStmt
      }

      if (x.getThenStmt() != null) {
        stmt.setThenStmt((JsStatement) pop()); // thenStmt
      } else {
        stmt.setThenStmt(jsProgram.getEmptyStmt());
      }

      stmt.setIfExpr((JsExpression) pop()); // ifExpr
      push(stmt);
    }

    // @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      List/* <JsFunction> */jsFuncs = popList(x.methods.size()); // methods
      List/* <JsStatement> */jsFields = popList(x.fields.size()); // fields

      JsStatements globalStmts = jsProgram.getGlobalBlock().getStatements();

      if (typeOracle.hasClinit(x)) {
        JsFunction clinitFunc = (JsFunction) jsFuncs.get(0);
        handleClinit(clinitFunc);
        globalStmts.add(clinitFunc.makeStmt());
      }

      // setup fields
      JsVars vars = new JsVars();
      for (int i = 0; i < jsFields.size(); ++i) {
        JsNode node = (JsNode) jsFields.get(i);
        assert (node instanceof JsVar);
        vars.add((JsVar) node);
      }
      if (!vars.isEmpty()) {
        globalStmts.add(vars);
      }
    }

    // @Override
    public void endVisit(JIntLiteral x, Context ctx) {
      push(jsProgram.getIntegralLiteral(BigInteger.valueOf(x.getValue())));
    }

    // @Override
    public void endVisit(JLabel x, Context ctx) {
      push(new JsLabel(getName(x)));
    }

    // @Override
    public void endVisit(JLabeledStatement x, Context ctx) {
      JsStatement body = (JsStatement) pop(); // body
      JsLabel label = (JsLabel) pop(); // label
      label.setStmt(body);
      push(label);
    }

    // @Override
    public void endVisit(JLocal x, Context ctx) {
      push(getName(x).makeRef());
    }

    // @Override
    public void endVisit(JLocalDeclarationStatement x, Context ctx) {

      if (x.getInitializer() == null) {
        pop(); // localRef
        /*
         * local decls can only appear in blocks, so it's okay to push null
         * instead of an empty statement
         */
        push(null);
        return;
      }

      JsExpression initializer = (JsExpression) pop(); // initializer
      JsNameRef localRef = (JsNameRef) pop(); // localRef

      JsBinaryOperation binOp = new JsBinaryOperation(JsBinaryOperator.ASG,
          localRef, initializer);

      push(binOp.makeStmt());
    }

    // @Override
    public void endVisit(JLocalRef x, Context ctx) {
      push(getName(x.getTarget()).makeRef());
    }

    // @Override
    public void endVisit(JLongLiteral x, Context ctx) {
      push(jsProgram.getIntegralLiteral(BigInteger.valueOf(x.getValue())));
    }

    // @Override
    public void endVisit(JMethod x, Context ctx) {
      if (x.isAbstract()) {
        push(null);
        return;
      }

      JsFunction jsFunc = (JsFunction) pop(); // body
      List/* <JsParameter> */params = popList(x.params.size()); // params

      if (!x.isNative()) {
        // Setup params on the generated function. A native method already got
        // its jsParams set in BuildTypeMap.
        // TODO: Do we really need to do that in BuildTypeMap?
        JsParameters jsParams = jsFunc.getParameters();
        for (int i = 0; i < params.size(); ++i) {
          JsParameter param = (JsParameter) params.get(i);
          jsParams.add(param);
        }
      }

      JsInvocation jsInvocation = maybeCreateClinitCall(x);
      if (jsInvocation != null) {
        jsFunc.getBody().getStatements().add(0, jsInvocation.makeStmt());
      }

      push(jsFunc);
      currentMethod = null;
    }

    // @Override
    public void endVisit(JMethodBody x, Context ctx) {

      JsBlock body = (JsBlock) pop();
      List/* <JsNameRef> */locals = popList(x.locals.size()); // locals

      JsFunction jsFunc = (JsFunction) methodBodyMap.get(x);
      jsFunc.setBody(body); // body

      /*
       * Emit a statement to declare the method's complete set of local
       * variables. JavaScript doesn't have the same concept of lexical scoping
       * as Java, so it's okay to just predeclare all local vars at the top of
       * the function, which saves us having to use the "var" keyword over and
       * over.
       * 
       * Note: it's fine to use the same JS ident to represent two different
       * Java locals of the same name since they could never conflict with each
       * other in Java. We use the alreadySeen set to make sure we don't declare
       * the same-named local var twice.
       */
      JsVars vars = new JsVars();
      Set alreadySeen = new HashSet();
      for (int i = 0; i < locals.size(); ++i) {
        JsName name = (JsName) names.get(x.locals.get(i));
        String ident = name.getIdent();
        if (!alreadySeen.contains(ident)) {
          alreadySeen.add(ident);
          vars.add(new JsVar(name));
        }
      }

      if (!vars.isEmpty()) {
        jsFunc.getBody().getStatements().add(0, vars);
      }

      push(jsFunc);
    }

    // @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      JsInvocation jsInvocation = new JsInvocation();

      popList(jsInvocation.getArguments(), x.getArgs().size()); // args

      JsNameRef qualifier;
      JsExpression unnecessaryQualifier = null;
      if (method.isStatic()) {
        if (x.getInstance() != null) {
          unnecessaryQualifier = (JsExpression) pop(); // instance
        }
        qualifier = getName(method).makeRef();
      } else {
        if (x.isStaticDispatchOnly()) {
          /*
           * Dispatch statically (odd case). This happens when a call that must
           * be static is targeting an instance method that could not be
           * transformed into a static. For example, making a super call into a
           * native method currently causes this, because we cannot currently
           * staticify native methods.
           * 
           * Have to use a "call" construct.
           */
          JsName callName = objectScope.declareName("call");
          callName.setObfuscatable(false);
          qualifier = callName.makeRef();
          qualifier.setQualifier(getName(method).makeRef());
          jsInvocation.getArguments().add(0, (JsExpression) pop()); // instance
        } else {
          // Dispatch polymorphically (normal case).
          qualifier = getPolyName(method).makeRef();
          qualifier.setQualifier((JsExpression) pop()); // instance
        }
      }
      jsInvocation.setQualifier(qualifier);
      push(createCommaExpression(unnecessaryQualifier, jsInvocation));
    }

    // @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      List/* <JsExpression> */exprs = popList(x.exprs.size());
      JsExpression cur = null;
      for (int i = 0; i < exprs.size(); ++i) {
        JsExpression next = (JsExpression) exprs.get(i);
        cur = createCommaExpression(cur, next);
      }
      push(cur);
    }

    // @Override
    public void endVisit(JNewArray x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JNewInstance x, Context ctx) {
      JsNew newOp = new JsNew();
      JsNameRef nameRef = getName(x.getType()).makeRef();
      newOp.setConstructorExpression(nameRef);
      push(newOp);
    }

    // @Override
    public void endVisit(JNullLiteral x, Context ctx) {
      push(jsProgram.getNullLiteral());
    }

    // @Override
    public void endVisit(JParameter x, Context ctx) {
      push(new JsParameter(getName(x)));
    }

    // @Override
    public void endVisit(JParameterRef x, Context ctx) {
      push(getName(x.getTarget()).makeRef());
    }

    // @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      JsUnaryOperation op = new JsPostfixOperation(
          JavaToJsOperatorMap.get(x.getOp()), ((JsExpression) pop())); // arg
      push(op);
    }

    // @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      JsUnaryOperation op = new JsPrefixOperation(
          JavaToJsOperatorMap.get(x.getOp()), ((JsExpression) pop())); // arg
      push(op);
    }

    // @Override
    public void endVisit(JProgram x, Context ctx) {
      JsStatements globalStmts = jsProgram.getGlobalBlock().getStatements();

      // types don't push

      // Generate entry methods
      List/* <JsFunction> */entryFuncs = popList(x.entryMethods.size()); // entryMethods
      for (int i = 0; i < entryFuncs.size(); ++i) {
        JsFunction func = (JsFunction) entryFuncs.get(i);
        if (func != null) {
          globalStmts.add(func.makeStmt());
        }
      }

      generateGwtOnLoad(entryFuncs, globalStmts);

      // a few more vars on the very end
      JsVars vars = new JsVars();
      generateTypeTable(vars);
      generateClassLiterals(vars);
      globalStmts.add(vars);
    }

    // @Override
    public void endVisit(JReturnStatement x, Context ctx) {
      if (x.getExpr() != null) {
        push(new JsReturn((JsExpression) pop())); // expr
      } else {
        push(new JsReturn());
      }
    }

    // @Override
    public void endVisit(JsniMethodBody x, Context ctx) {
      JsFunction jsFunc = x.getFunc();

      // replace all JSNI idents with a real JsName now that we know it
      new JsModVisitor() {
        // @Override
        public void endVisit(JsNameRef x, JsContext ctx) {
          String ident = x.getIdent();
          if (ident.charAt(0) == '@') {
            HasEnclosingType node = (HasEnclosingType) program.jsniMap.get(ident);
            assert (node != null);
            if (node instanceof JField) {
              JField field = (JField) node;
              JsName jsName = getName(field);
              assert (jsName != null);
              x.resolve(jsName);

              // See if we need to add a clinit call to a static field ref
              JsInvocation clinitCall = maybeCreateClinitCall(field);
              if (clinitCall != null) {
                JsExpression commaExpr = createCommaExpression(clinitCall, x);
                ctx.replaceMe(commaExpr);
              }
            } else {
              JMethod method = (JMethod) node;
              if (x.getQualifier() == null) {
                JsName jsName = getName(method);
                assert (jsName != null);
                x.resolve(jsName);
              } else {
                JsName jsName = getPolyName(method);
                if (jsName == null) {
                  // this can occur when JSNI references an instance method on a
                  // type that was never actually instantiated.
                  jsName = nullMethodName;
                }
                x.resolve(jsName);
              }
            }
          }
        }
      }.accept(jsFunc);

      push(jsFunc);
    }

    // @Override
    public void endVisit(JsonArray x, Context ctx) {
      JsArrayLiteral jsArrayLiteral = new JsArrayLiteral();
      popList(jsArrayLiteral.getExpressions(), x.exprs.size());
      push(jsArrayLiteral);
    }

    // @Override
    public void endVisit(JsonObject x, Context ctx) {
      JsObjectLiteral jsObjectLiteral = new JsObjectLiteral();
      popList(jsObjectLiteral.getPropertyInitializers(), x.propInits.size());
      push(jsObjectLiteral);
    }

    // @Override
    public void endVisit(JsonPropInit init, Context ctx) {
      JsExpression valueExpr = (JsExpression) pop();
      JsExpression labelExpr = (JsExpression) pop();
      push(new JsPropertyInitializer(labelExpr, valueExpr));
    }

    // @Override
    public void endVisit(JStringLiteral x, Context ctx) {
      push(jsProgram.getStringLiteral(x.getValue()));
    }

    // @Override
    public void endVisit(JThisRef x, Context ctx) {
      push(new JsThisRef());
    }

    // @Override
    public void endVisit(JThrowStatement x, Context ctx) {
      push(new JsThrow((JsExpression) pop())); // expr
    }

    // @Override
    public void endVisit(JTryStatement x, Context ctx) {
      JsTry jsTry = new JsTry();

      if (x.getFinallyBlock() != null) {
        JsBlock finallyBlock = (JsBlock) pop(); // finallyBlock
        if (finallyBlock.getStatements().size() > 0) {
          jsTry.setFinallyBlock(finallyBlock);
        }
      }

      int size = x.getCatchArgs().size();
      assert (size < 2 && size == x.getCatchBlocks().size());
      if (size == 1) {
        JsBlock catchBlock = (JsBlock) pop(); // catchBlocks
        pop(); // catchArgs
        JsCatch jsCatch = (JsCatch) catchMap.get(x.getCatchBlocks().get(0));
        jsCatch.setBody(catchBlock);
        jsTry.getCatches().add(jsCatch);
      }

      jsTry.setTryBlock((JsBlock) pop()); // tryBlock

      push(jsTry);
    }

    // @Override
    public void endVisit(JWhileStatement x, Context ctx) {
      JsWhile stmt = new JsWhile();
      if (x.getBody() != null) {
        stmt.setBody((JsStatement) pop()); // body
      } else {
        stmt.setBody(jsProgram.getEmptyStmt());
      }
      stmt.setCondition((JsExpression) pop()); // testExpr
      push(stmt);
    }

    // @Override
    public boolean visit(JClassType x, Context ctx) {
      if (alreadyRan.contains(x)) {
        return false;
      }

      // force super type to generate code first, this is required for prototype
      // chaining to work properly
      if (x.extnds != null && !alreadyRan.contains(x)) {
        accept(x.extnds);
      }

      return true;
    }

    // @Override
    public boolean visit(JMethod x, Context ctx) {
      if (x.isAbstract()) {
        return false;
      }
      currentMethod = x;
      return true;
    }

    // @Override
    public boolean visit(JProgram x, Context ctx) {
      JsStatements globalStatements = jsProgram.getGlobalBlock().getStatements();

      // declare some global vars
      JsVars vars = new JsVars();

      // reserve the "_" identifier
      vars.add(new JsVar(globalTemp));
      generatePackageNames(vars);
      globalStatements.add(vars);

      generateNullFunc(globalStatements);
      return true;
    }

    public boolean visit(JSwitchStatement x, Context ctx) {
      /*
       * What a pain.. JSwitchStatement and JsSwitch are modeled completely
       * differently. Here we try to resolve those differences.
       */
      JsSwitch jsSwitch = new JsSwitch();
      accept(x.getExpr());
      jsSwitch.setExpr((JsExpression) pop()); // expr

      List/* <JStatement> */bodyStmts = x.getBody().statements;
      if (bodyStmts.size() > 0) {
        JsStatements curStatements = null;
        for (int i = 0; i < bodyStmts.size(); ++i) {
          JStatement stmt = (JStatement) bodyStmts.get(i);
          accept(stmt);
          if (stmt instanceof JCaseStatement) {
            // create a new switch member
            JsSwitchMember switchMember = (JsSwitchMember) pop(); // stmt
            jsSwitch.getCases().add(switchMember);
            curStatements = switchMember.getStmts();
          } else {
            // add to statements for current case
            assert (curStatements != null);
            JsStatement newStmt = (JsStatement) pop(); // stmt
            if (newStmt != null) {
              // Empty JLocalDeclarationStatement produces a null
              curStatements.add(newStmt);
            }
          }
        }
      }

      push(jsSwitch);
      return false;
    }

    private JsExpression createAssignment(JsExpression lhs, JsExpression rhs) {
      return new JsBinaryOperation(JsBinaryOperator.ASG, lhs, rhs);
    }

    private JsExpression createCommaExpression(JsExpression lhs,
        JsExpression rhs) {
      if (lhs == null) {
        return rhs;
      } else if (rhs == null) {
        return lhs;
      }
      return new JsBinaryOperation(JsBinaryOperator.COMMA, lhs, rhs);
    }

    private void generateClassLiterals(JsVars vars) {
      /*
       * Class literals are useless right now, just use String literals and the
       * Object methods will basically work.
       */
      for (Iterator itType = classLits.keySet().iterator(); itType.hasNext();) {
        JType type = (JType) itType.next();
        JsName jsName = (JsName) classLits.get(type);
        String string;
        if (type instanceof JArrayType) {
          string = "class " + type.getJsniSignatureName().replace('/', '.');
        } else if (type instanceof JClassType) {
          string = "class " + type.getName();
        } else if (type instanceof JInterfaceType) {
          string = "interface " + type.getName();
        } else {
          string = type.getName();
        }
        JsStringLiteral stringLiteral = jsProgram.getStringLiteral(string);
        JsVar var = new JsVar(jsName);
        var.setInitExpr(stringLiteral);
        vars.add(var);
      }
    }

    private void generateClassSetup(JClassType x, JsStatements globalStmts) {
      generateSeedFuncAndPrototype(x, globalStmts);
      generateVTables(x, globalStmts);

      if (x == program.getTypeJavaLangObject()) {
        // special: setup a "toString" alias for java.lang.Object.toString()
        generateToStringAlias(x, globalStmts);
      }

      generateTypeName(x, globalStmts);
      generateTypeId(x, globalStmts);
    }

    private void generateGwtOnLoad(List entryFuncs, JsStatements globalStmts) {
      /**
       * <pre>
       * function gwtOnLoad(errFn, modName, modBase){
       *   $moduleName = modName;
       *   $moduleBase = modBase;
       *   if (errFn) {
       *     try {
       *       init();
       *     } catch(e) {
       *       errFn(modName);
       *     }
       *   } else {
       *     init();
       *   }
       * }
       * </pre>
       */
      JsFunction gwtOnLoad = new JsFunction(topScope);
      globalStmts.add(gwtOnLoad.makeStmt());
      JsName gwtOnLoadName = topScope.declareName("gwtOnLoad");
      gwtOnLoadName.setObfuscatable(false);
      gwtOnLoad.setName(gwtOnLoadName);
      JsBlock body = new JsBlock();
      gwtOnLoad.setBody(body);
      JsScope fnScope = gwtOnLoad.getScope();
      JsParameters params = gwtOnLoad.getParameters();
      JsName errFn = fnScope.declareName("errFn");
      JsName modName = fnScope.declareName("modName");
      JsName modBase = fnScope.declareName("modBase");
      params.add(new JsParameter(errFn));
      params.add(new JsParameter(modName));
      params.add(new JsParameter(modBase));
      JsExpression asg = createAssignment(
          topScope.findExistingUnobfuscatableName("$moduleName").makeRef(),
          modName.makeRef());
      body.getStatements().add(asg.makeStmt());
      asg = createAssignment(topScope.findExistingUnobfuscatableName(
          "$moduleBase").makeRef(), modBase.makeRef());
      body.getStatements().add(asg.makeStmt());
      JsIf jsIf = new JsIf();
      body.getStatements().add(jsIf);
      jsIf.setIfExpr(errFn.makeRef());
      JsTry jsTry = new JsTry();
      jsIf.setThenStmt(jsTry);
      JsBlock callBlock = new JsBlock();
      jsIf.setElseStmt(callBlock);
      jsTry.setTryBlock(callBlock);
      for (int i = 0; i < entryFuncs.size(); ++i) {
        JsFunction func = (JsFunction) entryFuncs.get(i);
        if (func != null) {
          JsInvocation call = new JsInvocation();
          call.setQualifier(func.getName().makeRef());
          callBlock.getStatements().add(call.makeStmt());
        }
      }
      JsCatch jsCatch = new JsCatch(fnScope, "e");
      jsTry.getCatches().add(jsCatch);
      JsBlock catchBlock = new JsBlock();
      jsCatch.setBody(catchBlock);
      JsInvocation errCall = new JsInvocation();
      catchBlock.getStatements().add(errCall.makeStmt());
      errCall.setQualifier(errFn.makeRef());
      errCall.getArguments().add(modName.makeRef());
    }

    private void generateNullFunc(JsStatements globalStatements) {
      // handle null method
      JsFunction nullFunc = new JsFunction(topScope, nullMethodName);
      nullFunc.setBody(new JsBlock());
      globalStatements.add(nullFunc.makeStmt());
    }

    private void generatePackageNames(JsVars vars) {
      for (Iterator it = packageNames.entrySet().iterator(); it.hasNext();) {
        Map.Entry entry = (Entry) it.next();
        String packageName = (String) entry.getKey();
        JsName name = (JsName) entry.getValue();
        JsVar jsVar = new JsVar(name);
        jsVar.setInitExpr(jsProgram.getStringLiteral(packageName));
        vars.add(jsVar);
      }
    }

    private void generateSeedFuncAndPrototype(JClassType x,
        JsStatements globalStmts) {
      if (x != program.getTypeJavaLangString()) {
        JsName seedFuncName = getName(x);

        // seed function
        // function com_example_foo_Foo() { }
        JsFunction seedFunc = new JsFunction(topScope, seedFuncName);
        JsBlock body = new JsBlock();
        seedFunc.setBody(body);
        globalStmts.add(seedFunc.makeStmt());

        // setup prototype, assign to temp
        // _ = com_example_foo_Foo.prototype = new com_example_foo_FooSuper();
        JsNameRef lhs = prototype.makeRef();
        lhs.setQualifier(seedFuncName.makeRef());
        JsExpression rhs;
        if (x.extnds != null) {
          JsNew newExpr = new JsNew();
          newExpr.setConstructorExpression(getName(x.extnds).makeRef());
          rhs = newExpr;
        } else {
          rhs = new JsObjectLiteral();
        }
        JsExpression protoAsg = createAssignment(lhs, rhs);
        JsExpression tmpAsg = createAssignment(globalTemp.makeRef(), protoAsg);
        globalStmts.add(tmpAsg.makeStmt());
      } else {
        /*
         * MAGIC: java.lang.String is implemented as a JavaScript String
         * primitive with a modified prototype.
         */
        JsNameRef rhs = prototype.makeRef();
        rhs.setQualifier(jsProgram.getRootScope().declareName("String").makeRef());
        JsExpression tmpAsg = createAssignment(globalTemp.makeRef(), rhs);
        globalStmts.add(tmpAsg.makeStmt());
      }
    }

    private void generateToStringAlias(JClassType x, JsStatements globalStmts) {
      JMethod toStringMeth = program.getSpecialMethod("Object.toString");
      if (x.methods.contains(toStringMeth)) {
        // _.toString = function(){return this.java_lang_Object_toString();}

        // lhs
        JsName lhsName = objectScope.declareName("toString");
        lhsName.setObfuscatable(false);
        JsNameRef lhs = lhsName.makeRef();
        lhs.setQualifier(globalTemp.makeRef());

        // rhs
        JsInvocation call = new JsInvocation();
        JsNameRef toStringRef = new JsNameRef(getPolyName(toStringMeth));
        toStringRef.setQualifier(new JsThisRef());
        call.setQualifier(toStringRef);
        JsReturn jsReturn = new JsReturn(call);
        JsFunction rhs = new JsFunction(topScope);
        JsBlock body = new JsBlock();
        body.getStatements().add(jsReturn);
        rhs.setBody(body);

        // asg
        JsExpression asg = createAssignment(lhs, rhs);
        globalStmts.add(new JsExprStmt(asg));
      }
    }

    private void generateTypeId(JClassType x, JsStatements globalStmts) {
      int typeId = program.getTypeId(x);
      if (typeId >= 0) {
        JField typeIdField = program.getSpecialField("Object.typeId");
        JsNameRef fieldRef = getName(typeIdField).makeRef();
        fieldRef.setQualifier(globalTemp.makeRef());
        JsIntegralLiteral typeIdLit = jsProgram.getIntegralLiteral(BigInteger.valueOf(typeId));
        JsExpression asg = createAssignment(fieldRef, typeIdLit);
        globalStmts.add(new JsExprStmt(asg));
      }
    }

    private void generateTypeName(JClassType x, JsStatements globalStmts) {
      JField typeIdField = program.getSpecialField("Object.typeName");
      JsNameRef lhs = getName(typeIdField).makeRef();
      lhs.setQualifier(globalTemp.makeRef());

      // Split the full class name into package + class so package strings
      // can be merged.
      String className = getClassName(x.getName());
      String packageName = getPackageName(x.getName());
      JsExpression rhs;
      if (packageName.length() > 0) {
        // use "com.example.foo." + "Foo"
        JsName name = (JsName) packageNames.get(packageName);
        rhs = new JsBinaryOperation(JsBinaryOperator.ADD, name.makeRef(),
            jsProgram.getStringLiteral(className));
      } else {
        // no package name could be split, just use the full name
        rhs = jsProgram.getStringLiteral(x.getName());
      }
      JsExpression asg = createAssignment(lhs, rhs);
      globalStmts.add(new JsExprStmt(asg));
    }

    private void generateTypeTable(JsVars vars) {
      JField typeIdArray = program.getSpecialField("Cast.typeIdArray");
      JsName jsName = getName(typeIdArray);
      JsArrayLiteral arrayLit = new JsArrayLiteral();
      for (int i = 0; i < program.getJsonTypeTable().size(); ++i) {
        JsonObject jsonObject = (JsonObject) program.getJsonTypeTable().get(i);
        accept(jsonObject);
        arrayLit.getExpressions().add((JsExpression) pop());
      }
      JsVar var = new JsVar(jsName);
      var.setInitExpr(arrayLit);
      vars.add(var);
    }

    private void generateVTables(JClassType x, JsStatements globalStmts) {
      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        if (!method.isStatic() && !method.isAbstract()) {
          JsNameRef lhs = getPolyName(method).makeRef();
          lhs.setQualifier(globalTemp.makeRef());
          JsNameRef rhs = getName(method).makeRef();
          JsExpression asg = createAssignment(lhs, rhs);
          globalStmts.add(new JsExprStmt(asg));
        }
      }
    }

    private void handleClinit(JsFunction clinitFunc) {
      JsStatements statements = clinitFunc.getBody().getStatements();
      // self-assign to the null method immediately (to prevent reentrancy)
      JsExpression asg = createAssignment(clinitFunc.getName().makeRef(),
          nullMethodName.makeRef());
      statements.add(0, asg.makeStmt());
    }

    private JsInvocation maybeCreateClinitCall(JField x) {
      if (!x.isStatic()) {
        return null;
      }

      JReferenceType enclosingType = x.getEnclosingType();
      if (!typeOracle.checkClinit(currentMethod.getEnclosingType(),
          enclosingType)) {
        return null;
      }

      JMethod clinitMethod = (JMethod) enclosingType.methods.get(0);
      JsInvocation jsInvocation = new JsInvocation();
      jsInvocation.setQualifier(getName(clinitMethod).makeRef());
      return jsInvocation;
    }

    private JsInvocation maybeCreateClinitCall(JMethod x) {
      if (!x.isStatic()) {
        return null;
      }
      JReferenceType enclosingType = x.getEnclosingType();
      if (!typeOracle.hasClinit(enclosingType)) {
        return null;
      }
      if (program.isStaticImpl(x)) {
        return null;
      }
      // avoid recursion sickness
      if (x == enclosingType.methods.get(0)) {
        return null;
      }

      JMethod clinitMethod = (JMethod) enclosingType.methods.get(0);
      JsInvocation jsInvocation = new JsInvocation();
      jsInvocation.setQualifier(getName(clinitMethod).makeRef());
      return jsInvocation;
    }

    private JsNode pop() {
      return (JsNode) nodeStack.pop();
    }

    private/* <T extends JsNode> */List/* <T> */popList(int count) {
      JsNode[] array = new JsNode[count];
      while (count > 0) {
        array[--count] = pop();
      }

      List/* <T> */list = new ArrayList/* <T> */();
      for (int i = 0; i < array.length; i++) {
        JsNode item = array[i];
        if (item != null) {
          list.add(/* (T) */item);
        }
      }
      return list;
    }

    private void popList(JsCollection collection, int count) {
      JsNode[] array = new JsNode[count];
      while (count > 0) {
        array[--count] = pop();
      }

      for (int i = 0; i < array.length; i++) {
        JsNode item = array[i];
        if (item != null) {
          collection.addNode(item);
        }
      }
    }

    private void push(JsNode node) {
      nodeStack.push(node);
    }
  }

  private static class JavaToJsOperatorMap {
    private static final Map/* <JBinaryOperator, JsBinaryOperator> */bOpMap = new IdentityHashMap();
    private static final Map/* <JUnaryOperator, JsUnaryOperator> */uOpMap = new IdentityHashMap();

    static {
      bOpMap.put(JBinaryOperator.MUL, JsBinaryOperator.MUL);
      bOpMap.put(JBinaryOperator.DIV, JsBinaryOperator.DIV);
      bOpMap.put(JBinaryOperator.MOD, JsBinaryOperator.MOD);
      bOpMap.put(JBinaryOperator.ADD, JsBinaryOperator.ADD);
      bOpMap.put(JBinaryOperator.SUB, JsBinaryOperator.SUB);
      bOpMap.put(JBinaryOperator.SHL, JsBinaryOperator.SHL);
      bOpMap.put(JBinaryOperator.SHR, JsBinaryOperator.SHR);
      bOpMap.put(JBinaryOperator.SHRU, JsBinaryOperator.SHRU);
      bOpMap.put(JBinaryOperator.LT, JsBinaryOperator.LT);
      bOpMap.put(JBinaryOperator.LTE, JsBinaryOperator.LTE);
      bOpMap.put(JBinaryOperator.GT, JsBinaryOperator.GT);
      bOpMap.put(JBinaryOperator.GTE, JsBinaryOperator.GTE);
      bOpMap.put(JBinaryOperator.EQ, JsBinaryOperator.EQ);
      bOpMap.put(JBinaryOperator.NEQ, JsBinaryOperator.NEQ);
      bOpMap.put(JBinaryOperator.BIT_AND, JsBinaryOperator.BIT_AND);
      bOpMap.put(JBinaryOperator.BIT_XOR, JsBinaryOperator.BIT_XOR);
      bOpMap.put(JBinaryOperator.BIT_OR, JsBinaryOperator.BIT_OR);
      bOpMap.put(JBinaryOperator.AND, JsBinaryOperator.AND);
      bOpMap.put(JBinaryOperator.OR, JsBinaryOperator.OR);
      bOpMap.put(JBinaryOperator.ASG, JsBinaryOperator.ASG);
      bOpMap.put(JBinaryOperator.ASG_ADD, JsBinaryOperator.ASG_ADD);
      bOpMap.put(JBinaryOperator.ASG_SUB, JsBinaryOperator.ASG_SUB);
      bOpMap.put(JBinaryOperator.ASG_MUL, JsBinaryOperator.ASG_MUL);
      bOpMap.put(JBinaryOperator.ASG_DIV, JsBinaryOperator.ASG_DIV);
      bOpMap.put(JBinaryOperator.ASG_MOD, JsBinaryOperator.ASG_MOD);
      bOpMap.put(JBinaryOperator.ASG_SHL, JsBinaryOperator.ASG_SHL);
      bOpMap.put(JBinaryOperator.ASG_SHR, JsBinaryOperator.ASG_SHR);
      bOpMap.put(JBinaryOperator.ASG_SHRU, JsBinaryOperator.ASG_SHRU);
      bOpMap.put(JBinaryOperator.ASG_BIT_AND, JsBinaryOperator.ASG_BIT_AND);
      bOpMap.put(JBinaryOperator.ASG_BIT_OR, JsBinaryOperator.ASG_BIT_OR);
      bOpMap.put(JBinaryOperator.ASG_BIT_XOR, JsBinaryOperator.ASG_BIT_XOR);

      uOpMap.put(JUnaryOperator.INC, JsUnaryOperator.INC);
      uOpMap.put(JUnaryOperator.DEC, JsUnaryOperator.DEC);
      uOpMap.put(JUnaryOperator.NEG, JsUnaryOperator.NEG);
      uOpMap.put(JUnaryOperator.NOT, JsUnaryOperator.NOT);
      uOpMap.put(JUnaryOperator.BIT_NOT, JsUnaryOperator.BIT_NOT);
    }

    public static JsBinaryOperator get(JBinaryOperator op) {
      return (JsBinaryOperator) bOpMap.get(op);
    }

    public static JsUnaryOperator get(JUnaryOperator op) {
      return (JsUnaryOperator) uOpMap.get(op);
    }
  }

  private class SortVisitor extends JVisitor {

    private final HasNameSort hasNameSort = new HasNameSort();

    public void endVisit(JClassType x, Context ctx) {
      Collections.sort(x.fields, hasNameSort);

      // Sort the methods manually to avoid sorting clinit out of place!
      List methods = x.methods;
      Object a[] = methods.toArray();
      Arrays.sort(a, 1, a.length, hasNameSort);
      for (int i = 1; i < a.length; i++) {
        methods.set(i, a[i]);
      }
    }

    public void endVisit(JInterfaceType x, Context ctx) {
      Collections.sort(x.fields, hasNameSort);
      Collections.sort(x.methods, hasNameSort);
    }

    public void endVisit(JMethodBody x, Context ctx) {
      Collections.sort(x.locals, hasNameSort);
    }

    public void endVisit(JProgram x, Context ctx) {
      Collections.sort(x.entryMethods, hasNameSort);
      Collections.sort(x.getDeclaredTypes(), hasNameSort);
    }
  }

  public static void exec(JProgram program, JsProgram jsProgram) {
    GenerateJavaScriptAST generateJavaScriptAST = new GenerateJavaScriptAST(
        program, jsProgram);
    generateJavaScriptAST.execImpl();
  }

  private static String getClassName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(pos + 1);
  }

  private static String getPackageName(String fullName) {
    int pos = fullName.lastIndexOf(".");
    return fullName.substring(0, pos + 1);
  }

  private final Map/* <JBlock, JsCatch> */catchMap = new IdentityHashMap();
  private final Map/* <JType, JsName> */classLits = new IdentityHashMap();
  private final Map/* <JClassType, JsScope> */classScopes = new IdentityHashMap();

  /**
   * Contains JsNames for all interface methods. A special scope is needed so
   * that independent classes will obfuscate their interface implementation
   * methods the same way.
   */
  private final JsScope interfaceScope;

  private final JsProgram jsProgram;
  private final Map/* <JMethodBody, JsFunction> */methodBodyMap = new IdentityHashMap();
  private final Map/* <HasName, JsName> */names = new IdentityHashMap();
  private JsName nullMethodName;

  /**
   * Contains JsNames for the Object instance methods, such as equals, hashCode,
   * and toString. All other class scopes have this scope as an ultimate parent.
   */
  private final JsScope objectScope;
  private final Map/* <String, JsName> */packageNames = new TreeMap();
  private final Map/* <JMethod, JsName> */polymorphicNames = new IdentityHashMap();
  private final JProgram program;

  /**
   * Contains JsNames for all globals, such as static fields and methods.
   */
  private final JsScope topScope;

  private final JTypeOracle typeOracle;

  private GenerateJavaScriptAST(JProgram program, JsProgram jsProgram) {
    this.program = program;
    typeOracle = program.typeOracle;
    this.jsProgram = jsProgram;
    topScope = jsProgram.getScope();
    objectScope = jsProgram.getObjectScope();
    interfaceScope = new JsScope(objectScope, "Interfaces");
  }

  String getNameString(HasName hasName) {
    String s = hasName.getName().replaceAll("_", "_1").replace('.', '_');
    return s;
  }

  String mangleName(JField x) {
    String s = getNameString(x.getEnclosingType()) + '_' + getNameString(x);
    return s;
  }

  String mangleNameForGlobal(JMethod x) {
    String s = getNameString(x.getEnclosingType()) + '_' + getNameString(x)
        + "__";
    for (int i = 0; i < x.getOriginalParamTypes().size(); ++i) {
      JType type = (JType) x.getOriginalParamTypes().get(i);
      s += type.getJavahSignatureName();
    }
    return s;
  }

  String mangleNameForPoly(JMethod x) {
    String s = getNameString(x) + "__";
    for (int i = 0; i < x.getOriginalParamTypes().size(); ++i) {
      JType type = (JType) x.getOriginalParamTypes().get(i);
      s += type.getJavahSignatureName();
    }
    return s;
  }

  private void execImpl() {
    SortVisitor sorter = new SortVisitor();
    sorter.accept(program);
    CreateNamesAndScopesVisitor creator = new CreateNamesAndScopesVisitor();
    creator.accept(program);
    GenerateJavaScriptVisitor generator = new GenerateJavaScriptVisitor();
    generator.accept(program);
  }

  private JsName getName(HasName x) {
    return (JsName) names.get(x);
  }

  private JsName getPolyName(HasName x) {
    return (JsName) polymorphicNames.get(x);
  }

}
