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

import com.google.gwt.core.ext.linker.CastableTypeMap;
import com.google.gwt.core.ext.linker.impl.StandardCastableTypeMap;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JAbstractMethodBody;
import com.google.gwt.dev.jjs.ast.JArrayLength;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JAssertStatement;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
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
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
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
import com.google.gwt.dev.jjs.ast.JNumericEntry;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JSeedIdOf;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsCastMap;
import com.google.gwt.dev.jjs.ast.js.JsCastMap.JsQueryType;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsStackEmulator;
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
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsSeedIdOf;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsSwitchMember;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsUnaryOperation;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.collect.IdentityHashSet;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Maps;
import com.google.gwt.dev.util.collect.Sets;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

/**
 * Creates a JavaScript AST from a <code>JProgram</code> node.
 */
public class GenerateJavaScriptAST {

  private class CreateNamesAndScopesVisitor extends JVisitor {

    /**
     * Cache of computed Java source file names to URI strings for symbol
     * export. By using a cache we also ensure the miminum number of String
     * instances are serialized.
     */
    private final Map<String, String> fileNameToUriString = new HashMap<String, String>();

    private final Stack<JsScope> scopeStack = new Stack<JsScope>();


    @Override
    public void endVisit(JArrayType x, Context ctx) {
      JsName name = topScope.declareName(x.getName());
      names.put(x, name);
      recordSymbol(x, name);
    }

    @Override
    public void endVisit(JClassType x, Context ctx) {
      pop();
    }

    @Override
    public void endVisit(JsCastMap x, Context ctx) {
      /*
       * Intern JsCastMaps, at this stage, they are only present in Array initialization,
       * so we always intern them even if they occur once, since every array initialization
       * makes a copy.
       */
      internedCastMap.add(castMapToString(x));
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      String name = x.getName();
      String mangleName = mangleName(x);
      if (x.isStatic()) {
        JsName jsName = topScope.declareName(mangleName, name);
        names.put(x, jsName);
        recordSymbol(x, jsName);
      } else {
        JsName jsName;
        if (specialObfuscatedFields.containsKey(x)) {
          jsName = peek().declareName(mangleNameSpecialObfuscate(x));
          jsName.setObfuscatable(false);
        } else {
          jsName = peek().declareName(mangleName, name);
        }
        names.put(x, jsName);
        recordSymbol(x, jsName);
      }
    }

    @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      pop();
    }

    @Override
    public void endVisit(JLabel x, Context ctx) {
      if (names.get(x) != null) {
        return;
      }
      names.put(x, peek().declareName(x.getName()));
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      // locals can conflict, that's okay just reuse the same variable
      JsScope scope = peek();
      JsName jsName = scope.declareName(x.getName());
      names.put(x, jsName);
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      pop();
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      names.put(x, peek().declareName(x.getName()));
    }

    @Override
    public void endVisit(JProgram x, Context ctx) {
      /*
       * put the null method and field into objectScope since they can be
       * referenced as instance on null-types (as determined by type flow)
       */
      JMethod nullMethod = x.getNullMethod();
      polymorphicNames.put(nullMethod, objectScope.declareName(nullMethod.getName()));
      JField nullField = x.getNullField();
      JsName nullFieldName = objectScope.declareName(nullField.getName());
      names.put(nullField, nullFieldName);

      /*
       * put nullMethod in the global scope, too; it's the replacer for clinits
       */
      nullFunc = createGlobalFunction("function " + nullMethod.getName() + "(){}");
      names.put(nullMethod, nullFunc.getName());

      /*
       * Create names for instantiable array types since JProgram.traverse()
       * doesn't iterate over them.
       */
      for (JArrayType arrayType : program.getAllArrayTypes()) {
        if (typeOracle.isInstantiatedType(arrayType)) {
          accept(arrayType);
        }
      }

      // Generate symbolic names for all query type ids.
      if (!output.shouldMinimize()) {
        setupSymbolicCastMaps();
      }
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // have I already been visited as a super type?
      JsScope myScope = classScopes.get(x);
      if (myScope != null) {
        push(myScope);
        return false;
      }

      // My seed function name
      JsName jsName = topScope.declareName(getNameString(x), x.getShortName());
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

      push(myScope);
      return true;
    }

