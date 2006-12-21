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
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;
import com.google.gwt.dev.js.JsAbstractVisitorWithAllVisits;
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
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsObfuscatableName;
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
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Creates a JavaScript AST from a <code>JProgram</code> node.
 */
public class GenerateJavaScriptAST {

  private class CreateNamesAndScopesVisitor extends JVisitor {

    private final Stack/* <JsScope> */scopeStack = new Stack();

    // @Override
    public void endVisit(JClassType x) {
      pop();
    }

    // @Override
    public void endVisit(JField x) {
      String name = x.getName();
      String mangleName = mangleName(x);
      if (x.isStatic()) {
        names.put(x, rootScope.createUniqueObfuscatableName(mangleName, name));
      } else {
        names.put(x, peek().createUniqueObfuscatableName(mangleName, name));
      }
    }

    // @Override
    public void endVisit(JInterfaceType x) {
      pop();
    }

    // @Override
    public void endVisit(JLabel x) {
      if (getName(x) != null) {
        return;
      }
      names.put(x, peek().getOrCreateObfuscatableName(x.getName()));
    }

    // @Override
    public void endVisit(JLocal x) {
      // locals can conflict, that's okay just reuse the same variable
      JsScope scope = peek();
      JsName jsName = scope.getOrCreateObfuscatableName(x.getName());
      names.put(x, jsName);
    }

    // @Override
    public void endVisit(JMethod x) {
      pop();
    }

    // @Override
    public void endVisit(JParameter x) {
      names.put(x, peek().createUniqueObfuscatableName(x.getName()));
    }

    // @Override
    public void endVisit(JProgram x) {
      // visit special things that may have been culled
      JField field = x.getSpecialField("Object.typeId");
      names.put(field, objectScope.getOrCreateObfuscatableName(
          mangleName(field), field.getName()));

      field = x.getSpecialField("Object.typeName");
      names.put(field,
          objectScope.getOrCreateObfuscatableName(mangleName(field)));

      field = x.getSpecialField("Cast.typeIdArray");
      names.put(field, rootScope.getOrCreateObfuscatableName(mangleName(field),
          field.getName()));

      /*
       * put the null method and field into fObjectScope since they can be
       * referenced as instance on null-types (as determined by type flow)
       */
      JMethod nullMethod = x.getNullMethod();
      polymorphicNames.put(nullMethod,
          objectScope.createUniqueObfuscatableName(nullMethod.getName()));
      JField nullField = x.getNullField();
      JsName nullFieldName = objectScope.createUniqueObfuscatableName(nullField.getName());
      polymorphicNames.put(nullField, nullFieldName);
      names.put(nullField, nullFieldName);

      /*
       * put nullMethod in the global scope, too; it's the replacer for clinits
       */
      nullMethodName = rootScope.createUniqueObfuscatableName(nullMethod.getName());
      names.put(nullMethod, nullMethodName);
    }

    // @Override
    public void endVisit(JsniMethod x) {
      // didn't push anything
    }

    // @Override
    public boolean visit(JClassType x) {
      // have I already been visited as a supertype?
      JsScope myScope = (JsScope) classScopes.get(x);
      if (myScope != null) {
        push(myScope);
        return false;
      }

      // My seed function name
      names.put(x, rootScope.createUniqueObfuscatableName(getNameString(x),
          x.getShortName()));

      // My class scope
      if (x.extnds == null) {
        myScope = objectScope;
      } else {
        JsScope parentScope = (JsScope) classScopes.get(x.extnds);
        // Run my superclass first!
        if (parentScope == null) {
          x.extnds.traverse(this);
        }
        parentScope = (JsScope) classScopes.get(x.extnds);
        assert (parentScope != null);
        /*
         * Wacky, we wedge the global interface scope in between object and all
         * of its subclasses; this ensures that interface method names trump all
         * (except Object methods names)
         */
        if (parentScope == objectScope) {
          parentScope = interfaceScope;
        }
        myScope = new JsScope(parentScope);
        myScope.setDescription("class " + x.getShortName());
      }
      classScopes.put(x, myScope);

      push(myScope);
      return true;
    }

