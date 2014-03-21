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

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.linker.CastableTypeMap;
import com.google.gwt.core.ext.linker.impl.StandardCastableTypeMap;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.dev.CompilerContext;
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
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLabel;
import com.google.gwt.dev.jjs.ast.JLabeledStatement;
import com.google.gwt.dev.jjs.ast.JLiteral;
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
import com.google.gwt.dev.jjs.ast.JNumericEntry;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReboundEntryPoint;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
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
import com.google.gwt.dev.jjs.ast.js.JDebuggerStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;
import com.google.gwt.dev.js.JsInliner;
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
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
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
import com.google.gwt.dev.js.ast.JsUnaryOperation;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.Name.SourceName;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

/**
 * Creates a JavaScript AST from a <code>JProgram</code> node.
 */
public class GenerateJavaScriptAST {

  /**
   * The GWT Java AST might contain different local variables with the same name in the same
   * scope. This fixup pass renames variables in the case they clash in a scope.
   */
  private static class FixNameClashesVisitor extends JVisitor {

    /**
     * Represents the scope tree defined by nested statement blocks. It is a temporary
     * structure to track local variable lifetimes.
     */
    private static class Scope {
      private Scope parent;

      // Keeps track what names are used in children.
      private Set<String> usedInChildScope = Sets.newHashSet();

      // Keeps track what names have this scope as its lifetime.
      private Set<String> namesInThisScope = Sets.newHashSet();

      /**
       * The depth at which this scope is in the tree.
       */
      private int level;

      private Scope() {
        this.parent = null;
        this.level = 0;
      }

      private Scope(Scope parent) {
        this.parent = parent;
        this.level = parent.level + 1;
      }

      private static Scope getInnermostEnclosingScope(Scope thisScope, Scope thatScope) {
        if (thisScope == null) {
          return thatScope;
        }

        if (thatScope == null) {
          return thisScope;
        }

        if (thisScope == thatScope) {
          return thisScope;
        }

        if (thisScope.level > thatScope.level) {
          return getInnermostEnclosingScope(thatScope, thisScope);
        }

        if (thisScope.level == thatScope.level) {
          return getInnermostEnclosingScope(thisScope.parent, thatScope.parent);
        }
        return getInnermostEnclosingScope(thisScope, thatScope.parent);
      }

      private void addChildUsage(String name) {
        usedInChildScope.add(name);
        if (parent != null) {
          parent.addChildUsage(name);
       }
      }

      protected void addUsedName(String name) {
        namesInThisScope.add(name);
        if (parent != null) {
          parent.addChildUsage(name);
        }
      }

      private boolean isUsedInParent(String name) {
        return namesInThisScope.contains(name) ||
            (parent != null && parent.isUsedInParent(name));
      }

      protected boolean isConflictingName(String name) {
        return usedInChildScope.contains(name) || isUsedInParent(name);
      }
    }

    private Scope currentScope;
    private Map<JLocal, Scope> scopesByLocal;
    private Multimap<String, JLocal> localsByName;

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      // Start constructing the scope tree.
      currentScope = new Scope();
      scopesByLocal = Maps.newHashMap();
      localsByName = LinkedHashMultimap.create();
      return true;
    }

    @Override
    public boolean visit(JBlock x, Context ctx) {
      currentScope = new Scope(currentScope);
      return true;
    }

    @Override
    public void endVisit(JBlock x, Context ctx) {
      currentScope = currentScope.parent;
    }