    @Override
    public boolean visit(JInterfaceType x, Context ctx) {
      // interfaces have no name at run time
      push(interfaceScope);
      return true;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      // my polymorphic name
      String name = x.getName();
      if (x.needsVtable()) {
        if (polymorphicNames.get(x) == null) {
          JsName polyName;
          if (x.isPrivate()) {
            polyName = interfaceScope.declareName(mangleNameForPrivatePoly(x), name);
          } else if (specialObfuscatedMethodSigs.containsKey(x.getSignature())) {
            polyName = interfaceScope.declareName(mangleNameSpecialObfuscate(x));
            polyName.setObfuscatable(false);
          } else {
            polyName = interfaceScope.declareName(mangleNameForPoly(x), name);
          }
          polymorphicNames.put(x, polyName);
        }
      }

      if (x.isAbstract()) {
        // just push a dummy scope that we can pop in endVisit
        push(null);
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
       * or stack-stripping is disabled.
       */
      if (!stripStack || !polymorphicNames.containsKey(x) || x.isNative()) {
        globalName = topScope.declareName(mangleName, name);
        names.put(x, globalName);
        recordSymbol(x, globalName);
      }
      JsFunction jsFunction;
      if (x.isNative()) {
        // set the global name of the JSNI peer
        JsniMethodBody body = (JsniMethodBody) x.getBody();
        jsFunction = body.getFunc();
        jsFunction.setName(globalName);
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
        jsFunction = new JsFunction(x.getSourceInfo(), topScope, globalName, true);
      }
      if (polymorphicNames.containsKey(x)) {
        polymorphicJsFunctions.add(jsFunction);
      }
      methodBodyMap.put(x.getBody(), jsFunction);
      push(jsFunction.getScope());

      if (program.getIndexedMethods().contains(x)) {
        indexedFunctions =
            Maps.put(indexedFunctions, x.getEnclosingType().getShortName() + "." + x.getName(),
                jsFunction);
      }

      return true;
    }

    @Override
    public boolean visit(JTryStatement x, Context ctx) {
      accept(x.getTryBlock());

      List<JLocalRef> catchArgs = x.getCatchArgs();
      List<JBlock> catchBlocks = x.getCatchBlocks();
      for (int i = 0, c = catchArgs.size(); i < c; ++i) {
        JLocalRef arg = catchArgs.get(i);
        JBlock catchBlock = catchBlocks.get(i);
        JsCatch jsCatch = new JsCatch(x.getSourceInfo(), peek(), arg.getTarget().getName());
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

    private JsFunction createGlobalFunction(String code) {
      try {
        List<JsStatement> stmts =
            JsParser.parse(SourceOrigin.UNKNOWN, topScope, new StringReader(code));
        assert stmts.size() == 1;
        JsExprStmt stmt = (JsExprStmt) stmts.get(0);
        List<JsStatement> globalStmts = jsProgram.getGlobalBlock().getStatements();
        globalStmts.add(0, stmt);
        return (JsFunction) stmt.getExpression();
      } catch (Exception e) {
        throw new InternalCompilerException("Unexpected exception parsing '" + code + "'", e);
      }
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

    private JsScope peek() {
      return scopeStack.peek();
    }

    private void pop() {
      scopeStack.pop();
    }

    private void push(JsScope scope) {
      scopeStack.push(scope);
    }

    private void recordSymbol(JReferenceType x, JsName jsName) {
      StringBuilder sb = new StringBuilder();
      sb.append('{');
      JsCastMap castMap = program.getCastMap(x);
      if (castMap != null) {
        boolean isFirst = true;
        for (JExpression expr : castMap.getExprs()) {
          JsQueryType queryType = (JsQueryType) expr;
          if (isFirst) {
            isFirst = false;
          } else {
            sb.append(',');
          }
          sb.append(queryType.getQueryId());
          sb.append(":1");
        }
      }
      sb.append('}');
      CastableTypeMap castableTypeMap = new StandardCastableTypeMap(sb.toString());

      StandardSymbolData symbolData =
          StandardSymbolData.forClass(x.getName(), x.getSourceInfo().getFileName(), x
              .getSourceInfo().getStartLine(), program.getQueryId(x), castableTypeMap,
              x instanceof JClassType || x instanceof JArrayType ? getSeedId(x) : -1);
      assert !symbolTable.containsKey(symbolData);
      symbolTable.put(symbolData, jsName);
    }

    private <T extends HasEnclosingType & HasName & HasSourceInfo> void recordSymbol(T x,
        JsName jsName) {
      /*
       * NB: The use of x.getName() can produce confusion in cases where a type
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

      String methodSig;
      if (x instanceof JMethod) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        JMethod method = ((JMethod) x);
        for (JType t : method.getOriginalParamTypes()) {
          sb.append(t.getJsniSignatureName());
        }
        sb.append(')');
        sb.append(method.getOriginalReturnType().getJsniSignatureName());
        methodSig = StringInterner.get().intern(sb.toString());
      } else {
        methodSig = null;
      }

      StandardSymbolData symbolData =
          StandardSymbolData.forMember(x.getEnclosingType().getName(), x.getName(), methodSig,
              makeUriString(x), x.getSourceInfo().getStartLine());
      assert !symbolTable.containsKey(symbolData) : "Duplicate symbol " + "recorded "
          + jsName.getIdent() + " for " + x.getName() + " and key " + symbolData.getJsniIdent();
      symbolTable.put(symbolData, jsName);
    }

    /**
     * Create more readable output by generating symbolic constants for query
     * ids.
     */
    private void setupSymbolicCastMaps() {
      namesByQueryId = new ArrayList<JsName>();

      for (JReferenceType type : program.getTypesByQueryId()) {
        String shortName;
        String longName;
        if (type instanceof JArrayType) {
          JArrayType arrayType = (JArrayType) type;
          JType leafType = arrayType.getLeafType();
          if (leafType instanceof JReferenceType) {
            shortName = ((JReferenceType) leafType).getShortName();
          } else {
            shortName = leafType.getName();
          }
          shortName += "_$" + arrayType.getDims();
          longName = getNameString(leafType) + "_$" + arrayType.getDims();
        } else {
          shortName = type.getShortName();
          longName = getNameString(type);
        }
        JsName name = topScope.declareName("Q$" + longName, "Q$" + shortName);
        namesByQueryId.add(name);
      }
      // TODO(cromwellian): see about moving this into an immortal type
      StringBuilder sb = new StringBuilder();
      sb.append("function makeCastMap(a) {");
      sb.append("  var result = {};");
      sb.append("  for (var i = 0, c = a.length; i < c; ++i) {");
      sb.append("    result[a[i]] = 1;");
      sb.append("  }");
      sb.append("  return result;");
      sb.append("}");
      makeMapFunction = createGlobalFunction(sb.toString());
    }
  }

  private class GenerateJavaScriptVisitor extends GenerateJavaScriptLiterals {

    private final Set<JClassType> alreadyRan = new HashSet<JClassType>();

    private final JsName arrayLength = objectScope.declareName("length");

    private final Set<String> castMapSeen = new HashSet<String>();

    private final Map<JClassType, JsFunction> clinitMap = new HashMap<JClassType, JsFunction>();

    private JMethod currentMethod = null;

    /**
     * The JavaScript functions corresponding to the entry methods of the
     * program ({@link JProgram#getEntryMethods()}).
     */
    private JsFunction[] entryFunctions;

    /**
     * A reverse index for the entry methods of the program (
     * {@link JProgram#getEntryMethods()}). Each entry method is mapped to its
     * integer index.
     */
    private Map<JMethod, Integer> entryMethodToIndex;

    private final JsName globalTemp = topScope.declareName("_");

    private final JsName prototype = objectScope.declareName("prototype");


    {
      globalTemp.setObfuscatable(false);
      prototype.setObfuscatable(false);
      arrayLength.setObfuscatable(false);
    }

    public GenerateJavaScriptVisitor() {
    }
    
    @Override
    public void endVisit(JAbsentArrayDimension x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    @Override
    public void endVisit(JArrayLength x, Context ctx) {
      assert x.getInstance() != null : "Can't access the length of a null array";
      JsExpression qualifier = (JsExpression) pop();
      JsNameRef ref = arrayLength.makeRef(x.getSourceInfo());
      ref.setQualifier(qualifier);
      push(ref);
    }

    @Override
    public void endVisit(JArrayRef x, Context ctx) {
      JsArrayAccess jsArrayAccess = new JsArrayAccess(x.getSourceInfo());
      jsArrayAccess.setIndexExpr((JsExpression) pop());
      jsArrayAccess.setArrayExpr((JsExpression) pop());
      push(jsArrayAccess);
    }

    @Override
    public void endVisit(JAssertStatement x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JsExpression rhs = (JsExpression) pop(); // rhs
      JsExpression lhs = (JsExpression) pop(); // lhs
      JsBinaryOperator myOp = JavaToJsOperatorMap.get(x.getOp());

      /*
       * Use === and !== on reference types, or else you can get wrong answers
       * when Object.toString() == 'some string'.
       */
      if (myOp == JsBinaryOperator.EQ && x.getLhs().getType() instanceof JReferenceType
          && x.getRhs().getType() instanceof JReferenceType) {
        myOp = JsBinaryOperator.REF_EQ;
      } else if (myOp == JsBinaryOperator.NEQ && x.getLhs().getType() instanceof JReferenceType
          && x.getRhs().getType() instanceof JReferenceType) {
        myOp = JsBinaryOperator.REF_NEQ;
      }

      push(new JsBinaryOperation(x.getSourceInfo(), myOp, lhs, rhs));
    }

    @Override
    public void endVisit(JBlock x, Context ctx) {
      JsBlock jsBlock = new JsBlock(x.getSourceInfo());
      List<JsStatement> stmts = jsBlock.getStatements();
      popList(stmts, x.getStatements().size()); // stmts
      Iterator<JsStatement> iterator = stmts.iterator();
      while (iterator.hasNext()) {
        JsStatement stmt = iterator.next();
        if (stmt instanceof JsEmpty) {
          iterator.remove();
        }
      }
      push(jsBlock);
    }

    @Override
    public void endVisit(JBreakStatement x, Context ctx) {
      JsNameRef labelRef = null;
      if (x.getLabel() != null) {
        JsLabel label = (JsLabel) pop(); // label
        labelRef = label.getName().makeRef(x.getSourceInfo());
      }
      push(new JsBreak(x.getSourceInfo(), labelRef));
    }

    @Override
    public void endVisit(JCaseStatement x, Context ctx) {
      if (x.getExpr() == null) {
        push(new JsDefault(x.getSourceInfo()));
      } else {
        JsCase jsCase = new JsCase(x.getSourceInfo());
        jsCase.setCaseExpr((JsExpression) pop()); // expr
        push(jsCase);
      }
    }

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      // These are left in when cast checking is disabled.
    }

    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      JsName classLit = names.get(x.getField());
      push(classLit.makeRef(x.getSourceInfo()));
    }

    @Override
    public void endVisit(JClassType x, Context ctx) {
      if (alreadyRan.contains(x)) {
        return;
      }

      if (program.getTypeClassLiteralHolder() == x) {
        // Handled in generateClassLiterals.
        return;
      }

      if (program.immortalCodeGenTypes.contains(x)) {
        // Handled in generateImmortalTypes
        return;
      }

      alreadyRan.add(x);

      List<JsFunction> jsFuncs = popList(x.getMethods().size()); // methods
      List<JsNode> jsFields = popList(x.getFields().size()); // fields

      if (x.getClinitTarget() == x) {
        JsFunction superClinit = clinitMap.get(x.getSuperClass());
        JsFunction myClinit = jsFuncs.get(0);
        handleClinit(myClinit, superClinit);
        clinitMap.put(x, myClinit);
      } else {
        jsFuncs.set(0, null);
      }

      List<JsStatement> globalStmts = jsProgram.getGlobalBlock().getStatements();

      // declare all methods into the global scope
      for (int i = 0; i < jsFuncs.size(); ++i) {
        JsFunction func = jsFuncs.get(i);

        // don't add polymorphic JsFuncs, inline decl into vtable assignment
        if (func != null && !polymorphicJsFunctions.contains(func)) {
          globalStmts.add(func.makeStmt());
        }
      }

      if (typeOracle.isInstantiatedType(x) && !program.isJavaScriptObject(x)) {
        generateClassSetup(x, globalStmts);
      }

      // setup fields
      JsVars vars = new JsVars(x.getSourceInfo());
      for (int i = 0; i < jsFields.size(); ++i) {
        JsNode node = jsFields.get(i);
        if (node instanceof JsVar) {
          vars.add((JsVar) node);
        } else {
          assert (node instanceof JsStatement);
          JsStatement stmt = (JsStatement) node;
          globalStmts.add(stmt);
          typeForStatMap.put(stmt, x);
        }
      }

      if (!vars.isEmpty()) {
        globalStmts.add(vars);
      }

      for (JNode node : x.getArtificialRescues()) {
        if (node instanceof JMethod) {
          JsName jsName = names.get(node);
          if (jsName != null) {
            JsFunction func = (JsFunction) jsName.getStaticRef();
            func.setArtificiallyRescued(true);
          }
        }
      }
      
      // TODO(zundel): Check that each unique method has a unique
      // name / poly name.
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      JsExpression elseExpr = (JsExpression) pop(); // elseExpr
      JsExpression thenExpr = (JsExpression) pop(); // thenExpr
      JsExpression ifTest = (JsExpression) pop(); // ifTest
      push(new JsConditional(x.getSourceInfo(), ifTest, thenExpr, elseExpr));
    }

    @Override
    public void endVisit(JContinueStatement x, Context ctx) {
      JsNameRef labelRef = null;
      if (x.getLabel() != null) {
        JsLabel label = (JsLabel) pop(); // label
        labelRef = label.getName().makeRef(x.getSourceInfo());
      }
      push(new JsContinue(x.getSourceInfo(), labelRef));
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      if (x.getInitializer() == null) {
        pop(); // variableRef
        /*
         * Declaration statements can only appear in blocks, so it's okay to
         * push null instead of an empty statement
         */
        push(null);
        return;
      }

      JsExpression initializer = (JsExpression) pop(); // initializer
      JsNameRef localRef = (JsNameRef) pop(); // localRef

      JVariable target = x.getVariableRef().getTarget();
      if (target instanceof JField && ((JField) target).getLiteralInitializer() != null) {
        // Will initialize at top scope; no need to double-initialize.
        push(null);
        return;
      }

      JsBinaryOperation binOp =
          new JsBinaryOperation(x.getSourceInfo(), JsBinaryOperator.ASG, localRef, initializer);

      push(binOp.makeStmt());
    }

    @Override
    public void endVisit(JDoStatement x, Context ctx) {
      JsDoWhile stmt = new JsDoWhile(x.getSourceInfo());
      if (x.getBody() != null) {
        stmt.setBody((JsStatement) pop()); // body
      } else {
        stmt.setBody(new JsEmpty(x.getSourceInfo()));
      }
      stmt.setCondition((JsExpression) pop()); // testExpr
      push(stmt);
    }

    @Override
    public void endVisit(JExpressionStatement x, Context ctx) {
      JsExpression expr = (JsExpression) pop(); // expr
      push(expr.makeStmt());
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      // if we need an initial value, create an assignment
      if (x.getLiteralInitializer() != null) {
        // setup the constant value
        accept(x.getLiteralInitializer());
      } else if (!x.hasInitializer() && x.getEnclosingType() != program.getTypeJavaLangObject()) {
        // setup a default value
        accept(x.getType().getDefaultValue());
      } else {
        // the variable is setup during clinit, no need to initialize here
        push(null);
      }
      JsExpression rhs = (JsExpression) pop();
      JsName name = names.get(x);

      if (program.getIndexedFields().contains(x)) {
        indexedFields =
            Maps.put(indexedFields, x.getEnclosingType().getShortName() + "." + x.getName(), name);
      }

      if (x.isStatic()) {
        // setup a var for the static
        JsVar var = new JsVar(x.getSourceInfo(), name);
        var.setInitExpr(rhs);
        push(var);
      } else {
        // for non-statics, only setup an assignment if needed
        if (rhs != null) {
          JsNameRef fieldRef = name.makeRef(x.getSourceInfo());
          fieldRef.setQualifier(globalTemp.makeRef(x.getSourceInfo()));
          JsExpression asg = createAssignment(fieldRef, rhs);
          push(new JsExprStmt(x.getSourceInfo(), asg));
        } else {
          push(null);
        }
      }
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      JField field = x.getField();
      JsName jsFieldName = names.get(field);
      JsNameRef nameRef = jsFieldName.makeRef(x.getSourceInfo());
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

    @Override
    public void endVisit(JForStatement x, Context ctx) {
      JsFor jsFor = new JsFor(x.getSourceInfo());

      // body
      if (x.getBody() != null) {
        jsFor.setBody((JsStatement) pop());
      } else {
        jsFor.setBody(new JsEmpty(x.getSourceInfo()));
      }

      // increments
      {
        JsExpression incrExpr = null;
        List<JsExprStmt> exprStmts = popList(x.getIncrements().size());
        for (int i = 0; i < exprStmts.size(); ++i) {
          JsExprStmt exprStmt = exprStmts.get(i);
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
      List<JsExprStmt> initStmts = popList(x.getInitializers().size());
      for (int i = 0; i < initStmts.size(); ++i) {
        JsExprStmt initStmt = initStmts.get(i);
        if (initStmt != null) {
          initExpr = createCommaExpression(initExpr, initStmt.getExpression());
        }
      }
      jsFor.setInitExpr(initExpr);

      push(jsFor);
    }

    @Override
    public void endVisit(JGwtCreate x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    @Override
    public void endVisit(JIfStatement x, Context ctx) {
      JsIf stmt = new JsIf(x.getSourceInfo());

      if (x.getElseStmt() != null) {
        stmt.setElseStmt((JsStatement) pop()); // elseStmt
      }

      if (x.getThenStmt() != null) {
        stmt.setThenStmt((JsStatement) pop()); // thenStmt
      } else {
        stmt.setThenStmt(new JsEmpty(x.getSourceInfo()));
      }

      stmt.setIfExpr((JsExpression) pop()); // ifExpr
      push(stmt);
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      List<JsFunction> jsFuncs = popList(x.getMethods().size()); // methods
      List<JsVar> jsFields = popList(x.getFields().size()); // fields
      List<JsStatement> globalStmts = jsProgram.getGlobalBlock().getStatements();

      if (x.getClinitTarget() == x) {
        JsFunction clinitFunc = jsFuncs.get(0);
        handleClinit(clinitFunc, null);
        globalStmts.add(clinitFunc.makeStmt());
      }

      // setup fields
      JsVars vars = new JsVars(x.getSourceInfo());
      for (int i = 0; i < jsFields.size(); ++i) {
        vars.add(jsFields.get(i));
      }
      if (!vars.isEmpty()) {
        globalStmts.add(vars);
      }
    }

    @Override
    public void endVisit(JLabel x, Context ctx) {
      push(new JsLabel(x.getSourceInfo(), names.get(x)));
    }

    @Override
    public void endVisit(JLabeledStatement x, Context ctx) {
      JsStatement body = (JsStatement) pop(); // body
      JsLabel label = (JsLabel) pop(); // label
      label.setStmt(body);
      push(label);
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      push(names.get(x).makeRef(x.getSourceInfo()));
    }

    @Override
    public void endVisit(JLocalRef x, Context ctx) {
      push(names.get(x.getTarget()).makeRef(x.getSourceInfo()));
    }

    @Override
    public void endVisit(JLongLiteral x, Context ctx) {
      super.endVisit(x, ctx);
      JsExpression longLiteralAllocation = pop();

      // My seed function name
      String nameString = Long.toString(x.getValue(), 16);
      if (nameString.charAt(0) == '-') {
        nameString = "N" + nameString.substring(1);
      } else {
        nameString = "P" + nameString;
      }
      nameString += "_longLit";
      JsName longLit = topScope.declareName(nameString);
      longLits.put(x.getValue(), longLit);
      longObjects.put(longLit, longLiteralAllocation);
      push(longLit.makeRef(x.getSourceInfo()));
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (x.isAbstract()) {
        push(null);
        return;
      }

      JsFunction jsFunc = (JsFunction) pop(); // body
      List<JsParameter> params = popList(x.getParams().size()); // params

      if (!x.isNative()) {
        // Setup params on the generated function. A native method already got
        // its jsParams set in BuildTypeMap.
        // TODO: Do we really need to do that in BuildTypeMap?
        List<JsParameter> jsParams = jsFunc.getParameters();
        for (int i = 0; i < params.size(); ++i) {
          JsParameter param = params.get(i);
          jsParams.add(param);
        }
      }

      JsInvocation jsInvocation = maybeCreateClinitCall(x);
      if (jsInvocation != null) {
        jsFunc.getBody().getStatements().add(0, jsInvocation.makeStmt());
      }

      if (x.isTrace()) {
        jsFunc.setTrace();
      }

      push(jsFunc);
      Integer entryIndex = entryMethodToIndex.get(x);
      if (entryIndex != null) {
        entryFunctions[entryIndex] = jsFunc;
      }
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {

      JsBlock body = (JsBlock) pop();
      List<JsNameRef> locals = popList(x.getLocals().size()); // locals

      JsFunction jsFunc = methodBodyMap.get(x);
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
      JsVars vars = new JsVars(x.getSourceInfo());
      Set<String> alreadySeen = new HashSet<String>();
      for (int i = 0; i < locals.size(); ++i) {
        JsName name = names.get(x.getLocals().get(i));
        String ident = name.getIdent();
        if (!alreadySeen.contains(ident)) {
          alreadySeen.add(ident);
          vars.add(new JsVar(x.getSourceInfo(), name));
        }
      }

      if (!vars.isEmpty()) {
        jsFunc.getBody().getStatements().add(0, vars);
      }

      push(jsFunc);
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      JsInvocation jsInvocation = new JsInvocation(x.getSourceInfo());

      popList(jsInvocation.getArguments(), x.getArgs().size()); // args

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
          if (x.getInstance() != null) {
            pop(); // instance
          }
          // generate a null expression, which will get optimized out
          push(JsNullLiteral.INSTANCE);
          return;
        } else if (type != clinitTarget) {
          // replace the method with its retargeted clinit
          method = clinitTarget.getMethods().get(0);
        }
      }

      JsNameRef qualifier;
      JsExpression unnecessaryQualifier = null;
      if (method.isStatic()) {
        if (x.getInstance() != null) {
          unnecessaryQualifier = (JsExpression) pop(); // instance
        }
        qualifier = names.get(method).makeRef(x.getSourceInfo());
      } else {
        if (x.isStaticDispatchOnly()) {
          /*
           * Dispatch statically (odd case). This happens when a call that must
           * be static is targeting an instance method that could not be
           * transformed into a static. Super/this constructor calls work this
           * way. Have to use a "call" construct.
           */
          JsName callName = objectScope.declareName("call");
          callName.setObfuscatable(false);
          qualifier = callName.makeRef(x.getSourceInfo());
          qualifier.setQualifier(names.get(method).makeRef(x.getSourceInfo()));
          jsInvocation.getArguments().add(0, (JsExpression) pop()); // instance
        } else {
          // Dispatch polymorphically (normal case).
          qualifier = polymorphicNames.get(method).makeRef(x.getSourceInfo());
          qualifier.setQualifier((JsExpression) pop()); // instance
        }
      }
      jsInvocation.setQualifier(qualifier);
      push(createCommaExpression(unnecessaryQualifier, jsInvocation));
    }

    @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      List<JsExpression> exprs = popList(x.exprs.size());
      JsExpression cur = null;
      for (int i = 0; i < exprs.size(); ++i) {
        JsExpression next = exprs.get(i);
        cur = createCommaExpression(cur, next);
      }
      if (cur == null) {
        // the multi-expression was empty; use undefined
        cur = new JsNameRef(x.getSourceInfo(), JsRootScope.INSTANCE.getUndefined());
      }
      push(cur);
    }

    @Override
    public void endVisit(JNameOf x, Context ctx) {
      JsName name = names.get(x.getNode());
      assert name != null : "Missing JsName for " + x.getNode().getName();
      push(new JsNameOf(x.getSourceInfo(), name));
    }

    @Override
    public void endVisit(JNewArray x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      JsNameRef nameRef = names.get(x.getTarget()).makeRef(x.getSourceInfo());
      JsNew newOp = new JsNew(x.getSourceInfo(), nameRef);
      popList(newOp.getArguments(), x.getArgs().size()); // args
      push(newOp);
    }
    
    @Override
    public void endVisit(JNumericEntry x, Context ctx) {
      push(new JsNumericEntry(x.getSourceInfo(), x.getKey(), x.getValue()));
    }
    
    @Override
    public void endVisit(JParameter x, Context ctx) {
      push(new JsParameter(x.getSourceInfo(), names.get(x)));
    }

    @Override
    public void endVisit(JParameterRef x, Context ctx) {
      push(names.get(x.getTarget()).makeRef(x.getSourceInfo()));
    }

    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      JsUnaryOperation op =
          new JsPostfixOperation(x.getSourceInfo(), JavaToJsOperatorMap.get(x.getOp()),
              ((JsExpression) pop())); // arg
      push(op);
    }

    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      JsUnaryOperation op =
          new JsPrefixOperation(x.getSourceInfo(), JavaToJsOperatorMap.get(x.getOp()),
              ((JsExpression) pop())); // arg
      push(op);
    }

    @Override
    public void endVisit(JProgram x, Context ctx) {
      List<JsStatement> globalStmts = jsProgram.getGlobalBlock().getStatements();

      // Generate entry methods
      generateGwtOnLoad(Lists.create(entryFunctions), globalStmts);

      // Add a few things onto the beginning.

      // Reserve the "_" identifier.
      JsVars vars = new JsVars(jsProgram.getSourceInfo());
      vars.add(new JsVar(jsProgram.getSourceInfo(), globalTemp));
      globalStmts.add(0, vars);

      // Long lits must go at the top, they can be constant field initializers.
      generateLongLiterals(vars);
      generateImmortalTypes(vars);
      generateQueryIdConstants(vars);
      generateInternedCastMapLiterals(vars);
     
      // Class objects, but only if there are any.
      if (x.getDeclaredTypes().contains(x.getTypeClassLiteralHolder())) {
        // TODO: perhaps they could be constant field initializers also?
        vars = new JsVars(jsProgram.getSourceInfo());
        generateClassLiterals(vars);
        if (!vars.isEmpty()) {
          globalStmts.add(vars);
        }
      }

      if (program.getRunAsyncs().size() > 0) {
        // Prevent onLoad from being pruned.
        JMethod onLoadMethod = program.getIndexedMethod("AsyncFragmentLoader.onLoad");
        JsName name = names.get(onLoadMethod);
        assert name != null;
        JsFunction func = (JsFunction) name.getStaticRef();
        func.setArtificiallyRescued(true);
      }
    }

    @Override
    public void endVisit(JReboundEntryPoint x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    @Override
    public void endVisit(JReturnStatement x, Context ctx) {
      if (x.getExpr() != null) {
        push(new JsReturn(x.getSourceInfo(), (JsExpression) pop())); // expr
      } else {
        push(new JsReturn(x.getSourceInfo()));
      }
    }

    @Override
    public void endVisit(JSeedIdOf x, Context ctx) {
      JsName name = names.get(x.getNode());
      push(new JsSeedIdOf(x.getSourceInfo(), name, getSeedId((JReferenceType) x.getNode())));
    }

    @Override
    public void endVisit(JsCastMap x, Context ctx) {
      super.endVisit(x, ctx);
      JsArrayLiteral arrayLit = (JsArrayLiteral) pop();
      SourceInfo sourceInfo = x.getSourceInfo();
      if (namesByQueryId == null || x.getExprs().size() == 0) {
        String stringMap = castMapToString(x);
        // if interned, use variable reference
        if (namesByCastMap.containsKey(stringMap)) {
          push(namesByCastMap.get(stringMap).makeRef(x.getSourceInfo()));
        } else if (internedCastMap.contains(stringMap)) {
          // interned variable hasn't been created yet
          String internName = "CM$";
          boolean first = true;
          for (JExpression expr : x.getExprs()) {
            if (first) {
              first = false;
            } else {
              internName += "_";
            }
            // Name is CM$queryId_queryId_queryId
            internName += ((JsQueryType) expr).getQueryId();
          }
          JsName internedCastMapName = topScope.declareName(internName, internName);
          namesByCastMap.put(stringMap, internedCastMapName);
          castMapByString.put(stringMap, castMapToObjectLiteral(arrayLit, sourceInfo));
          push(internedCastMapName.makeRef(x.getSourceInfo()));
        } else {
          push(castMapToObjectLiteral(arrayLit, sourceInfo));
        }
      } else {
        // makeMap([Q_Object, Q_Foo, Q_Bar]);
        JsInvocation inv = new JsInvocation(sourceInfo);
        inv.setQualifier(makeMapFunction.getName().makeRef(sourceInfo));
        inv.getArguments().add(arrayLit);
        push(inv);
      }
    }

    @Override
    public void endVisit(JsniMethodRef x, Context ctx) {
      JMethod method = x.getTarget();
      JsNameRef nameRef = names.get(method).makeRef(x.getSourceInfo());
      push(nameRef);
    }

    @Override
    public void endVisit(JsonArray x, Context ctx) {
      JsArrayLiteral jsArrayLiteral = new JsArrayLiteral(x.getSourceInfo());
      popList(jsArrayLiteral.getExpressions(), x.getExprs().size());
      push(jsArrayLiteral);
    }

    @Override
    public void endVisit(JsonObject x, Context ctx) {
      JsObjectLiteral jsObjectLiteral = new JsObjectLiteral(x.getSourceInfo());
      popList(jsObjectLiteral.getPropertyInitializers(), x.propInits.size());
      push(jsObjectLiteral);
    }

    @Override
    public void endVisit(JsonPropInit init, Context ctx) {
      JsExpression valueExpr = (JsExpression) pop();
      JsExpression labelExpr = (JsExpression) pop();
      push(new JsPropertyInitializer(init.getSourceInfo(), labelExpr, valueExpr));
    }

    @Override
    public void endVisit(JsQueryType x, Context ctx) {
      if (namesByQueryId == null || x.getQueryId() < 0) {
        super.endVisit(x, ctx);
      } else {
        JsName name = namesByQueryId.get(x.getQueryId());
        push(name.makeRef(x.getSourceInfo()));
      }
    }

    @Override
    public void endVisit(JThisRef x, Context ctx) {
      push(new JsThisRef(x.getSourceInfo()));
    }

    @Override
    public void endVisit(JThrowStatement x, Context ctx) {
      push(new JsThrow(x.getSourceInfo(), (JsExpression) pop())); // expr
    }

    @Override
    public void endVisit(JTryStatement x, Context ctx) {
      JsTry jsTry = new JsTry(x.getSourceInfo());

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
        JsCatch jsCatch = catchMap.get(x.getCatchBlocks().get(0));
        jsCatch.setBody(catchBlock);
        jsTry.getCatches().add(jsCatch);
      }

      jsTry.setTryBlock((JsBlock) pop()); // tryBlock

      push(jsTry);
    }

    @Override
    public void endVisit(JWhileStatement x, Context ctx) {
      JsWhile stmt = new JsWhile(x.getSourceInfo());
      if (x.getBody() != null) {
        stmt.setBody((JsStatement) pop()); // body
      } else {
        stmt.setBody(new JsEmpty(x.getSourceInfo()));
      }
      stmt.setCondition((JsExpression) pop()); // testExpr
      push(stmt);
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      if (alreadyRan.contains(x)) {
        return false;
      }

      if (program.getTypeClassLiteralHolder() == x) {
        // Handled in generateClassLiterals.
        return false;
      }

      if (program.immortalCodeGenTypes.contains(x)) {
        // Handled in generateImmortalTypes
        return false;
      }

      // force super type to generate code first, this is required for prototype
      // chaining to work properly
      if (x.getSuperClass() != null && !alreadyRan.contains(x)) {
        accept(x.getSuperClass());
      }
      
      return super.visit(x, ctx);
    }

    @Override
    public boolean visit(JDeclaredType x, Context ctx) {
      checkForDupMethods(x);
      return true;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      if (x.isAbstract()) {
        return false;
      }
      currentMethod = x;
      return true;
    }

    @Override
    public boolean visit(JProgram x, Context ctx) {
      /*
       * Arrange for entryFunctions to be filled in as functions are visited.
       * See their Javadoc comments for more details.
       */
      List<JMethod> entryMethods = x.getEntryMethods();
      entryFunctions = new JsFunction[entryMethods.size()];
      entryMethodToIndex = new IdentityHashMap<JMethod, Integer>();
      for (int i = 0; i < entryMethods.size(); i++) {
        entryMethodToIndex.put(entryMethods.get(i), i);
      }

      for (JDeclaredType type : x.getDeclaredTypes()) {
        if (program.typeOracle.isInstantiatedType(type)) {
          internCastMap(program.getCastMap(type));
        }
      }
      return true;
    }

    @Override
    public boolean visit(JsniMethodBody x, Context ctx) {
      final Map<String, JNode> jsniMap = new HashMap<String, JNode>();
      for (JsniClassLiteral ref : x.getClassRefs()) {
        jsniMap.put(ref.getIdent(), ref.getField());
      }
      for (JsniFieldRef ref : x.getJsniFieldRefs()) {
        jsniMap.put(ref.getIdent(), ref.getField());
      }
      for (JsniMethodRef ref : x.getJsniMethodRefs()) {
        jsniMap.put(ref.getIdent(), ref.getTarget());
      }

      final JsFunction jsFunc = x.getFunc();

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
          // Replace invocation to ctor with a new op.
          if (x.getQualifier() instanceof JsNameRef) {
            JsNameRef ref = (JsNameRef) x.getQualifier();
            String ident = ref.getIdent();
            if (isJsniIdent(ident)) {
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
          }
        }

        @Override
        public void endVisit(JsNameRef x, JsContext ctx) {
          String ident = x.getIdent();
          if (isJsniIdent(ident)) {
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
                JsExpression commaExpr = createCommaExpression(clinitCall, x);
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
                JsFunction closureFunc = new JsFunction(info, jsFunc.getScope());
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
                  jsName = nullFunc.getName();
                }
                x.resolve(jsName);
              }
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

        private boolean isJsniIdent(String ident) {
          return ident.charAt(0) == '@';
        }
      }.accept(jsFunc);

      push(jsFunc);

      // Do NOT visit JsniMethodRefs/JsniFieldRefs.
      return false;
    }

    @Override
    public boolean visit(JSwitchStatement x, Context ctx) {
      /*
       * What a pain.. JSwitchStatement and JsSwitch are modeled completely
       * differently. Here we try to resolve those differences.
       */
      JsSwitch jsSwitch = new JsSwitch(x.getSourceInfo());
      accept(x.getExpr());
      jsSwitch.setExpr((JsExpression) pop()); // expr

      List<JStatement> bodyStmts = x.getBody().getStatements();
      if (bodyStmts.size() > 0) {
        List<JsStatement> curStatements = null;
        for (int i = 0; i < bodyStmts.size(); ++i) {
          JStatement stmt = bodyStmts.get(i);
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
              // Empty JDeclarationStatement produces a null
              curStatements.add(newStmt);
            }
          }
        }
      }

      push(jsSwitch);
      return false;
    }

    private JsObjectLiteral castMapToObjectLiteral(JsArrayLiteral arrayLit, SourceInfo sourceInfo) {
      JsObjectLiteral objLit = new JsObjectLiteral(sourceInfo);
      List<JsPropertyInitializer> props = objLit.getPropertyInitializers();
      JsNumberLiteral one = new JsNumberLiteral(sourceInfo, 1);
      for (JsExpression expr : arrayLit.getExpressions()) {
        JsPropertyInitializer prop = new JsPropertyInitializer(sourceInfo, expr, one);
        props.add(prop);
      }
      return objLit;
    }

    private void checkForDupMethods(JDeclaredType x) {
      // Sanity check to see that all methods are uniquely named.
      List<JMethod> methods = x.getMethods();
      Set<String> methodSignatures = Sets.create();
      for (JMethod method : methods) {
        String sig = method.getSignature();
        if (methodSignatures.contains(sig)) {
          throw new InternalCompilerException("Signature collision in Type " + x.getName()
              + " for method " + sig);
        }
        methodSignatures = Sets.add(methodSignatures, sig);
      }
    }

    private JsExpression createAssignment(JsExpression lhs, JsExpression rhs) {
      return new JsBinaryOperation(lhs.getSourceInfo(), JsBinaryOperator.ASG, lhs, rhs);
    }

    private JsExpression createCommaExpression(JsExpression lhs, JsExpression rhs) {
      if (lhs == null) {
        return rhs;
      } else if (rhs == null) {
        return lhs;
      }
      return new JsBinaryOperation(lhs.getSourceInfo(), JsBinaryOperator.COMMA, lhs, rhs);
    }

    private JsNameRef createNativeToStringRef(JsExpression qualifier) {
      JsName toStringName = objectScope.declareName("toString");
      toStringName.setObfuscatable(false);
      JsNameRef toStringRef = toStringName.makeRef(qualifier.getSourceInfo());
      toStringRef.setQualifier(qualifier);
      return toStringRef;
    }

    private JsExpression generateCastableTypeMap(JClassType x) {
      JsCastMap castMap = program.getCastMap(x);
      if (castMap != null) {
        JField castableTypeMapField = program.getIndexedField("Object.castableTypeMap");
        JsName castableTypeMapName = names.get(castableTypeMapField);
        if (castableTypeMapName == null) {
          // Was pruned; this compilation must have no dynamic casts.
          return new JsObjectLiteral(SourceOrigin.UNKNOWN);
        }

        accept(castMap);
        return (JsExpression) pop();
      }
      return new JsObjectLiteral(SourceOrigin.UNKNOWN);
    }

    private void generateClassLiteral(JDeclarationStatement decl, JsVars vars) {
      JField field = (JField) decl.getVariableRef().getTarget();
      JsName jsName = names.get(field);
      this.accept(decl.getInitializer());
      JsExpression classObjectAlloc = pop();
      JsVar var = new JsVar(decl.getSourceInfo(), jsName);
      var.setInitExpr(classObjectAlloc);
      vars.add(var);
    }

    private void generateClassLiterals(JsVars vars) {
      /*
       * Must execute in clinit statement order, NOT field order, so that back
       * refs to super classes are preserved.
       */
      JMethodBody clinitBody =
          (JMethodBody) program.getTypeClassLiteralHolder().getMethods().get(0).getBody();
      for (JStatement stmt : clinitBody.getStatements()) {
        if (stmt instanceof JDeclarationStatement) {
          generateClassLiteral((JDeclarationStatement) stmt, vars);
        }
      }
    }

    private void generateClassSetup(JClassType x, List<JsStatement> globalStmts) {
      generateSeedFuncAndPrototype(x, globalStmts);
      generateVTables(x, globalStmts);

      if (x == program.getTypeJavaLangObject()) {
        // special: setup a "toString" alias for java.lang.Object.toString()
        generateToStringAlias(x, globalStmts);
        // special: setup the identifying typeMarker field
        generateTypeMarker(globalStmts);
      }
    }

    private void generateGwtOnLoad(List<JsFunction> entryFuncs, List<JsStatement> globalStmts) {
      /**
       * <pre>
       * var $entry = Impl.registerEntry();
       * function gwtOnLoad(errFn, modName, modBase, softPermutationId){
       *   $moduleName = modName;
       *   $moduleBase = modBase;
       *   CollapsedPropertyHolder.permutationId = softPermutationId;
       *   if (errFn) {
       *     try {
       *       $entry(init)();
       *     } catch(e) {
       *       errFn(modName);
       *     }
       *   } else {
       *     $entry(init)();
       *   }
       * }
       * </pre>
       */
      SourceInfo sourceInfo = SourceOrigin.UNKNOWN;

      JsName entryName = topScope.declareName("$entry");
      JsVar entryVar = new JsVar(sourceInfo, entryName);
      JsInvocation registerEntryCall = new JsInvocation(sourceInfo);
      JsFunction registerEntryFunction = indexedFunctions.get("Impl.registerEntry");
      registerEntryCall.setQualifier(registerEntryFunction.getName().makeRef(sourceInfo));
      entryVar.setInitExpr(registerEntryCall);
      JsVars entryVars = new JsVars(sourceInfo);
      entryVars.add(entryVar);
      globalStmts.add(entryVars);

      JsName gwtOnLoadName = topScope.declareName("gwtOnLoad");
      gwtOnLoadName.setObfuscatable(false);
      JsFunction gwtOnLoad = new JsFunction(sourceInfo, topScope, gwtOnLoadName, true);
      gwtOnLoad.setArtificiallyRescued(true);
      globalStmts.add(gwtOnLoad.makeStmt());
      JsBlock body = new JsBlock(sourceInfo);
      gwtOnLoad.setBody(body);
      JsScope fnScope = gwtOnLoad.getScope();
      List<JsParameter> params = gwtOnLoad.getParameters();
      JsName errFn = fnScope.declareName("errFn");
      JsName modName = fnScope.declareName("modName");
      JsName modBase = fnScope.declareName("modBase");
      JsName softPermutationId = fnScope.declareName("softPermutationId");
      params.add(new JsParameter(sourceInfo, errFn));
      params.add(new JsParameter(sourceInfo, modName));
      params.add(new JsParameter(sourceInfo, modBase));
      params.add(new JsParameter(sourceInfo, softPermutationId));
      JsExpression asg =
          createAssignment(topScope.findExistingUnobfuscatableName("$moduleName").makeRef(
              sourceInfo), modName.makeRef(sourceInfo));
      body.getStatements().add(asg.makeStmt());
      asg =
          createAssignment(topScope.findExistingUnobfuscatableName("$moduleBase").makeRef(
              sourceInfo), modBase.makeRef(sourceInfo));
      body.getStatements().add(asg.makeStmt());

      // Assignment to CollapsedPropertyHolder.permutationId only if it's used
      JsName permutationIdFieldName =
          names.get(program.getIndexedField("CollapsedPropertyHolder.permutationId"));
      if (permutationIdFieldName != null) {
        asg =
            createAssignment(permutationIdFieldName.makeRef(sourceInfo), softPermutationId
                .makeRef(sourceInfo));
        body.getStatements().add(asg.makeStmt());
      }

      JsIf jsIf = new JsIf(sourceInfo);
      body.getStatements().add(jsIf);
      jsIf.setIfExpr(errFn.makeRef(sourceInfo));
      JsTry jsTry = new JsTry(sourceInfo);
      jsIf.setThenStmt(jsTry);
      JsBlock callBlock = new JsBlock(sourceInfo);
      jsIf.setElseStmt(callBlock);
      jsTry.setTryBlock(callBlock);
      for (JsFunction func : entryFuncs) {
        if (func == registerEntryFunction) {
          continue;
        } else if (func != null) {
          JsInvocation call = new JsInvocation(sourceInfo);
          call.setQualifier(entryName.makeRef(sourceInfo));
          call.getArguments().add(func.getName().makeRef(sourceInfo));
          JsInvocation entryCall = new JsInvocation(sourceInfo);
          entryCall.setQualifier(call);
          callBlock.getStatements().add(entryCall.makeStmt());
        }
      }
      JsCatch jsCatch = new JsCatch(sourceInfo, fnScope, "e");
      jsTry.getCatches().add(jsCatch);
      JsBlock catchBlock = new JsBlock(sourceInfo);
      jsCatch.setBody(catchBlock);
      JsInvocation errCall = new JsInvocation(sourceInfo);
      catchBlock.getStatements().add(errCall.makeStmt());
      errCall.setQualifier(errFn.makeRef(sourceInfo));
      errCall.getArguments().add(modName.makeRef(sourceInfo));
    }

    private void generateImmortalTypes(JsVars globals) {
      List<JsStatement> globalStmts = jsProgram.getGlobalBlock().getStatements();
      List<JClassType> immortalTypes = new ArrayList<JClassType>(
          program.immortalCodeGenTypes);
      // visit in reverse order since insertions start at head
      Collections.reverse(immortalTypes);
      JMethod createObjMethod = program.getIndexedMethod("JavaScriptObject.createObject");
      JMethod createArrMethod = program.getIndexedMethod("JavaScriptObject.createArray");

      for (JClassType x : immortalTypes) {
        // should not be pruned
        assert x.getMethods().size() > 0;
        // insert all static methods
        for (JMethod method : x.getMethods()) {
          /*
           * Skip virtual methods and constructors. Even in cases where there is no constructor
           * defined, the compiler will synthesize a default constructor which invokes
           * a synthensized $init() method. We must skip both of these inserted methods.
           */
          if (method.needsVtable() || method instanceof JConstructor) {
            continue;
          }
          if (JProgram.isClinit(method)) {
            /**
             * Emit empty clinits that will be pruned. If a type B extends A, then even if
             * B and A have no fields to initialize, there will be a call inserted in B's clinit
             * to invoke A's clinit. Likewise, if you have a static field initialized to
             * JavaScriptObject.createObject(), the clinit() will include this initializer code,
             * which we don't want.
             */
            JsFunction func = new JsFunction(x.getSourceInfo(), topScope,
                topScope.declareName(mangleNameForGlobal(method)), true);
            func.setBody(new JsBlock(method.getBody().getSourceInfo()));
            push(func);
          } else {
            accept(method);
          }
          // add after var declaration, but before everything else
          JsFunction func = (JsFunction) pop();
          assert func.getName() != null;
          globalStmts.add(1, func.makeStmt());
        }

        // insert fields into global var declaration
        for (JField field : x.getFields()) {
          assert field.isStatic() : "All fields on immortal types must be static.";
          accept(field);
          JsNode node = pop();
          assert node instanceof JsVar;
          JsVar fieldVar = (JsVar) node;
          JExpression init = field.getInitializer();
          if (init != null
              && field.getLiteralInitializer() == null) {
            // no literal, but it could be a JavaScriptObject
            if (init.getType() == program.getJavaScriptObject()) {
              assert init instanceof JMethodCall;
              JMethod meth = ((JMethodCall) init).getTarget();
              // immortal types can only have non-primitive literal initializers of createArray,createObject
              if (meth == createObjMethod) {
                fieldVar.setInitExpr(new JsObjectLiteral(init.getSourceInfo()));
              } else if (meth == createArrMethod) {
                fieldVar.setInitExpr(new JsArrayLiteral(init.getSourceInfo()));
              } else {
                assert false : "Illegal initializer expression for immortal field " + field;
              }
            }
          }
          globals.add(fieldVar);
        }
      }
    }

    private void generateInternedCastMapLiterals(JsVars vars) {
      SourceInfo info = vars.getSourceInfo();
      int id = 0;
      for (Map.Entry<String, JsName> castMapEntry : namesByCastMap.entrySet()) {
        JsVar var = new JsVar(info, castMapEntry.getValue());
        var.setInitExpr(castMapByString.get(castMapEntry.getKey()));
        vars.add(var);
      }
    }

    private void generateLongLiterals(JsVars vars) {
      for (Entry<Long, JsName> entry : longLits.entrySet()) {
        JsName jsName = entry.getValue();
        JsExpression longObjectAlloc = longObjects.get(jsName);
        JsVar var = new JsVar(vars.getSourceInfo(), jsName);
        var.setInitExpr(longObjectAlloc);
        vars.add(var);
      }
    }

    private void generateQueryIdConstants(JsVars vars) {
      if (namesByQueryId != null) {
        SourceInfo info = vars.getSourceInfo();
        int id = 0;
        for (JsName jsName : namesByQueryId) {
          JsVar var = new JsVar(info, jsName);
          var.setInitExpr(new JsNumberLiteral(info, id++));
          vars.add(var);
        }
      }
    }



    private void generateSeedFuncAndPrototype(JClassType x, List<JsStatement> globalStmts) {
      SourceInfo sourceInfo = x.getSourceInfo();
      if (x != program.getTypeJavaLangString()) {
        JsInvocation defineSeed = new JsInvocation(x.getSourceInfo());
        JsName seedNameRef = indexedFunctions.get(
            "SeedUtil.defineSeed").getName();
        defineSeed.setQualifier(seedNameRef.makeRef(x.getSourceInfo()));
        int newSeed = getSeedId(x);
        assert newSeed > 0;
        JClassType superClass = x.getSuperClass();
        int superSeed = (superClass == null) ? -1 : getSeedId(x.getSuperClass());
        // SeedUtil.defineSeed(queryId, superId, castableMap, constructors)
        defineSeed.getArguments().add(new JsNumberLiteral(x.getSourceInfo(),
            newSeed));
        defineSeed.getArguments().add(new JsNumberLiteral(x.getSourceInfo(),
            superSeed));
        JsExpression castMap = generateCastableTypeMap(x);
        defineSeed.getArguments().add(castMap);
       
        // Chain assign the same prototype to every live constructor.
        for (JMethod method : x.getMethods()) {
          if (liveCtors.contains(method)) {
            defineSeed.getArguments().add(names.get(method).makeRef(
                sourceInfo));
          }
        }

        JsStatement tmpAsgStmt = defineSeed.makeStmt();
        globalStmts.add(tmpAsgStmt);
        typeForStatMap.put(tmpAsgStmt, x);
      } else {
        /*
         * MAGIC: java.lang.String is implemented as a JavaScript String
         * primitive with a modified prototype.
         */
        JsNameRef rhs = prototype.makeRef(sourceInfo);
        rhs.setQualifier(JsRootScope.INSTANCE.findExistingUnobfuscatableName("String").makeRef(
            sourceInfo));
        JsExpression tmpAsg = createAssignment(globalTemp.makeRef(sourceInfo), rhs);
        JsExprStmt tmpAsgStmt = tmpAsg.makeStmt();
        globalStmts.add(tmpAsgStmt);
        typeForStatMap.put(tmpAsgStmt, x);
        JField castableTypeMapField = program.getIndexedField("Object.castableTypeMap");
        JsName castableTypeMapName = names.get(castableTypeMapField);
        JsNameRef ctmRef = castableTypeMapName.makeRef(sourceInfo);
        ctmRef.setQualifier(globalTemp.makeRef(sourceInfo));
        JsExpression castMapLit = generateCastableTypeMap(x);
        JsExpression ctmAsg = createAssignment(ctmRef,
            castMapLit);
        JsExprStmt ctmAsgStmt = ctmAsg.makeStmt();
        globalStmts.add(ctmAsgStmt);
        typeForStatMap.put(ctmAsgStmt, x);
      }
    }

    private void generateToStringAlias(JClassType x, List<JsStatement> globalStmts) {
      JMethod toStringMeth = program.getIndexedMethod("Object.toString");
      if (x.getMethods().contains(toStringMeth)) {
        SourceInfo sourceInfo = x.getSourceInfo();
        // _.toString = function(){return this.java_lang_Object_toString();}

        // lhs
        JsNameRef lhs = createNativeToStringRef(globalTemp.makeRef(sourceInfo));

        // rhs
        JsInvocation call = new JsInvocation(sourceInfo);
        JsNameRef toStringRef = new JsNameRef(sourceInfo, polymorphicNames.get(toStringMeth));
        toStringRef.setQualifier(new JsThisRef(sourceInfo));
        call.setQualifier(toStringRef);
        JsReturn jsReturn = new JsReturn(sourceInfo, call);
        JsFunction rhs = new JsFunction(sourceInfo, topScope);
        JsBlock body = new JsBlock(sourceInfo);
        body.getStatements().add(jsReturn);
        rhs.setBody(body);

        // asg
        JsExpression asg = createAssignment(lhs, rhs);
        JsExprStmt stmt = asg.makeStmt();
        globalStmts.add(stmt);
        typeForStatMap.put(stmt, program.getTypeJavaLangObject());
      }
    }

    private void generateTypeMarker(List<JsStatement> globalStmts) {
      JField typeMarkerField = program.getIndexedField("Object.typeMarker");
      JsName typeMarkerName = names.get(typeMarkerField);
      if (typeMarkerName == null) {
        // Was pruned; this compilation must have no JSO instanceof tests.
        return;
      }
      SourceInfo sourceInfo = jsProgram.createSourceInfoSynthetic(GenerateJavaScriptAST.class);
      JsNameRef fieldRef = typeMarkerName.makeRef(sourceInfo);
      fieldRef.setQualifier(globalTemp.makeRef(sourceInfo));
      JsExpression asg = createAssignment(fieldRef, nullFunc.getName().makeRef(sourceInfo));
      JsExprStmt stmt = asg.makeStmt();
      globalStmts.add(stmt);
      typeForStatMap.put(stmt, program.getTypeJavaLangObject());
    }

    private void generateVTables(JClassType x, List<JsStatement> globalStmts) {
      boolean isString = (x == program.getTypeJavaLangString());
      for (JMethod method : x.getMethods()) {
        SourceInfo sourceInfo = method.getSourceInfo();
        if (method.needsVtable() && !method.isAbstract()) {
          JsNameRef lhs = polymorphicNames.get(method).makeRef(sourceInfo);
          lhs.setQualifier(globalTemp.makeRef(sourceInfo));

          JsExpression rhs;
          if (isString && "toString".equals(method.getName()))  {
            // special-case String.toString: alias to the native JS toString()
            rhs = createNativeToStringRef(globalTemp.makeRef(sourceInfo));
          } else {
            /*
             * Inline JsFunction rather than reference, e.g. _.vtableName =
             * function functionName() { ... }
             */
            rhs = methodBodyMap.get(method.getBody());
          }
          JsExpression asg = createAssignment(lhs, rhs);
          JsExprStmt asgStat = new JsExprStmt(x.getSourceInfo(), asg);
          globalStmts.add(asgStat);
          vtableInitForMethodMap.put(asgStat, method);
        }
      }
    }

    private void handleClinit(JsFunction clinitFunc, JsFunction superClinit) {
      clinitFunc.setExecuteOnce(true);
      clinitFunc.setImpliedExecute(superClinit);
      List<JsStatement> statements = clinitFunc.getBody().getStatements();
      SourceInfo sourceInfo = clinitFunc.getSourceInfo();
      // self-assign to the null method immediately (to prevent reentrancy)
      JsExpression asg =
          createAssignment(clinitFunc.getName().makeRef(sourceInfo), nullFunc.getName().makeRef(
              sourceInfo));
      statements.add(0, asg.makeStmt());
    }

    private void internCastMap(JsCastMap x) {
      String stringMap = castMapToString(x);
      if (castMapSeen.contains(stringMap)) {
        internedCastMap.add(stringMap);
      } else {
        castMapSeen.add(stringMap);
      }
    }

    private JsInvocation maybeCreateClinitCall(JField x) {
      if (!x.isStatic()) {
        return null;
      }

      JDeclaredType targetType = x.getEnclosingType().getClinitTarget();
      if (!currentMethod.getEnclosingType().checkClinitTo(targetType)) {
        return null;
      } else if (targetType.equals(program.getTypeClassLiteralHolder())) {
        return null;
      }

      JMethod clinitMethod = targetType.getMethods().get(0);
      SourceInfo sourceInfo = x.getSourceInfo();
      JsInvocation jsInvocation = new JsInvocation(sourceInfo);
      jsInvocation.setQualifier(names.get(clinitMethod).makeRef(sourceInfo));
      return jsInvocation;
    }

    private JsInvocation maybeCreateClinitCall(JMethod x) {
      if (!crossClassTargets.contains(x)) {
        return null;
      }
      if (x.canBePolymorphic() || program.isStaticImpl(x)) {
        return null;
      }
      JDeclaredType enclosingType = x.getEnclosingType();
      if (enclosingType == null || !enclosingType.hasClinit()) {
        return null;
      }
      // avoid recursion sickness
      if (JProgram.isClinit(x)) {
        return null;
      }

      JMethod clinitMethod = enclosingType.getClinitTarget().getMethods().get(0);
      SourceInfo sourceInfo = x.getSourceInfo();
      JsInvocation jsInvocation = new JsInvocation(sourceInfo);
      jsInvocation.setQualifier(names.get(clinitMethod).makeRef(sourceInfo));
      return jsInvocation;
    }
  }

