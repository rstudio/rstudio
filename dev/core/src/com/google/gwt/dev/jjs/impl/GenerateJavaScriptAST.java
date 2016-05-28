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

import static com.google.gwt.dev.js.JsUtils.createAssignment;
import static com.google.gwt.dev.js.JsUtils.createInvocationOrPropertyAccess;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.PrecompileTaskOptions;
import com.google.gwt.dev.cfg.PermutationProperties;
import com.google.gwt.dev.common.InliningMode;
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.HasJsInfo.JsMemberType;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JAbstractMethodBody;
import com.google.gwt.dev.jjs.ast.JArrayLength;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JCastMap;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLabel;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMember;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNameOf;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JNumericEntry;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPermutationDependentValue;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTransformer;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JDebuggerStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeMapper;
import com.google.gwt.dev.js.JsStackEmulator;
import com.google.gwt.dev.js.JsUtils;
import com.google.gwt.dev.js.JsUtils.InvocationStyle;
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDebugger;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsNormalScope;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsNumericEntry;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsPositionMarker;
import com.google.gwt.dev.js.ast.JsPositionMarker.Type;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsScope;
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
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.collect.Stack;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSortedSet;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Creates a JavaScript AST from a <code>JProgram</code> node.
 */
public class GenerateJavaScriptAST {

  /**
   * Finds the nodes that are targets of JNameOf so that a name is assigned to them.
   */
  private class FindNameOfTargets extends JVisitor {
    @Override
    public void endVisit(JNameOf x, Context ctx) {
      nameOfTargets.add(x.getNode());
    }
  }

  private class CreateNamesAndScopesVisitor extends JVisitor {

    /**
     * Cache of computed Java source file names to URI strings for symbol
     * export. By using a cache we also ensure the miminum number of String
     * instances are serialized.
     */
    private final Map<String, String> fileNameToUriString = Maps.newHashMap();

    private final Stack<JsScope> scopeStack = new Stack<JsScope>();

    private JMethod currentMethod;

    @Override
    public boolean visit(JProgram x, Context ctx) {
      // Scopes and name objects need to be calculated within all types, even reference-only ones.
      // This information is used to be able to detect and avoid name collisions during pretty or
      // obfuscated JS variable name generation.
      x.visitAllTypes(this);
      return false;
    }

    @Override
    public void endVisit(JArrayType x, Context ctx) {
      JsName name = topScope.declareName(x.getName());
      names.put(x, name);
      recordSymbol(x, name);
    }

    @Override
    public void endVisit(JClassType x, Context ctx) {
      scopeStack.pop();
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      JsName jsName;
      if (x.isStatic()) {
        jsName = topScope.declareName(mangleName(x), x.getName());
      } else {
        jsName =
            x.getJsMemberType() != JsMemberType.NONE
                ? scopeStack.peek().declareUnobfuscatableName(x.getJsName())
                : scopeStack.peek().declareName(mangleName(x), x.getName());
      }
      names.put(x, jsName);
      recordSymbol(x, jsName);
    }

    @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      scopeStack.pop();
    }

    @Override
    public void endVisit(JLabel x, Context ctx) {
      if (names.get(x) != null) {
        return;
      }
      names.put(x, scopeStack.peek().declareName(x.getName()));
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      // locals can conflict, that's okay just reuse the same variable
      JsScope scope = scopeStack.peek();
      JsName jsName = scope.declareName(x.getName());
      names.put(x, jsName);
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (doesNotHaveConcreteImplementation(x)) {
        return;
      }
      scopeStack.pop();
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      if (x.isVarargs() && currentMethod.isJsMethodVarargs()) {
        names.put(x, scopeStack.peek().declareUnobfuscatableName("arguments"));
        return;
      }
      names.put(x, scopeStack.peek().declareName(x.getName()));
    }

    @Override
    public void endVisit(JProgram x, Context ctx) {
      /*
       * put the null method and field into objectScope since they can be
       * referenced as instance on null-types (as determined by type flow)
       */
      JMethod nullMethod = x.getNullMethod();
      polymorphicNames.put(nullMethod, objectScope.declareName("$_nullMethod"));
      JField nullField = x.getNullField();
      JsName nullFieldName = objectScope.declareName("$_nullField");
      names.put(nullField, nullFieldName);

      /*
       * Create names for instantiable array types since JProgram.traverse()
       * doesn't iterate over them.
       */
      for (JArrayType arrayType : program.getAllArrayTypes()) {
        if (program.typeOracle.isInstantiatedType(arrayType)) {
          accept(arrayType);
        }
      }
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // have I already been visited as a super type?
      JsScope myScope = classScopes.get(x);
      if (myScope != null) {
        scopeStack.push(myScope);
        return false;
      }

      // My seed function name
      JsName jsName = topScope.declareName(JjsUtils.mangledNameString(x), x.getShortName());
      names.put(x, jsName);
      recordSymbol(x, jsName);

      // My class scope
      if (x.getSuperClass() == null) {
        myScope = objectScope;
      } else {
        JsScope parentScope = classScopes.get(x.getSuperClass());
        // Run my superclass first!
        if (parentScope == null) {
          accept(x.getSuperClass());
        }
        parentScope = classScopes.get(x.getSuperClass());
        assert (parentScope != null);
        /*
         * WEIRD: we wedge the global interface scope in between object and all
         * of its subclasses; this ensures that interface method names trump all
         * (except Object method names)
         */
        if (parentScope == objectScope) {
          parentScope = interfaceScope;
        }
        myScope = new JsNormalScope(parentScope, "class " + x.getShortName());
      }
      classScopes.put(x, myScope);

      scopeStack.push(myScope);
      return true;
    }

    @Override
    public boolean visit(JInterfaceType x, Context ctx) {
      // interfaces have no name at run time
      scopeStack.push(interfaceScope);
      return true;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      // my polymorphic name
      String name = x.getName();
      if (x.needsDynamicDispatch()) {
        if (polymorphicNames.get(x) == null) {
          JsName polyName =
              x.getJsMemberType() != JsMemberType.NONE
                  ? interfaceScope.declareUnobfuscatableName(x.getJsName())
                  : interfaceScope.declareName(mangleNameForPoly(x), name);
          polymorphicNames.put(x, polyName);
        }
      }

      if (doesNotHaveConcreteImplementation(x)) {
        return false;
      }

      // my global name
      JsName globalName = null;
      assert x.getEnclosingType() != null;
      String mangleName = mangleNameForGlobal(x);

      if (JProgram.isClinit(x)) {
        name = name + "_" + x.getEnclosingType().getShortName();
      }

      /*
       * Only allocate a name for a function if it is native, not polymorphic,
       * is a JNameOf target or stack-stripping is disabled.
       */
      if (!stripStack || !polymorphicNames.containsKey(x) || x.isJsniMethod()
          || nameOfTargets.contains(x)) {
        globalName = topScope.declareName(mangleName, name);
        names.put(x, globalName);
        recordSymbol(x, globalName);
      }
      JsFunction function;
      if (x.isJsniMethod()) {
        // set the global name of the JSNI peer
        JsniMethodBody body = (JsniMethodBody) x.getBody();
        function = body.getFunc();
        function.setName(globalName);
      } else {
        /*
         * It would be more correct here to check for an inline assignment, such
         * as var foo = function blah() {} and introduce a separate scope for
         * the function's name according to EcmaScript-262, but this would mess
         * up stack traces by allowing two inner scope function names to
         * obfuscate to the same identifier, making function names no longer a
         * 1:1 mapping to obfuscated symbols. Leaving them in global scope
         * causes no harm.
         */
        function = new JsFunction(x.getSourceInfo(), topScope, globalName, !x.isJsNative());
      }

      jsFunctionsByJavaMethodBody.put(x.getBody(), function);
      scopeStack.push(function.getScope());

      // Don't traverse the method body of methods in referenceOnly types since those method bodies
      // only exist in JS output of other modules it is their responsibility to handle their naming.
      return !program.isReferenceOnly(x.getEnclosingType());
    }

    @Override
    public boolean visit(JTryStatement x, Context ctx) {
      accept(x.getTryBlock());
      for (JTryStatement.CatchClause clause : x.getCatchClauses()) {
        JLocalRef arg = clause.getArg();
        JBlock catchBlock = clause.getBlock();
        JsCatch jsCatch =
            new JsCatch(x.getSourceInfo(), scopeStack.peek(), arg.getTarget().getName());
        JsParameter jsParam = jsCatch.getParameter();
        names.put(arg.getTarget(), jsParam.getName());
        catchMap.put(catchBlock, jsCatch);
        catchParamIdentifiers.add(jsParam.getName());

        scopeStack.push(jsCatch.getScope());
        accept(catchBlock);
        scopeStack.pop();
      }

      // TODO: normalize this so it's never null?
      if (x.getFinallyBlock() != null) {
        accept(x.getFinallyBlock());
      }
      return false;
    }

    /**
     * Generate a file name URI string for a source info, for symbol data
     * export.
     */
    private String makeUriString(HasSourceInfo x) {
      String fileName = x.getSourceInfo().getFileName();
      if (fileName == null) {
        return null;
      }
      String uriString = fileNameToUriString.get(fileName);
      if (uriString == null) {
        uriString = StandardSymbolData.toUriString(fileName);
        fileNameToUriString.put(fileName, uriString);
      }
      return uriString;
    }

    private void recordSymbol(JReferenceType type, JsName jsName) {
      if (getRuntimeTypeReference(type) == null || !program.typeOracle.isInstantiatedType(type)) {
        return;
      }

      String typeId = getRuntimeTypeReference(type).toSource();
      StandardSymbolData symbolData =
          StandardSymbolData.forClass(type.getName(), type.getSourceInfo().getFileName(),
              type.getSourceInfo().getStartLine(), typeId);
      assert !symbolTable.containsKey(symbolData);
      symbolTable.put(symbolData, jsName);
    }