    // @Override
    public boolean visit(JInterfaceType x) {
      // interfaces have no name at run time
      push(interfaceScope);
      return true;
    }

    // @Override
    public boolean visit(JMethod x) {

      // my polymorphic name
      String name = x.getName();
      if (!x.isStatic()) {
        if (getPolyName(x) == null) {
          String mangleName = mangleNameForPoly(x);
          JsName polyName = interfaceScope.getOrCreateObfuscatableName(
              mangleName, name);
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
        globalName = rootScope.createUniqueObfuscatableName(name);
      } else {
        String mangleName = mangleNameForGlobal(x);
        globalName = rootScope.createUniqueObfuscatableName(mangleName, name);
      }
      names.put(x, globalName);

      // create my peer JsFunction
      JsFunction jsFunction = new JsFunction(rootScope, globalName);
      methodMap.put(x, jsFunction);

      push(jsFunction.getScope());
      return true;
    }

    // @Override
    public boolean visit(JsniMethod x) {
      // my polymorphic name
      String name = x.getName();
      if (!x.isStatic()) {
        if (getPolyName(x) == null) {
          String mangleName = mangleNameForPoly(x);
          JsName polyName = interfaceScope.getOrCreateObfuscatableName(
              mangleName, name);
          polymorphicNames.put(x, polyName);
        }
      }

      // set my global name now that we have a name allocator
      String fnName = mangleNameForGlobal(x);
      JsName globalName = rootScope.createUniqueObfuscatableName(fnName, name);
      x.getFunc().setName(globalName);
      names.put(x, globalName);

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

    private final JsName globalTemp = rootScope.getOrCreateUnobfuscatableName("_");

    private final JsName prototype = objectScope.getOrCreateUnobfuscatableName("prototype");

    private final JsName window = rootScope.getOrCreateUnobfuscatableName("window");

    private final Stack/* <JsNode> */nodeStack = new Stack/* <JsNode> */();

    // @Override
    public void endVisit(JAbsentArrayDimension x, Mutator m) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JArrayRef x, Mutator m) {
      JsArrayAccess jsArrayAccess = new JsArrayAccess();
      jsArrayAccess.setIndexExpr((JsExpression) pop());
      jsArrayAccess.setArrayExpr((JsExpression) pop());
      push(jsArrayAccess);
    }

    // @Override
    public void endVisit(JAssertStatement x) {
      // TODO: implement assert
      if (x.getArg() != null) {
        pop(); // arg
      }
      pop(); // testExpr
      push(jsProgram.getEmptyStmt());
    }

    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      JsExpression rhs = (JsExpression) pop(); // rhs
      JsExpression lhs = (JsExpression) pop(); // lhs
      JsBinaryOperator myOp = JavaToJsOperatorMap.get(x.op);

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
      JsBinaryOperation binOp = new JsBinaryOperation(myOp, lhs, rhs);
      push(binOp);
    }

    // @Override
    public void endVisit(JBlock x) {
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
    public void endVisit(JBooleanLiteral x, Mutator m) {
      push(x.value ? jsProgram.getTrueLiteral() : jsProgram.getFalseLiteral());
    }

    // @Override
    public void endVisit(JBreakStatement x) {
      JsNameRef labelRef = null;
      if (x.label != null) {
        JsLabel label = (JsLabel) pop(); // label
        labelRef = label.getName().makeRef();
      }
      push(new JsBreak(labelRef));
    }

    // @Override
    public void endVisit(JCaseStatement x) {
      if (x.getExpression() == null) {
        push(new JsDefault());
      } else {
        JsCase jsCase = new JsCase();
        jsCase.setCaseExpr((JsExpression) pop()); // expr
        push(jsCase);
      }
    }

    // @Override
    public void endVisit(JCastOperation x, Mutator m) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JCharLiteral x, Mutator m) {
      push(jsProgram.getIntegralLiteral(BigInteger.valueOf(x.value)));
    }