  private static class JavaToJsOperatorMap {
    private static final Map<JBinaryOperator, JsBinaryOperator> bOpMap =
        new EnumMap<JBinaryOperator, JsBinaryOperator>(JBinaryOperator.class);
    private static final Map<JUnaryOperator, JsUnaryOperator> uOpMap =
        new EnumMap<JUnaryOperator, JsUnaryOperator>(JUnaryOperator.class);

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

  private class RecordCrossClassCalls extends JVisitor {

    private JMethod currentMethod;

    @Override
    public void endVisit(JMethod x, Context ctx) {
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

    private final HasNameSort hasNameSort = new HasNameSort();

    private final Comparator<JMethod> methodSort = new Comparator<JMethod>() {
      @Override
      public int compare(JMethod m1, JMethod m2) {
        return m1.getSignature().compareTo(m2.getSignature());
      }
    };

    @Override
    public void endVisit(JClassType x, Context ctx) {
      x.sortFields(hasNameSort);
      x.sortMethods(methodSort);
    }

    @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      x.sortFields(hasNameSort);
      x.sortMethods(methodSort);
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      x.sortLocals(hasNameSort);
    }

    @Override
    public void endVisit(JProgram x, Context ctx) {
      Collections.sort(x.getEntryMethods(), methodSort);
      Collections.sort(x.getDeclaredTypes(), hasNameSort);
    }
  }