    private <T extends HasEnclosingType & HasName & HasSourceInfo> void recordSymbol(T member,
        JsName jsName) {
      /*
       * NB: The use of member.getName() can produce confusion in cases where a type
       * has both polymorphic and static dispatch for a method, because you
       * might see HashSet::$add() and HashSet::add(). Logically, these methods
       * should be treated equally, however they will be implemented with
       * separate global functions and must be recorded independently.
       *
       * Automated systems that process the symbol information can easily map
       * the statically-dispatched function by looking for method names that
       * begin with a dollar-sign and whose first parameter is the enclosing
       * type.
       */

      String methodSignature = null;
      if (member instanceof JMethod) {
        JMethod method = ((JMethod) member);
        methodSignature =
            StringInterner.get().intern(method.getSignature().substring(method.getName().length()));
      }

      StandardSymbolData symbolData =
          StandardSymbolData.forMember(member.getEnclosingType().getName(), member.getName(),
              methodSignature,  makeUriString(member), member.getSourceInfo().getStartLine());
      assert !symbolTable.containsKey(symbolData) : "Duplicate symbol recorded " + jsName.getIdent()
          + " for " + member.getName() + " and key " + symbolData.getJsniIdent();
      symbolTable.put(symbolData, jsName);
    }
  }

  private class GenerateJavaScriptTransformer extends JTransformer<JsNode> {

    public static final String GOOG_ABSTRACT_METHOD = "goog.abstractMethod";
    public static final String GOOG_INHERITS = "goog.inherits";
    public static final String GOOG_OBJECT_CREATE_SET = "goog.object.createSet";

    private final Set<JDeclaredType> alreadyRan = Sets.newLinkedHashSet();

    private final Map<JDeclaredType, JsFunction> clinitFunctionForType = Maps.newHashMap();

    private JMethod currentMethod = null;

    private final JsName arrayLength = objectScope.declareUnobfuscatableName("length");
    private final JsName globalTemp = topScope.declareUnobfuscatableName("_");
    private final JsName prototype = objectScope.declareUnobfuscatableName("prototype");
    private final JsName call = objectScope.declareUnobfuscatableName("call");

    @Override
    public JsExpression transformArrayLength(JArrayLength expression) {
      assert expression.getInstance() != null : "Can't access the length of a null array";
      return arrayLength.makeQualifiedRef(expression.getSourceInfo(),
          transform(expression.getInstance()));
    }

    @Override
    public JsExpression transformArrayRef(JArrayRef arrayRef) {
      JsArrayAccess jsArrayAccess = new JsArrayAccess(arrayRef.getSourceInfo());
      jsArrayAccess.setIndexExpr(transform(arrayRef.getIndexExpr()));
      jsArrayAccess.setArrayExpr(transform(arrayRef.getInstance()));
      return jsArrayAccess;
    }

    @Override
    public JsExpression transformBinaryOperation(JBinaryOperation binaryOperation) {
      JsExpression lhs = transform(binaryOperation.getLhs());
      JsExpression rhs = transform(binaryOperation.getRhs());
      JsBinaryOperator op = JavaToJsOperatorMap.get(binaryOperation.getOp());

      /*
       * Use === and !== on reference types, or else you can get wrong answers
       * when Object.toString() == 'some string'.
       */
      if (binaryOperation.getLhs().getType() instanceof JReferenceType
          && binaryOperation.getRhs().getType() instanceof JReferenceType) {
        switch (op) {
          case EQ:
            op = JsBinaryOperator.REF_EQ;
            break;
          case NEQ:
            op = JsBinaryOperator.REF_NEQ;
            break;
        }
      }
      return new JsBinaryOperation(binaryOperation.getSourceInfo(), op, lhs, rhs);
    }

    @Override
    public JsStatement transformBlock(JBlock block) {
      JsBlock jsBlock = new JsBlock(block.getSourceInfo());
      List<JsStatement> stmts = jsBlock.getStatements();

      transformIntoExcludingNulls(block.getStatements(), stmts);
      Iterables.removeIf(stmts, Predicates.instanceOf(JsEmpty.class));
      return jsBlock;
    }

    @Override
    public JsNode transformBreakStatement(JBreakStatement breakStatement) {
      SourceInfo info = breakStatement.getSourceInfo();
      return new JsBreak(info, transformIntoLabelReference(info, breakStatement.getLabel()));
    }

    @Override
    public JsNode transformCaseStatement(JCaseStatement caseStatement) {
      if (caseStatement.getExpr() == null) {
        return new JsDefault(caseStatement.getSourceInfo());
      } else {
        JsCase jsCase = new JsCase(caseStatement.getSourceInfo());
        jsCase.setCaseExpr(transform(caseStatement.getExpr()));
        return jsCase;
      }
    }

    @Override
    public JsNode transformCastOperation(JCastOperation castOperation) {
      // These are left in when cast checking is disabled.
      return transform(castOperation.getExpr());
    }

    @Override
    public JsNode transformClassLiteral(JClassLiteral classLiteral) {
      JsName classLit = names.get(classLiteral.getField());
      return classLit.makeRef(classLiteral.getSourceInfo());
    }

    @Override
    public JsNode transformDeclaredType(JDeclaredType type) {
      // Don't generate JS for types not in current module if separate compilation is on.
      if (program.isReferenceOnly(type)) {
        return null;
      }

      if (alreadyRan.contains(type)) {
        return null;
      }

      alreadyRan.add(type);

      if (type.isJsNative()) {
        // Emit JsOverlay static methods for native JsTypes.
        emitStaticMethods(type);
        return null;
      }

      checkForDuplicateMethods(type);

      assert program.getTypeClassLiteralHolder() != type;
      assert !program.immortalCodeGenTypes.contains(type);
      // Super classes should be emitted before the actual class.
      assert type.getSuperClass() == null || program.isReferenceOnly(type.getSuperClass()) ||
          alreadyRan.contains(type.getSuperClass());

      emitStaticMethods(type);

      generateTypeSetup(type);

      emitFields(type);
      return null;
    }

    @Override
    public JsNode transformConditional(JConditional conditional) {
      JsExpression ifTest = transform(conditional.getIfTest());
      JsExpression thenExpr = transform(conditional.getThenExpr());
      JsExpression elseExpr = transform(conditional.getElseExpr());
      return new JsConditional(conditional.getSourceInfo(), ifTest, thenExpr, elseExpr);
    }

    @Override
    public JsNode transformContinueStatement(JContinueStatement continueStatement) {
      SourceInfo info = continueStatement.getSourceInfo();
      return new JsContinue(info, transformIntoLabelReference(info, continueStatement.getLabel()));
    }

    @Override
    public JsNode transformDebuggerStatement(JDebuggerStatement debuggerStatement) {
      return new JsDebugger(debuggerStatement.getSourceInfo());
    }

    @Override
    public JsNode transformDeclarationStatement(JDeclarationStatement declarationStatement) {
      if (declarationStatement.getInitializer() == null) {
        return null;
      }

      JVariable target = declarationStatement.getVariableRef().getTarget();
      if (target instanceof JField && initializeAtTopScope((JField) target)) {
        // Will initialize at top scope; no need to double-initialize.
        return null;
      }

      JsExpression initializer = transform(declarationStatement.getInitializer());
      JsNameRef localRef = transform(declarationStatement.getVariableRef());

      SourceInfo info = declarationStatement.getSourceInfo();
      return JsUtils.createAssignment(info, localRef, initializer).makeStmt();
    }

    @Override
    public JsNode transformDoStatement(JDoStatement doStatement) {
      JsDoWhile stmt = new JsDoWhile(doStatement.getSourceInfo());
      stmt.setCondition(transform(doStatement.getTestExpr()));
      stmt.setBody(jsEmptyIfNull(doStatement.getSourceInfo(), transform(doStatement.getBody())));
      return stmt;
    }

    @Override
    public JsNode transformExpressionStatement(JExpressionStatement statement) {
      return ((JsExpression) transform(statement.getExpr())).makeStmt();
    }

    @Override
    public JsNode transformFieldRef(JFieldRef fieldRef) {
      JsExpression qualifier = transform(fieldRef.getInstance());
      boolean isStatic = fieldRef.getField().isStatic();
      return isStatic ? dispatchToStaticField(fieldRef, qualifier)
          : dispatchToInstanceField(fieldRef, qualifier);
    }

    private JsExpression dispatchToStaticField(
        JFieldRef fieldRef, JsExpression unnecessaryQualifier) {
      /*
       * Note: the comma expressions here would cause an illegal tree state if
       * the result expression ended up on the lhs of an assignment.
       * {@link JsNormalizer} will fix this situation.
       */

      JsExpression result = createStaticReference(fieldRef.getField(), fieldRef.getSourceInfo());

      return JsUtils.createCommaExpression(
          unnecessaryQualifier, maybeCreateClinitCall(fieldRef.getField()), result);
    }

    private JsExpression dispatchToInstanceField(JFieldRef x, JsExpression instance) {
      return names.get(x.getField()).makeQualifiedRef(x.getSourceInfo(), instance);
    }

    @Override
    public JsNode transformForStatement(JForStatement forStatement) {
      JsFor result = new JsFor(forStatement.getSourceInfo());

      JsExpression initExpr = null;
      List<JsExprStmt> initStmts = transform(forStatement.getInitializers());
      for (int i = 0; i < initStmts.size(); ++i) {
        JsExprStmt initStmt = initStmts.get(i);
        if (initStmt != null) {
          initExpr = JsUtils.createCommaExpression(initExpr, initStmt.getExpression());
        }
      }
      result.setInitExpr(initExpr);
      result.setCondition(transform(forStatement.getCondition()));
      result.setIncrExpr(transform(forStatement.getIncrements()));
      result.setBody(jsEmptyIfNull(forStatement.getSourceInfo(), transform(forStatement.getBody())));

      return result;
    }

    @Override
    public JsNode transformIfStatement(JIfStatement ifStatement) {
      JsIf result = new JsIf(ifStatement.getSourceInfo());

      result.setIfExpr(transform(ifStatement.getIfExpr()));
      result.setThenStmt(jsEmptyIfNull(ifStatement.getSourceInfo(),
          transform(ifStatement.getThenStmt())));
      result.setElseStmt(transform(ifStatement.getElseStmt()));

      return result;
    }

    @Override
    public JsLabel transformLabel(JLabel label) {
      return new JsLabel(label.getSourceInfo(), names.get(label));
    }

    @Override
    public JsStatement transformLabeledStatement(JLabeledStatement labeledStatement) {
      JsLabel label = transform(labeledStatement.getLabel());
      label.setStmt(transform(labeledStatement.getBody()));
      return label;
    }

    @Override
    public JsLiteral transformLiteral(JLiteral literal) {
      return JjsUtils.translateLiteral(literal);
    }

    @Override
    public JsNode transformLocalRef(JLocalRef localRef) {
      return names.get(localRef.getTarget()).makeRef(localRef.getSourceInfo());
    }

    @Override
    public JsNode transformMethod(JMethod method) {
      if (method.isAbstract()) {
        return generateAbstractMethodDefinition(method);
      } else if (doesNotHaveConcreteImplementation(method)) {
        return null;
      }
      currentMethod = method;

      JsFunction function = transform(method.getBody());
      function.setInliningMode(method.getInliningMode());

      if (!method.isJsniMethod()) {
        // Setup params on the generated function. A native method already got
        // its jsParams set when parsed from JSNI.
        List<JParameter> parameterList = method.getParams();
        if (method.isJsMethodVarargs()) {
          parameterList = parameterList.subList(0, parameterList.size() - 1);
        }
        transformInto(parameterList, function.getParameters());
      }

      JsInvocation jsInvocation = maybeCreateClinitCall(method);
      if (jsInvocation != null) {
        function.getBody().getStatements().add(0, jsInvocation.makeStmt());
      }

      if (JProgram.isClinit(method)) {
        function.markAsClinit();
      }

      currentMethod = null;
      return function;
    }

    @Override
    public JsNode transformMethodBody(JMethodBody methodBody) {

      JsBlock body = transform(methodBody.getBlock());

      JsFunction function = jsFunctionsByJavaMethodBody.get(methodBody);
      function.setBody(body);

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
      JsVars vars = new JsVars(methodBody.getSourceInfo());
      Set<String> alreadySeen = Sets.newHashSet();
      for (JLocal local : methodBody.getLocals()) {
        JsName name = names.get(local);
        String ident = name.getIdent();
        if (!alreadySeen.contains(ident)
            // Catch block params don't need var declarations
            && !catchParamIdentifiers.contains(name)) {
          alreadySeen.add(ident);
          vars.add(new JsVar(methodBody.getSourceInfo(), name));
        }
      }

      if (!vars.isEmpty()) {
        function.getBody().getStatements().add(0, vars);
      }

      return function;
    }

    @Override
    public JsNode transformMethodCall(JMethodCall methodCall) {
      JMethod method = methodCall.getTarget();
      if (JProgram.isClinit(method)) {
        /*
         * It is possible for clinits to be referenced here that have actually
         * been retargeted (see {@link
         * JTypeOracle.recomputeAfterOptimizations}). Most of the time, these
         * will get cleaned up by other optimization passes prior to this point,
         * but it's not guaranteed. In this case we need to replace the method
         * call with the replaced clinit, unless the replacement is null, in
         * which case we generate a JsNullLiteral as a place-holder expression.
         */
        JDeclaredType type = method.getEnclosingType();
        JDeclaredType clinitTarget = type.getClinitTarget();
        if (clinitTarget == null) {
          // generate a null expression, which will get optimized out
          return JsNullLiteral.INSTANCE;
        }
        method = clinitTarget.getClinitMethod();
      }

      JsExpression qualifier = transform(methodCall.getInstance());
      List<JsExpression> args = transform(methodCall.getArgs());
      SourceInfo sourceInfo = methodCall.getSourceInfo();
      if (method.isStatic()) {
        return dispatchToStatic(qualifier, method, args, sourceInfo);
      } else if (methodCall.isStaticDispatchOnly()) {
        return dispatchToSuper(qualifier, method, args, sourceInfo);
      } else if (method.isOrOverridesJsFunctionMethod()) {
        return dispatchToJsFunction(qualifier, method, args, sourceInfo);
      } else {
        return dispatchToInstanceMethod(qualifier, method, args, sourceInfo);
      }
    }

    private JsExpression dispatchToStatic(JsExpression unnecessaryQualifier, JMethod method,
        List<JsExpression> args, SourceInfo sourceInfo) {
      JsNameRef methodName = createStaticReference(method, sourceInfo);
      return JsUtils.createCommaExpression(
          unnecessaryQualifier,
          createInvocationOrPropertyAccess(
              InvocationStyle.NORMAL, sourceInfo, method, null, methodName, args));
    }

    private JsExpression dispatchToSuper(
        JsExpression instance, JMethod method, List<JsExpression> args, SourceInfo sourceInfo) {
      JsNameRef methodNameRef;
      if (method.isJsNative()) {
        // Construct Constructor.prototype.jsname or Constructor.
        methodNameRef = createJsQualifier(method.getQualifiedJsName(), sourceInfo);
      } else if (method.isConstructor()) {
        /*
         * Constructor calls through {@code this} and {@code super} are always dispatched statically
         * using the constructor function name (constructors are always defined as top level
         * functions).
         *
         * Because constructors are modeled like instance methods they have an implicit {@code this}
         * parameter, hence they are invoked like: "constructor.call(this, ...)".
         */
        methodNameRef = names.get(method).makeRef(sourceInfo);
      } else {
        // These are regular super method call. These calls are always dispatched statically and
        // optimizations will devirtualize them (except in a few cases, like being target of
        // {@link Impl.getNameOf} or calls to the native classes.

        JDeclaredType superClass = method.getEnclosingType();
        JsExpression protoRef = getPrototypeQualifierViaLookup(superClass, sourceInfo);
        methodNameRef = polymorphicNames.get(method).makeQualifiedRef(sourceInfo, protoRef);
      }

      return JsUtils.createInvocationOrPropertyAccess(InvocationStyle.SUPER,
          sourceInfo, method, instance, methodNameRef, args);
    }

    private JsExpression getPrototypeQualifierViaLookup(JDeclaredType type, SourceInfo sourceInfo) {
      if (closureCompilerFormatEnabled) {
        return getPrototypeQualifierOf(type, type.getSourceInfo());
      } else {
        // Construct JCHSU.getPrototypeFor(type).polyname
        // TODO(rluble): Ideally we would want to construct the inheritance chain the JS way and
        // then we could do Type.prototype.polyname.call(this, ...). Currently prototypes do not
        // have global names instead they are stuck into the prototypesByTypeId array.
        return constructInvocation(sourceInfo, RuntimeConstants.RUNTIME_GET_CLASS_PROTOTYPE,
            (JsExpression) transform(getRuntimeTypeReference(type)));
      }
    }

    private JsExpression dispatchToJsFunction(JsExpression instance, JMethod method,
        List<JsExpression> args, SourceInfo sourceInfo) {
      return createInvocationOrPropertyAccess(
          InvocationStyle.FUNCTION, sourceInfo, method, instance, null, args);
    }

    private JsExpression dispatchToInstanceMethod(JsExpression instance, JMethod method,
        List<JsExpression> args, SourceInfo sourceInfo) {
      JsNameRef reference = polymorphicNames.get(method).makeQualifiedRef(sourceInfo, instance);
      return createInvocationOrPropertyAccess(
          InvocationStyle.NORMAL, sourceInfo, method, instance, reference, args);
    }

    @Override
    public JsNode transformMultiExpression(JMultiExpression multiExpression) {
      if (multiExpression.isEmpty()) {
        // the multi-expression was empty; use undefined
        return JsRootScope.INSTANCE.getUndefined().makeRef(multiExpression.getSourceInfo());
      }

      List<JsExpression> exprs = transform(multiExpression.getExpressions());
      JsExpression cur = null;
      for (int i = 0; i < exprs.size(); ++i) {
        JsExpression next = exprs.get(i);
        cur = JsUtils.createCommaExpression(cur, next);
      }
      return cur;
    }

    @Override
    public JsNode transformNameOf(JNameOf nameof) {
      JsName name = names.get(nameof.getNode());
      if (name == null) {
        return JsRootScope.INSTANCE.getUndefined().makeRef(nameof.getSourceInfo());
      }
      return new JsNameOf(nameof.getSourceInfo(), name);
    }

    @Override
    public JsNode transformNewInstance(JNewInstance newInstance) {
      SourceInfo sourceInfo = newInstance.getSourceInfo();
      JConstructor ctor = newInstance.getTarget();
      JsName ctorName = names.get(ctor);
      JsNameRef  reference = ctor.isJsNative()
          ? createJsQualifier(ctor.getQualifiedJsName(), sourceInfo)
          : ctorName.makeRef(sourceInfo);
      List<JsExpression> arguments = transform(newInstance.getArgs());

      JsNew newExpr = (JsNew) JsUtils.createInvocationOrPropertyAccess(
          InvocationStyle.NEWINSTANCE, sourceInfo, ctor, null, reference, arguments);

      if (newInstance.getClassType().isJsFunctionImplementation()) {
        return constructJsFunctionObject(sourceInfo, newInstance.getClassType(), ctorName, newExpr);
      }

      return newExpr;
    }

    private JsNode constructJsFunctionObject(SourceInfo sourceInfo, JClassType type,
        JsName ctorName, JsNew newExpr) {
      // Foo.prototype.functionMethodName
      JMethod jsFunctionMethod = getJsFunctionMethod(type);
      JsNameRef funcNameRef = JsUtils.createQualifiedNameRef(sourceInfo,
          ctorName, prototype, polymorphicNames.get(jsFunctionMethod));

      // makeLambdaFunction(Foo.prototype.functionMethodName, new Foo(...))
      return constructInvocation(sourceInfo, RuntimeConstants.RUNTIME_MAKE_LAMBDA_FUNCTION,
          funcNameRef, newExpr);
    }

    private JMethod getJsFunctionMethod(JClassType type) {
      for (JMethod method : type.getMethods()) {
        if (method.isOrOverridesJsFunctionMethod()) {
          return method;
        }
      }
      throw new AssertionError("Should never reach here.");
    }

    @Override
    public JsNode transformNumericEntry(JNumericEntry entry) {
      return new JsNumericEntry(entry.getSourceInfo(), entry.getKey(), entry.getValue());
    }

    @Override
    public JsNode transformParameter(JParameter parameter) {
      assert !(currentMethod.isJsMethodVarargs() && parameter.isVarargs());
      return new JsParameter(parameter.getSourceInfo(), names.get(parameter));
    }

    @Override
    public JsNode transformParameterRef(JParameterRef parameterRef) {
      return names.get(parameterRef.getTarget()).makeRef(parameterRef.getSourceInfo());
    }

    @Override
    public JsNode transformPermutationDependentValue(JPermutationDependentValue dependentValue) {
      throw new AssertionError("AST should not contain permutation dependent values at " +
          "this point but contains " + dependentValue);
    }

    @Override
    public JsNode transformPostfixOperation(JPostfixOperation expression) {
      return new JsPostfixOperation(expression.getSourceInfo(), JavaToJsOperatorMap.get(expression.getOp()),
          transform(expression.getArg()));
    }

    @Override
    public JsNode transformPrefixOperation(JPrefixOperation expression) {
      return new JsPrefixOperation(expression.getSourceInfo(),
          JavaToJsOperatorMap.get(expression.getOp()), transform(expression.getArg()));
    }

    /**
     * Embeds properties into permProps for easy access from JavaScript.
     */
    private void embedBindingProperties() {
      SourceInfo sourceInfo = SourceOrigin.UNKNOWN;

      // Generates a list of lists of pairs: [[["key", "value"], ...], ...]
      // The outermost list is indexed by soft permutation id. Each item represents
      // a map from binding properties to their values, but is stored as a list of pairs
      // for easy iteration.
      JsArrayLiteral permutationProperties = new JsArrayLiteral(sourceInfo);
      for (Map<String, String> propertyValueByPropertyName :
          properties.findEmbeddedProperties(TreeLogger.NULL)) {
        JsArrayLiteral entryList = new JsArrayLiteral(sourceInfo);
        for (Entry<String, String> entry : propertyValueByPropertyName.entrySet()) {
          JsArrayLiteral pair = new JsArrayLiteral(sourceInfo,
              new JsStringLiteral(sourceInfo, entry.getKey()),
              new JsStringLiteral(sourceInfo, entry.getValue()));
          entryList.getExpressions().add(pair);
        }
        permutationProperties.getExpressions().add(entryList);
      }

      getGlobalStatements().add(
          constructInvocation(sourceInfo, "ModuleUtils.setGwtProperty",
              new JsStringLiteral(sourceInfo, "permProps"), permutationProperties).makeStmt());
    }

    @Override
    public JsNode transformReturnStatement(JReturnStatement returnStatement) {
      return new JsReturn(returnStatement.getSourceInfo(), transform(returnStatement.getExpr()));
    }

    @Override
    public JsNode transformRunAsync(JRunAsync runAsync) {
      return transform(runAsync.getRunAsyncCall());
    }

    @Override
    public JsNode transformCastMap(JCastMap castMap) {
      SourceInfo sourceInfo = castMap.getSourceInfo();
      List<JsExpression> jsCastToTypes = transform(castMap.getCanCastToTypes());
      return buildJsCastMapLiteral(jsCastToTypes, sourceInfo);
    }

    @Override
    public JsNameRef transformJsniMethodRef(JsniMethodRef jsniMethodRef) {
      JMethod method = jsniMethodRef.getTarget();
      if (method.isJsNative()) {
        // Construct Constructor.prototype.jsname or Constructor.
        return createJsQualifier(method.getQualifiedJsName(), jsniMethodRef.getSourceInfo());
      }
      return names.get(method).makeRef(jsniMethodRef.getSourceInfo());
    }

    @Override
    public JsArrayLiteral transformJsonArray(JsonArray jsonArray) {
      JsArrayLiteral jsArrayLiteral = new JsArrayLiteral(jsonArray.getSourceInfo());
      transformInto(jsonArray.getExpressions(), jsArrayLiteral.getExpressions());
      return jsArrayLiteral;
    }

    @Override
    public JsNode transformThisRef(JThisRef thisRef) {
      return new JsThisRef(thisRef.getSourceInfo());
    }

    @Override
    public JsNode transformThrowStatement(JThrowStatement throwStatement) {
      return new JsThrow(throwStatement.getSourceInfo(), transform(throwStatement.getExpr()));
    }

    @Override
    public JsNode transformTryStatement(JTryStatement tryStatement) {
      JsTry jsTry = new JsTry(tryStatement.getSourceInfo());

      jsTry.setTryBlock(transform(tryStatement.getTryBlock()));

      int size = tryStatement.getCatchClauses().size();
      assert (size < 2);
      if (size == 1) {
        JBlock block = tryStatement.getCatchClauses().get(0).getBlock();
        JsCatch jsCatch = catchMap.get(block);
        jsCatch.setBody(transform(block));
        jsTry.getCatches().add(jsCatch);
      }

      JsBlock finallyBlock = transform(tryStatement.getFinallyBlock());
      if (finallyBlock != null && finallyBlock.getStatements().size() > 0) {
        jsTry.setFinallyBlock(finallyBlock);
      }

      return jsTry;
    }

    @Override
    public JsNode transformWhileStatement(JWhileStatement whileStatement) {
      SourceInfo info = whileStatement.getSourceInfo();
      JsWhile stmt = new JsWhile(info);
      stmt.setCondition(transform(whileStatement.getTestExpr()));
      stmt.setBody(jsEmptyIfNull(info, transform(whileStatement.getBody())));
      return stmt;
    }

    public JsStatement jsEmptyIfNull(SourceInfo info, JsStatement statement) {
      return statement != null ? statement : new JsEmpty(info);
    }

    private void insertInTopologicalOrder(JDeclaredType type,
        Set<JDeclaredType> topologicallySortedSet) {
      if (type == null || topologicallySortedSet.contains(type) || program.isReferenceOnly(type)) {
        return;
      }
      insertInTopologicalOrder(type.getSuperClass(), topologicallySortedSet);

      for (JInterfaceType intf : type.getImplements()) {
        if (program.typeOracle.isInstantiatedType(type)) {
          insertInTopologicalOrder(intf, topologicallySortedSet);
        }
      }

      topologicallySortedSet.add(type);
    }

    @Override
    public JsNode transformProgram(JProgram program) {
      // Handle the visiting here as we need to slightly change the order.
      // 1.1 (preamble) Immortal code gentypes.
      // 1.2 (preamble) Classes in the preamble, i.e. all the classes that are needed
      //                to support creation of class literals (reachable through Class.createFor* ).
      // 1.3 (preamble) Class literals for classes in the preamble.
      // 2.  (body)     Normal classes, each with its corresponding class literal (if live).
      // 3.  (epilogue) Code to start the execution of the program (gwtOnLoad, etc).

      Set<JDeclaredType> preambleTypes = generatePreamble(program);

      if (incremental) {
        // Record the names of preamble types so that it's possible to invalidate caches when the
        // preamble types are known to have become stale.
        if (!minimalRebuildCache.hasPreambleTypeNames()) {
          Set<String> preambleTypeNames =  Sets.newHashSet();
          for (JDeclaredType preambleType : preambleTypes) {
            preambleTypeNames.add(preambleType.getName());
          }
          minimalRebuildCache.setPreambleTypeNames(logger, preambleTypeNames);
        }
      }

      // Sort normal types according to superclass relationship.
      Set<JDeclaredType> topologicallySortedBodyTypes = Sets.newLinkedHashSet();
      for (JDeclaredType type : program.getModuleDeclaredTypes()) {
        insertInTopologicalOrder(type, topologicallySortedBodyTypes);
      }
      // Remove all preamble types that might have been inserted here.
      topologicallySortedBodyTypes.removeAll(preambleTypes);

      // Iterate over each type in the right order.
      markPosition("Program", Type.PROGRAM_START);
      for (JDeclaredType type : topologicallySortedBodyTypes) {
        markPosition(type.getName(), Type.CLASS_START);
        transform(type);
        maybeGenerateClassLiteral(type);
        installClassLiterals(Arrays.asList(type));
        markPosition(type.getName(), Type.CLASS_END);
      }
      markPosition("Program", Type.PROGRAM_END);

      generateEpilogue();

      // All done, do not visit children.
      return null;
    }

    private Set<JDeclaredType> generatePreamble(JProgram program) {
      // Reserve the "_" identifier.
      JsVars vars = new JsVars(jsProgram.getSourceInfo());
      vars.add(new JsVar(jsProgram.getSourceInfo(), globalTemp));
      addVarsIfNotEmpty(vars);

      // Generate immortal types in the preamble.
      generateImmortalTypes(vars);

      //  Perform necessary polyfills.
      addTypeDefinitionStatement(
          program.getIndexedType(RuntimeConstants.RUNTIME),
          constructInvocation(program.getSourceInfo(), RuntimeConstants.RUNTIME_BOOTSTRAP)
              .makeStmt());

      Set<JDeclaredType> alreadyProcessed =
          Sets.<JDeclaredType>newLinkedHashSet(program.immortalCodeGenTypes);
      alreadyProcessed.add(program.getTypeClassLiteralHolder());
      alreadyRan.addAll(alreadyProcessed);

      List<JDeclaredType> classLiteralSupportClasses =
          computeClassLiteralsSupportClasses(program, alreadyProcessed);

      // Make sure immortal classes are not doubly processed.
      classLiteralSupportClasses.removeAll(alreadyProcessed);
      for (JDeclaredType type : classLiteralSupportClasses) {
        transform(type);
      }
      generateClassLiterals(classLiteralSupportClasses);
      installClassLiterals(classLiteralSupportClasses);

      Set<JDeclaredType> preambleTypes = Sets.newLinkedHashSet(alreadyProcessed);
      preambleTypes.addAll(classLiteralSupportClasses);
      return preambleTypes;
    }

    private JsNameRef transformIntoLabelReference(SourceInfo info, JLabel label) {
      if (label == null) {
        return null;
      }

      return  ((JsLabel) transform(label)).getName().makeRef(info);
    }

    private void installClassLiterals(List<JDeclaredType> classLiteralTypesToInstall) {
      if (!closureCompilerFormatEnabled) {
        // let createForClass() install them until a follow on CL
        // TODO(cromwellian) remove after approval from rluble in follow up CL
        return;
      }

      for (JDeclaredType type : classLiteralTypesToInstall) {
        if (shouldNotEmitTypeDefinition(type)) {
          continue;
        }

        JsNameRef classLiteralRef = createClassLiteralReference(type);
        if (classLiteralRef == null) {
          continue;
        }

        SourceInfo sourceInfo = type.getSourceInfo();
        JsExpression protoRef = getPrototypeQualifierOf(type, sourceInfo);
        JsNameRef clazzField =
            getIndexedFieldJsName(RuntimeConstants.OBJECT_CLAZZ).makeRef(sourceInfo);
        clazzField.setQualifier(protoRef);
        JsExprStmt stmt = createAssignment(clazzField, classLiteralRef).makeStmt();
        addTypeDefinitionStatement(type, stmt);
      }
    }

    private boolean shouldNotEmitTypeDefinition(JDeclaredType type) {
      // Interfaces, Unboxed Types, JSOs, Native Types, JsFunction, and uninstantiated types
      // Do not have vtables/prototype setup
      return type instanceof JInterfaceType && !closureCompilerFormatEnabled
          || program.isRepresentedAsNativeJsPrimitive(type)
          || !program.typeOracle.isInstantiatedType(type)
          || type.isJsoType()
          || type.isJsNative()
          || type.isJsFunction();
    }

    private List<JDeclaredType> computeClassLiteralsSupportClasses(JProgram program,
        Set<JDeclaredType> alreadyProcessedTypes) {
      if (program.isReferenceOnly(program.getIndexedType("Class"))) {
        return Collections.emptyList();
      }
      // Include in the preamble all classes that are reachable for Class.createForClass,
      // Class.createForInterface
      SortedSet<JDeclaredType> reachableClasses =
          computeReachableTypes(METHODS_PROVIDED_BY_PREAMBLE);

      assert !incremental || checkCoreModulePreambleComplete(program,
          program.getTypeClassLiteralHolder().getClinitMethod());

      Set<JDeclaredType> orderedPreambleClasses = Sets.newLinkedHashSet();
      for (JDeclaredType type : reachableClasses) {
        if (alreadyProcessedTypes.contains(type)) {
          continue;
        }
        insertInTopologicalOrder(type, orderedPreambleClasses);
      }

      // TODO(rluble): The set of preamble types might be overly large, in particular will include
      // JSOs that need clinit. This is due to {@link ControlFlowAnalyzer} making all JSOs live if
      // there is a cast to that type anywhere in the program. See the use of
      // {@link JTypeOracle.getInstantiatedJsoTypesViaCast} in the constructor.
      return Lists.newArrayList(orderedPreambleClasses);
    }

    /**
     * Check that in modular compiles the preamble is complete.
     * <p>
     * In modular compiles the preamble has to include code for creating all 4 types of class
     * literals.
     */
    private boolean checkCoreModulePreambleComplete(JProgram program,
        JMethod classLiteralInitMethod) {
      final Set<JMethod> calledMethods = Sets.newHashSet();
      new JVisitor() {
        @Override
        public void endVisit(JMethodCall x, Context ctx) {
          calledMethods.add(x.getTarget());
        }
      }.accept(classLiteralInitMethod);

      for (String createForMethodName : METHODS_PROVIDED_BY_PREAMBLE) {
        if (!calledMethods.contains(program.getIndexedMethod(createForMethodName))) {
          return false;
        }
      }
      return true;
    }

    /**
     * Computes the set of types whose methods or fields are reachable from {@code methods}.
     */
    private SortedSet<JDeclaredType> computeReachableTypes(Iterable<String> methodNames) {
      ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(program);
      for (String methodName : methodNames) {
        JMethod method = program.getIndexedMethodOrNull(methodName);
        // Only traverse it if it has not been pruned.
        if (method != null) {
          cfa.traverseFrom(method);
        }
      }

      // Get the list of enclosing classes that were not excluded.
      SortedSet<JDeclaredType> reachableTypes =
          ImmutableSortedSet.copyOf(HasName.BY_NAME_COMPARATOR,
              Iterables.filter(
                  Iterables.transform(cfa.getLiveFieldsAndMethods(),
                      new Function<JNode, JDeclaredType>() {
                        @Override
                        public JDeclaredType apply(JNode member) {
                          if (member instanceof JMethod) {
                            return ((JMethod) member).getEnclosingType();
                          } else if (member instanceof JField) {
                            return ((JField) member).getEnclosingType();
                          } else {
                            assert member instanceof JParameter || member instanceof JLocal;
                            // Discard locals and parameters, only need the enclosing instances of reachable
                            // fields and methods.
                            return null;
                          }
                        }
                      }), Predicates.notNull()));
      return reachableTypes;
    }

    private JsExpression generateAbstractMethodDefinition(JMethod method) {
      return (closureCompilerFormatEnabled) ?
          JsUtils.createQualifiedNameRef(GOOG_ABSTRACT_METHOD, method.getSourceInfo()) : null;
    }

    private void generateEpilogue() {
      generateRemainingClassLiterals();

      // add all @JsExport assignments
      generateExports();

      // Generate entry methods. Needs to be after class literal insertion since class literal will
      // be referenced by runtime rebind and property provider bootstrapping.
      setupGwtOnLoad();

      embedBindingProperties();
    }

    private void generateRemainingClassLiterals() {
      if (!incremental) {
        // Emit classliterals that are references but whose classes are not live.
        generateClassLiterals(Iterables.filter(classLiteralDeclarationsByType.keySet(),
            Predicates.not(Predicates.<JType>in(alreadyRan))));
        return;
      }

      // In incremental, class literal references to class literals that were not generated
      // as part of the current compile have to be from reference only classes.
      assert FluentIterable.from(classLiteralDeclarationsByType.keySet())
          .filter(Predicates.instanceOf(JDeclaredType.class))
          .filter(Predicates.not(Predicates.<JType>in(alreadyRan)))
          .filter(
              new Predicate<JType>() {
                @Override
                public boolean apply(JType type) {
                  return !program.isReferenceOnly((JDeclaredType) type);
                }
              })
          .isEmpty();

      // In incremental only the class literals for the primitive types should be part of the
      // epilogue.
      generateClassLiterals(JPrimitiveType.types);
    }

    private void generateClassLiterals(Iterable<? extends JType> orderedTypes) {
      for (JType type : orderedTypes) {
        maybeGenerateClassLiteral(type);
      }
    }

    private void generateExports() {
      Map<String, Object> exportedMembersByExportName = new TreeMap<String, Object>();
      Set<JDeclaredType> hoistedClinits = Sets.newHashSet();
      JsInteropExportsGenerator exportGenerator =
          closureCompilerFormatEnabled
              ? new ClosureJsInteropExportsGenerator(getGlobalStatements(), names)
              : new DefaultJsInteropExportsGenerator(getGlobalStatements(), globalTemp,
                  getIndexedMethodJsName(RuntimeConstants.RUNTIME_PROVIDE));

      // Gather exported things in JsNamespace order.
      for (JDeclaredType type : program.getDeclaredTypes()) {
        if (type.isJsNative()) {
          // JsNative types have no implementation and so shouldn't export anything.
          continue;
        }

        if (type.isJsType()) {
          exportedMembersByExportName.put(type.getQualifiedJsName(), type);
        }

        for (JMember member : type.getMembers()) {
          if (member.isJsInteropEntryPoint()) {
            if (member.getJsMemberType() == JsMemberType.PROPERTY && !member.isFinal()) {
              // TODO(goktug): Remove the warning when we export via Object.defineProperty
              logger.log(
                  TreeLogger.Type.WARN,
                  "Exporting effectively non-final field "
                      + member.getQualifiedName()
                      + ". Due to the way exporting works, the value of the"
                      + " exported field will not be reflected across Java/JavaScript border.");
            }
            exportedMembersByExportName.put(member.getQualifiedJsName(), member);
          }
        }
      }

      // Output the exports.
      for (Object exportedEntity : exportedMembersByExportName.values()) {
        if (exportedEntity instanceof JDeclaredType) {
          exportGenerator.exportType((JDeclaredType) exportedEntity);
        } else {
          JMember member = (JMember) exportedEntity;
          maybeHoistClinit(hoistedClinits, member);
          exportGenerator.exportMember(member, names.get(member).makeRef(member.getSourceInfo()));
        }
      }
    }

    private void maybeHoistClinit(Set<JDeclaredType> hoistedClinits, JMember member) {
      JDeclaredType enclosingType = member.getEnclosingType();
      if (hoistedClinits.contains(enclosingType)) {
        return;
      }

      JsInvocation clinitCall = member instanceof JMethod ? maybeCreateClinitCall((JMethod) member)
          : maybeCreateClinitCall((JField) member);
      if (clinitCall != null) {
        hoistedClinits.add(enclosingType);
        getGlobalStatements().add(clinitCall.makeStmt());
      }
    }

    @Override
    public JsFunction transformJsniMethodBody(JsniMethodBody jsniMethodBody) {
      final Map<String, JNode> jsniMap = Maps.newHashMap();
      for (JsniClassLiteral ref : jsniMethodBody.getClassRefs()) {
        jsniMap.put(ref.getIdent(), ref.getField());
      }
      for (JsniFieldRef ref : jsniMethodBody.getJsniFieldRefs()) {
        jsniMap.put(ref.getIdent(), ref.getField());
      }
      for (JsniMethodRef ref : jsniMethodBody.getJsniMethodRefs()) {
        jsniMap.put(ref.getIdent(), ref.getTarget());
      }

      final JsFunction function = jsniMethodBody.getFunc();

      // replace all JSNI idents with a real JsName now that we know it
      new JsModVisitor() {

        /**
         * Marks a ctor that is a direct child of an invocation. Instead of
         * replacing the ctor with a tear-off, we replace the invocation with a
         * new operation.
         */
        private JsNameRef dontReplaceCtor;

        @Override
        public void endVisit(JsInvocation x, JsContext ctx) {
          // TODO(rluble): this fixup should be done during the initial JSNI processing in
          // GwtAstBuilder.JsniReferenceCollector.
          if (!(x.getQualifier() instanceof JsNameRef)) {
            // If the invocation does not have a name as a qualifier (it might be an expression).
            return;
          }
          JsNameRef ref = (JsNameRef) x.getQualifier();
          if (!ref.isJsniReference()) {
            // The invocation is not to a JSNI method.
            return;
          }
          // Only constructors reach this point, all other JSNI references in the method body
          // would have already been replaced at endVisit(JsNameRef).

          // Replace invocation to ctor with a new op.
          String ident = ref.getIdent();
          JNode node = jsniMap.get(ident);
          assert node instanceof JConstructor;
          assert ref.getQualifier() == null;
          JsName jsName = names.get(node);
          assert (jsName != null);
          ref.resolve(jsName);
          JsNew jsNew = new JsNew(x.getSourceInfo(), ref);
          jsNew.getArguments().addAll(x.getArguments());
          ctx.replaceMe(jsNew);
        }

        @Override
        public void endVisit(JsNameRef x, JsContext ctx) {
          if (!x.isJsniReference()) {
            return;
          }

          String ident = x.getIdent();
          JNode node = jsniMap.get(ident);
          assert (node != null);
          if (node instanceof JField) {
            JField field = (JField) node;
            JsName jsName = names.get(field);
            assert (jsName != null);
            x.resolve(jsName);

            // See if we need to add a clinit call to a static field ref
            JsInvocation clinitCall = maybeCreateClinitCall(field);
            if (clinitCall != null) {
              JsExpression commaExpr = JsUtils.createCommaExpression(clinitCall, x);
              ctx.replaceMe(commaExpr);
            }
          } else if (node instanceof JConstructor) {
            if (x == dontReplaceCtor) {
              // Do nothing, parent will handle.
            } else {
              // Replace with a local closure function.
              // function(a,b,c){return new Obj(a,b,c);}
              JConstructor ctor = (JConstructor) node;
              JsName jsName = names.get(ctor);
              assert (jsName != null);
              x.resolve(jsName);
              SourceInfo info = x.getSourceInfo();
              JsFunction closureFunc = new JsFunction(info, function.getScope());
              for (JParameter p : ctor.getParams()) {
                JsName name = closureFunc.getScope().declareName(p.getName());
                closureFunc.getParameters().add(new JsParameter(info, name));
              }
              JsNew jsNew = new JsNew(info, x);
              for (JsParameter p : closureFunc.getParameters()) {
                jsNew.getArguments().add(p.getName().makeRef(info));
              }
              JsBlock block = new JsBlock(info);
              block.getStatements().add(new JsReturn(info, jsNew));
              closureFunc.setBody(block);
              ctx.replaceMe(closureFunc);
            }
          } else {
            JMethod method = (JMethod) node;
            if (x.getQualifier() == null) {
              JsName jsName = names.get(method);
              assert (jsName != null);
              x.resolve(jsName);
            } else {
              JsName jsName = polymorphicNames.get(method);
              if (jsName == null) {
                // this can occur when JSNI references an instance method on a
                // type that was never actually instantiated.
                jsName = getIndexedMethodJsName(RuntimeConstants.RUNTIME_EMPTY_METHOD);
              }
              x.resolve(jsName);
            }
          }
        }

        @Override
        public boolean visit(JsInvocation x, JsContext ctx) {
          if (x.getQualifier() instanceof JsNameRef) {
            dontReplaceCtor = (JsNameRef) x.getQualifier();
          }
          return true;
        }
      }.accept(function);

      return function;
    }

    @Override
    public JsStatement transformSwitchStatement(JSwitchStatement switchStatement) {
      /*
       * What a pain.. JSwitchStatement and JsSwitch are modeled completely
       * differently. Here we try to resolve those differences.
       */
      JsSwitch jsSwitch = new JsSwitch(switchStatement.getSourceInfo());
      jsSwitch.setExpr(transform(switchStatement.getExpr()));

      List<JStatement> bodyStmts = switchStatement.getBody().getStatements();
      List<JsStatement> curStatements = null;
      for (JStatement stmt : bodyStmts) {
        if (stmt instanceof JCaseStatement) {
          // create a new switch member
          JsSwitchMember switchMember = transform((JNode) stmt);
          jsSwitch.getCases().add(switchMember);
          curStatements = switchMember.getStmts();
        } else {
          // add to statements for current case
          assert (curStatements != null);
          JsStatement newStmt = transform(stmt);
          if (newStmt != null) {
            // Empty JDeclarationStatement produces a null
            curStatements.add(newStmt);
          }
        }
      }

      return jsSwitch;
    }

    private JsExpression buildJsCastMapLiteral(List<JsExpression> runtimeTypeIdLiterals,
        SourceInfo sourceInfo) {
      if (JjsUtils.closureStyleLiteralsNeeded(incremental, closureCompilerFormatEnabled)) {
        return buildClosureStyleCastMapFromArrayLiteral(runtimeTypeIdLiterals, sourceInfo);
      }

      JsNumberLiteral one = new JsNumberLiteral(sourceInfo, 1);
      JsObjectLiteral.Builder objectLiteralBuilder = JsObjectLiteral.builder(sourceInfo)
          .setInternable();
      for (JsExpression runtimeTypeIdLiteral : runtimeTypeIdLiterals) {
        objectLiteralBuilder.add(runtimeTypeIdLiteral, one);
      }
      return objectLiteralBuilder.build();
    }

    private JsExpression buildClosureStyleCastMapFromArrayLiteral(
            List<JsExpression> runtimeTypeIdLiterals, SourceInfo sourceInfo) {
      /*
       * goog.object.createSet('foo', 'bar', 'baz') is optimized by closure compiler into
       * {'foo': !0, 'bar': !0, baz: !0}
       */
      JsNameRef createSet = new JsNameRef(sourceInfo, GOOG_OBJECT_CREATE_SET);
      JsInvocation jsInvocation = new JsInvocation(sourceInfo, createSet);

      for (JsExpression expr : runtimeTypeIdLiterals) {
        jsInvocation.getArguments().add(expr);
      }

      return jsInvocation;
    }

    private void checkForDuplicateMethods(JDeclaredType type) {
      // Sanity check to see that all methods are uniquely named.
      List<JMethod> methods = type.getMethods();
      Set<String> methodSignatures = Sets.newHashSet();
      for (JMethod method : methods) {
        String sig = method.getSignature();
        if (methodSignatures.contains(sig)) {
          throw new InternalCompilerException("Signature collision in Type " + type.getName()
              + " for method " + sig);
        }
        methodSignatures.add(sig);
      }
    }

    private JsNameRef createStaticReference(JMember member, SourceInfo sourceInfo) {
      assert member.isStatic();
      return member.isJsNative()
          ? createJsQualifier(member.getQualifiedJsName(), sourceInfo)
          : names.get(member).makeRef(sourceInfo);
    }

    private void emitFields(JDeclaredType type) {
      JsVars vars = new JsVars(type.getSourceInfo());
      for (JField field : type.getFields()) {
        JsExpression initializer = null;
        // if we need an initial value, create an assignment
        if (initializeAtTopScope(field)) {
          // setup the constant value
          initializer = transform(field.getLiteralInitializer());
        } else if (field.getType().getDefaultValue() == JNullLiteral.INSTANCE) {
          // Fields whose default value is null are left uninitialized and will
          // have a JS value of undefined.
        } else {
          // setup the default value, see Issue 380
          initializer = transform(field.getType().getDefaultValue());
        }

        JsName name = names.get(field);

        if (field.isStatic()) {
          // setup a var for the static
          JsVar var = new JsVar(type.getSourceInfo(), name);
          var.setInitExpr(initializer);
          vars.add(var);
        } else if (initializer != null) {
          // Instance field initilized at top.
          JsNameRef fieldRef =
              name.makeQualifiedRef(field.getSourceInfo(), getPrototypeQualifierOf(field));
          addTypeDefinitionStatement(type, createAssignment(fieldRef, initializer).makeStmt());
        }
      }
      addVarsIfNotEmpty(vars);
    }

    private void emitStaticMethods(JDeclaredType type) {
      // declare all methods into the global scope
      for (JMethod method : type.getMethods()) {
        if (method.needsDynamicDispatch()) {
          continue;
        }

        JsFunction function = transform(method);
        if (function == null) {
          continue;
        }

        if (JProgram.isClinit(method)) {
          handleClinit(type, function);
        }

        emitMethodImplementation(method,
            function.getName().makeRef(function.getSourceInfo()), function.makeStmt());
      }
    }

    private JsExpression generateCastableTypeMap(JDeclaredType type) {
      JCastMap castMap = program.getCastMap(type);
      JsName castableTypeMapName = getIndexedFieldJsName(RuntimeConstants.OBJECT_CASTABLE_TYPE_MAP);

      if (castMap != null && castableTypeMapName != null) {
        return transform(castMap);
      }
      return JsObjectLiteral.EMPTY;
    }

    private JField getClassLiteralField(JType type) {
      JDeclarationStatement decl = classLiteralDeclarationsByType.get(type);
      if (decl == null) {
        return null;
      }

      return (JField) decl.getVariableRef().getTarget();
    }

    private void maybeGenerateClassLiteral(JType type) {

      JField field = getClassLiteralField(type);
      if (field == null) {
        return;
      }

      // TODO(rluble): refactor so that all output related to a class is decided together.
      if (type != null && type instanceof JDeclaredType
          && program.isReferenceOnly((JDeclaredType) type)) {
        // Only generate class literals for classes in the current module.
        // TODO(rluble): In separate compilation some class literals will be duplicated, which if
        // not done with care might violate java semantics of getClass(). There are class literals
        // for primitives and arrays. Currently, because they will be assigned to the same field
        // the one defined later will be the one used and Java semantics are preserved.
        return;
      }

      JsVars vars = new JsVars(jsProgram.getSourceInfo());
      JsName jsName = names.get(field);
      JsExpression classLiteralObject = transform(field.getInitializer());
      JsVar var = new JsVar(field.getSourceInfo(), jsName);
      var.setInitExpr(classLiteralObject);
      vars.add(var);
      addVarsIfNotEmpty(vars);
    }

    private JsNameRef createClassLiteralReference(JType type) {
      JField field = getClassLiteralField(type);
      if (field == null) {
        return null;
      }
      JsName jsName = names.get(field);
      return jsName.makeRef(type.getSourceInfo());
    }

    private void generateTypeSetup(JDeclaredType type) {
      if (program.isRepresentedAsNativeJsPrimitive(type)
          && program.typeOracle.isInstantiatedType(type)) {
        setupCastMapForUnboxedType(type,
            program.getRepresentedAsNativeTypesDispatchMap().get(type).getCastMapField());
        return;
      }

      if (shouldNotEmitTypeDefinition(type)) {
        return;
      }

      generateClassDefinition(type);
      generatePrototypeDefinitions(type);

      maybeGenerateToStringAlias(type);
    }

    private void markPosition(String name, Type type) {
      getGlobalStatements().add(new JsPositionMarker(SourceOrigin.UNKNOWN, name, type));
    }

    /**
     * Sets up gwtOnLoad bootstrapping code. Unusually, the created code is executed as part of
     * source loading and runs in the global scope (not inside of any function scope).
     */
    private void setupGwtOnLoad() {
      /**
       * <pre>
       * var $entry = Impl.registerEntry();
       * var gwtOnLoad = ModuleUtils.gwtOnLoad();
       * ModuleUtils.addInitFunctions(init1, init2,...)
       * </pre>
       */

      final SourceInfo sourceInfo = SourceOrigin.UNKNOWN;

      // var $entry = ModuleUtils.registerEntry();
      JsStatement entryVars = constructFunctionCallStatement(
          topScope.declareName("$entry"), "ModuleUtils.registerEntry");
      getGlobalStatements().add(entryVars);

      // var gwtOnLoad = ModuleUtils.gwtOnLoad;
      JsName gwtOnLoad = topScope.findExistingUnobfuscatableName("gwtOnLoad");
      JsVar varGwtOnLoad = new JsVar(sourceInfo, gwtOnLoad);
      varGwtOnLoad.setInitExpr(createAssignment(gwtOnLoad.makeRef(sourceInfo),
          getIndexedMethodJsName(RuntimeConstants.MODULE_UTILS_GWT_ON_LOAD).makeRef(sourceInfo)));
      getGlobalStatements().add(new JsVars(sourceInfo, varGwtOnLoad));

      // ModuleUtils.addInitFunctions(init1, init2,...)
      List<JsExpression> arguments = Lists.newArrayList();
      for (JMethod entryPointMethod : program.getEntryMethods()) {
        JsFunction entryFunction = getJsFunctionFor(entryPointMethod);
        arguments.add(entryFunction.getName().makeRef(sourceInfo));
      }

      JsStatement createGwtOnLoadFunctionCall =
          constructInvocation("ModuleUtils.addInitFunctions", arguments).makeStmt();

      getGlobalStatements().add(createGwtOnLoadFunctionCall);
    }

    /**
     * Creates a (var) assignment a statement for a function call to an indexed function.
     */
    private JsStatement constructFunctionCallStatement(JsName assignToVariableName,
        String indexedFunctionName, JsExpression... args) {
      return constructFunctionCallStatement(assignToVariableName, indexedFunctionName,
          Arrays.asList(args));
    }

    /**
     * Creates a (var) assignment a statement for a function call to an indexed function.
     */
    private JsStatement constructFunctionCallStatement(JsName assignToVariableName,
        String indexedFunctionName, List<JsExpression> args) {

      SourceInfo sourceInfo = SourceOrigin.UNKNOWN;
      JsInvocation invocation = constructInvocation(indexedFunctionName, args);
      JsVar var = new JsVar(sourceInfo, assignToVariableName);
      var.setInitExpr(invocation);
      JsVars entryVars = new JsVars(sourceInfo);
      entryVars.add(var);
      return entryVars;
    }

    /**
     * Constructs an invocation for an indexed function.
     */
    private JsInvocation constructInvocation(SourceInfo sourceInfo,
        String indexedFunctionName, JsExpression... args) {
      return constructInvocation(sourceInfo, indexedFunctionName, Arrays.asList(args));
    }

    /**
     * Constructs an invocation for an indexed function.
     */
    private JsInvocation constructInvocation(String indexedFunctionName,
        List<JsExpression> args) {
      SourceInfo sourceInfo = SourceOrigin.UNKNOWN;
      return constructInvocation(sourceInfo, indexedFunctionName, args);
    }

    /**
     * Constructs an invocation for an indexed function.
     */
    private JsInvocation constructInvocation(SourceInfo sourceInfo,
        String indexedFunctionName, List<JsExpression> args) {
      JsName functionToInvoke = getIndexedMethodJsName(indexedFunctionName);
      return new JsInvocation(sourceInfo, functionToInvoke.makeRef(sourceInfo), args);
    }

    private void generateImmortalTypes(JsVars globals) {
      List<JClassType> immortalTypesReversed = Lists.reverse(program.immortalCodeGenTypes);
      // visit in reverse order since insertions start at head
      for (JClassType x : immortalTypesReversed) {
        // Don't generate JS for referenceOnly types.
        if (program.isReferenceOnly(x)) {
          continue;
        }
        // should not be pruned
        assert x.getMethods().size() > 0;
        // insert all static methods
        for (JMethod method : x.getMethods()) {
          /*
           * Skip virtual methods and constructors. Even in cases where there is no constructor
           * defined, the compiler will synthesize a default constructor which invokes
           * a synthesized $init() method. We must skip both of these inserted methods.
           */
          if (method.needsDynamicDispatch() || method instanceof JConstructor
              || doesNotHaveConcreteImplementation(method)) {
            continue;
          }
          // add after var declaration, but before everything else
          JsFunction function = transform(method);
          assert function.getName() != null;
          addMethodDefinitionStatement(1, method, function.makeStmt());
        }

        // insert fields into global var declaration
        for (JField field : x.getFields()) {
          assert field.isStatic() : "'" + field.getName()
              + "' is not static. Only static fields are allowed on immortal types";
          assert field.getInitializer() == field.getLiteralInitializer() : "'" + field.getName()
              + "' is not initilialized to a literal."
              + " Only literal initializers are allowed on immortal types";

          JsVar var = new JsVar(x.getSourceInfo(), names.get(field));
          var.setInitExpr(transform(field.getLiteralInitializer()));
          globals.add(var);
        }
      }
    }

    private void generateCallToDefineClass(JClassType type, List<JsNameRef> constructorArgs) {
      JClassType superClass = type.getSuperClass();
      JExpression superTypeId = (superClass == null) ? JNullLiteral.INSTANCE :
          getRuntimeTypeReference(superClass);
      String jsPrototype = getSuperPrototype(type);

      List<JsExpression> defineClassArguments = Lists.newArrayList();

      defineClassArguments.add(transform(getRuntimeTypeReference(type)));
      defineClassArguments.add(jsPrototype == null ? transform(superTypeId) :
          createJsQualifier(jsPrototype, type.getSourceInfo()));
      defineClassArguments.add(generateCastableTypeMap(type));
      defineClassArguments.addAll(constructorArgs);

      // Runtime.defineClass(typeId, superTypeId, castableMap, constructors)
      JsStatement defineClassStatement = constructInvocation(type.getSourceInfo(),
          RuntimeConstants.RUNTIME_DEFINE_CLASS, defineClassArguments).makeStmt();
      addTypeDefinitionStatement(type, defineClassStatement);

      if (jsPrototype != null) {
        JsStatement statement =
        constructInvocation(type.getSourceInfo(),
            RuntimeConstants.RUNTIME_COPY_OBJECT_PROPERTIES,
            getPrototypeQualifierViaLookup(program.getTypeJavaLangObject(), type.getSourceInfo()),
            globalTemp.makeRef(type.getSourceInfo()))
            .makeStmt();
        addTypeDefinitionStatement(type, statement);
      }
    }

    private String getSuperPrototype(JDeclaredType type) {
      if (type.isJsFunctionImplementation()) {
        return "Function";
      }
      JClassType superClass = type.getSuperClass();
      if (superClass != null && superClass.isJsNative()) {
        return superClass.getQualifiedJsName();
      }
      return null;
    }

    private void generateClassDefinition(JDeclaredType type) {
        assert !program.isRepresentedAsNativeJsPrimitive(type);

      if (closureCompilerFormatEnabled) {
        generateClosureTypeDefinition(type);
      } else {
        generateJsClassDefinition((JClassType) type);
      }
    }

    /*
     * Class definition for regular output looks like:
     *
     * defineClass(id, superId, castableTypeMap, ctor1, ctor2, ctor3);
     * _.method1 = function() { ... }
     * _.method2 = function() { ... }
     */
    private void generateJsClassDefinition(JClassType classType) {
      // Add constructors as varargs to define class.
      List<JsNameRef> constructorArgs = Lists.newArrayList();
      for (JMethod method : getPotentiallyAliveConstructors(classType)) {
        constructorArgs.add(names.get(method).makeRef(classType.getSourceInfo()));
      }

      // defineClass(..., Ctor1, Ctor2, ...)
      generateCallToDefineClass(classType, constructorArgs);
    }

    /*
     * Class definition for closure output looks like:
     *
     * function ClassName() {}
     * ClassName.prototype.method1 = function() { ... };
     * ClassName.prototype.method2 = function() { ... };
     * ClassName.prototype.castableTypeMap = {...}
     * ClassName.prototype.___clazz = classLit;
     * function Ctor1() {}
     * function Ctor2() {}
     *
     * goog$inherits(Ctor1, ClassName);
     * goog$inherits(Ctor2, ClassName);
     *
     * The primary change is to make the prototype assignment look like regular closure code to help
     * the compiler disambiguate which methods belong to which type. Elimination of defineClass()
     * makes the setup more transparent and eliminates a global table holding a reference to
     * every prototype.
     */
    private void generateClosureTypeDefinition(JDeclaredType type) {
      // function ClassName(){}
      JsName classVar = declareSynthesizedClosureConstructor(type);
      generateInlinedDefineClass(type, classVar);

       /*
       * Closure style prefers 1 single ctor per type. To model this without radical changes,
       * we simply model each concrete ctor as a subtype. This works because GWT doesn't use the
       * native instanceof operator. So for example, class A() { A(int type){}, A(String s){} }
       * becomes (pseudo code):
       *
       * function A() {}
       * A.prototype.method = ...
       *
       * function A_int(x) {}
       * function A_String(s) {}
       * goog$inherits(A_int, A);
       * goog$inherits(A_string, A);
       *
       */
      for (JMethod method : getPotentiallyAliveConstructors(type)) {
        SourceInfo typeSourceInfo = type.getSourceInfo();
        JsNameRef googInherits = JsUtils.createQualifiedNameRef(GOOG_INHERITS, typeSourceInfo);

        SourceInfo methodSourceInfo = method.getSourceInfo();
        JsExprStmt callGoogInherits = new JsInvocation(typeSourceInfo, googInherits,
            names.get(method).makeRef(methodSourceInfo),
            names.get(method.getEnclosingType()).makeRef(methodSourceInfo)).makeStmt();
        addMethodDefinitionStatement(method, callGoogInherits);
      }
    }

    /**
     * Does everything JCHSU.defineClass does, but inlined into global statements. Roughly
     * parallels argument order of generateCallToDefineClass.
     */
    private void generateInlinedDefineClass(JDeclaredType type, JsName classVar) {
      if (type instanceof JInterfaceType) {
        return;
      }
      JClassType superClass = type.getSuperClass();
      // check if there's an overriding prototype
      String jsPrototype = getSuperPrototype(type);
      SourceInfo info = type.getSourceInfo();
      JsNameRef parentCtor = jsPrototype != null ?
          createJsQualifier(jsPrototype, info) :
            superClass != null ?
              names.get(superClass).makeRef(info) :
              null;

      if (parentCtor != null) {
        JsNameRef googInherits = JsUtils.createQualifiedNameRef(GOOG_INHERITS, info);
        // Use goog$inherits(ChildCtor, ParentCtor) to setup inheritance
        JsExprStmt callGoogInherits = new JsInvocation(info, googInherits,
            classVar.makeRef(info), parentCtor).makeStmt();
        addTypeDefinitionStatement(type, callGoogInherits);
      }

      if (type == program.getTypeJavaLangObject()) {
        setupTypeMarkerOnJavaLangObjectPrototype(type);
      }

      // inline assignment of castableTypeMap field instead of using defineClass()
      setupCastMapOnPrototype(type);
      if (jsPrototype != null) {
        JsStatement statement =
            constructInvocation(info,
                RuntimeConstants.RUNTIME_COPY_OBJECT_PROPERTIES,
                getPrototypeQualifierOf(program.getTypeJavaLangObject(), info),
                getPrototypeQualifierOf(type, info)).makeStmt();
        addTypeDefinitionStatement(type, statement);
      }
    }

    private void setupCastMapOnPrototype(JDeclaredType type) {
      JsExpression castMap = generateCastableTypeMap(type);
      generatePrototypeAssignmentForJavaField(type, "Object.castableTypeMap", castMap);
    }

    private void setupTypeMarkerOnJavaLangObjectPrototype(JDeclaredType type) {
      JsName typeMarkerMethod = getIndexedMethodJsName(RuntimeConstants.RUNTIME_TYPE_MARKER_FN);
      generatePrototypeAssignmentForJavaField(type, RuntimeConstants.OBJECT_TYPEMARKER,
          typeMarkerMethod.makeRef(type.getSourceInfo()));
    }

    private void generatePrototypeAssignmentForJavaField(JDeclaredType type, String javaField,
        JsExpression rhs) {
      SourceInfo sourceInfo = type.getSourceInfo();
      JsNameRef protoRef = getPrototypeQualifierOf(type, sourceInfo);
      JsNameRef fieldRef = getIndexedFieldJsName(javaField).makeQualifiedRef(sourceInfo, protoRef);
      addTypeDefinitionStatement(type, createAssignment(fieldRef, rhs).makeStmt());
    }

    private void addMethodDefinitionStatement(JMethod method,
        JsExprStmt methodDefinitionStatement) {
      getGlobalStatements().add(methodDefinitionStatement);
      methodByGlobalStatement.put(methodDefinitionStatement, method);
    }

    private void addMethodDefinitionStatement(int position, JMethod method,
        JsExprStmt methodDefinitionStatement) {
      getGlobalStatements().add(position, methodDefinitionStatement);
      methodByGlobalStatement.put(methodDefinitionStatement, method);
    }

    private void addTypeDefinitionStatement(JDeclaredType x, JsStatement statement) {
      getGlobalStatements().add(statement);
      javaTypeByGlobalStatement.put(statement, x);
    }

    /*
     * Declare an empty synthesized constructor that looks like:
     * function ClassName(){}
     *
     * Closure Compiler's RewriteFunctionExpressions pass can be enabled to turn these back
     * into a factory method after optimizations.
     *
     * TODO(goktug): throw Error in the body to prevent instantiation via this constructor.
     */
    private JsName declareSynthesizedClosureConstructor(JDeclaredType x) {
      SourceInfo sourceInfo = x.getSourceInfo();
      JsName classVar = topScope.declareName(JjsUtils.mangledNameString(x));
      JsFunction closureCtor = JsUtils.createEmptyFunctionLiteral(sourceInfo, topScope, classVar);
      JsExprStmt statement = closureCtor.makeStmt();
      // This synthetic statement must be in the initial fragment, do not add to typeDefinitions
      getGlobalStatements().add(statement);
      names.put(x, classVar);
      return classVar;
    }

    /*
     * Sets up the castmap for type X
     */
    private void setupCastMapForUnboxedType(JDeclaredType type, String castMapField) {
      //  Cast.[castMapName] = /* cast map */ { ..:1, ..:1}
      JsName castableTypeMapName = getIndexedFieldJsName(castMapField);
      JsNameRef castMapVarRef = castableTypeMapName.makeRef(type.getSourceInfo());

      JsExpression castMapLiteral = generateCastableTypeMap(type);
      addTypeDefinitionStatement(type, createAssignment(castMapVarRef, castMapLiteral).makeStmt());
    }

    private void maybeGenerateToStringAlias(JDeclaredType type) {
      if (type == program.getTypeJavaLangObject()) {
        // special: setup a "toString" alias for java.lang.Object.toString()
        JMethod toStringMethod = program.getIndexedMethod(RuntimeConstants.OBJECT_TO_STRING);
        if (type.getMethods().contains(toStringMethod)) {
          JsName toStringName = objectScope.declareUnobfuscatableName("toString");
          generatePrototypeDefinitionAlias(toStringMethod, toStringName);
        }
      }
    }

    private void generatePrototypeAssignment(JMethod method, JsName name, JsExpression rhs) {
      generatePrototypeAssignment(method, name, rhs, method.getJsMemberType());
    }

     /**
      * Create a vtable assignment of the form _.polyname = rhs; and register the line as
      * created for {@code method}.
      */
    private void generatePrototypeAssignment(JMethod method, JsName name, JsExpression rhs,
        JsMemberType memberType) {
      SourceInfo sourceInfo = method.getSourceInfo();
      JsNameRef prototypeQualifierOf = getPrototypeQualifierOf(method);
      JsNameRef lhs = name.makeQualifiedRef(sourceInfo, prototypeQualifierOf);
      switch (memberType) {
        case GETTER:
        case SETTER:
          emitPropertyImplementation(method, prototypeQualifierOf, name.makeRef(sourceInfo), rhs);
          break;
        default:
          emitMethodImplementation(method, lhs, createAssignment(lhs, rhs).makeStmt());
          break;
      }
    }

    private void emitPropertyImplementation(JMethod method, JsNameRef prototype, JsNameRef name,
        JsExpression methodDefinitionStatement) {
      SourceInfo sourceInfo = method.getSourceInfo();

      // We use Object.defineProperties instead of Object.defineProperty to make sure the
      // property name appears as an identifier and not as a string.
      // Some JS optimizers, e.g. the closure compiler, relies on this subtle difference for
      // obfuscating property names.
      JsNameRef definePropertyMethod =
          getIndexedMethodJsName(RuntimeConstants.RUNTIME_DEFINE_PROPERTIES).makeRef(sourceInfo);

      JsObjectLiteral definePropertyLiteral =
          JsObjectLiteral.builder(sourceInfo)
               // {name: {get: function() { ..... }} or {set : function (v) {....}}}
              .add(name, JsObjectLiteral.builder(sourceInfo)
                      // {get: function() { ..... }} or {set : function (v) {....}}
                  .add(method.getJsMemberType().getPropertyAccessorKey(), methodDefinitionStatement)
                  .build())
              .build();

      addMethodDefinitionStatement(method, new JsInvocation(sourceInfo, definePropertyMethod,
          prototype, definePropertyLiteral).makeStmt());
    }

    private void emitMethodImplementation(JMethod method, JsNameRef functionNameRef,
        JsExprStmt methodDefinitionStatement) {
      addMethodDefinitionStatement(method, methodDefinitionStatement);

      if (shouldEmitDisplayNames()) {
        JsExprStmt displayNameAssignment = outputDisplayName(functionNameRef, method);
        addMethodDefinitionStatement(method, displayNameAssignment);
      }
    }

    private void generatePrototypeDefinitionAlias(JMethod method, JsName alias) {
      JsName polyName = polymorphicNames.get(method);
      JsExpression bridge = JsUtils.createBridge(method, polyName, topScope);
      // Aliases are never property accessors.
      generatePrototypeAssignment(method, alias, bridge, JsMemberType.NONE);
    }

    private JsExprStmt outputDisplayName(JsNameRef function, JMethod method) {
      JsNameRef displayName = new JsNameRef(function.getSourceInfo(), "displayName");
      displayName.setQualifier(function);
      String displayStringName = getDisplayName(method);
      JsStringLiteral displayMethodName =
          new JsStringLiteral(function.getSourceInfo(), displayStringName);
      return createAssignment(displayName, displayMethodName).makeStmt();
    }

    private boolean shouldEmitDisplayNames() {
      return methodNameMappingMode != OptionMethodNameDisplayMode.Mode.NONE;
    }

    private String getDisplayName(JMethod method) {
      switch (methodNameMappingMode) {
        case ONLY_METHOD_NAME:
          return method.getName();
        case ABBREVIATED:
          return method.getEnclosingType().getShortName() + "." + method.getName();
        case FULL:
          return method.getEnclosingType().getName() + "." + method.getName();
        default:
          assert false : "Invalid display mode option " + methodNameMappingMode;
      }
      return null;
    }

    /**
     * Creates the assignment for all polynames for a certain class, assumes that the global
     * variable _ points the JavaScript prototype for {@code type}.
     */
    private void generatePrototypeDefinitions(JDeclaredType type) {
        assert !program.isRepresentedAsNativeJsPrimitive(type);

      // Emit synthetic methods first. In JsInterop we allow a more user written method to be named
      // with the same name as a synthetic bridge (required due to generics) relying that the
      // synthetic method is output first into the prototype slot and rewritten in this situation.
      // TODO(rluble): this is a band aid. The user written method (and its overrides) should be
      // automatically JsIgnored. Otherwise some semantics become looser. E.g. the synthetic bridge
      // method may be casting some of the parameters. Such casts are lost in this scheme.
      Iterable<JMethod> orderedInstanceMethods = Iterables.concat(
          Iterables.filter(type.getMethods(),
              Predicates.and(
                  JjsPredicates.IS_SYNTHETIC,
                  JjsPredicates.NEEDS_DYNAMIC_DISPATCH)),
          Iterables.filter(type.getMethods(),
              Predicates.and(
                  Predicates.not(JjsPredicates.IS_SYNTHETIC),
                  JjsPredicates.NEEDS_DYNAMIC_DISPATCH)));

      for (JMethod method : orderedInstanceMethods) {
        generatePrototypeDefinition(method, (JsExpression) transformMethod(method));
      }
    }

    private void generatePrototypeDefinition(JMethod method, JsExpression functionDefinition) {
      if (functionDefinition != null) {
        generatePrototypeAssignment(method, polymorphicNames.get(method), functionDefinition);
      }

      if (method.exposesNonJsMember()) {
        JsName internalMangledName = interfaceScope.declareName(mangleNameForPoly(method),
            method.getName());
        generatePrototypeDefinitionAlias(method, internalMangledName);
      }

      if (method.exposesPackagePrivateMethod()) {
        // Here is the situation where this is needed:
        //
        // class a.A { m() {} }
        // class b.B extends a.A { m() {} }
        // interface I { m(); }
        // class a.C {
        //  { A a = new b.B();  a.m() // calls A::m()} }
        //  { I i = new b.B();  a.m() // calls B::m()} }
        // }
        //
        // Up to this point it is clear that package private names need to be different than
        // public names.
        //
        // Add class a.D extends a.A implements I { public m() }
        //
        // a.D collapses A::m and I::m into the same function and it was clear that two
        // two different names were already needed, hence when creating the vtable for a.D
        // both names have to point to the same function.
        generatePrototypeDefinitionAlias(method, getPackagePrivateName(method));
      }
    }

    public JsNameRef createJsQualifier(String qualifier, SourceInfo sourceInfo) {
      assert !qualifier.isEmpty();
      return JsUtils.createQualifiedNameRef("$wnd." + qualifier, sourceInfo);
    }

    /**
     * Returns either _ or ClassCtor.prototype depending on output mode.
     */
    private JsNameRef getPrototypeQualifierOf(JMember member) {
      return getPrototypeQualifierOf(member.getEnclosingType(), member.getSourceInfo());
    }

    /**
     * Returns either _ or ClassCtor.prototype depending on output mode.
     */
    private JsNameRef getPrototypeQualifierOf(JDeclaredType type, SourceInfo info) {
      return closureCompilerFormatEnabled
          ? prototype.makeQualifiedRef(info, names.get(type).makeRef(info))
          : globalTemp.makeRef(info);
    }

    /**
     * Returns the package private JsName for {@code method}.
     */
    private JsName getPackagePrivateName(JMethod method) {
      for (JMethod overridenMethod : method.getOverriddenMethods()) {
        if (overridenMethod.isPackagePrivate()) {
          JsName name = polymorphicNames.get(overridenMethod);
          assert name != null;
          return name;
        }
      }
      throw new AssertionError(
          method.toString() + " overrides a package private method but was not found.");
    }

    private void handleClinit(JDeclaredType type, JsFunction clinitFunction) {
      clinitFunctionForType.put(type, clinitFunction);
      JDeclaredType superClass = type.getSuperClass();
      JsFunction superClinitFunction = superClass == null
          ? null : clinitFunctionForType.get(superClass.getClinitTarget());

      clinitFunction.setSuperClinit(superClinitFunction);
      List<JsStatement> statements = clinitFunction.getBody().getStatements();
      SourceInfo sourceInfo = clinitFunction.getSourceInfo();
      // Self-assign to the global noop method immediately (to prevent reentrancy). In incremental
      // mode the more costly Object constructor function is used as the noop method since doing so
      // provides a better debug experience that does not step into already used clinits.

      JsName emptyFunctionFnName = incremental ? objectConstructorFunction.getName()
          : getIndexedMethodJsName(RuntimeConstants.RUNTIME_EMPTY_METHOD);
      JsExpression assignment = createAssignment(clinitFunction.getName().makeRef(sourceInfo),
          emptyFunctionFnName.makeRef(sourceInfo));
      statements.add(0, assignment.makeStmt());
    }

    private boolean isMethodPotentiallyCalledAcrossClasses(JMethod method) {
      assert incremental || crossClassTargets != null;
      return crossClassTargets == null || crossClassTargets.contains(method)
          || method.isJsInteropEntryPoint();
    }

    private Iterable<JMethod> getPotentiallyAliveConstructors(JDeclaredType x) {
      return Iterables.filter(x.getMethods(), new Predicate<JMethod>() {
        @Override
        public boolean apply(JMethod m) {
          return isMethodPotentiallyALiveConstructor(m);
        }
      });
    }

    /**
     * Whether a method is a constructor that is actually newed. Note that in absence of whole
     * world knowledge evey constructor is potentially live.
     */
    private boolean isMethodPotentiallyALiveConstructor(JMethod method) {
      if (!(method instanceof JConstructor)) {
        return false;
      }
      assert incremental || liveCtors != null;
      return liveCtors == null || liveCtors.contains(method);
    }

    private JsInvocation maybeCreateClinitCall(JField x) {
      if (!x.isStatic() || x.isCompileTimeConstant()) {
        // Access to compile time constants do not trigger class initialization (JLS 12.4.1).
        return null;
      }

      JDeclaredType targetType = x.getEnclosingType().getClinitTarget();
      if (targetType == null
          || targetType == program.getTypeClassLiteralHolder()
          // When  currentMethod == null, the clinit is being hoisted to the global scope.
          || (currentMethod != null
             && !currentMethod.getEnclosingType().checkClinitTo(targetType))) {
        return null;
      }

      JMethod clinitMethod = targetType.getClinitMethod();
      SourceInfo sourceInfo = x.getSourceInfo();
      return new JsInvocation(sourceInfo, names.get(clinitMethod).makeRef(sourceInfo));
    }

    private JsInvocation maybeCreateClinitCall(JMethod method) {
      if (!isMethodPotentiallyCalledAcrossClasses(method)) {
        // Global optimized compile can prune some clinit calls.
        return null;
      }
      JDeclaredType enclosingType = method.getEnclosingType();
      if (method.canBePolymorphic() || (program.isStaticImpl(method) &&
          !method.isJsOverlay())) {
        return null;
      }
      if (enclosingType == null || !enclosingType.hasClinit()) {
        return null;
      }
      // Avoid recursion sickness.
      if (JProgram.isClinit(method)) {
        return null;
      }

      JMethod clinitMethod = enclosingType.getClinitTarget().getClinitMethod();
      SourceInfo sourceInfo = method.getSourceInfo();
      return new JsInvocation(sourceInfo, names.get(clinitMethod).makeRef(sourceInfo));
    }

    /**
     * If a field is a literal, we can potentially treat it as immutable and assign it once on the
     * prototype, to be reused by all instances of the class, instead of re-assigning the same
     * literal in each constructor.
     */
    private boolean initializeAtTopScope(JField x) {
      if (x.getLiteralInitializer() == null) {
        return false;
      }
      if (x.isFinal() || x.isStatic() || x.isCompileTimeConstant()) {
        // we can definitely initialize at top-scope, as JVM does so as well
        return true;
      }

      return !uninitializedValuePotentiallyObservable.apply(x);
    }

    /**
     * Helpers to avoid casting (can be removed when compiling in Java 8).
     */
    private <T extends JsExpression> T transform(JExpression expression) {
      return transform((JNode) expression);
    }

    private <T extends JsStatement> T transform(JStatement statement) {
      return transform((JNode) statement);
    }

    private JsBlock transform(JBlock statement) {
      return transform((JNode) statement);
    }
  }

  private void addVarsIfNotEmpty(JsVars vars) {
    if (!vars.isEmpty()) {
      getGlobalStatements().add(vars);
    }
  }

  private List<JsStatement> getGlobalStatements() {
    return jsProgram.getGlobalBlock().getStatements();
  }

  /**
   * Return false if the method needs to be generated. Some methods do not need any output,
   * in particular abstract methods and static intializers that are never called.
   */
  private static boolean doesNotHaveConcreteImplementation(JMethod method) {
    return method.isAbstract()
        || method.isJsNative()
        || JjsUtils.isJsMemberUnnecessaryAccidentalOverride(method)
        || (JProgram.isClinit(method)
            && method.getEnclosingType().getClinitTarget() != method.getEnclosingType());
  }

  private static class JavaToJsOperatorMap {
    private static final Map<JBinaryOperator, JsBinaryOperator> bOpMap =
        Maps.newEnumMap(JBinaryOperator.class);
    private static final Map<JUnaryOperator, JsUnaryOperator> uOpMap =
        Maps.newEnumMap(JUnaryOperator.class);

    static {
      bOpMap.put(JBinaryOperator.MUL, JsBinaryOperator.MUL);
      bOpMap.put(JBinaryOperator.DIV, JsBinaryOperator.DIV);
      bOpMap.put(JBinaryOperator.MOD, JsBinaryOperator.MOD);
      bOpMap.put(JBinaryOperator.ADD, JsBinaryOperator.ADD);
      bOpMap.put(JBinaryOperator.CONCAT, JsBinaryOperator.ADD);
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
      bOpMap.put(JBinaryOperator.ASG_CONCAT, JsBinaryOperator.ASG_ADD);
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
      return bOpMap.get(op);
    }

    public static JsUnaryOperator get(JUnaryOperator op) {
      return uOpMap.get(op);
    }
  }

  private class CollectJsFunctionsForInlining extends JVisitor {

    // JavaScript functions that arise from methods that were not inlined in the Java AST
    // NOTE: We use a LinkedHashSet to preserve the order of insertion. So that the following passes
    // that use this result are deterministic.
    private Set<JsNode> functionsForJsInlining = Sets.newLinkedHashSet();
    private JMethod currentMethod;

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (x.isJsniMethod()) {
        // These are methods whose bodies where not traversed by the Java method inliner.
        JsFunction function = jsFunctionsByJavaMethodBody.get(x.getBody());
        if (function != null && function.getBody() != null) {
          functionsForJsInlining.add(function);
        }
        // Add all functions declared inside JSNI blocks as well.
        assert function != null;
        new JsModVisitor() {
          @Override
          public void endVisit(JsFunction x, JsContext ctx) {
            functionsForJsInlining.add(x);
          }
        }.accept(function);
      }

      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod target = x.getTarget();
      if (target.isInliningAllowed() && (target.isJsniMethod()
          || program.getIndexedTypes().contains(target.getEnclosingType())
          || target.getInliningMode() == InliningMode.FORCE_INLINE)) {
        // These are either: 1) callsites to JSNI functions, in which case MethodInliner did not
        // attempt to inline; 2) inserted by normalizations passes AFTER all inlining or 3)
        // calls to methods annotated with @ForceInline that were not inlined by the simple
        // MethodInliner.
        JsFunction function = jsFunctionsByJavaMethodBody.get(currentMethod.getBody());
        if (function != null && function.getBody() != null) {
          functionsForJsInlining.add(function);
        }
      }
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      return true;
    }

    public Set<JsNode> getFunctionsForJsInlining() {
      accept(program);
      return functionsForJsInlining;
    }
  }

  /**
   * Computes:<p>
   * <ul>
   * <li> 1. whether a constructors are live directly (through being in a new operation) or
   * indirectly (only called by other constructors). Only directly live constructors become
   * JS constructor, otherwise they will behave like regular static functions.
   * </li> 2. whether there exists cross class (static) calls or accesses that would need clinits to
   * be triggered. If not clinits need only be called in constructors.
   * <li>
   * </li>
   * </ul>
   */
  private class RecordCrossClassCallsAndConstructorLiveness extends JVisitor {
    // TODO(rluble): This analysis should be extracted from GenerateJavaScriptAST into its own
    // JAVA optimization pass. Constructors that are not newed can be transformed into statified
    // regular methods; and methods that are not called from outside the class boundary can be
    // privatized. Currently we do not use the private modifier to avoid emitting clinits, instead
    // we use the result of this analysis (private methods CAN be called from JSNI in an unrelated
    // class, touche!).
    {
      crossClassTargets =  Sets.newHashSet();
      liveCtors = Sets.newIdentityHashSet();
    }

    private JMethod currentMethod;

    @Override
    public void endVisit(JMethod x, Context ctx) {
      // methods which are exported or static indexed methods may be called externally
      if (x.isJsInteropEntryPoint()
          || (x.isStatic() && program.getIndexedMethods().contains(x))) {
        if (x instanceof JConstructor) {
          // exported ctors always considered live
          liveCtors.add((JConstructor) x);
        }
        // could be called from JS, so clinit must be called from body
        crossClassTargets.add(x);
      }
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JDeclaredType sourceType = currentMethod.getEnclosingType();
      JDeclaredType targetType = x.getTarget().getEnclosingType();
      if (sourceType.checkClinitTo(targetType)) {
        crossClassTargets.add(x.getTarget());
      }
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      super.endVisit(x, ctx);
      liveCtors.add(x.getTarget());
    }

    @Override
    public void endVisit(JProgram x, Context ctx) {
      // Entry methods can be called externally, so they must run clinit.
      crossClassTargets.addAll(x.getEntryMethods());
    }

    @Override
    public void endVisit(JsniMethodRef x, Context ctx) {
      if (x.getTarget() instanceof JConstructor) {
        liveCtors.add((JConstructor) x.getTarget());
      }

      endVisit((JMethodCall) x, ctx);
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      return true;
    }
  }

  private static class SortVisitor extends JVisitor {

    @Override
    public void endVisit(JClassType x, Context ctx) {
      x.sortFields(HasName.BY_NAME_COMPARATOR);
      x.sortMethods(JMethod.BY_SIGNATURE_COMPARATOR);
    }

    @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      x.sortFields(HasName.BY_NAME_COMPARATOR);
      x.sortMethods(JMethod.BY_SIGNATURE_COMPARATOR);
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      x.sortLocals(HasName.BY_NAME_COMPARATOR);
    }

    @Override
    public void endVisit(JProgram x, Context ctx) {
      Collections.sort(x.getEntryMethods(), JMethod.BY_SIGNATURE_COMPARATOR);
      Collections.sort(x.getDeclaredTypes(), HasName.BY_NAME_COMPARATOR);
    }

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      // No need to visit method bodies.
      return false;
    }
  }

  /**
   * This is the main entry point for the translation from Java to JavaScript. Starts from a
   * Java AST and constructs a JavaScript AST while collecting other useful information that
   * is used in subsequent passes.
   *
   * @param logger            a TreeLogger
   * @param program           a Java AST
   * @param jsProgram         an (empty) JavaScript AST
   * @param symbolTable       an (empty) symbol table that will be populated here
   *
   * @return A pair containing a JavaToJavaScriptMap and a Set of JsFunctions that need to be
   *         considered for inlining.
   */
  public static Pair<JavaToJavaScriptMap, Set<JsNode>> exec(TreeLogger logger, JProgram program,
      JsProgram jsProgram, CompilerContext compilerContext, TypeMapper<?> typeMapper,
      Map<StandardSymbolData, JsName> symbolTable, PermutationProperties props) {

    Event event = SpeedTracerLogger.start(CompilerEventType.GENERATE_JS_AST);
    try {
      GenerateJavaScriptAST generateJavaScriptAST = new GenerateJavaScriptAST(logger, program,
          jsProgram, compilerContext, typeMapper, symbolTable, props);
      return generateJavaScriptAST.execImpl();
    } finally {
      event.end();
    }
  }

  private static final ImmutableList<String> METHODS_PROVIDED_BY_PREAMBLE = ImmutableList.of(
      "Class.createForClass", "Class.createForPrimitive", "Class.createForInterface",
      "Class.createForEnum");

  private final Map<JBlock, JsCatch> catchMap = Maps.newIdentityHashMap();

  private final Set<JsName> catchParamIdentifiers = Sets.newHashSet();

  private final Map<JClassType, JsScope> classScopes = Maps.newIdentityHashMap();

  /**
   * A list of methods that are called from another class (ie might need to
   * clinit).
   */
  private Set<JMethod> crossClassTargets = null;

  /**
   * Contains JsNames for all interface methods. A special scope is needed so
   * that independent classes will obfuscate their interface implementation
   * methods the same way.
   */
  private final JsScope interfaceScope;

  private final JsProgram jsProgram;

  private Set<JConstructor> liveCtors = null;

  /**
   * Classes that could potentially see uninitialized values for fields that are initialized in the
   * declaration.
   */
  private Predicate<JField> uninitializedValuePotentiallyObservable;

  private final Map<JAbstractMethodBody, JsFunction> jsFunctionsByJavaMethodBody =
      Maps.newIdentityHashMap();
  private final Map<HasName, JsName> names = Maps.newIdentityHashMap();

  /**
   * Contains JsNames for the Object instance methods, such as equals, hashCode,
   * and toString. All other class scopes have this scope as an ultimate parent.
   */
  private final JsScope objectScope;
  private final Map<JMethod, JsName> polymorphicNames = Maps.newIdentityHashMap();
  private final JProgram program;

  /**
   * SEt of all targets of JNameOf.
   */
  private Set<HasName> nameOfTargets = Sets.newHashSet();

  private final TreeLogger logger;

  /**
   * Maps JsNames to machine-usable identifiers.
   */
  private final Map<StandardSymbolData, JsName> symbolTable;

  /**
   * Contains JsNames for all globals, such as static fields and methods.
   */
  private final JsScope topScope;

  private final Map<JsStatement, JDeclaredType> javaTypeByGlobalStatement = Maps.newHashMap();

  private final Map<JsStatement, JMethod> methodByGlobalStatement = Maps.newHashMap();

  private final TypeMapper<?> typeMapper;

  private final MinimalRebuildCache minimalRebuildCache;

  private final PermutationProperties properties;

  private JsFunction objectConstructorFunction;

  private OptionMethodNameDisplayMode.Mode methodNameMappingMode;

  private final boolean closureCompilerFormatEnabled;

  private final boolean optimize;

  // This is also used to do some final optimizations.
  // TODO(rluble) move optimizations to a Java AST optimization pass.
  private final boolean incremental;

  /**
   * If true, polymorphic functions are made anonymous vtable declarations and
   * not assigned topScope identifiers.
   */
  private final boolean stripStack;

  private GenerateJavaScriptAST(TreeLogger logger, JProgram program, JsProgram jsProgram,
      CompilerContext compilerContext, TypeMapper<?> typeMapper,
      Map<StandardSymbolData, JsName> symbolTable, PermutationProperties properties) {
    this.logger = logger;
    this.program = program;
    this.jsProgram = jsProgram;
    this.topScope = jsProgram.getScope();
    this.objectScope = jsProgram.getObjectScope();
    this.interfaceScope = new JsNormalScope(objectScope, "Interfaces");
    this.minimalRebuildCache = compilerContext.getMinimalRebuildCache();
    this.symbolTable = symbolTable;
    this.typeMapper = typeMapper;
    this.properties = properties;

    PrecompileTaskOptions options = compilerContext.getOptions();
    this.optimize = options.getOptimizationLevel() > OptionOptimize.OPTIMIZE_LEVEL_DRAFT;
    this.methodNameMappingMode = options.getMethodNameDisplayMode();
    assert methodNameMappingMode != null;
    this.incremental = options.isIncrementalCompileEnabled();

    this.stripStack = JsStackEmulator.getStackMode(properties) == JsStackEmulator.StackMode.STRIP;
    this.closureCompilerFormatEnabled = options.isClosureCompilerFormatEnabled();
    this.objectConstructorFunction =
        new JsFunction(SourceOrigin.UNKNOWN, topScope, topScope.findExistingName("Object"));
  }

  /**
   * Retrieves the runtime typeId for {@code type}.
   */
  JExpression getRuntimeTypeReference(JReferenceType type) {
    return typeMapper.get(type);
  }

  private String mangleName(JField x) {
    return JjsUtils.mangleMemberName(x.getEnclosingType().getName(), x.getName());
  }

  private String mangleNameForGlobal(JMethod method) {
    String s =
        JjsUtils.mangleMemberName(method.getEnclosingType().getName(), method.getName()) + "__";
    for (JType type : method.getOriginalParamTypes()) {
      s += type.getJavahSignatureName();
    }
    s += method.getOriginalReturnType().getJavahSignatureName();
    return StringInterner.get().intern(s);
  }

  private String mangleNameForPackagePrivatePoly(JMethod method) {
    assert method.isPackagePrivate() && !method.isStatic();
    /*
     * Package private instance methods in different package should not override each
     * other, so they must have distinct polymorphic names. Therefore, add the
     * package to the mangled name.
     */
    String mangledName = Joiner.on("$").join(
        "package_private",
        JjsUtils.mangledNameString(method.getEnclosingType().getPackageName()),
        JjsUtils.mangledNameString(method));
    return StringInterner.get().intern(JjsUtils.constructManglingSignature(method, mangledName));
  }

  private String mangleNameForPoly(JMethod method) {
    if (method.isPrivate()) {
      return mangleNameForPrivatePoly(method);
    } else if (method.isPackagePrivate()) {
      return mangleNameForPackagePrivatePoly(method);
    } else {
      return mangleNameForPublicPoly(method);
    }
  }

  private String mangleNameForPublicPoly(JMethod method) {
    return StringInterner.get().intern(
        JjsUtils.constructManglingSignature(method, JjsUtils.mangledNameString(method)));
  }

  private String mangleNameForPrivatePoly(JMethod method) {
    assert method.isPrivate() && !method.isStatic();
    /*
     * Private instance methods in different classes should not override each
     * other, so they must have distinct polymorphic names. Therefore, add the
     * class name to the mangled name.
     */
    String mangledName = Joiner.on("$").join(
        "private",
        JjsUtils.mangledNameString(method.getEnclosingType()),
        JjsUtils.mangledNameString(method));

    return StringInterner.get().intern(JjsUtils.constructManglingSignature(method, mangledName));
  }

  private final Map<JType, JDeclarationStatement> classLiteralDeclarationsByType =
      Maps.newLinkedHashMap();

  private void contructTypeToClassLiteralDeclarationMap() {
      /*
       * Must execute in clinit statement order, NOT field order, so that back
       * refs to super classes are preserved.
       */
    JMethodBody clinitBody =
        (JMethodBody) program.getTypeClassLiteralHolder().getClinitMethod().getBody();
    for (JStatement stmt : clinitBody.getStatements()) {
      if (!(stmt instanceof JDeclarationStatement)) {
        continue;
      }
      JDeclarationStatement classLiteralDeclaration = (JDeclarationStatement) stmt;

      JType type = program.getTypeByClassLiteralField(
          (JField) ((JDeclarationStatement) stmt).getVariableRef().getTarget());

      assert !classLiteralDeclarationsByType.containsKey(type);
      classLiteralDeclarationsByType.put(type, classLiteralDeclaration);
    }
  }

  private Pair<JavaToJavaScriptMap, Set<JsNode>> execImpl() {
    NameClashesFixer.exec(program);
    uninitializedValuePotentiallyObservable = optimize
        ? ComputePotentiallyObservableUninitializedValues.analyze(program)
        : Predicates.<JField>alwaysTrue();
    new FindNameOfTargets().accept(program);
    new SortVisitor().accept(program);
    if (!incremental) {
      // TODO(rluble): pull out this analysis and make it a Java AST optimization pass.
      new RecordCrossClassCallsAndConstructorLiveness().accept(program);
    }

    // Map class literals to their respective types.
    contructTypeToClassLiteralDeclarationMap();

    new CreateNamesAndScopesVisitor().accept(program);
    new GenerateJavaScriptTransformer().transform(program);

    // TODO(spoon): Instead of gathering the information here, get it via
    // SourceInfo
    JavaToJavaScriptMap jjsMap = new JavaToJavaScriptMapImpl(program.getDeclaredTypes(),
        names, javaTypeByGlobalStatement, methodByGlobalStatement);

    Set<JsNode> functionsForJsInlining = incremental ? Collections.<JsNode>emptySet() :
        new CollectJsFunctionsForInlining().getFunctionsForJsInlining();

    return Pair.create(jjsMap, functionsForJsInlining);
  }

  private JsFunction getJsFunctionFor(JMethod jMethod) {
    return jsFunctionsByJavaMethodBody.get(jMethod.getBody());
  }

  private JsName getIndexedMethodJsName(String indexedName) {
    return names.get(program.getIndexedMethod(indexedName));
  }

  private JsName getIndexedFieldJsName(String indexedName) {
    return names.get(program.getIndexedField(indexedName));
  }
}