    // @Override
    public void endVisit(JClassLiteral x, Mutator m) {
      // My seed function name
      String nameString = x.refType.getJavahSignatureName() + "_classlit";
      JsName classLit = rootScope.getOrCreateObfuscatableName(nameString);
      classLits.put(x.refType, classLit);
      push(classLit.makeRef());
    }

    // @Override
    public void endVisit(JClassSeed x, Mutator m) {
      push(getName(x.refType).makeRef());
    }

    // @Override
    public void endVisit(JClassType x) {
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
      for (int i = 0; i < jsFuncs.size(); ++i) {
        JsFunction func = (JsFunction) jsFuncs.get(i);
        if (func != null) {
          globalStmts.add(func.makeStmt());
        }
      }

      if (typeOracle.isInstantiatedType(x)) {

        // Setup my seed function and prototype
        if (x != program.getTypeJavaLangString()) {
          JsName seedFuncName = getName(x);

          // seed function
          JsFunction seedFunc = new JsFunction(rootScope, seedFuncName);
          JsBlock body = new JsBlock();
          seedFunc.setBody(body);
          globalStmts.add(seedFunc.makeStmt());

          // prototype
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
          rhs.setQualifier(rootScope.getOrCreateUnobfuscatableName("String").makeRef());
          JsExpression tmpAsg = createAssignment(globalTemp.makeRef(), rhs);
          globalStmts.add(tmpAsg.makeStmt());
        }

        // setup vtables
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

        // special: setup a "toString" alias for java.lang.Object.toString()
        if (x == program.getTypeJavaLangObject()) {
          JMethod toStringMeth = program.getSpecialMethod("Object.toString");
          if (x.methods.contains(toStringMeth)) {
            // _.toString = function(){return this.java_lang_Object_toString();}

            // lhs
            JsName lhsName = rootScope.getOrCreateUnobfuscatableName("toString");
            JsNameRef lhs = lhsName.makeRef();
            lhs.setQualifier(globalTemp.makeRef());

            // rhs
            JsInvocation call = new JsInvocation();
            JsNameRef toStringRef = new JsNameRef(getPolyName(toStringMeth));
            toStringRef.setQualifier(new JsThisRef());
            call.setQualifier(toStringRef);
            JsReturn jsReturn = new JsReturn(call);
            JsFunction rhs = new JsFunction(rootScope);
            JsBlock body = new JsBlock();
            body.getStatements().add(jsReturn);
            rhs.setBody(body);

            // asg
            JsExpression asg = createAssignment(lhs, rhs);
            globalStmts.add(new JsExprStmt(asg));
          }
        }

        // set typeName to be the class name
        {
          JField typeIdField = program.getSpecialField("Object.typeName");
          JsNameRef lhs = getName(typeIdField).makeRef();
          lhs.setQualifier(globalTemp.makeRef());
          JsStringLiteral rhs = jsProgram.getStringLiteral(x.getName());
          JsExpression asg = createAssignment(lhs, rhs);
          globalStmts.add(new JsExprStmt(asg));
        }

        // setup my typeId if needed
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

      // setup fields
      for (int i = 0; i < jsFields.size(); ++i) {
        JsStatement stmt = (JsStatement) jsFields.get(i);
        globalStmts.add(stmt);
      }
    }

    // @Override
    public void endVisit(JConditional x, Mutator m) {
      JsExpression elseExpr = (JsExpression) pop(); // elseExpr
      JsExpression thenExpr = (JsExpression) pop(); // thenExpr
      JsExpression ifTest = (JsExpression) pop(); // ifTest
      push(new JsConditional(ifTest, thenExpr, elseExpr));
    }

    // @Override
    public void endVisit(JContinueStatement x) {
      JsNameRef labelRef = null;
      if (x.label != null) {
        JsLabel label = (JsLabel) pop(); // label
        labelRef = label.getName().makeRef();
      }
      push(new JsContinue(labelRef));
    }

    // @Override
    public void endVisit(JDoStatement x) {
      JsDoWhile stmt = new JsDoWhile();
      if (x.body != null) {
        stmt.setBody((JsStatement) pop()); // body
      } else {
        stmt.setBody(jsProgram.getEmptyStmt());
      }
      stmt.setCondition((JsExpression) pop()); // testExpr
      push(stmt);
    }

    // @Override
    public void endVisit(JDoubleLiteral x, Mutator m) {
      push(jsProgram.getDecimalLiteral(String.valueOf(x.value)));
    }

    // @Override
    public void endVisit(JExpressionStatement x) {
      JsExpression expr = (JsExpression) pop(); // expr
      if (x.getExpression().hasSideEffects()) {
        push(expr.makeStmt());
      } else {
        push(jsProgram.getEmptyStmt());
      }
    }

    // @Override
    public void endVisit(JField x) {
      if (x.hasInitializer() && x.constInitializer == null) {
        // do nothing
        push(null);
        return;
      }

      // if we need an initial value, create an assignment
      if (x.constInitializer != null) {
        x.constInitializer.traverse(this);
      } else {
        x.getType().getDefaultValue().traverse(this);
      }

      JsNameRef fieldRef = getName(x).makeRef();
      if (!x.isStatic()) {
        fieldRef.setQualifier(globalTemp.makeRef());
      }
      JsExpression asg = createAssignment(fieldRef, (JsExpression) pop());
      push(new JsExprStmt(asg));
    }

    // @Override
    public void endVisit(JFieldRef x, Mutator m) {
      JsName jsFieldName = getName(x.field);
      JsNameRef nameRef = jsFieldName.makeRef();
      JsExpression qualifier = null;

      if (x.getInstance() != null) {
        qualifier = (JsExpression) pop();
      }

      JField field = x.getField();
      JsInvocation jsInvocation = maybeCreateClinitCall(field);
      if (jsInvocation != null) {
        qualifier = createCommaExpression(qualifier, jsInvocation);
      }

      nameRef.setQualifier(qualifier); // instance
      push(nameRef);
    }

    // @Override
    public void endVisit(JFloatLiteral x, Mutator m) {
      push(jsProgram.getDecimalLiteral(String.valueOf(x.value)));
    }

    // @Override
    public void endVisit(JForStatement x) {
      JsFor jsFor = new JsFor();

      // body
      if (x.body != null) {
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
    public void endVisit(JIfStatement x) {
      JsIf stmt = new JsIf();

      if (x.elseStmt != null) {
        stmt.setElseStmt((JsStatement) pop()); // elseStmt
      }

      if (x.thenStmt != null) {
        stmt.setThenStmt((JsStatement) pop()); // thenStmt
      } else {
        stmt.setThenStmt(jsProgram.getEmptyStmt());
      }

      stmt.setIfExpr((JsExpression) pop()); // ifExpr
      push(stmt);
    }

    // @Override
    public void endVisit(JInstanceOf x, Mutator m) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JInterfaceType x) {
      List/* <JsFunction> */jsFuncs = popList(x.methods.size()); // methods
      List/* <JsStatement> */jsFields = popList(x.fields.size()); // fields

      JsStatements globalStmts = jsProgram.getGlobalBlock().getStatements();

      if (typeOracle.hasClinit(x)) {
        JsFunction clinitFunc = (JsFunction) jsFuncs.get(0);
        handleClinit(clinitFunc);
        globalStmts.add(clinitFunc.makeStmt());
      }

      // setup fields
      for (int i = 0; i < jsFields.size(); ++i) {
        JsStatement stmt = (JsStatement) jsFields.get(i);
        globalStmts.add(stmt);
      }
    }

    // @Override
    public void endVisit(JIntLiteral x, Mutator m) {
      push(jsProgram.getIntegralLiteral(BigInteger.valueOf(x.value)));
    }

    // @Override
    public void endVisit(JLabel x) {
      push(new JsLabel(getName(x)));
    }

    // @Override
    public void endVisit(JLabeledStatement x) {
      JsStatement body = (JsStatement) pop(); // body
      JsLabel label = (JsLabel) pop(); // label
      label.setStmt(body);
      push(label);
    }

    // @Override
    public void endVisit(JLocal x) {
      push(getName(x).makeRef());
    }

    // @Override
    public void endVisit(JLocalDeclarationStatement x) {

      if (x.getInitializer() == null) {
        pop(); // localRef
        /*
         * local decls can only appear in blocks, so it's ok to push null
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
    public void endVisit(JLocalRef x, Mutator m) {
      push(getName(x.getTarget()).makeRef());
    }

    // @Override
    public void endVisit(JLongLiteral x, Mutator m) {
      push(jsProgram.getIntegralLiteral(BigInteger.valueOf(x.value)));
    }

    // @Override
    public void endVisit(JMethod x) {

      JsBlock body = (JsBlock) pop();
      List/* <JsNameRef> */locals = popList(x.locals.size()); // locals
      List/* <JsParameter> */params = popList(x.params.size()); // params

      if (x.isAbstract()) {
        push(null);
        return;
      }

      JsFunction jsFunc = (JsFunction) methodMap.get(x);
      jsFunc.setBody(body); // body

      JsParameters jsParams = jsFunc.getParameters();
      for (int i = 0; i < params.size(); ++i) {
        JsParameter param = (JsParameter) params.get(i);
        jsParams.add(param);
      }

      JsVars vars = new JsVars();
      for (int i = 0; i < locals.size(); ++i) {
        JsNameRef localRef = (JsNameRef) locals.get(i);
        vars.add(new JsVar(localRef.getName()));
      }

      if (vars.iterator().hasNext()) {
        jsFunc.getBody().getStatements().add(0, vars);
      }

      JsInvocation jsInvocation = maybeCreateClinitCall(x);
      if (jsInvocation != null) {
        jsFunc.getBody().getStatements().add(0, jsInvocation.makeStmt());
      }

      push(jsFunc);
      currentMethod = null;
    }

    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      JMethod method = x.getTarget();
      JsInvocation jsInvocation = new JsInvocation();

      popList(jsInvocation.getArguments(), x.args.size()); // args

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
           * be static is targetting an instance method that could not be
           * transformed into a static. For example, making a super call into a
           * native method currently causes this, because we cannot currently
           * staticify native methods.
           * 
           * Have to use a "call" construct.
           */
          qualifier = objectScope.getOrCreateUnobfuscatableName("call").makeRef();
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
    public void endVisit(JMultiExpression x, Mutator m) {
      List/* <JsExpression> */exprs = popList(x.exprs.size());
      JsExpression cur = null;
      for (int i = 0; i < exprs.size(); ++i) {
        JsExpression next = (JsExpression) exprs.get(i);
        cur = createCommaExpression(cur, next);
      }
      push(cur);
    }

    // @Override
    public void endVisit(JNewArray x, Mutator m) {
      throw new InternalCompilerException("Should not get here.");
    }

    // @Override
    public void endVisit(JNewInstance x, Mutator m) {
      JsNew newOp = new JsNew();
      JsNameRef nameRef = getName(x.getType()).makeRef();
      newOp.setConstructorExpression(nameRef);
      push(newOp);
    }

    // @Override
    public void endVisit(JNullLiteral x, Mutator m) {
      push(jsProgram.getNullLiteral());
    }

    // @Override
    public void endVisit(JParameter x) {
      push(new JsParameter(getName(x)));
    }

    // @Override
    public void endVisit(JParameterRef x, Mutator m) {
      push(getName(x.getTarget()).makeRef());
    }

    // @Override
    public void endVisit(JPostfixOperation x, Mutator m) {
      push(new JsPostfixOperation(JavaToJsOperatorMap.get(x.op),
          (JsExpression) pop())); // arg
    }

    // @Override
    public void endVisit(JPrefixOperation x, Mutator m) {
      push(new JsPrefixOperation(JavaToJsOperatorMap.get(x.op),
          (JsExpression) pop())); // arg
    }

    // @Override
    public void endVisit(JProgram x) {
      JsStatements globalStmts = jsProgram.getGlobalBlock().getStatements();

      // types don't push

      List/* <JsFunction> */funcs = popList(x.entryMethods.size()); // entrymethods
      for (int i = 0; i < funcs.size(); ++i) {
        JsFunction func = (JsFunction) funcs.get(i);
        if (func != null) {
          globalStmts.add(func.makeStmt());
        }
      }

      /**
       * <pre>
       * function gwtOnLoad(errFn, modName){
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
      JsFunction gwtOnLoad = new JsFunction(rootScope);
      globalStmts.add(gwtOnLoad.makeStmt());
      gwtOnLoad.setName(rootScope.getOrCreateUnobfuscatableName("gwtOnLoad"));
      JsBlock body = new JsBlock();
      gwtOnLoad.setBody(body);
      JsScope fnScope = gwtOnLoad.getScope();
      JsParameters params = gwtOnLoad.getParameters();
      JsObfuscatableName errFn = fnScope.createUniqueObfuscatableName("errFn");
      JsObfuscatableName modName = fnScope.createUniqueObfuscatableName("modName");
      params.add(new JsParameter(errFn));
      params.add(new JsParameter(modName));
      JsIf jsIf = new JsIf();
      body.getStatements().add(jsIf);
      jsIf.setIfExpr(errFn.makeRef());
      JsTry jsTry = new JsTry();
      jsIf.setThenStmt(jsTry);
      JsBlock callBlock = new JsBlock();
      jsIf.setElseStmt(callBlock);
      jsTry.setTryBlock(callBlock);
      for (int i = 0; i < funcs.size(); ++i) {
        JsFunction func = (JsFunction) funcs.get(i);
        if (func != null) {
          JsInvocation call = new JsInvocation();
          call.setQualifier(func.getName().makeRef());
          callBlock.getStatements().add(call.makeStmt());
        }
      }
      JsCatch jsCatch = new JsCatch(fnScope.createUniqueObfuscatableName("e"));
      jsTry.getCatches().add(jsCatch);
      JsBlock catchBlock = new JsBlock();
      jsCatch.setBody(catchBlock);
      JsInvocation errCall = new JsInvocation();
      catchBlock.getStatements().add(errCall.makeStmt());
      errCall.setQualifier(errFn.makeRef());
      errCall.getArguments().add(modName.makeRef());

      // setup the global type table
      JField typeIdArray = program.getSpecialField("Cast.typeIdArray");
      JsNameRef fieldRef = getName(typeIdArray).makeRef();
      JsArrayLiteral arrayLit = new JsArrayLiteral();
      for (int i = 0; i < program.getJsonTypeTable().size(); ++i) {
        JsonObject jsonObject = (JsonObject) program.getJsonTypeTable().get(i);
        jsonObject.traverse(this);
        arrayLit.getExpressions().add((JsExpression) pop());
      }
      JsExpression asg = createAssignment(fieldRef, arrayLit);
      globalStmts.add(new JsExprStmt(asg));

      // Class literals are useless right now, just use String literals and the
      // Object methods will basically work
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
        asg = createAssignment(jsName.makeRef(), stringLiteral);
        globalStmts.add(asg.makeStmt());
      }
    }

    // @Override
    public void endVisit(JReturnStatement x) {
      if (x.getExpression() != null) {
        push(new JsReturn((JsExpression) pop())); // expr
      } else {
        push(new JsReturn());
      }
    }

    // @Override
    public void endVisit(JsniMethod x) {
      JsFunction jsFunc = x.getFunc();

      // replace all jsni idents with a real JsName now that we know it
      jsFunc.traverse(new JsAbstractVisitorWithAllVisits() {
        // @Override
        public void endVisit(JsNameRef x) {
          String ident = x.getName().getIdent();
          if (ident.charAt(0) == '@') {
            HasEnclosingType node = (HasEnclosingType) program.jsniMap.get(ident);
            assert (node != null);
            if (node instanceof JField) {
              JField field = (JField) node;
              JsName jsName = getName(field);
              assert (jsName != null);
              x.setName(jsName);
              JsInvocation clinitCall = maybeCreateClinitCall(field);
              if (clinitCall != null) {
                assert (x.getQualifier() == null);
                x.setQualifier(clinitCall);
              }
            } else {
              JMethod method = (JMethod) node;
              if (x.getQualifier() == null) {
                JsName jsName = getName(method);
                assert (jsName != null);
                x.setName(jsName);
              } else {
                JsName jsName = getPolyName(method);
                if (jsName == null) {
                  // this can occur when JSNI references an instance method on a
                  // type that was never actually instantiated.
                  jsName = nullMethodName;
                }
                x.setName(jsName);
              }
            }
          }
        }
      });

      JsInvocation jsInvocation = maybeCreateClinitCall(x);
      if (jsInvocation != null) {
        jsFunc.getBody().getStatements().add(0, jsInvocation.makeStmt());
      }

      push(jsFunc);
      currentMethod = null;
    }

    // @Override
    public void endVisit(JsonArray x, Mutator m) {
      JsArrayLiteral jsArrayLiteral = new JsArrayLiteral();
      popList(jsArrayLiteral.getExpressions(), x.exprs.size());
      push(jsArrayLiteral);
    }

    // @Override
    public void endVisit(JsonObject x, Mutator mutator) {
      JsObjectLiteral jsObjectLiteral = new JsObjectLiteral();
      popList(jsObjectLiteral.getPropertyInitializers(), x.propInits.size());
      push(jsObjectLiteral);
    }

    // @Override
    public void endVisit(JsonPropInit init) {
      JsExpression valueExpr = (JsExpression) pop();
      JsExpression labelExpr = (JsExpression) pop();
      push(new JsPropertyInitializer(labelExpr, valueExpr));
    }

    // @Override
    public void endVisit(JStringLiteral x, Mutator m) {
      push(jsProgram.getStringLiteral(x.value));
    }

    // @Override
    public void endVisit(JThisRef x, Mutator m) {
      push(new JsThisRef());
    }

    // @Override
    public void endVisit(JThrowStatement x) {
      push(new JsThrow((JsExpression) pop())); // expr
    }

    // @Override
    public void endVisit(JTryStatement x) {
      JsTry jsTry = new JsTry();

      if (x.finallyBlock != null) {
        jsTry.setFinallyBlock((JsBlock) pop()); // finallyBlock
      }

      int size = x.catchArgs.size();
      assert (size < 2 && size == x.catchBlocks.size());
      if (size == 1) {
        JsBlock catchBlock = (JsBlock) pop(); // catchBlocks
        JsNameRef arg = (JsNameRef) pop(); // catchArgs
        JsCatch jsCatch = new JsCatch(arg.getName());
        jsCatch.setBody(catchBlock);
        jsTry.getCatches().add(jsCatch);
      }

      jsTry.setTryBlock((JsBlock) pop()); // tryBlock

      push(jsTry);
    }

    // @Override
    public void endVisit(JWhileStatement x) {
      JsWhile stmt = new JsWhile();
      if (x.body != null) {
        stmt.setBody((JsStatement) pop()); // body
      } else {
        stmt.setBody(jsProgram.getEmptyStmt());
      }
      stmt.setCondition((JsExpression) pop()); // testExpr
      push(stmt);
    }

    // @Override
    public boolean visit(JClassType x) {
      if (alreadyRan.contains(x)) {
        return false;
      }

      // force supertype to generate code first, this is required for prototype
      // chaining to work properly
      if (x.extnds != null && !alreadyRan.contains(x)) {
        x.extnds.traverse(this);
      }

      return true;
    }

    // @Override
    public boolean visit(JMethod x) {
      currentMethod = x;
      return true;
    }

    // @Override
    public boolean visit(JProgram x) {
      // handle null method
      // return 'window' so that fields can be referenced
      JsReturn jsReturn = new JsReturn(window.makeRef());
      JsBlock body = new JsBlock();
      body.getStatements().add(jsReturn);
      JsFunction nullFunc = new JsFunction(rootScope, nullMethodName);
      nullFunc.setBody(body);
      jsProgram.getGlobalBlock().getStatements().add(nullFunc.makeStmt());
      return true;
    }

    // @Override
    public boolean visit(JsniMethod x) {
      currentMethod = x;
      return false;
    }

    public boolean visit(JSwitchStatement x) {
      /*
       * What a pain.. JSwitchStatement and JsSwitch are modelled completely
       * differently. Here we try to resolve those differences.
       */
      JsSwitch jsSwitch = new JsSwitch();
      x.getExpression().traverse(this);
      jsSwitch.setExpr((JsExpression) pop()); // expr

      List/* <JStatement> */bodyStmts = x.body.statements;
      if (bodyStmts.size() > 0) {
        JsStatements curStatements = null;
        for (int i = 0; i < bodyStmts.size(); ++i) {
          JStatement stmt = (JStatement) bodyStmts.get(i);
          stmt.traverse(this);
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

    private void handleClinit(JsFunction clinitFunc) {
      // self-assign to the null func immediately (to prevent reentrancy)
      JsExpression asg = createAssignment(clinitFunc.getName().makeRef(),
          nullMethodName.makeRef());
      clinitFunc.getBody().getStatements().add(0, asg.makeStmt());

      // return 'window' so that fields can be referenced
      JsReturn jsReturn = new JsReturn(window.makeRef());
      clinitFunc.getBody().getStatements().add(jsReturn);
    }

    private JsInvocation maybeCreateClinitCall(JField x) {
      if (!x.isStatic()) {
        return null;
      }

      JReferenceType enclosingType = x.getEnclosingType();
      if (!typeOracle.hasClinit(enclosingType)) {
        return null;
      }
      // don't need to clinit on my own static fields
      if (enclosingType == currentMethod.getEnclosingType()) {
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

      // avoid recursion sickness
      if (x == enclosingType.methods.get(0)) {
        return null;
      }

      if (program.isStaticImpl(x)) {
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

  public static void exec(JProgram program, JsProgram jsProgram) {
    GenerateJavaScriptAST generateJavaScriptAST = new GenerateJavaScriptAST(
        program, jsProgram);
    generateJavaScriptAST.execImpl();
  }

  private final Map/* <JType, JsName> */classLits = new IdentityHashMap();

  private final Map/* <JClassType, JsScope> */classScopes = new IdentityHashMap();

  private final JsScope interfaceScope;

  private JsName nullMethodName;

  private final JsScope objectScope;

  private final JsScope rootScope;

  private final JsProgram jsProgram;

  private final Map/* <JMethod, JsFunction> */methodMap = new IdentityHashMap();

  private final Map/* <HasName, JsName> */names = new IdentityHashMap();
  private final Map/* <JMethod, JsName> */polymorphicNames = new IdentityHashMap();
  private final JProgram program;
  private final JTypeOracle typeOracle;

  private GenerateJavaScriptAST(JProgram program, JsProgram jsProgram) {
    this.program = program;
    typeOracle = program.typeOracle;
    this.jsProgram = jsProgram;
    rootScope = jsProgram.getScope();
    objectScope = new JsScope(rootScope);
    objectScope.setDescription("Object scope");
    interfaceScope = new JsScope(objectScope);
    interfaceScope.setDescription("Interfaces");
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
    CreateNamesAndScopesVisitor creator = new CreateNamesAndScopesVisitor();
    program.traverse(creator);
    GenerateJavaScriptVisitor generator = new GenerateJavaScriptVisitor();
    program.traverse(generator);
  }

  private JsName getName(HasName x) {
    return (JsName) names.get(x);
  }

  private JsName getPolyName(HasName x) {
    return (JsName) polymorphicNames.get(x);
  }

}