  public static JavaToJavaScriptMap exec(JProgram program, JsProgram jsProgram,
      JsOutputOption output, Map<StandardSymbolData, JsName> symbolTable,
      PropertyOracle[] propertyOracles) {
    GenerateJavaScriptAST generateJavaScriptAST =
        new GenerateJavaScriptAST(program, jsProgram, output, symbolTable, propertyOracles);
    return generateJavaScriptAST.execImpl();
  }

  private Map<String, JsExpression> castMapByString = new HashMap<String, JsExpression>();

  private final Map<JBlock, JsCatch> catchMap = new IdentityHashMap<JBlock, JsCatch>();

  private final Map<JClassType, JsScope> classScopes = new IdentityHashMap<JClassType, JsScope>();

  /**
   * A list of methods that are called from another class (ie might need to
   * clinit).
   */
  private final Set<JMethod> crossClassTargets = new HashSet<JMethod>();

  private Map<String, JsFunction> indexedFunctions = Maps.create();

  private Map<String, JsName> indexedFields = Maps.create();

  /**
   * Contains JsNames for all interface methods. A special scope is needed so
   * that independent classes will obfuscate their interface implementation
   * methods the same way.
   */
  private final JsScope interfaceScope;

  private final Set<String> internedCastMap = new HashSet<String>();

