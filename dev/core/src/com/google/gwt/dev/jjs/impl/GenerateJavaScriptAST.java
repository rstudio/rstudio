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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.impl.StandardSymbolData;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.cfg.PermProps;
import com.google.gwt.dev.javac.JsInteropUtil;
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
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
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
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.js.JDebuggerStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniClassLiteral;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.impl.ResolveRuntimeTypeReferences.TypeMapper;
import com.google.gwt.dev.js.JsStackEmulator;
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
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.Name.SourceName;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.arg.OptionJsInteropMode;
import com.google.gwt.dev.util.arg.OptionMethodNameDisplayMode;
import com.google.gwt.dev.util.arg.OptionOptimize;
import com.google.gwt.dev.util.collect.Stack;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSortedSet;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

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
    private Map<JVariable, Scope> scopesByLocal;
    private Multimap<String, JVariable> localsByName;

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
    public void endVisit(JVariableRef x, Context ctx) {
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
      if (x instanceof JFieldRef) {
        // Skip fields as they are always qualified in JavaScript and their name resolution logic
        // is in {@link CreateNameAndScopesVisitor}.
        return;
      }
      JVariable local = x.getTarget();
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
        Collection<JVariable> localSet = localsByName.get(name);
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
      String name = x.getName();
      String mangleName = mangleName(x);
      if (x.isStatic()) {
        JsName jsName = topScope.declareName(mangleName, name);
        names.put(x, jsName);
        recordSymbol(x, jsName);
      } else {
        JsName jsName;
        if (specialObfuscatedFields.containsKey(x)) {
          jsName = scopeStack.peek().declareName(mangleNameSpecialObfuscate(x));
          jsName.setObfuscatable(false);
        } else {
          jsName = scopeStack.peek().declareName(mangleName, name);
        }
        names.put(x, jsName);
        recordSymbol(x, jsName);
      }
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
      scopeStack.pop();
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      names.put(x, scopeStack.peek().declareName(x.getName()));
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
        scopeStack.push(myScope);
        return false;
      }

      // My seed function name
      JsName jsName = topScope.declareName(getNameString(x), x.getShortName());
      names.put(x, jsName);
      recordSymbol(x, jsName);

      // My class scope
      if (x.getSuperClass() == null || x.getSuperClass().isJsPrototypeStub()) {
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
      // my polymorphic name
      String name = x.getName();
      if (x.needsVtable()) {
        if (polymorphicNames.get(x) == null) {
          JsName polyName;
          if (x.isPrivate()) {
            polyName = interfaceScope.declareName(mangleNameForPrivatePoly(x), name);
          } else if (x.isDefault()) {
            polyName = interfaceScope.declareName(mangleNameForPackagePrivatePoly(x), name);
            // Also add the mapping from the top of the package private overriding chain, so
            // so that it can be referred when generating the vtable of a subclass that
            // increases the visibility of this method.
            polymorphicNames.put(typeOracle.getTopMostDefinition(x), polyName);

          } else if (specialObfuscatedMethodSigs.containsKey(x.getSignature())) {
            polyName = interfaceScope.declareName(mangleNameSpecialObfuscate(x));
            polyName.setObfuscatable(false);
            // if a JsType and we can set set the interface method to non-obfuscatable
          } else if (!x.isNoExport() && typeOracle.isJsTypeMethod(x) &&
              !typeOracle.needsJsInteropBridgeMethod(x)) {
              polyName = interfaceScope.declareName(name, name);
              polyName.setObfuscatable(false);
          } else {
            polyName = interfaceScope.declareName(mangleNameForPoly(x), name);
          }
          polymorphicNames.put(x, polyName);
        }
      }

      if (x.isAbstract()) {
        // just push a dummy scope that we can pop in endVisit
        scopeStack.push(null);
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
      if (!stripStack || !polymorphicNames.containsKey(x) || x.isNative()
          || nameOfTargets.contains(x)) {
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
      scopeStack.push(jsFunction.getScope());

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
        JsCatch jsCatch = new JsCatch(x.getSourceInfo(), scopeStack.peek(), arg.getTarget().getName());
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

    private void recordSymbol(JReferenceType x, JsName jsName) {
      if (getRuntimeTypeReference(x) == null || !typeOracle.isInstantiatedType(x)) {
        return;
      }

      String typeId = getRuntimeTypeReference(x).toSource();
      StandardSymbolData symbolData =
          StandardSymbolData.forClass(x.getName(), x.getSourceInfo().getFileName(),
              x.getSourceInfo().getStartLine(), typeId);
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

  private class GenerateJavaScriptVisitor extends JVisitor {

    private final Set<JClassType> alreadyRan = Sets.newLinkedHashSet();

    private final List<JsStatement> exportStmts = new ArrayList<JsStatement>();
    private final JsName arrayLength = objectScope.declareName("length");

    private final Map<JClassType, JsFunction> clinitMap = Maps.newHashMap();

    public static final String LOCAL_TEMP_PREFIX = "$tmp$";

    private JMethod currentMethod = null;

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

    /**
     * Holds any local variable declarations which must be inserted into the current JS function
     * body under construction.
     */
    private JsVars pendingLocals;

    /**
     * Counter for assigning locals.
     */
    int tmpNumber = 0;

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
        JsLabel label = pop(); // label
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

      if (program.isJsTypePrototype(x)) {
        // Don't generate JS for magic @PrototypeOfJsType stubs classes, strip them from output
        return;
      }

      if (alreadyRan.contains(x)) {
        return;
      }
      assert program.getTypeClassLiteralHolder() != x;
      assert !program.immortalCodeGenTypes.contains(x);
      // Super classes should be emitted before the actual class.
      assert x.getSuperClass() == null || program.isReferenceOnly(x.getSuperClass()) ||
          program.isJsTypePrototype(x.getSuperClass()) ||
          alreadyRan.contains(x.getSuperClass());

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

          if (shouldEmitDisplayNames()) {
            // get the original method for this function
            JMethod originalMethod = javaMethodForJSFunction.get(func);
            JsExprStmt displayNameAssignment =
                outputDisplayName(func.getName().makeRef(func.getSourceInfo()), originalMethod);
            globalStmts.add(displayNameAssignment);
          }
        }
      }

      if (typeOracle.isInstantiatedType(x) && !typeOracle.isJavaScriptObject(x) &&
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

      if (typeOracle.isInteropEnabled() &&
          typeOracle.isInstantiatedType(x) && !typeOracle.isJavaScriptObject(x) &&
        x !=  program.getTypeJavaLangString()) {
        // done after class setup because exports may rely on static vars
        generateExports(x, exportStmts);
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
        JsLabel label = pop(); // label
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

      JsExpression initializer = pop(); // initializer
      JsNameRef localRef = pop(); // localRef

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
      JsExpression expr = pop(); // expr
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
      JsExpression rhs = pop();
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
      JsInvocation jsInvocation = maybeCreateClinitCall(field, false);
      if (jsInvocation != null) {
        curExpr = createCommaExpression(jsInvocation, curExpr);
      }

      if (x.getInstance() != null) {
        JsExpression qualifier = pop();
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

      if (typeOracle.isInteropEnabled()) {
        generateExports(x, exportStmts);
      }
    }

    @Override
    public void endVisit(JLabel x, Context ctx) {
      push(new JsLabel(x.getSourceInfo(), names.get(x)));
    }

    @Override
    public void endVisit(JLabeledStatement x, Context ctx) {
      JsStatement body = pop(); // body
      JsLabel label = pop(); // label
      label.setStmt(body);
      push(label);
    }

    @Override
    public void endVisit(JLiteral x, Context ctx) {
      push(JjsUtils.translateLiteral(x));
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
    public void endVisit(JMethod x, Context ctx) {
      if (x.isAbstract()) {
        push(null);
        return;
      }

      JsFunction jsFunc = pop(); // body

      javaMethodForJSFunction.put(jsFunc, x);

      if (!program.isInliningAllowed(x)) {
        jsProgram.disallowInlining(jsFunc);
      }

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

      if (!pendingLocals.isEmpty()) {
        jsFunc.getBody().getStatements().add(0, pendingLocals);
      }

      push(jsFunc);
      currentMethod = null;
      pendingLocals = null;
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {

      JsBlock body = pop();
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
        if (clinitTarget == null || program.isJsTypePrototype(clinitTarget)) {
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

      JsNameRef qualifier = null;
      JsExpression unnecessaryQualifier = null;
      JsExpression result = null;
      boolean isJsProperty = false;
      result = jsInvocation;

      if (method.isStatic()) {
        if (x.getInstance() != null) {
          unnecessaryQualifier = pop(); // instance
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
        JsNameRef methodRef = names.get(method).makeRef(x.getSourceInfo());
        qualifier.setQualifier(methodRef);
        jsInvocation.getArguments().add(0, (JsExpression) pop()); // instance
        if (program.isJsTypePrototype(method.getEnclosingType())) {
          result = dispatchToSuperPrototype(x, method, qualifier, methodRef, jsInvocation);
        }
      } else if (x.isStaticDispatchOnly() && !method.isConstructor()) {
        // Regular super call. This calls are always static and optimizations normally statify them.
        // They can appear in completely unoptimized code, hence need to be handled here.

        // Construct JCHSU.getPrototypeFor(type).polyname
        // TODO(rluble): Ideally we would want to construct the inheritance chain the JS way and
        // then we could do Type.prototype.polyname.call(this, ...). Currently prototypes do not
        // have global names instead they are stuck into the prototypesByTypeId array.
        final JDeclaredType superMethodTargetType = method.getEnclosingType();

        JsInvocation getPrototypeCall = constructInvocation(x.getSourceInfo(),
            "JavaClassHierarchySetupUtil.getClassPrototype",
            JjsUtils.translateLiteral(program.getLiteral(typeMapper.get(superMethodTargetType))));

        JsNameRef methodNameRef = polymorphicNames.get(method).makeRef(x.getSourceInfo());
        methodNameRef.setQualifier(getPrototypeCall);

        // Construct JCHSU.getPrototypeFor(type).polyname.call(this,...)
        JsName callName = objectScope.declareName("call");
        callName.setObfuscatable(false);
        qualifier = callName.makeRef(x.getSourceInfo());
        qualifier.setQualifier(methodNameRef);
        jsInvocation.getArguments().add(0, (JsExpression) pop()); // instance
        // Is this method targeting a Foo_Prototype class?
        if (program.isJsTypePrototype(method.getEnclosingType())) {
          result = dispatchToSuperPrototype(x, method, qualifier, methodNameRef, jsInvocation);
        }
      } else {
        JsName polyName = polymorphicNames.get(method);
        // potentially replace method call with property access
        JMethod target = x.getTarget();
        for (JMethod overrideMethod : target.getOverriddenMethods()) {
          if (overrideMethod.isJsProperty()) {
            isJsProperty = true;
            break;
          }
        }
        if (isJsProperty || target.isJsProperty()) {
          String getter = isGetter(target);
          String setter = isSetter(target);
          String has = isHas(target);

          // if fluent
          JType type = target.getType();

          boolean isFluent = type instanceof JReferenceType
              && type != program.getTypeJavaLangObject() && typeOracle.canTriviallyCast(
                  x.getTarget().getEnclosingType(), type.getUnderlyingType());
          JsExpression qualExpr = pop();

          if (getter != null) {
            result = dispatchAsGetter(x, unnecessaryQualifier, getter, qualExpr);
          } else if (setter != null) {
            result = dispatchAsSetter(x, jsInvocation, setter, isFluent, qualExpr);
          } else if (has != null) {
            result = dispatchAsHas(x, has, qualExpr);
          } else {
            throw new InternalCompilerException("JsProperty not a setter, getter, or has.");
          }
        } else {
          // insert trampoline (_ = instance, trampoline(_, _.jsBridgeMethRef,
          // _.javaMethRef)).bind(_)(args)
          if (typeOracle.needsJsInteropBridgeMethod(method)) {
            maybeDispatchViaTrampolineToBridgeMethod(x, method, jsInvocation,
                unnecessaryQualifier, result, polyName);

            return;
          } else {
            // Dispatch polymorphically (normal case).
            qualifier = polyName.makeRef(x.getSourceInfo());
            qualifier.setQualifier((JsExpression) pop()); // instance
          }
        }
      }
      if (!isJsProperty) {
        jsInvocation.setQualifier(qualifier);
      }
      push(createCommaExpression(unnecessaryQualifier, result));
    }

    private void maybeDispatchViaTrampolineToBridgeMethod(JMethodCall x,
        JMethod method, JsInvocation jsInvocation,
        JsExpression unnecessaryQualifier, JsExpression result,
        JsName polyName) {

      JsName tempLocal = createTmpLocal();

      // tempLocal = instance value
      JsExpression tmp = createAssignment(tempLocal.makeRef(
          x.getSourceInfo()), ((JsExpression) pop()));
      JsName trampMethName = indexedFunctions.get(
          "JavaClassHierarchySetupUtil.trampolineBridgeMethod").getName();

      // tempLocal.jsBridgeMethRef
      JsName bridgejsName = polyName.getEnclosing().findExistingName(method.getName());
      JsNameRef bridgeRef = bridgejsName != null ? bridgejsName.makeRef(x.getSourceInfo())
          : new JsNameRef(x.getSourceInfo(), method.getName());
      bridgeRef.setQualifier(tempLocal.makeRef(x.getSourceInfo()));

      // tempLocal.javaMethRef
      JsNameRef javaMethRef = polyName.makeRef(x.getSourceInfo());
      javaMethRef.setQualifier(tempLocal.makeRef(x.getSourceInfo()));

      JsInvocation callTramp = new JsInvocation(x.getSourceInfo(),
          trampMethName.makeRef(x.getSourceInfo()),
          tempLocal.makeRef(x.getSourceInfo()),
          bridgeRef,
          javaMethRef);

      JsNameRef bind = new JsNameRef(x.getSourceInfo(), "bind");
      JsInvocation callBind = new JsInvocation(x.getSourceInfo());
      callBind.setQualifier(bind);
      callBind.getArguments().add(tempLocal.makeRef(x.getSourceInfo()));
      // (tempLocal = instance, tramp(tempLocal, tempLocal.bridgeRef, tempLocal.javaRef)).bind(tempLocal)
      bind.setQualifier(callTramp);
      jsInvocation.setQualifier(callBind);
      result = createCommaExpression(tmp, jsInvocation);
      push(createCommaExpression(unnecessaryQualifier, result));
    }

    private JsName createTmpLocal() {
      SourceInfo sourceInfo = currentMethod.getSourceInfo();
      JsFunction func = getJsFunctionFor(currentMethod);
      JsScope funcScope = func.getScope();
      JsName tmpName;
      String tmpIdent = LOCAL_TEMP_PREFIX + tmpNumber;

      while (funcScope.findExistingName(tmpIdent) != null) {
        tmpNumber++;
        tmpIdent = LOCAL_TEMP_PREFIX + tmpNumber;
      }

      tmpName = funcScope.declareName(tmpIdent);

      JsVar var = new JsVar(sourceInfo, tmpName);
      pendingLocals.add(var);
      return tmpName;
    }

    private JsExpression dispatchAsHas(JMethodCall x, String has, JsExpression qualExpr) {
      JsExpression result;JsNameRef property = new JsNameRef(x.getSourceInfo(), has);
      result = new JsBinaryOperation(x.getSourceInfo(), JsBinaryOperator.INOP,
          property, qualExpr);
      return result;
    }

    private JsExpression dispatchAsSetter(JMethodCall x, JsInvocation jsInvocation, String setter, boolean isFluent, JsExpression qualExpr) {
      JsExpression result;JsNameRef property = new JsNameRef(x.getSourceInfo(), setter);
      // either qualExpr.prop or _.prop depending on fluent or not
      property.setQualifier(isFluent ? globalTemp.makeRef(x.getSourceInfo()) : qualExpr);
      // propExpr = arg
      result = createAssignment(property, jsInvocation.getArguments().get(0));
      if (isFluent) {
        // (_ = qualExpr, _.prop = arg, _)
        result = createCommaExpression(
            createAssignment(globalTemp.makeRef(x.getSourceInfo()), qualExpr),
            createCommaExpression(result,
              globalTemp.makeRef(x.getSourceInfo())));
      }
      return result;
    }

    private JsExpression dispatchAsGetter(JMethodCall x, JsExpression unnecessaryQualifier, String getter, JsExpression qualExpr) {
      JsExpression result;// replace with qualExpr.property
      JsNameRef property = new JsNameRef(x.getSourceInfo(), getter);
      property.setQualifier(qualExpr);
      result = createCommaExpression(unnecessaryQualifier, property);
      return result;
    }

    /**
     * Setup qualifier and methodRef to dispatch to super-ctor or super-method.
     */
    private JsExpression dispatchToSuperPrototype(JMethodCall x, JMethod method, JsNameRef qualifier,
                                                  JsNameRef methodRef, JsInvocation jsInvocation) {
      String jsPrototype = null;
      // find JsType of Prototype method being invoked.
      for (JInterfaceType intf : method.getEnclosingType().getImplements()) {
        JDeclaredType jsIntf = typeOracle.getNearestJsType(intf, true);
        assert jsIntf instanceof JInterfaceType;

        if (jsIntf != null) {
          jsPrototype = jsIntf.getJsPrototype();
          break;
        }
      }
      assert jsPrototype != null : "Unable to find JsType with prototype";

      // in JsType case, super.foo() call requires SuperCtor.prototype.foo.call(this, args)
      // the method target should be on a class that ends with $Prototype and implements a JsType
      if (!(method instanceof JConstructor) && typeOracle.isJsTypeMethod(method)) {
        JsNameRef protoRef = prototype.makeRef(x.getSourceInfo());
        methodRef = new JsNameRef(methodRef.getSourceInfo(), method.getName());
        // add qualifier so we have jsPrototype.prototype.methodName.call(this, args)
        protoRef.setQualifier(JsInteropUtil.convertQualifiedPrototypeToNameRef(
            x.getSourceInfo(), jsPrototype));
        methodRef.setQualifier(protoRef);
        qualifier.setQualifier(methodRef);
        return jsInvocation;
      }

      return JsNullLiteral.INSTANCE;
    }

    private String isGetter(JMethod method) {
      String name = method.getName();
      // zero arg non-void getX()
      if (name.length() > 3 && name.startsWith("get") && Character.isUpperCase(name.charAt(3)) &&
          method.getType() != JPrimitiveType.VOID && method.getParams().size() == 0) {
        String propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        return propName;
      } else  if (name.length() > 3 && name.startsWith("is")
          && Character.isUpperCase(name.charAt(2)) && method.getType() == JPrimitiveType.BOOLEAN
          && method.getParams().size() == 0) {
        String propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        return propName;
      } else if (method.getParams().size() == 0 && method.getType() != JPrimitiveType.VOID) {
        return name;
      }
      return null;
    }

    private String isSetter(JMethod method) {
      String name = method.getName();
      if (name.length() > 3 && name.startsWith("set") && Character.isUpperCase(name.charAt(3))
          && method.getParams().size() == 1) {
        String propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        return propName;
      } else if (method.getParams().size() == 1) {
        return name;
      }
      return null;
    }

    private String isHas(JMethod method) {
      String name = method.getName();
      if (name.length() > 3 && name.startsWith("has") && Character.isUpperCase(name.charAt(3))
          && method.getParams().size() == 0 && method.getType() == JPrimitiveType.BOOLEAN) {
        String propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        return propName;
      }
      return null;
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
      if (name == null) {
        push(new JsNameRef(x.getSourceInfo(), JsRootScope.INSTANCE.getUndefined()));
        return;
      }
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

    /**
     * Embeds properties into permProps for easy access from JavaScript.
     */
    private void embedBindingProperties() {
      SourceInfo sourceInfo = SourceOrigin.UNKNOWN;

      // Generates a list of lists of pairs: [[["key", "value"], ...], ...]
      // The outermost list is indexed by soft permutation id. Each item represents
      // a map from binding properties to their values, but is stored as a list of pairs
      // for easy iteration.
      JsArrayLiteral permProps = new JsArrayLiteral(sourceInfo);
      for (ImmutableMap<String, String> propMap : props.findEmbeddedProperties(TreeLogger.NULL)) {
        JsArrayLiteral entryList = new JsArrayLiteral(sourceInfo);
        for (Entry<String, String> entry : propMap.entrySet()) {
          JsArrayLiteral pair = new JsArrayLiteral(sourceInfo,
              new JsStringLiteral(sourceInfo, entry.getKey()),
              new JsStringLiteral(sourceInfo, entry.getValue()));
          entryList.getExpressions().add(pair);
        }
        permProps.getExpressions().add(entryList);
      }

      jsProgram.getGlobalBlock().getStatements().add(
          constructInvocation(sourceInfo, "ModuleUtils.setGwtProperty",
              new JsStringLiteral(sourceInfo, "permProps"), permProps).makeStmt());
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

      // Don't generate JS for magic @PrototypeOfJsType classes
      if (program.isJsTypePrototype(x)) {
        return false;
      }

      if (alreadyRan.contains(x)) {
        return false;
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
      pendingLocals = new JsVars(x.getSourceInfo());
      return true;
    }

    private void insertInTopologicalOrder(JDeclaredType type,
        Set<JDeclaredType> topologicallySortedSet) {
      if (type == null || topologicallySortedSet.contains(type) || program.isReferenceOnly(type)) {
        return;
      }
      insertInTopologicalOrder(type.getSuperClass(), topologicallySortedSet);
      topologicallySortedSet.add(type);
    }

    @Override
    public boolean visit(JProgram x, Context ctx) {
      // Handle the visiting here as we need to slightly change the order.
      // 1.1 (preamble) Immortal code gentypes.
      // 1.2 (preamble) Classes in the preamble, i.e. all the classes that are needed
      //                to support creation of class literals (reachable through Class.createFor* ).
      // 1.3 (preamble) Class literals for classes in the preamble.
      // 2.  (body)     Normal classes, each with its corresponding class literal (if live).
      // 3.  (epilogue) Code to start the execution of the program (gwtOnLoad, etc).

      List<JsStatement> globalStmts = jsProgram.getGlobalBlock().getStatements();

      Set<JDeclaredType> preambleTypes = generatePreamble(x, globalStmts);

      if (compilePerFile) {
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
      for (JDeclaredType type : x.getModuleDeclaredTypes()) {
        insertInTopologicalOrder(type, topologicallySortedBodyTypes);
      }
      // Remove all preamble types that might have been inserted here.
      topologicallySortedBodyTypes.removeAll(preambleTypes);

      // Iterate over each type in the right order.
      markPosition(globalStmts, "Program", Type.PROGRAM_START);
      for (JDeclaredType type : topologicallySortedBodyTypes) {
        markPosition(globalStmts, type.getName(), Type.CLASS_START);
        accept(type);
        JsVars classLiteralVars = new JsVars(jsProgram.getSourceInfo());
        maybeGenerateClassLiteral(type, classLiteralVars);
        if (!classLiteralVars.isEmpty()) {
          globalStmts.add(classLiteralVars);
        }
        markPosition(globalStmts, type.getName(), Type.CLASS_END);
      }
      markPosition(globalStmts, "Program", Type.PROGRAM_END);

      generateEpilogue(globalStmts);

      // All done, do not visit children.
      return false;
    }

    private Set<JDeclaredType> generatePreamble(JProgram program, List<JsStatement> globalStmts) {
      // Reserve the "_" identifier.
      JsVars vars = new JsVars(jsProgram.getSourceInfo());
      vars.add(new JsVar(jsProgram.getSourceInfo(), globalTemp));
      globalStmts.add(vars);

      // Generate immortal types in the preamble.
      generateImmortalTypes(vars);

      Set<JDeclaredType> alreadyProcessed =
          Sets.<JDeclaredType>newLinkedHashSet(program.immortalCodeGenTypes);
      alreadyProcessed.add(program.getTypeClassLiteralHolder());

      List<JDeclaredType> classLiteralSupportClasses =
          computeClassLiteralsSupportClasses(program, alreadyProcessed);

      // Make sure immortal classes are not doubly processed.
      classLiteralSupportClasses.removeAll(alreadyProcessed);
      for (JDeclaredType type : classLiteralSupportClasses) {
        accept(type);
      }
      generateClassLiterals(globalStmts, classLiteralSupportClasses);

      Set<JDeclaredType> preambleTypes = Sets.newLinkedHashSet(alreadyProcessed);
      preambleTypes.addAll(classLiteralSupportClasses);
      return preambleTypes;
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

      assert !modularCompile || checkCoreModulePreambleComplete(program,
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

    private void generateEpilogue(List<JsStatement> globalStmts) {
      // Emit all the class literals for classes that where pruned.
      // NOTE: this should not happen in fully unoptimized compiles.
      generateClassLiterals(globalStmts, Iterables.filter(classLiteralDeclarationsByType.keySet(),
          Predicates.not(Predicates.<JType>in(alreadyRan))));

      // add all @JsExport assignments
      globalStmts.addAll(exportStmts);

      // Generate entry methods. Needs to be after class literal insertion since class literal will
      // be referenced by runtime rebind and property provider bootstrapping.
      setupGwtOnLoad(globalStmts);

      embedBindingProperties();

      if (program.getRunAsyncs().size() > 0) {
        // Prevent onLoad from being pruned.
        JMethod onLoadMethod = program.getIndexedMethod("AsyncFragmentLoader.onLoad");
        JsName name = names.get(onLoadMethod);
        assert name != null;
        JsFunction func = (JsFunction) name.getStaticRef();
        func.setArtificiallyRescued(true);
      }
    }

    private void generateClassLiterals(List<JsStatement> globalStmts,
        Iterable<? extends JType> orderedTypes) {
      JsVars vars = new JsVars(jsProgram.getSourceInfo());
      for (JType type : orderedTypes) {
        maybeGenerateClassLiteral(type, vars);
      }
      if (!vars.isEmpty()) {
        globalStmts.add(vars);
      }
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
            JsInvocation clinitCall = maybeCreateClinitCall(field, false);
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
                jsName =
                    indexedFunctions.get("JavaClassHierarchySetupUtil.emptyMethod").getName();
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
            JsSwitchMember switchMember = pop(); // stmt
            jsSwitch.getCases().add(switchMember);
            curStatements = switchMember.getStmts();
          } else {
            // add to statements for current case
            assert (curStatements != null);
            JsStatement newStmt = pop(); // stmt
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
      List<JsPropertyInitializer> propInitializers = objLit.getPropertyInitializers();
      JsNumberLiteral one = new JsNumberLiteral(sourceInfo, 1);
      for (JsExpression runtimeTypeIdLiteral : runtimeTypeIdLiterals) {
        JsPropertyInitializer propInitializer = new JsPropertyInitializer(sourceInfo,
            runtimeTypeIdLiteral, one);
        propInitializers.add(propInitializer);
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

    private void maybeGenerateClassLiteral(JType type, JsVars vars) {
      JDeclarationStatement decl = classLiteralDeclarationsByType.get(type);
      if (decl == null) {
        return;
      }

      JField field = (JField) decl.getVariableRef().getTarget();

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

      JsName jsName = names.get(field);
      this.accept(decl.getInitializer());
      JsExpression classObjectAlloc = pop();
      JsVar var = new JsVar(decl.getSourceInfo(), jsName);
      var.setInitExpr(classObjectAlloc);
      vars.add(var);
    }

    private void generateClassSetup(JClassType x, List<JsStatement> globalStmts) {
      generateClassDefinition(x, globalStmts);
      generateVTables(x, globalStmts);

      if (x == program.getTypeJavaLangObject()) {
        // special: setup a "toString" alias for java.lang.Object.toString()
        generateToStringAlias(x, globalStmts);

        // Set up the artificial castmap for string.
        setupStringCastMap(program.getTypeJavaLangString(), globalStmts);

        //  Perform necessary polyfills.
        globalStmts.add(constructInvocation(x.getSourceInfo(),
            "JavaClassHierarchySetupUtil.modernizeBrowser").makeStmt());
      }
    }

    private void markPosition(List<JsStatement> statements, String name, Type type) {
      statements.add(new JsPositionMarker(SourceOrigin.UNKNOWN, name, type));
    }

    /**
     * Sets up gwtOnLoad bootstrapping code. Unusually, the created code is executed as part of
     * source loading and runs in the global scope (not inside of any function scope).
     */
    private void setupGwtOnLoad(List<JsStatement> globalStmts) {
      /**
       * <pre>
       * {MODULE_RuntimeRebindRegistrator}.register();
       * {MODULE_PropertyProviderRegistrator}.register();
       * var $entry = Impl.registerEntry();
       * var gwtOnLoad = ModuleUtils.gwtOnLoad();
       * ModuleUtils.addInitFunctions(init1, init2,...)
       * </pre>
       */

      final SourceInfo sourceInfo = SourceOrigin.UNKNOWN;

      // {MODULE_RuntimeRebindRegistrator}.register();
      // {MODULE_PropertyProviderRegistrator}.register();
      List<String> registerFnList = Lists.newArrayList();
      mayAddProviderRegisterFn(registerFnList,
          program.getRuntimeRebindRegistratorTypeSourceName());
      mayAddProviderRegisterFn(registerFnList,
          program.getPropertyProviderRegistratorTypeSourceName());
      for (String registerFnName : registerFnList) {
        globalStmts.add(constructInvocation(sourceInfo, registerFnName).makeStmt());
      }

      // var $entry = ModuleUtils.registerEntry();
      JsStatement entryVars = constructFunctionCallStatement(
          topScope.declareName("$entry"), "ModuleUtils.registerEntry");
      globalStmts.add(entryVars);

      // var gwtOnLoad = ModuleUtils.gwtOnLoad;
      JsName gwtOnLoad = topScope.findExistingUnobfuscatableName("gwtOnLoad");
      JsVar varGwtOnLoad = new JsVar(sourceInfo, gwtOnLoad);
      varGwtOnLoad.setInitExpr(createAssignment(gwtOnLoad.makeRef(sourceInfo),
          indexedFunctions.get("ModuleUtils.gwtOnLoad").getName().makeRef(sourceInfo)));
      globalStmts.add(new JsVars(sourceInfo, varGwtOnLoad));

      // ModuleUtils.addInitFunctions(init1, init2,...)
      List<JsExpression> arguments = Lists.newArrayList();
      for (JMethod entryPointMethod : program.getEntryMethods()) {
        JsFunction entryFunction = getJsFunctionFor(entryPointMethod);
        arguments.add(entryFunction.getName().makeRef(sourceInfo));
      }

      JsStatement createGwtOnLoadFunctionCall =
          constructInvocation("ModuleUtils.addInitFunctions", arguments).makeStmt();

      globalStmts.add(createGwtOnLoadFunctionCall);
    }

    private void mayAddProviderRegisterFn(List<String> providerFnNames,
        String providerTypeSourceName) {
      // ProviderClass.register();
      if (providerTypeSourceName == null) {
        return;
      }
      providerFnNames.add(SourceName.getShortClassName(providerTypeSourceName) + ".register");
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
      JsFunction functionToInvoke = indexedFunctions.get(indexedFunctionName);
      return new JsInvocation(sourceInfo, functionToInvoke, args);
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
          JsFunction func = pop();
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

    private JsLiteral convertJavaLiteral(JLiteral javaLiteral) {
      return JjsUtils.translateLiteral(javaLiteral);
    }

    private void generateClassDefinition(JClassType x, List<JsStatement> globalStmts) {
      SourceInfo sourceInfo = x.getSourceInfo();
      assert x != program.getTypeJavaLangString();

      JLiteral typeId = getRuntimeTypeReference(x);
      JClassType superClass = x.getSuperClass();
      JLiteral superTypeId = (superClass == null) ? JNullLiteral.INSTANCE :
          getRuntimeTypeReference(x.getSuperClass());
      // check if there's an overriding prototype
      JInterfaceType jsPrototypeIntf = JProgram.maybeGetJsTypeFromPrototype(superClass);
      String jsPrototype = jsPrototypeIntf != null ? jsPrototypeIntf.getJsPrototype() : null;

      List<JsExpression> defineClassArguments = Lists.newArrayList();

      defineClassArguments.add(convertJavaLiteral(typeId));
      // setup superclass normally
      if (jsPrototype == null) {
        defineClassArguments.add(convertJavaLiteral(superTypeId));
      } else {
        // setup extension of native JS object
        JsNameRef jsProtoClassRef =
            JsInteropUtil.convertQualifiedPrototypeToNameRef(
                x.getSourceInfo(), jsPrototype);
        // TODO(cromwellian) deal with module vs global scoping issue
        // jsProtoClassRef.setQualifier(new JsNameRef(x.getSourceInfo(), "$wnd"));
        JsNameRef jsProtoFieldRef = new JsNameRef(x.getSourceInfo(), "prototype");

        jsProtoFieldRef.setQualifier(jsProtoClassRef);
        defineClassArguments.add(jsProtoClassRef);
      }
      JsExpression castMap = generateCastableTypeMap(x);
      defineClassArguments.add(castMap);

      // Chain assign the same prototype to every live constructor.
      for (JMethod method : x.getMethods()) {
        if (!isMethodPotentiallyALiveConstructor(method)) {
          // Some constructors are never newed hence don't need to be registered with defineClass.
          continue;
        }
        defineClassArguments.add(names.get(method).makeRef(sourceInfo));
      }

      // choose appropriate setup function
      // JavaClassHierarchySetupUtil.defineClass(typeId, superTypeId, castableMap, constructors)
      JsStatement defineClassStatement = constructInvocation(sourceInfo,
          jsPrototype == null ? "JavaClassHierarchySetupUtil.defineClass" :
              "JavaClassHierarchySetupUtil.defineClassWithPrototype",
          defineClassArguments).makeStmt();
      globalStmts.add(defineClassStatement);
      typeForStatMap.put(defineClassStatement, x);
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
        JsNameRef toStringRef = new JsNameRef(sourceInfo, polymorphicNames.get(toStringMeth));
        toStringRef.setQualifier(new JsThisRef(sourceInfo));
        JsInvocation call = new JsInvocation(sourceInfo, toStringRef);
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

    /**
     * Create a vtable assignment of the form _.polyname = rhs; and register the line as
     * created for {@code method}.
     */
    private void generateVTableAssignment(List<JsStatement> globalStmts, JMethod method,
        JsName lhsName, JsExpression rhs) {
      SourceInfo sourceInfo = method.getSourceInfo();
      JsNameRef lhs = lhsName.makeRef(sourceInfo);
      lhs.setQualifier(globalTemp.makeRef(sourceInfo));
      JsExprStmt polyAssignment = createAssignment(lhs, rhs).makeStmt();
      globalStmts.add(polyAssignment);
      vtableInitForMethodMap.put(polyAssignment, method);

      if (shouldEmitDisplayNames()) {
        JsExprStmt displayNameAssignment = outputDisplayName(lhs, method);
        globalStmts.add(displayNameAssignment);
        vtableInitForMethodMap.put(displayNameAssignment, method);
      }
    }

    private JsExprStmt outputDisplayName(JsNameRef function, JMethod method) {
      JsNameRef displayName = new JsNameRef(function.getSourceInfo(), "displayName");
      displayName.setQualifier(function);
      String displayStringName = getDisplayName(method);
      JsStringLiteral displayMethodName = new JsStringLiteral(function.getSourceInfo(), displayStringName);
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
     * variable _ points the JavaScript prototype for {@code x}.
     */
    private void generateVTables(JClassType x, List<JsStatement> globalStmts) {
      assert x != program.getTypeJavaLangString();
      for (JMethod method : x.getMethods()) {
        SourceInfo sourceInfo = method.getSourceInfo();
        if (method.needsVtable() && !method.isAbstract()) {
          /*
           * Inline JsFunction rather than reference, e.g. _.vtableName =
           * function functionName() { ... }
           */
          JsExpression rhs = getJsFunctionFor(method);
          JsName polyJsName = polymorphicNames.get(method);
          generateVTableAssignment(globalStmts, method, polyJsName, rhs);
          if (!method.isNoExport()
              && typeOracle.needsJsInteropBridgeMethod(method)) {
            JsName exportedName = polyJsName.getEnclosing().declareName(
                method.getName(), method.getName());
            // _.exportedName = makeBridgeMethod(_.polyName)
            exportedName.setObfuscatable(false);
            JsNameRef polyRef = polyJsName.makeRef(sourceInfo);
            polyRef.setQualifier(globalTemp.makeRef(sourceInfo));
            generateVTableAssignment(globalStmts, method,
                exportedName,
                createJsInteropBridgeMethod(method,
                    polyRef));
          }
          if (method.exposesOverriddenPackagePrivateMethod() &&
              getPackagePrivateName(method) != null) {
            // This method exposes a package private method that is actually live, hence it needs
            // to make the package private name and the public name to be the same implementation at
            // runtime. This is done by an assignment of the form.
            // _.package_private_name = _.exposed_name

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
            //
            // It should be noted that all subclasses of a.D will have the two methods collapsed,
            // and hence this assignment will be present in the vtable setup for all subclasses.

            JsNameRef polyname = polyJsName.makeRef(sourceInfo);
            polyname.setQualifier(globalTemp.makeRef(sourceInfo));

            generateVTableAssignment(globalStmts, method,
                getPackagePrivateName(method),
                polyname);
          }
        }
      }
    }

    private void generateExports(JDeclaredType x, List<JsStatement> globalStmts) {

      String lastProvidedNamespace = "";
      boolean createdClinit = false;

      // export 1 constructor
      JConstructor ctor = null;
      for (JMethod m : x.getMethods()) {
        if (m instanceof JConstructor) {
          if (!((JConstructor) m).isDefaultConstructor()
              && typeOracle.isExportedMethod(m)) {
            ctor = (JConstructor) m;
            break;
          }
        }
      }

      for (JMethod m : x.getMethods()) {
        if (m instanceof JConstructor && m != ctor) {
          continue;
        }
        // static functions or constructors may be exported
        if (m == ctor && !m.isPrivate() ||
            (m.isStatic()) && typeOracle.isExportedMethod(m)) {

          createdClinit = maybeHoistClinit(exportStmts, createdClinit, maybeCreateClinitCall(m));
          JsExpression exportRhs = createJsInteropBridgeMethod(m,
              names.get(m).makeRef(m.getSourceInfo()));
          String exportName = m.getQualifiedExportName();
          lastProvidedNamespace = exportMember(x, globalStmts, lastProvidedNamespace, exportRhs, exportName);
        }
      }

      for (JField f : x.getFields()) {
        if (f.isStatic() && f.getExportName() != null) {
          createdClinit = maybeHoistClinit(exportStmts, createdClinit,
              maybeCreateClinitCall(f, true));
          JsNameRef exportRhs = names.get(f).makeRef(f.getSourceInfo());
          String exportName = f.getQualifiedExportName();
          lastProvidedNamespace = exportMember(x, globalStmts, lastProvidedNamespace, exportRhs, exportName);
        }
      }
    }

    private JsExpression createJsInteropBridgeMethod(JMethod m, JsNameRef methodRef) {
      if (m.isStatic() || m instanceof JConstructor) {
        return methodRef;
      } else {
        // call JHCSU.makeBridgeMethod(functionRefToBeCalled)
        JsFunction makeBridgeMethod = indexedFunctions.get("JavaClassHierarchySetupUtil.makeBridgeMethod");
        JsNameRef makeBridgeMethodRef = makeBridgeMethod.getName().makeRef(methodRef.getSourceInfo());
        JsInvocation invokeBridge = new JsInvocation(methodRef.getSourceInfo());
        invokeBridge.setQualifier(makeBridgeMethodRef);
        invokeBridge.getArguments().add(methodRef);
        invokeBridge.getArguments().add(m.getType() == JPrimitiveType.LONG ?
            JsBooleanLiteral.TRUE : JsBooleanLiteral.FALSE);
        JsArrayLiteral arrayLiteral = new JsArrayLiteral(m.getSourceInfo());
        for (JParameter p : m.getParams()) {
          if (p.getType() == JPrimitiveType.LONG) {
            arrayLiteral.getExpressions().add(JsBooleanLiteral.TRUE);
          } else {
            arrayLiteral.getExpressions().add(JsBooleanLiteral.FALSE);
          }
        }
        invokeBridge.getArguments().add(arrayLiteral);
        return invokeBridge;
      }
    }

    private boolean maybeHoistClinit(List<JsStatement> stmts, boolean createdClinit,
                                     JsInvocation clInitJsInvocation) {
      // TODO(cromwellian): move to using Object.defineProperty and bridge functions
      // Hoist clinit to first member that needs it
      if (!createdClinit) {
        if (clInitJsInvocation != null) {
          stmts.add(clInitJsInvocation.makeStmt());
          createdClinit = true;
        }
      }
      return createdClinit;
    }

    private String exportMember(JDeclaredType x, List<JsStatement> globalStmts, String lastProvidedNamespace,
        JsExpression exportRhs, String exportName) {
      Pair<String, String> exportNamespacePair = getExportNamespace(exportName);
      lastProvidedNamespace = exportProvidedNamespace(x, globalStmts, lastProvidedNamespace, exportNamespacePair);
      createAndAddExportAssignment(x, globalStmts, exportRhs, exportNamespacePair);
      return lastProvidedNamespace;
    }

    private void createAndAddExportAssignment(JDeclaredType x, List<JsStatement> globalStmts,
        JsExpression exportRhs, Pair<String, String> exportNamespacePair) {
      JsNameRef leaf = new JsNameRef(x.getSourceInfo(), exportNamespacePair.getRight());
      leaf.setQualifier(getExportLhsQualifier(x, exportNamespacePair.getLeft()));
      JsExprStmt astStat = new JsExprStmt(x.getSourceInfo(),
           createAssignment(leaf,
               exportRhs));
      globalStmts.add(astStat);
    }

    private JsNameRef getExportLhsQualifier(JDeclaredType x, String namespace) {
      if (!jsExportClosureStyle) {
        return globalTemp.makeRef(x.getSourceInfo());
      }

      if ("".equals(namespace)) {
        return new JsNameRef(x.getSourceInfo(), "window");
      }

      String parts[] = namespace.split("\\.");
      JsNameRef ref = new JsNameRef(x.getSourceInfo(), parts[parts.length - 1]);
      JsNameRef toReturn = ref;
      for (int i = parts.length - 2; i >= 0; i--) {
        JsNameRef qualifier = new JsNameRef(x.getSourceInfo(), parts[i]);
        ref.setQualifier(qualifier);
        ref = qualifier;
      }
      return toReturn;
    }

    private String exportProvidedNamespace(JDeclaredType x, List<JsStatement> globalStmts,
                                           String lastProvidedNamespace, Pair<String, String> exportNamespacePair) {
      if (!lastProvidedNamespace.equals(exportNamespacePair.getLeft())) {
        if (jsInteropMode == OptionJsInteropMode.Mode.JS) {
          JsName provideFunc = indexedFunctions.get("JavaClassHierarchySetupUtil.provide").getName();
          JsNameRef provideFuncRef = provideFunc.makeRef(x.getSourceInfo());
          JsInvocation provideCall = new JsInvocation(x.getSourceInfo());

          provideCall.setQualifier(provideFuncRef);
          provideCall.getArguments().add(new JsStringLiteral(x.getSourceInfo(),
              exportNamespacePair.getLeft()));

          // _ = JCHSU.provide('foo.bar')
          JsExprStmt provideStat = createAssignment(globalTemp.makeRef(x.getSourceInfo()),
              provideCall).makeStmt();
          globalStmts.add(provideStat);
        } else if (jsInteropMode == OptionJsInteropMode.Mode.CLOSURE) {
          // goog.provide statements prepended by linker, so namespace already exists
          // but enclosing constructor exports may have overwritten them
          // so write foo.bar.Baz = foo.bar.Baz || {}
          if (x.getEnclosingType() != null) {
            JsNameRef lhs = getExportLhsQualifier(x, x.getQualifiedExportName());
            JsNameRef rhsRef = getExportLhsQualifier(x, x.getQualifiedExportName());
            globalStmts.add(createAssignment(lhs, new JsBinaryOperation(x.getSourceInfo(),
                JsBinaryOperator.OR, rhsRef, new JsObjectLiteral(x.getSourceInfo()))).makeStmt());
          }
        }

        lastProvidedNamespace = exportNamespacePair.getLeft();
      }
      return lastProvidedNamespace;
    }

    /**
     * Returns a pair of namespace and leaf name.
     */
    private Pair<String, String> getExportNamespace(String exportName) {
      String[] parts = exportName.split("\\.");
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < parts.length - 1; i++) {
        if (i != 0) {
          sb.append('.');
        }
        sb.append(parts[i]);
      }
      return Pair.create(sb.toString(), parts[parts.length -  1]);
    }

    /**
     * Returns the package private JsName for {@code method}.
     */
    private JsName getPackagePrivateName(JMethod method) {
      return polymorphicNames.get(typeOracle.getTopMostDefinition(method));
    }

    private void handleClinit(JsFunction clinitFunc, JsFunction superClinit) {
      clinitFunc.setExecuteOnce(true);
      clinitFunc.setImpliedExecute(superClinit);
      List<JsStatement> statements = clinitFunc.getBody().getStatements();
      SourceInfo sourceInfo = clinitFunc.getSourceInfo();
      // self-assign to the global noop method immediately (to prevent reentrancy)

      JsFunction emptyFunctionFn =
          indexedFunctions.get("JavaClassHierarchySetupUtil.emptyMethod");
      JsExpression asg = createAssignment(clinitFunc.getName().makeRef(sourceInfo),
          emptyFunctionFn.getName().makeRef(sourceInfo));
      statements.add(0, asg.makeStmt());
    }

    private boolean isMethodPotentiallyCalledAcrossClasses(JMethod method) {
      assert !hasWholeWorldKnowledge || crossClassTargets != null;
      return crossClassTargets == null || crossClassTargets.contains(method) ||
          typeOracle.isExportedMethod(method) ||
          typeOracle.isJsTypeMethod(method);
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

    private JsInvocation maybeCreateClinitCall(JField x, boolean isExported) {
      if (!x.isStatic() || x.isCompileTimeConstant()) {
        // Access to compile time constants do not trigger class initialization (JLS 12.4.1).
        return null;
      }

      JDeclaredType targetType = x.getEnclosingType().getClinitTarget();
      if (targetType == null) {
        return null;
      }

      if (!isExported &&
          (currentMethod == null || !currentMethod.getEnclosingType().checkClinitTo(targetType))) {
        return null;
      } else if (targetType.equals(program.getTypeClassLiteralHolder())) {
        return null;
      }

      JMethod clinitMethod = targetType.getClinitMethod();
      SourceInfo sourceInfo = x.getSourceInfo();
      return new JsInvocation(sourceInfo, names.get(clinitMethod).makeRef(sourceInfo));
    }

    private JsInvocation maybeCreateClinitCall(JMethod x) {
      if (!isMethodPotentiallyCalledAcrossClasses(x)) {
        // Global optimized compile can prune some clinit calls.
        return null;
      }
      JDeclaredType enclosingType = x.getEnclosingType();
      if (x.canBePolymorphic() || (program.isStaticImpl(x) &&
          !typeOracle.isJavaScriptObject(enclosingType))) {
        return null;
      }
      if (enclosingType == null || !enclosingType.hasClinit()) {
        return null;
      }
      // Avoid recursion sickness.
      if (JProgram.isClinit(x)) {
        return null;
      }

      JMethod clinitMethod = enclosingType.getClinitTarget().getClinitMethod();
      SourceInfo sourceInfo = x.getSourceInfo();
      return new JsInvocation(sourceInfo, names.get(clinitMethod).makeRef(sourceInfo));
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
      if (x.isFinal() || x.isStatic() || x.isCompileTimeConstant()) {
        // we can definitely initialize at top-scope, as JVM does so as well
        return true;
      }

      return !uninitializedValuePotentiallyObservable.apply(x);
    }

    // Keep track of a translation stack.
    private final Stack<JsVisitable> nodeStack = new Stack<JsVisitable>();

    @SuppressWarnings("unchecked")
    private <T extends JsVisitable> T pop() {
      return (T) nodeStack.pop();
    }

    private <T extends JsVisitable> List<T> popList(int count) {
      return (List<T>)
          Lists.newArrayList(Iterables.filter(nodeStack.pop(count), Predicates.notNull()));
    }

    @SuppressWarnings("unchecked")
    private <T extends JsVisitable> void popList(List<T> collection, int count) {
      collection.addAll((List<T>) popList(count));
    }

    private <T extends JsVisitable> void push(T node) {
      nodeStack.push(node);
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

  private class RecordJSInlinableMethods extends JVisitor {

    private JMethod currentMethod;

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (x.isNative()) {
        // These are methods that were not considered by the Java method inliner.
        methodsForJsInlining.add(x);
      }

      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod target = x.getTarget();
      if (program.isInliningAllowed(target) && (target.isNative()
          || program.getIndexedTypes().contains(target.getEnclosingType()))) {
        // the currentMethod calls a method that was not considered by the Java MethodInliner; these
        // include JSNI methods and methods whose calls were inserted by normalization passes.
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
      if (typeOracle.isExportedMethod(x) && x instanceof JConstructor) {
        // exported ctors always considered live
        liveCtors.add((JConstructor) x);
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
      Map<StandardSymbolData, JsName> symbolTable, PermProps props) {

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

  private final OptionJsInteropMode.Mode jsInteropMode;

  private final JsProgram jsProgram;

  private boolean jsExportClosureStyle;

  private Set<JConstructor> liveCtors = null;

  /**
   * Classes that could potentially see uninitialized values for fields that are initialized in the
   * declaration.
   */
  private Predicate<JField> uninitializedValuePotentiallyObservable;

  private final Map<JAbstractMethodBody, JsFunction> methodBodyMap = Maps.newIdentityHashMap();
  private final Map<HasName, JsName> names = Maps.newIdentityHashMap();
  private final Map<JsFunction, JMethod> javaMethodForJSFunction = Maps.newIdentityHashMap();

  /**
   * Contains JsNames for the Object instance methods, such as equals, hashCode,
   * and toString. All other class scopes have this scope as an ultimate parent.
   */
  private final JsScope objectScope;
  private final Set<JsFunction> polymorphicJsFunctions = Sets.newIdentityHashSet();
  private final Map<JMethod, JsName> polymorphicNames = Maps.newIdentityHashMap();
  private final JProgram program;

  /**
   * SEt of all targets of JNameOf.
   */
  private Set<HasName> nameOfTargets = Sets.newHashSet();

  private final JsOutputOption output;
  // Whether the AST for the whole program arrived to this pass or just for one module.
  // This is used to do some final optimizations.
  // TODO(rluble) move optimization to a Java AST optimization pass.
  private final boolean hasWholeWorldKnowledge;

  private final boolean optimize;

  private final TreeLogger logger;

  private final boolean modularCompile;

  private final boolean compilePerFile;

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

  private final TypeMapper<?> typeMapper;

  private final MinimalRebuildCache minimalRebuildCache;

  private final PermProps props;

  private OptionMethodNameDisplayMode.Mode methodNameMappingMode;

  private GenerateJavaScriptAST(TreeLogger logger, JProgram program, JsProgram jsProgram,
      CompilerContext compilerContext, TypeMapper<?> typeMapper,
      Map<StandardSymbolData, JsName> symbolTable, PermProps props) {
    this.logger = logger;
    this.program = program;
    typeOracle = program.typeOracle;
    this.jsProgram = jsProgram;
    topScope = jsProgram.getScope();
    objectScope = jsProgram.getObjectScope();
    interfaceScope = new JsNormalScope(objectScope, "Interfaces");
    this.minimalRebuildCache = compilerContext.getMinimalRebuildCache();
    this.output = compilerContext.getOptions().getOutput();
    this.optimize =
        compilerContext.getOptions().getOptimizationLevel() > OptionOptimize.OPTIMIZE_LEVEL_DRAFT;
    this.methodNameMappingMode = compilerContext.getOptions().getMethodNameDisplayMode();
    assert methodNameMappingMode != null;
    this.hasWholeWorldKnowledge = compilerContext.shouldCompileMonolithic()
        && !compilerContext.getOptions().isIncrementalCompileEnabled();
    this.compilePerFile = compilerContext.getOptions().isIncrementalCompileEnabled();
    this.modularCompile = !compilerContext.shouldCompileMonolithic();
    this.symbolTable = symbolTable;
    this.typeMapper = typeMapper;
    this.props = props;

    this.stripStack = JsStackEmulator.getStackMode(props) == JsStackEmulator.StackMode.STRIP;
    this.jsExportClosureStyle = compilerContext.getOptions().getJsInteropMode()
        == OptionJsInteropMode.Mode.CLOSURE;
    this.jsInteropMode = compilerContext.getOptions().getJsInteropMode();

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
    Object typeId = typeMapper.get(type);
    if (typeId == null) {
      return null;
    }
    return program.getLiteral(typeId);
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
    return StringInterner.get().intern(s);
  }

  String mangleNameForPackagePrivatePoly(JMethod x) {
    assert x.isDefault() && !x.isStatic();
    StringBuilder sb = new StringBuilder();
    /*
     * Package private instance methods in different classes should not override each
     * other, so they must have distinct polymorphic names. Therefore, add the
     * class name of where the method is first defined to the mangled name.
     */
    sb.append("package_private$");
    JMethod topDefinition = typeOracle.getTopMostDefinition(x);
    sb.append(getNameString(topDefinition.getEnclosingType()));
    sb.append("$");
    sb.append(getNameString(x));
    constructManglingSignature(x, sb);
    return StringInterner.get().intern(sb.toString());
  }

  String mangleNameForPoly(JMethod x) {
    assert !x.isPrivate() && !x.isStatic();
    StringBuilder sb = new StringBuilder();
    sb.append(getNameString(x));
    constructManglingSignature(x, sb);
    return StringInterner.get().intern(sb.toString());
  }

  String mangleNameForPrivatePoly(JMethod x) {
    assert x.isPrivate() && !x.isStatic();
    StringBuilder sb = new StringBuilder();
    /*
     * Private instance methods in different classes should not override each
     * other, so they must have distinct polymorphic names. Therefore, add the
     * class name to the mangled name.
     */
    sb.append("private$");
    sb.append(getNameString(x.getEnclosingType()));
    sb.append("$");
    sb.append(getNameString(x));
    constructManglingSignature(x, sb);
    return StringInterner.get().intern(sb.toString());
  }

  private void constructManglingSignature(JMethod x, StringBuilder partialSignature) {
    partialSignature.append("__");
    for (int i = 0; i < x.getOriginalParamTypes().size(); ++i) {
      JType type = x.getOriginalParamTypes().get(i);
      partialSignature.append(type.getJavahSignatureName());
    }
    partialSignature.append(x.getOriginalReturnType().getJavahSignatureName());
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
    new FixNameClashesVisitor().accept(program);
    uninitializedValuePotentiallyObservable = optimize ?
        ComputePotentiallyObservableUninitializedValues.analyze(program) : Predicates.<JField>alwaysTrue();
    new FindNameOfTargets().accept(program);
    new SortVisitor().accept(program);
    if (hasWholeWorldKnowledge) {
      // TODO(rluble): pull out this analysis and make it a Java AST optimization pass.
      new RecordCrossClassCallsAndConstructorLiveness().accept(program);
      new RecordJSInlinableMethods().accept(program);
    }

    // Map class literals to their respective types.
    contructTypeToClassLiteralDeclarationMap();

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

  private JsFunction getJsFunctionFor(JMethod jMethod) {
    return methodBodyMap.get(jMethod.getBody());
  }
}