    @Override
    public void endVisit(JLocalRef x, Context ctx) {
      // We use the a block scope as a proxy for a lifetime which is safe to do albeit non optimal.
      //
      // Keep track of the scope that encloses a variable lifetime. E.g. assume the following code.
      // { // scope 1
      //   { // scope 1.1
      //     ... a... b...
      //     { // scope 1.1.1
      //        ... a ...
      //     }
      //   }
      //   { // scope 1.2
      //    ... b...
      //   }
      // }
      // Scope 1.1 is the innermost scope that encloses the lifetime of variable a and
      // scope 1 is the innermost scope that encloses the lifetime of variable b.
      JLocal local = x.getLocal();
      Scope oldVariableScope = scopesByLocal.get(local);
      Scope newVariableScope =  Scope.getInnermostEnclosingScope(oldVariableScope, currentScope);
      newVariableScope.addUsedName(local.getName());
      if (newVariableScope != oldVariableScope) {
        scopesByLocal.put(local, newVariableScope);
      }
      localsByName.put(local.getName(), local);
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      // Fix clashing variables here.  Two locals are clashing if they have the same name and their
      // computed lifetimes are intersecting. By using the scope to model lifetimes two variables
      // clash if their computed scopes are nested.
      for (String name : localsByName.keySet()) {
        Collection<JLocal> localSet = localsByName.get(name);
        if (localSet.size() == 1) {
          continue;
        }

        JLocal[] locals = localSet.toArray(new JLocal[localSet.size()]);
        // TODO(rluble): remove n^2 behaviour in conflict checking.
        // In practice each method has only a handful of locals so this process is not expected
        // to be a performance problem.
        for (int i = 0; i < locals.length; i++ ) {
          // See if local i conflicts with any local j > i
          for (int j = i + 1; j < locals.length; j++ ) {
            Scope iLocalScope = scopesByLocal.get(locals[i]);
            Scope jLocalScope = scopesByLocal.get(locals[j]);
            Scope commonAncestor = Scope.getInnermostEnclosingScope(iLocalScope, jLocalScope);
            if (commonAncestor != iLocalScope && commonAncestor != jLocalScope) {
              // no conflict
              continue;
            }
            // conflicting locals => find a unique name rename local i to it;
            int n = 0;
            String baseName = locals[i].getName();
            String newName;
            do {
              // The active namer will clean up these potentially long names.
              newName = baseName + n++;
            } while (iLocalScope.isConflictingName(newName));
            locals[i].setName(newName);
            iLocalScope.addUsedName(newName);
            // There is no need to update the localsByNameMap as newNames are always guaranteed to
            // be clash free.
            break;
          }
        }
      }

      // Only valid for the duration of one method body visit/endVisit pair.
      currentScope = null;
      scopesByLocal = null;
      localsByName = null;
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
      pop();
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
       * put nullMethod in the global scope, too; it's the replacer for clinits.
       */
      if (!program.isReferenceOnly(program.getIndexedType("Cast"))) {
        // This is the module that contains the intrinsics, hence define the null function.
        // TODO(rluble): this should be cleaner. Ideally there is a bootstrap library that
        // is minimal and has all compiler support code.
        nullFunctionJsName = createNullFunction(nullMethod.getName());
      } else {
        nullFunctionJsName = topScope.declareName(nullMethod.getName());
      }
      names.put(nullMethod, nullFunctionJsName);

      /*
       * Create names for instantiable array types since JProgram.traverse()
       * doesn't iterate over them.
       */
      for (JArrayType arrayType : program.getAllArrayTypes()) {
        if (typeOracle.isInstantiatedType(arrayType)) {
          accept(arrayType);
        }
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
        indexedFunctions.put(x.getEnclosingType().getShortName() + "." + x.getName(), jsFunction);
      }

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
        JsCatch jsCatch = new JsCatch(x.getSourceInfo(), peek(), arg.getTarget().getName());
        JsParameter jsParam = jsCatch.getParameter();
        names.put(arg.getTarget(), jsParam.getName());
        catchMap.put(catchBlock, jsCatch);
        catchParamIdentifiers.add(jsParam.getName());

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

    private JsName createNullFunction(String name) {
      return createGlobalJsFunctionFromSource(name, "function " + name + "(){}");
    }

    private JsName createGlobalJsFunctionFromSource(String functionName, String code) {
      try {
        List<JsStatement> stmts =
            JsParser.parse(SourceOrigin.UNKNOWN, topScope, new StringReader(code));
        assert stmts.size() == 1;
        JsExprStmt stmt = (JsExprStmt) stmts.get(0);
        JsFunction globalFunction =  (JsFunction) stmt.getExpression();

        assert functionName == globalFunction.getName().getIdent();

        List< JsStatement > globalStmts = jsProgram.getGlobalBlock().getStatements();
        globalStmts.add(0, stmt);
        return globalFunction.getName();
      }  catch (Exception e) {
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
      JCastMap castMap = program.getCastMap(x);
      if (castMap != null) {
        boolean isFirst = true;
        for (JExpression castToType : castMap.getCanCastToTypes()) {
          if (isFirst) {
            isFirst = false;
          } else {
            sb.append(',');
          }
          sb.append(castToType.toSource());
          sb.append(":1");
        }
      }
      sb.append('}');
      CastableTypeMap castableTypeMap = new StandardCastableTypeMap(sb.toString());


      String typeId = getRuntimeTypeReference(x) != null ?
         getRuntimeTypeReference(x).toSource() : null;
      StandardSymbolData symbolData =
          StandardSymbolData.forClass(x.getName(), x.getSourceInfo().getFileName(), x
              .getSourceInfo().getStartLine(), castableTypeMap, typeId);
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

      String methodSig = null;
      if (x instanceof JMethod) {
        JMethod method = ((JMethod) x);
        methodSig = StringInterner.get().intern(
            method.getSignature().substring(method.getName().length()));
      }

      StandardSymbolData symbolData =
          StandardSymbolData.forMember(x.getEnclosingType().getName(), x.getName(), methodSig,
              makeUriString(x), x.getSourceInfo().getStartLine());
      assert !symbolTable.containsKey(symbolData) : "Duplicate symbol " + "recorded "
          + jsName.getIdent() + " for " + x.getName() + " and key " + symbolData.getJsniIdent();
      symbolTable.put(symbolData, jsName);
    }
  }

  private class GenerateJavaScriptVisitor extends GenerateJavaScriptLiterals {

    private final Set<JClassType> alreadyRan = Sets.newHashSet();

    private final JsName arrayLength = objectScope.declareName("length");

    private final Map<JClassType, JsFunction> clinitMap = Maps.newHashMap();

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

    // JavaScript functions that arise from methods that were not inlined in the Java AST
    // NOTE: We use a LinkedHashSet to preserve the order of insertion. So that the following passes
    // that use this result are deterministic.
    private final Set<JsNode> functionsForJsInlining = Sets.newLinkedHashSet();

    {
      globalTemp.setObfuscatable(false);
      prototype.setObfuscatable(false);
      arrayLength.setObfuscatable(false);
    }

    @Override
    public void endVisit(JAbsentArrayDimension x, Context ctx) {
      throw new InternalCompilerException("Should not get here.");
    }

    @Override
    public void endVisit(JArrayLength x, Context ctx) {
      assert x.getInstance() != null : "Can't access the length of a null array";
      JsExpression qualifier = pop();
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
      JsExpression rhs = pop(); // rhs
      JsExpression lhs = pop(); // lhs
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
      // Don't generate JS for types not in current module if separate compilation is on.
      if (program.isReferenceOnly(x)) {
        return;
      }

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

      if (typeOracle.isInstantiatedType(x) && !program.isJavaScriptObject(x) &&
          x !=  program.getTypeJavaLangString()) {
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
      JsExpression elseExpr = pop(); // elseExpr
      JsExpression thenExpr = pop(); // thenExpr
      JsExpression ifTest = pop(); // ifTest
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
    public void endVisit(JDebuggerStatement x, Context ctx) {
      push(new JsDebugger(x.getSourceInfo()));
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
      if (target instanceof JField) {
        JField field = (JField) target;
        if (initializeAtTopScope(field)) {
          // Will initialize at top scope; no need to double-initialize.
          push(null);
          return;
        }
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
      if (initializeAtTopScope(x)) {
        // setup the constant value
        accept(x.getLiteralInitializer());
      } else if (x.getEnclosingType() == program.getTypeJavaLangObject()) {
        // Special fields whose initialization is done somewhere else.
        push(null);
      } else if (x.getType().getDefaultValue() == JNullLiteral.INSTANCE) {
        // Fields whose default value is null are left uninitialized and will
        // have a JS value of undefined.
        push(null);
      } else {
        // setup the default value, see Issue 380
        accept(x.getType().getDefaultValue());
      }
      JsExpression rhs = (JsExpression) pop();
      JsName name = names.get(x);

      if (program.getIndexedFields().contains(x)) {
        indexedFields.put(x.getEnclosingType().getShortName() + "." + x.getName(), name);
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
      if (x.getIncrements() != null) {
        jsFor.setIncrExpr((JsExpression) pop());
      }

      // condition
      if (x.getCondition() != null) {
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

      JsFunction jsFunc = pop(); // body

      // Collect the resulting function to be considered by the JsInliner.
      if (methodsForJsInlining.contains(x)) {
        functionsForJsInlining.add(jsFunc);
      }

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
      Set<String> alreadySeen = Sets.newHashSet();
      for (int i = 0; i < locals.size(); ++i) {
        JsName name = names.get(x.getLocals().get(i));
        String ident = name.getIdent();
        if (!alreadySeen.contains(ident)
            // Catch block params don't need var declarations
            && !catchParamIdentifiers.contains(name)) {
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
          method = clinitTarget.getClinitMethod();
        }
      }

      JsNameRef qualifier;
      JsExpression unnecessaryQualifier = null;
      if (method.isStatic()) {
        if (x.getInstance() != null) {
          unnecessaryQualifier = (JsExpression) pop(); // instance
        }
        qualifier = names.get(method).makeRef(x.getSourceInfo());
      } else if (x.isStaticDispatchOnly() && method.isConstructor()) {
        /*
         * Constructor calls through {@code this} and {@code super} are always dispatched statically
         * using the constructor function name (constructors are always defined as top level
         * functions).
         *
         * Because constructors are modeled like instance methods they have an implicit {@code this}
         * parameter, hence they are invoked like: "constructor.call(this, ...)".
         */
        JsName callName = objectScope.declareName("call");
        callName.setObfuscatable(false);
        qualifier = callName.makeRef(x.getSourceInfo());
        qualifier.setQualifier(names.get(method).makeRef(x.getSourceInfo()));
        jsInvocation.getArguments().add(0, (JsExpression) pop()); // instance

      } else if (x.isStaticDispatchOnly()) {
        // Regular super call. This calls are always static and optimizations normally statify them.
        // They can appear in completely unoptimized code, hence need to be handled here.
        assert !method.isConstructor();

        // Construct JCHSU.getPrototypeFor(type).polyname
        // TODO(rluble): Ideally we would want to construct the inheritance chain the JS way and
        // then we could do Type.prototype.polyname.call(this, ...). Currently prototypes do not
        // have global names instead they are stuck into the prototypesByTypeId array.
        JsNameRef getClassProtoFunctionRef = indexedFunctions.get(
            "JavaClassHierarchySetupUtil.getClassPrototype").getName().makeRef(x.getSourceInfo());
        JsInvocation getPrototypeCall = new JsInvocation(x.getSourceInfo());
        getPrototypeCall.setQualifier(getClassProtoFunctionRef);
        final JDeclaredType superMethodTargetType = method.getEnclosingType();
        getPrototypeCall.getArguments()
            .add(convertJavaLiteral(typeIdsByType.get(superMethodTargetType)));
        JsNameRef methodNameRef = polymorphicNames.get(method).makeRef(x.getSourceInfo());
        methodNameRef.setQualifier(getPrototypeCall);

        // Construct JCHSU.getPrototypeFor(type).polyname.call(this,...)
        JsName callName = objectScope.declareName("call");
        callName.setObfuscatable(false);
        qualifier = callName.makeRef(x.getSourceInfo());
        qualifier.setQualifier(methodNameRef);
        jsInvocation.getArguments().add(0, (JsExpression) pop()); // instance
      } else {
        // Dispatch polymorphically (normal case).
        qualifier = polymorphicNames.get(method).makeRef(x.getSourceInfo());
        qualifier.setQualifier((JsExpression) pop()); // instance
      }
      jsInvocation.setQualifier(qualifier);
      push(createCommaExpression(unnecessaryQualifier, jsInvocation));
    }

    @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      List<JsExpression> exprs = popList(x.getNumberOfExpressions());
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

      // Add a few things onto the beginning.

      // Reserve the "_" identifier.
      JsVars vars = new JsVars(jsProgram.getSourceInfo());
      vars.add(new JsVar(jsProgram.getSourceInfo(), globalTemp));
      globalStmts.add(0, vars);

      // Long lits must go at the top, they can be constant field initializers.
      generateLongLiterals(vars);
      generateImmortalTypes(vars);

      // Class objects, but only if there are any.
      if (x.getDeclaredTypes().contains(x.getTypeClassLiteralHolder())) {
        // TODO: perhaps they could be constant field initializers also?
        vars = new JsVars(jsProgram.getSourceInfo());
        generateClassLiterals(vars);
        if (!vars.isEmpty()) {
          globalStmts.add(vars);
        }
      }

      // Generate entry methods. Needs to be after class literal insertion since class literal will
      // be referenced by runtime rebind and property provider bootstrapping.
      generateGwtOnLoad(entryFunctions, globalStmts);

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
    public void endVisit(JCastMap x, Context ctx) {
      SourceInfo sourceInfo = x.getSourceInfo();

      List<JsExpression> castableToTypeIdLiterals = popList(x.getCanCastToTypes().size());
      push(buildJsCastMapLiteral(castableToTypeIdLiterals, sourceInfo));
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
      JsExpression valueExpr = pop();
      JsExpression labelExpr = pop();
      push(new JsPropertyInitializer(init.getSourceInfo(), labelExpr, valueExpr));
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
        JsBlock finallyBlock = pop(); // finallyBlock
        if (finallyBlock.getStatements().size() > 0) {
          jsTry.setFinallyBlock(finallyBlock);
        }
      }

      int size = x.getCatchClauses().size();
      assert (size < 2);
      if (size == 1) {
        JsBlock catchBlock = pop(); // catchBlocks
        pop(); // catchArgs
        JsCatch jsCatch = catchMap.get(x.getCatchClauses().get(0).getBlock());
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
      // Don't generate JS for types not in current module if separate compilation is on.
      if (program.isReferenceOnly(x)) {
        return false;
      }
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
      entryMethodToIndex = Maps.newIdentityHashMap();
      for (int i = 0; i < entryMethods.size(); i++) {
        entryMethodToIndex.put(entryMethods.get(i), i);
      }

      return true;
    }

    @Override
    public boolean visit(JsniMethodBody x, Context ctx) {
      final Map<String, JNode> jsniMap = Maps.newHashMap();
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
                  jsName = nullFunctionJsName;
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

    private JsObjectLiteral buildJsCastMapLiteral(List<JsExpression> runtimeTypeIdLiterals,
        SourceInfo sourceInfo) {
      JsObjectLiteral objLit = new JsObjectLiteral(sourceInfo);
      objLit.setInternable();
      List<JsPropertyInitializer> props = objLit.getPropertyInitializers();
      JsNumberLiteral one = new JsNumberLiteral(sourceInfo, 1);
      for (JsExpression runtimeTypeIdLiteral : runtimeTypeIdLiterals) {
        JsPropertyInitializer prop = new JsPropertyInitializer(sourceInfo,
            runtimeTypeIdLiteral, one);
        props.add(prop);
      }
      return objLit;
    }

    private void checkForDupMethods(JDeclaredType x) {
      // Sanity check to see that all methods are uniquely named.
      List<JMethod> methods = x.getMethods();
      Set<String> methodSignatures = Sets.newHashSet();
      for (JMethod method : methods) {
        String sig = method.getSignature();
        if (methodSignatures.contains(sig)) {
          throw new InternalCompilerException("Signature collision in Type " + x.getName()
              + " for method " + sig);
        }
        methodSignatures.add(sig);
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
      JCastMap castMap = program.getCastMap(x);
      if (castMap != null) {
        JField castableTypeMapField = program.getIndexedField("Object.castableTypeMap");
        JsName castableTypeMapName = names.get(castableTypeMapField);
        if (castableTypeMapName == null) {
          // Was pruned; this compilation must have no dynamic casts.
          return new JsObjectLiteral(SourceOrigin.UNKNOWN);
        }

        accept(castMap);
        return pop();
      }
      return new JsObjectLiteral(SourceOrigin.UNKNOWN);
    }

    private void generateClassLiteral(JDeclarationStatement decl, JsVars vars) {
      JField field = (JField) decl.getVariableRef().getTarget();

      // TODO(rluble): refactor so that all output related to a class is decided together.
      JType type = program.getTypeByClassLiteralField(field);
      if (type != null && type instanceof JDeclaredType
          && program.isReferenceOnly((JDeclaredType) type)) {
        // Only generate class literals for classes in the current module.
        // TODO(rluble): In separate compilation some class literals will be duplicated, which if
        // not done with care might violate java semantics of getClass(). There are class literals
        // for primitives and arrays. Currently, because they will be assigned to the same field
        // the one defined later will be the one used and Java semantics are preserved.
        return;
      }

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
          (JMethodBody) program.getTypeClassLiteralHolder().getClinitMethod().getBody();
      for (JStatement stmt : clinitBody.getStatements()) {
        if (stmt instanceof JDeclarationStatement) {
          generateClassLiteral((JDeclarationStatement) stmt, vars);
        }
      }
    }

    private void generateClassSetup(JClassType x, List<JsStatement> globalStmts) {
      generateClassDefinition(x, globalStmts);
      generateVTables(x, globalStmts);

      if (x == program.getTypeJavaLangObject()) {
        // special: setup a "toString" alias for java.lang.Object.toString()
        generateToStringAlias(x, globalStmts);
        // special: setup the identifying typeMarker field
        generateTypeMarker(globalStmts);

        // Set up the artificial castmap for string.
        setupStringCastMap(program.getTypeJavaLangString(), globalStmts);

        //  Perform necessary polyfills.
        JsFunction modernizerFn =
            indexedFunctions.get("JavaClassHierarchySetupUtil.modernizeBrowser");
        JsName modernizerFnName = modernizerFn.getName();
        JsInvocation callModernizerFn = new JsInvocation(x.getSourceInfo());
        callModernizerFn.setQualifier(modernizerFnName.makeRef(x.getSourceInfo()));
        globalStmts.add(callModernizerFn.makeStmt());
      }
    }

    /**
     * Creates gwtOnLoad bootstrapping code. Unusually, the created code is executed as part of
     * source loading and runs in the global scope (not inside of any function scope).
     */
    // TODO(stalcup): get rid of manually synthesized AST and replace it either with calls to static
    // functions that vary only in their data arguments or else create source with a Generator.
    private void generateGwtOnLoad(JsFunction[] entryFuncs,
        List<JsStatement> globalStmts) {
      /**
       * <pre>
       * {MODULE_RuntimeRebindRegistrator}.register();
       * {MODULE_PropertyProviderRegistrator}.register();
       * var $entry = Impl.registerEntry();
       * // Stub gwtOnLoad at top level so that HtmlUnit can find it.
       * // On first execution this will assign a value of null into gwtOnLoad.
       * // It's value will actually be built inside of the following anonymous
       * // function and subsequent executions will preserve the existing value.
       * // Since gwtOnLoad is being varred, this will work in the global scope
       * // as well as in speculative future scopes in which linkers might
       * // choose to place the output.
       * var gwtOnLoad = typeof gwtOnLoad === 'undefined' ? null : gwtOnLoad;
       * // Create gwtOnLoad in function scope so the previousGwtOnLoad reference is preserved.
       * (function() {
       *   var previousGwtOnLoad = gwtOnLoad;
       *   gwtOnLoad = function(errFn, modName, modBase, softPermutationId) {
       *     if (previousGwtOnLoad) {
       *       previousGwtOnLoad(errFn, modName, modBase, softPermutationId);
       *     }
       *     $moduleName = modName;
       *     $moduleBase = modBase;
       *     CollapsedPropertyHolder.permutationId = softPermutationId;
       *     if (errFn) {
       *       try {
       *         $entry(init)();
       *       } catch(e) {
       *         errFn(modName);
       *       }
       *     } else {
       *       $entry(init)();
       *     }
       *   }
       * }());
       * </pre>
       */

      SourceInfo sourceInfo = SourceOrigin.UNKNOWN;

      String moduleRuntimeRebindRegistratorSourceName =
          program.getRuntimeRebindRegistratorTypeSourceName();
      // Skip if no runtime rebind registry code in this module, probably a monolithic compile.
      if (moduleRuntimeRebindRegistratorSourceName != null) {
        // RuntimeRebindRegistrator.register();
        String runtimeRebindRegistratorTypeShortName =
            SourceName.getShortClassName(moduleRuntimeRebindRegistratorSourceName);
        JsFunction registerRuntimeRebindsFunction =
            indexedFunctions.get(runtimeRebindRegistratorTypeShortName + ".register");
        JsInvocation registerRuntimeRebindsCall = new JsInvocation(sourceInfo);
        registerRuntimeRebindsCall.setQualifier(
            registerRuntimeRebindsFunction.getName().makeRef(sourceInfo));
        globalStmts.add(registerRuntimeRebindsCall.makeStmt());
      }

      String modulepropertyProviderRegistratorSourceName =
          program.getPropertyProviderRegistratorTypeSourceName();
      // Skip if no runtime property registry code in this module, probably a monolithic compile.
      if (modulepropertyProviderRegistratorSourceName != null) {
        // PropertyProviderRegistrator.register();
        String propertyProviderRegistratorTypeShortName =
            SourceName.getShortClassName(program.getPropertyProviderRegistratorTypeSourceName());
        JsFunction registerPropertyProvidersFunction =
            indexedFunctions.get(propertyProviderRegistratorTypeShortName + ".register");
        JsInvocation registerPropertyProvidersCall = new JsInvocation(sourceInfo);
        registerPropertyProvidersCall.setQualifier(
            registerPropertyProvidersFunction.getName().makeRef(sourceInfo));
        globalStmts.add(registerPropertyProvidersCall.makeStmt());
      }

      // var $entry = Impl.registerEntry();
      JsName entryName = topScope.declareName("$entry");
      JsVar entryVar = new JsVar(sourceInfo, entryName);
      JsInvocation registerEntryCall = new JsInvocation(sourceInfo);
      JsFunction registerEntryFunction = indexedFunctions.get("Impl.registerEntry");
      registerEntryCall.setQualifier(registerEntryFunction.getName().makeRef(sourceInfo));
      entryVar.setInitExpr(registerEntryCall);
      JsVars entryVars = new JsVars(sourceInfo);
      entryVars.add(entryVar);
      globalStmts.add(entryVars);

      // Stub gwtOnLoad at top level so that HtmlUnit can find it.
      // var gwtOnLoad = typeof gwtOnLoad === 'undefined' ? null : gwtOnLoad;
      JsName gwtOnLoadName = topScope.declareName("gwtOnLoad");
      gwtOnLoadName.setObfuscatable(false);
      JsVar gwtOnLoadNameVar = new JsVar(sourceInfo, gwtOnLoadName);
      gwtOnLoadNameVar.setInitExpr(new JsConditional(sourceInfo, new JsBinaryOperation(sourceInfo,
          JsBinaryOperator.REF_EQ, new JsPrefixOperation(
              sourceInfo, JsUnaryOperator.TYPEOF, gwtOnLoadName.makeRef(sourceInfo)),
          new JsStringLiteral(sourceInfo, "undefined")), JsNullLiteral.INSTANCE,
          gwtOnLoadName.makeRef(sourceInfo)));
      JsVars gwtOnLoadNameVars = new JsVars(sourceInfo);
      gwtOnLoadNameVars.add(gwtOnLoadNameVar);
      globalStmts.add(gwtOnLoadNameVars);

      // Create gwtOnLoad in function scope so the previousGwtOnLoad reference is preserved.
      // (function() {
      JsFunction createGwtOnLoadFunction = new JsFunction(sourceInfo, topScope);
      JsBlock createGwtOnLoadBody = new JsBlock(sourceInfo);
      createGwtOnLoadFunction.setBody(createGwtOnLoadBody);

      // var previousGwtOnLoad = gwtOnLoad;
      JsName previousGwtOnLoadName =
          createGwtOnLoadFunction.getScope().declareName("previousGwtOnLoad");
      JsVar previousGwtOnLoadNameVar = new JsVar(sourceInfo, previousGwtOnLoadName);
      previousGwtOnLoadNameVar.setInitExpr(gwtOnLoadName.makeRef(sourceInfo));
      JsVars previousGwtOnLoadNameVars = new JsVars(sourceInfo);
      previousGwtOnLoadNameVars.add(previousGwtOnLoadNameVar);
      createGwtOnLoadBody.getStatements().add(previousGwtOnLoadNameVars);

      // gwtOnLoad = function(errFn, modName, modBase, softPermutationId) {
      JsFunction gwtOnLoad = new JsFunction(sourceInfo, createGwtOnLoadFunction.getScope());
      gwtOnLoad.setArtificiallyRescued(true);
      JsBlock gwtOnLoadFunctionBody = new JsBlock(sourceInfo);
      gwtOnLoad.setBody(gwtOnLoadFunctionBody);
      JsExpression gwtOnLoadAssignment =
          createAssignment(gwtOnLoadName.makeRef(sourceInfo), gwtOnLoad);
      createGwtOnLoadBody.getStatements().add(gwtOnLoadAssignment.makeStmt());
      JsScope fnScope = gwtOnLoad.getScope();
      List<JsParameter> gwtOnLoadParams = gwtOnLoad.getParameters();
      JsName errFn = fnScope.declareName("errFn");
      JsName modName = fnScope.declareName("modName");
      JsName modBase = fnScope.declareName("modBase");
      JsName softPermutationId = fnScope.declareName("softPermutationId");
      gwtOnLoadParams.add(new JsParameter(sourceInfo, errFn));
      gwtOnLoadParams.add(new JsParameter(sourceInfo, modName));
      gwtOnLoadParams.add(new JsParameter(sourceInfo, modBase));
      gwtOnLoadParams.add(new JsParameter(sourceInfo, softPermutationId));

      // if (previousGwtOnLoad) {
      //   previousGwtOnLoad();
      // }
      JsIf previousGwtOnLoadIf = new JsIf(sourceInfo);
      gwtOnLoadFunctionBody.getStatements().add(previousGwtOnLoadIf);
      previousGwtOnLoadIf.setIfExpr(previousGwtOnLoadName.makeRef(sourceInfo));
      JsInvocation previousGwtOnLoadCall = new JsInvocation(sourceInfo);
      previousGwtOnLoadCall.setQualifier(previousGwtOnLoadName.makeRef(sourceInfo));
      List<JsExpression> previousGwtOnLoadCallArguments = previousGwtOnLoadCall.getArguments();
      previousGwtOnLoadCallArguments.add(errFn.makeRef(sourceInfo));
      previousGwtOnLoadCallArguments.add(modName.makeRef(sourceInfo));
      previousGwtOnLoadCallArguments.add(modBase.makeRef(sourceInfo));
      previousGwtOnLoadCallArguments.add(softPermutationId.makeRef(sourceInfo));
      previousGwtOnLoadIf.setThenStmt(previousGwtOnLoadCall.makeStmt());

      // $moduleName = modName;
      JsExpression moduleNameAssignment =
          createAssignment(topScope.findExistingUnobfuscatableName("$moduleName").makeRef(
              sourceInfo), modName.makeRef(sourceInfo));
      gwtOnLoadFunctionBody.getStatements().add(moduleNameAssignment.makeStmt());

      // $moduleBase = modBase;
      JsExpression moduleBaseAssignment =
          createAssignment(topScope.findExistingUnobfuscatableName("$moduleBase").makeRef(
              sourceInfo), modBase.makeRef(sourceInfo));
      gwtOnLoadFunctionBody.getStatements().add(moduleBaseAssignment.makeStmt());

      // Assignment to CollapsedPropertyHolder.permutationId only if it's used
      // CollapsedPropertyHolder.permutationId = softPermutationId;
      JsName permutationIdFieldName =
          names.get(program.getIndexedField("CollapsedPropertyHolder.permutationId"));
      if (permutationIdFieldName != null) {
        JsExpression permutationIdAssignment =
            createAssignment(permutationIdFieldName.makeRef(sourceInfo), softPermutationId
                .makeRef(sourceInfo));
        gwtOnLoadFunctionBody.getStatements().add(permutationIdAssignment.makeStmt());
      }

      // if (errFn) {
      //   try {
      //     $entry(init)();
      //   } catch(e) {
      //     errFn(modName);
      //   }
      // } else {
      //   $entry(init)();
      // }
      JsIf jsIf = new JsIf(sourceInfo);
      gwtOnLoadFunctionBody.getStatements().add(jsIf);
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

      // }());
      JsInvocation createGwtOnLoadCall = new JsInvocation(sourceInfo);
      createGwtOnLoadCall.setQualifier(createGwtOnLoadFunction);
      globalStmts.add(createGwtOnLoadCall.makeStmt());
    }

    private void generateImmortalTypes(JsVars globals) {
      List<JsStatement> globalStmts = jsProgram.getGlobalBlock().getStatements();
      List<JClassType> immortalTypesReversed = Lists.reverse(program.immortalCodeGenTypes);
      // visit in reverse order since insertions start at head
      JMethod createObjMethod = program.getIndexedMethod("JavaScriptObject.createObject");
      JMethod createArrMethod = program.getIndexedMethod("JavaScriptObject.createArray");

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

    private void generateLongLiterals(JsVars vars) {
      for (Entry<Long, JsName> entry : longLits.entrySet()) {
        JsName jsName = entry.getValue();
        JsExpression longObjectAlloc = longObjects.get(jsName);
        JsVar var = new JsVar(vars.getSourceInfo(), jsName);
        var.setInitExpr(longObjectAlloc);
        vars.add(var);
      }
    }

    private final GenerateJavaScriptLiterals javaToJavaScriptLiteralConverter =
        new GenerateJavaScriptLiterals();

    private JsLiteral convertJavaLiteral(JLiteral javaLiteral) {
      javaToJavaScriptLiteralConverter.accept(javaLiteral);
      return (JsLiteral) javaToJavaScriptLiteralConverter.pop();
    }

    private void generateClassDefinition(JClassType x, List<JsStatement> globalStmts) {
      SourceInfo sourceInfo = x.getSourceInfo();
      assert x != program.getTypeJavaLangString();

      JsInvocation defineClass = new JsInvocation(sourceInfo);
      JsName defineClassRef = indexedFunctions.get(
          "JavaClassHierarchySetupUtil.defineClass").getName();
      defineClass.setQualifier(defineClassRef.makeRef(sourceInfo));
      JLiteral typeId = getRuntimeTypeReference(x);
      JClassType superClass = x.getSuperClass();
      JLiteral superTypeId = (superClass == null) ? JNullLiteral.INSTANCE :
          getRuntimeTypeReference(x.getSuperClass());
      // JavaClassHierarchySetupUtil.defineClass(typeId, superTypeId, castableMap, constructors)
      defineClass.getArguments().add(convertJavaLiteral(typeId));
      defineClass.getArguments().add(convertJavaLiteral(superTypeId));
      JsExpression castMap = generateCastableTypeMap(x);
      defineClass.getArguments().add(castMap);

      // Chain assign the same prototype to every live constructor.
      for (JMethod method : x.getMethods()) {
        if (!isMethodPotentiallyALiveConstructor(method)) {
          // Some constructors are never newed hence don't need to be registered with defineClass.
          continue;
        }

        defineClass.getArguments().add(names.get(method).makeRef(sourceInfo));
      }

      JsStatement tmpAsgStmt = defineClass.makeStmt();
      globalStmts.add(tmpAsgStmt);
      typeForStatMap.put(tmpAsgStmt, x);
    }

    /*
     * Sets up the catmap for String.
     */
    private void setupStringCastMap(JClassType x, List<JsStatement> globalStmts) {
      //  Cast.stringCastMap = /* String cast map */ { ..:1, ..:1}
      JField castableTypeMapField = program.getIndexedField("Cast.stringCastMap");
      JsName castableTypeMapName = names.get(castableTypeMapField);
      JsNameRef ctmRef = castableTypeMapName.makeRef(x.getSourceInfo());

      JsExpression castMapLit = generateCastableTypeMap(x);
      JsExpression ctmAsg = createAssignment(ctmRef,
          castMapLit);
      JsExprStmt ctmAsgStmt = ctmAsg.makeStmt();
      globalStmts.add(ctmAsgStmt);
      typeForStatMap.put(ctmAsgStmt, x);
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
      JsExpression asg = createAssignment(fieldRef, nullFunctionJsName.makeRef(sourceInfo));
      JsExprStmt stmt = asg.makeStmt();
      globalStmts.add(stmt);
      typeForStatMap.put(stmt, program.getTypeJavaLangObject());
    }

    private void generateVTables(JClassType x, List<JsStatement> globalStmts) {
      assert x != program.getTypeJavaLangString();
      for (JMethod method : x.getMethods()) {
        SourceInfo sourceInfo = method.getSourceInfo();
        if (method.needsVtable() && !method.isAbstract()) {
          JsNameRef lhs = polymorphicNames.get(method).makeRef(sourceInfo);
          lhs.setQualifier(globalTemp.makeRef(sourceInfo));

          /*
           * Inline JsFunction rather than reference, e.g. _.vtableName =
           * function functionName() { ... }
           */
          JsExpression rhs = methodBodyMap.get(method.getBody());
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
      JsExpression asg = createAssignment(clinitFunc.getName().makeRef(sourceInfo),
              nullFunctionJsName.makeRef(sourceInfo));
      statements.add(0, asg.makeStmt());
    }

    private boolean isMethodPotentiallyCalledAcrossClasses(JMethod method) {
      assert !hasWholeWorldKnowledge || crossClassTargets != null;
      return crossClassTargets == null || crossClassTargets.contains(method);
    }

    /**
     * Whether a method is a constructor that is actually newed. Note that in absence of whole
     * world knowledge evey constructor is potentially live.
     */
    private boolean isMethodPotentiallyALiveConstructor(JMethod method) {
      if (!(method instanceof JConstructor)) {
        return false;
      }
      assert !hasWholeWorldKnowledge || liveCtors != null;
      return liveCtors == null || liveCtors.contains(method);
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

      JMethod clinitMethod = targetType.getClinitMethod();
      SourceInfo sourceInfo = x.getSourceInfo();
      JsInvocation jsInvocation = new JsInvocation(sourceInfo);
      jsInvocation.setQualifier(names.get(clinitMethod).makeRef(sourceInfo));
      return jsInvocation;
    }

    private JsInvocation maybeCreateClinitCall(JMethod x) {
      if (!isMethodPotentiallyCalledAcrossClasses(x)) {
        // Global optimized compile can prune some clinit calls.
        return null;
      }
      if (x.canBePolymorphic() || program.isStaticImpl(x)) {
        return null;
      }
      JDeclaredType enclosingType = x.getEnclosingType();
      if (enclosingType == null || !enclosingType.hasClinit()) {
        return null;
      }
      // Avoid recursion sickness.
      if (JProgram.isClinit(x)) {
        return null;
      }

      JMethod clinitMethod = enclosingType.getClinitTarget().getClinitMethod();
      SourceInfo sourceInfo = x.getSourceInfo();
      JsInvocation jsInvocation = new JsInvocation(sourceInfo);
      jsInvocation.setQualifier(names.get(clinitMethod).makeRef(sourceInfo));
      return jsInvocation;
    }

    /**
     * If a field is a literal, we can potentially treat it as immutable and assign it once on the
     * prototype, to be reused by all instances of the class, instead of re-assigning the same
     * literal in each constructor.
     *
     * Technically, to match JVM semantics, we should only do this for final or static fields. For
     * non-final/non-static fields, a super class's cstr, when it calls a polymorphic method that is
     * overridden in the subclass, should actually see default values (not the literal initializer)
     * before the subclass's cstr runs.
     *
     * However, cstr's calling polymorphic methods is admittedly an uncommon case, so we apply some
     * heuristics to see if we can initialize the field on the prototype anyway.
     */
    private boolean initializeAtTopScope(JField x) {
      if (x.getLiteralInitializer() == null) {
        return false;
      }
      if (x.isFinal() || x.isStatic()) {
        // we can definitely initialize at top-scope, as JVM does so as well
        return true;
      }
      // if the superclass can observe the field, we need to leave the default value
      JDeclaredType current = x.getEnclosingType().getSuperClass();
      while (current != null) {
        if (canObserveSubclassFields.contains(current)) {
          return false;
        }
        current = current.getSuperClass();
      }
      // should be safe to initialize at top-scope, as no one can observe the difference
      return true;
    }
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

  /**
   * Determines which classes can potentially see uninitialized values of their subclasses' fields.
   *
   * If a class can not observe subclass uninitialized fields then the initialization of those could
   * be hoisted to the prototype.
   */
  private class CanObserveSubclassUninitializedFieldsVisitor extends JVisitor {
    private JDeclaredType currentClass;

    @Override
    public boolean visit(JConstructor x, Context ctx) {
      // Only look at constructor bodies.
      assert currentClass == null;
      currentClass = x.getEnclosingType();
      return true;
    }

    @Override
    public void endVisit(JConstructor x, Context ctx) {
      currentClass = null;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      if (x.getName().equals("$$init")) {
        assert currentClass == null;
        currentClass = x.getEnclosingType();
        return true;
      }
      // Do not traverse the method body if it is not a constructor.
      return false;
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      currentClass = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // This is a method call inside a constructor.
      assert currentClass != null;
      // Calls to this/super constructors are okay, as they will also get examined
      if (x.getTarget().isConstructor() && x.getInstance() instanceof JThisRef) {
        return;
      }
      // Calls to the instance initializer are okay, as execution will not escape it
      if (x.getTarget().getName().equals("$$init")) {
        return;
      }
      // Calls to static methods with no arguments are safe, because they have no
      // way to trace back into our new instance.
      if (x.getTarget().isStatic() && x.getTarget().getOriginalParamTypes().size() == 0) {
        return;
      }
      // Technically we could get fancier about trying to track the "this" reference
      // through other variable assignments, field assignments, and methods calls, to
      // see if it calls a polymorphic method against ourself (which would let subclasses
      // observe their fields), but that gets tricky, so we'll use the above heuristics
      // for now.
      canObserveSubclassFields.add(currentClass);
    }
  }

  private class RecordJSInlinableMethods extends JVisitor {

    private JMethod currentMethod;

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (x.isNative()) {
        methodsForJsInlining.add(x);
      }

      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      if (x.getTarget().isNative()
          && ((JsniMethodBody) x.getTarget().getBody()).getFunc().getBody().getStatements().size()
          <= JsInliner.MAX_INLINE_FN_SIZE) {
        // currentMethod calls a jsni method, currentMethod
        // will be consider for JavaScript inlining
        methodsForJsInlining.add(currentMethod);
      }
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      return true;
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
   * @param program           a Java AST
   * @param jsProgram         an (empty) JavaScript AST
   * @param outputOption      options that affect this transformation for this transformation
   *                          e.g. OBFUSCATED, etc.
   * @param symbolTable       an (empty) symbol table that will be populated here
   * @param propertyOracles   property oracles that correspond to the permutation being compiled.
   * @return A pair containing a JavaToJavaScriptMap and a Set of JsFunctions that need to be
   *         considered for inlining.
   */
  public static Pair<JavaToJavaScriptMap, Set<JsNode>> exec(JProgram program,
      JsProgram jsProgram, CompilerContext compilerContext, Map<JType, JLiteral> typeIdsByType,
      Map<StandardSymbolData, JsName> symbolTable,
      PropertyOracle[] propertyOracles) {
    GenerateJavaScriptAST generateJavaScriptAST =
        new GenerateJavaScriptAST(program, jsProgram, compilerContext, typeIdsByType,
            symbolTable, propertyOracles);
    return generateJavaScriptAST.execImpl();
  }

  private final Map<JBlock, JsCatch> catchMap = Maps.newIdentityHashMap();

  private final Set<JsName> catchParamIdentifiers = Sets.newHashSet();

  private final Map<JClassType, JsScope> classScopes = Maps.newIdentityHashMap();

  /**
   * A list of methods that are called from another class (ie might need to
   * clinit).
   */
  private Set<JMethod> crossClassTargets = null;

  private Map<String, JsFunction> indexedFunctions = Maps.newHashMap();

  private Map<String, JsName> indexedFields = Maps.newHashMap();

  /**
   * Methods where inlining hasn't happened yet because they are native or contain calls to native
   * methods. See {@link RecordJSInlinableMethods}.
   */
  private Set<JMethod> methodsForJsInlining = Sets.newHashSet();

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
  private final Set<JDeclaredType> canObserveSubclassFields = Sets.newHashSet();

  /**
   * Sorted to avoid nondeterministic iteration.
   */
  private final Map<Long, JsName> longLits = Maps.newTreeMap();

  private final Map<JsName, JsExpression> longObjects = Maps.newIdentityHashMap();
  private final Map<JAbstractMethodBody, JsFunction> methodBodyMap = Maps.newIdentityHashMap();
  private final Map<HasName, JsName> names = Maps.newIdentityHashMap();
  private JsName nullFunctionJsName;

  /**
   * Contains JsNames for the Object instance methods, such as equals, hashCode,
   * and toString. All other class scopes have this scope as an ultimate parent.
   */
  private final JsScope objectScope;
  private final Set<JsFunction> polymorphicJsFunctions = Sets.newIdentityHashSet();
  private final Map<JMethod, JsName> polymorphicNames = Maps.newIdentityHashMap();
  private final JProgram program;

  private final JsOutputOption output;
  // Whether the AST for the whole program arrived to this pass or just for one module.
  // This is used to do some final optimizations.
  // TODO(rluble) move optimization to a Java AST optimization pass.
  private final boolean hasWholeWorldKnowledge;

  /**
   * All of the fields in String and Array need special handling for interop.
   */
  private final Map<JField, String> specialObfuscatedFields = Maps.newHashMap();

  /**
   * All of the methods in String and Array need special handling for interop.
   */
  private final Map<String, String> specialObfuscatedMethodSigs = Maps.newHashMap();

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

  private final Map<JsStatement, JClassType> typeForStatMap = Maps.newHashMap();

  private final JTypeOracle typeOracle;

  private final Map<JsStatement, JMethod> vtableInitForMethodMap = Maps.newHashMap();

  private final Map<JType, JLiteral> typeIdsByType;

  private GenerateJavaScriptAST(JProgram program, JsProgram jsProgram,
      CompilerContext compilerContext, Map<JType, JLiteral> typeIdsByType,
      Map<StandardSymbolData, JsName> symbolTable, PropertyOracle[] propertyOracles) {
    this.program = program;
    typeOracle = program.typeOracle;
    this.jsProgram = jsProgram;
    topScope = jsProgram.getScope();
    objectScope = jsProgram.getObjectScope();
    interfaceScope = new JsNormalScope(objectScope, "Interfaces");
    this.output = compilerContext.getOptions().getOutput();
    this.hasWholeWorldKnowledge = compilerContext.shouldCompileMonolithic();
    this.symbolTable = symbolTable;
    this.typeIdsByType = typeIdsByType;

    this.stripStack =
        JsStackEmulator.getStackMode(propertyOracles) == JsStackEmulator.StackMode.STRIP;

    /*
     * Because we modify the JavaScript String prototype, all fields and
     * polymorphic methods on String and super types need special handling.
     */

    // Object polymorphic
    Map<String, String> namesToIdents = Maps.newHashMap();
    namesToIdents.put("getClass", "gC");
    namesToIdents.put("hashCode", "hC");
    namesToIdents.put("equals", "eQ");
    namesToIdents.put("toString", "tS");
    namesToIdents.put("finalize", "fZ");

    List<JMethod> methods = Lists.newArrayList(program.getTypeJavaLangObject().getMethods());
    for (JMethod method : methods) {
      if (method.canBePolymorphic()) {
        String ident = namesToIdents.get(method.getName());
        assert ident != null : method.getEnclosingType().getName() + "::" + method.getName() +
            " is not in the list of known methods.";
        specialObfuscatedMethodSigs.put(method.getSignature(), ident);
      }
    }

    namesToIdents.clear();
    // Object fields
    namesToIdents.put("expando", "eX");
    namesToIdents.put("typeMarker", "tM");
    namesToIdents.put("castableTypeMap", "cM");
    namesToIdents.put("___clazz", "cZ");

    for (JField field : program.getTypeJavaLangObject().getFields()) {
      if (!field.isStatic()) {
        String ident = namesToIdents.get(field.getName());
        assert ident != null : field.getEnclosingType().getName() + "::" + field.getName() +
            " is not in the list of known fields.";
        specialObfuscatedFields.put(field, ident);
      }
    }
  }

  String getNameString(HasName hasName) {
    String s = hasName.getName().replaceAll("_", "_1").replace('.', '_');
    return s;
  }

  /**
   * Retrieves the runtime typeId for {@code type}.
   */
  JLiteral getRuntimeTypeReference(JReferenceType type) {
    return typeIdsByType.get(type);
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

  private Pair<JavaToJavaScriptMap, Set<JsNode>> execImpl() {
    new FixNameClashesVisitor().accept(program);
    new CanObserveSubclassUninitializedFieldsVisitor().accept(program);
    new SortVisitor().accept(program);
    if (hasWholeWorldKnowledge) {
      // TODO(rluble): pull out this analysis and make it a Java AST optimization pass.
      new RecordCrossClassCallsAndConstructorLiveness().accept(program);
      new RecordJSInlinableMethods().accept(program);
    }

    CreateNamesAndScopesVisitor creator = new CreateNamesAndScopesVisitor();
    creator.accept(program);
    GenerateJavaScriptVisitor generator =
        new GenerateJavaScriptVisitor();
    generator.accept(program);

    jsProgram.setIndexedFields(indexedFields);
    jsProgram.setIndexedFunctions(indexedFunctions);

    // TODO(spoon): Instead of gathering the information here, get it via
    // SourceInfo
    JavaToJavaScriptMap jjsMap = new JavaToJavaScriptMapImpl(program.getDeclaredTypes(),
        names, typeForStatMap, vtableInitForMethodMap);

    return Pair.create(jjsMap, generator.functionsForJsInlining);
  }
}