  private final JsProgram jsProgram;

  private final Set<JConstructor> liveCtors = new IdentityHashSet<JConstructor>();

  /**
   * Sorted to avoid nondeterministic iteration.
   */
  private final Map<Long, JsName> longLits = new TreeMap<Long, JsName>();

  private final Map<JsName, JsExpression> longObjects = new IdentityHashMap<JsName, JsExpression>();
  private JsFunction makeMapFunction;
  private final Map<JAbstractMethodBody, JsFunction> methodBodyMap =
      new IdentityHashMap<JAbstractMethodBody, JsFunction>();
  private final Map<HasName, JsName> names = new IdentityHashMap<HasName, JsName>();
  private int nextSeedId = 1;
  private List<JsName> namesByQueryId;
  private Map<String, JsName> namesByCastMap = new HashMap<String, JsName>();
  private JsFunction nullFunc;

  /**
   * Contains JsNames for the Object instance methods, such as equals, hashCode,
   * and toString. All other class scopes have this scope as an ultimate parent.
   */
  private final JsScope objectScope;
  private final JsOutputOption output;
  private final Set<JsFunction> polymorphicJsFunctions = new IdentityHashSet<JsFunction>();
  private final Map<JMethod, JsName> polymorphicNames = new IdentityHashMap<JMethod, JsName>();
  private final JProgram program;

  /**
   * Map of class type to allocated seed id.
   */
  private final Map<JReferenceType, Integer> seedIdMap = new HashMap<JReferenceType, Integer>();

  /**
   * All of the fields in String and Array need special handling for interop.
   */
  private final Map<JField, String> specialObfuscatedFields = new HashMap<JField, String>();

  /**
   * All of the methods in String and Array need special handling for interop.
   */
  private final Map<String, String> specialObfuscatedMethodSigs = new HashMap<String, String>();

  /**
   * If true, polymorphic functions are made anonymous vtable declarations and
   * not assigned topScope identifiers.
   */
  private final boolean stripStack;

  /**
   * Maps JsNames to machine-usable identifiers.
   */
  private final Map<StandardSymbolData, JsName> symbolTable;

  /**
   * Contains JsNames for all globals, such as static fields and methods.
   */
  private final JsScope topScope;

  private final Map<JsStatement, JClassType> typeForStatMap =
      new HashMap<JsStatement, JClassType>();

  private final JTypeOracle typeOracle;

  private final Map<JsStatement, JMethod> vtableInitForMethodMap =
      new HashMap<JsStatement, JMethod>();

  private GenerateJavaScriptAST(JProgram program, JsProgram jsProgram, JsOutputOption output,
      Map<StandardSymbolData, JsName> symbolTable, PropertyOracle[] propertyOracles) {
    this.program = program;
    typeOracle = program.typeOracle;
    this.jsProgram = jsProgram;
    topScope = jsProgram.getScope();
    objectScope = jsProgram.getObjectScope();
    interfaceScope = new JsNormalScope(objectScope, "Interfaces");
    this.output = output;
    this.symbolTable = symbolTable;

    this.stripStack =
        JsStackEmulator.getStackMode(propertyOracles) == JsStackEmulator.StackMode.STRIP;

    /*
     * Because we modify the JavaScript String prototype, all fields and
     * polymorphic methods on String and super types need special handling.
     */

    // Object polymorphic
    Map<String, String> namesToIdents = new HashMap<String, String>();
    namesToIdents.put("getClass", "gC");
    namesToIdents.put("hashCode", "hC");
    namesToIdents.put("equals", "eQ");
    namesToIdents.put("toString", "tS");
    namesToIdents.put("finalize", "fZ");
    // String polymorphic
    namesToIdents.put("charAt", "cA");
    namesToIdents.put("compareTo", "cT");
    namesToIdents.put("length", "lN");
    namesToIdents.put("subSequence", "sS");

    List<JMethod> methods = new ArrayList<JMethod>(program.getTypeJavaLangObject().getMethods());
    methods.addAll(program.getIndexedType("Comparable").getMethods());
    methods.addAll(program.getIndexedType("CharSequence").getMethods());
    for (JMethod method : methods) {
      if (method.canBePolymorphic()) {
        String ident = namesToIdents.get(method.getName());
        assert ident != null;
        specialObfuscatedMethodSigs.put(method.getSignature(), ident);
      }
    }

    namesToIdents.clear();
    // Object fields
    namesToIdents.put("expando", "eX");
    namesToIdents.put("typeMarker", "tM");
    namesToIdents.put("castableTypeMap", "cM");
    namesToIdents.put("___clazz", "cZ");
    // Array magic field
    namesToIdents.put("queryId", "qI");

    List<JField> fields = new ArrayList<JField>(program.getTypeJavaLangObject().getFields());
    fields.addAll(program.getIndexedType("Array").getFields());
    for (JField field : fields) {
      if (!field.isStatic()) {
        String ident = namesToIdents.get(field.getName());
        assert ident != null;
        specialObfuscatedFields.put(field, ident);
      }
    }

    // force java.lang.Object,java.lang.String
    // to have seed ids 1,2
    getSeedId(program.getTypeJavaLangObject());
    getSeedId(program.getTypeJavaLangString());
  }

  String castMapToString(JsCastMap x) {
    if (x == null || x.getExprs() == null || x.getExprs().size() == 0) {
      return "{}";
    } else {
      TextOutput textOutput = new DefaultTextOutput(true);
      ToStringGenerationVisitor toStringer = new ToStringGenerationVisitor(textOutput);
      toStringer.accept(x);
      String stringMap = textOutput.toString();
      return stringMap;
    }
  }

  String getNameString(HasName hasName) {
    String s = hasName.getName().replaceAll("_", "_1").replace('.', '_');
    return s;
  }

  /**
   * Looks up or assigns a seed id for a type..
   */
  int getSeedId(JReferenceType type) {
    Integer val = seedIdMap.get(type);
    int seedId = val == null ? 0 : val;

    if (seedId == 0) {
      seedId = nextSeedId++;
      seedIdMap.put(type, seedId);
    }
    return seedId;
  }

  String mangleName(JField x) {
    String s = getNameString(x.getEnclosingType()) + '_' + getNameString(x);
    return s;
  }

  String mangleNameForGlobal(JMethod x) {
    String s = getNameString(x.getEnclosingType()) + '_' + getNameString(x) + "__";
    for (int i = 0; i < x.getOriginalParamTypes().size(); ++i) {
      JType type = x.getOriginalParamTypes().get(i);
      s += type.getJavahSignatureName();
    }
    s += x.getOriginalReturnType().getJavahSignatureName();
    return s;
  }

  String mangleNameForPoly(JMethod x) {
    assert !x.isPrivate() && !x.isStatic();
    StringBuffer sb = new StringBuffer();
    sb.append(getNameString(x));
    sb.append("__");
    for (int i = 0; i < x.getOriginalParamTypes().size(); ++i) {
      JType type = x.getOriginalParamTypes().get(i);
      sb.append(type.getJavahSignatureName());
    }
    sb.append(x.getOriginalReturnType().getJavahSignatureName());
    return sb.toString();
  }

  String mangleNameForPrivatePoly(JMethod x) {
    assert x.isPrivate() && !x.isStatic();
    StringBuffer sb = new StringBuffer();
    /*
     * Private instance methods in different classes should not override each
     * other, so they must have distinct polymorphic names. Therefore, add the
     * class name to the mangled name.
     */
    sb.append("private$");
    sb.append(getNameString(x.getEnclosingType()));
    sb.append("$");
    sb.append(getNameString(x));
    sb.append("__");
    for (int i = 0; i < x.getOriginalParamTypes().size(); ++i) {
      JType type = x.getOriginalParamTypes().get(i);
      sb.append(type.getJavahSignatureName());
    }
    sb.append(x.getOriginalReturnType().getJavahSignatureName());
    return sb.toString();
  }

  String mangleNameSpecialObfuscate(JField x) {
    assert (specialObfuscatedFields.containsKey(x));
    switch (output) {
      case OBFUSCATED:
        return specialObfuscatedFields.get(x);
      case PRETTY:
        return x.getName() + "$";
      case DETAILED:
        return mangleName(x) + "$";
    }
    throw new InternalCompilerException("Unknown output mode");
  }

  String mangleNameSpecialObfuscate(JMethod x) {
    assert (specialObfuscatedMethodSigs.containsKey(x.getSignature()));
    switch (output) {
      case OBFUSCATED:
        return specialObfuscatedMethodSigs.get(x.getSignature());
      case PRETTY:
        return x.getName() + "$";
      case DETAILED:
        return mangleNameForPoly(x) + "$";
    }
    throw new InternalCompilerException("Unknown output mode");
  }

  private JavaToJavaScriptMap execImpl() {
    SortVisitor sorter = new SortVisitor();
    sorter.accept(program);
    RecordCrossClassCalls recorder = new RecordCrossClassCalls();
    recorder.accept(program);
    CreateNamesAndScopesVisitor creator = new CreateNamesAndScopesVisitor();
    creator.accept(program);
    GenerateJavaScriptVisitor generator = new GenerateJavaScriptVisitor();
    generator.accept(program);
    final Map<JsName, JMethod> nameToMethodMap = new HashMap<JsName, JMethod>();
    final HashMap<JsName, JField> nameToFieldMap = new HashMap<JsName, JField>();
    final HashMap<JsName, JClassType> constructorNameToTypeMap = new HashMap<JsName, JClassType>();
    for (JDeclaredType type : program.getDeclaredTypes()) {
      JsName typeName = names.get(type);
      if (type instanceof JClassType && typeName != null) {
        constructorNameToTypeMap.put(typeName, (JClassType) type);
      }
      for (JField field : type.getFields()) {
        if (field.isStatic()) {
          JsName fieldName = names.get(field);
          if (fieldName != null) {
            nameToFieldMap.put(fieldName, field);
          }
        }
      }
      for (JMethod method : type.getMethods()) {
        JsName methodName = names.get(method);
        if (methodName != null) {
          nameToMethodMap.put(methodName, method);
        }
      }
    }

    jsProgram.setIndexedFields(indexedFields);
    jsProgram.setIndexedFunctions(indexedFunctions);

    // TODO(spoon): Instead of gathering the information here, get it via
    // SourceInfo
    return new JavaToJavaScriptMap() {
      public JsName nameForMethod(JMethod method) {
        return names.get(method);
      }

      public JsName nameForType(JClassType type) {
        return names.get(type);
      }

      public JField nameToField(JsName name) {
        return nameToFieldMap.get(name);
      }

      public JMethod nameToMethod(JsName name) {
        return nameToMethodMap.get(name);
      }

      public JClassType nameToType(JsName name) {
        return constructorNameToTypeMap.get(name);
      }

      public JClassType typeForStatement(JsStatement stat) {
        return typeForStatMap.get(stat);
      }

      public JMethod vtableInitToMethod(JsStatement stat) {
        return vtableInitForMethodMap.get(stat);
      }
    };
  }
}
