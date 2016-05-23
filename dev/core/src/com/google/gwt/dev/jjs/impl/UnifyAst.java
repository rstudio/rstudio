/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.PrecompilationContext;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JEnumType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethod.Specialization;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNameOf;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JPermutationDependentValue;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JDebuggerStatement;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.js.ast.JsNestingScope;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.dev.util.log.MetricName;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.LinkedListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.Sets.SetView;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Take independently-compiled types and merge them into a single AST.
 *
 * Works kind of like {@link ControlFlowAnalyzer} in terms of reachability,
 * except that in some cases it's easier to be conservative and visit relatively
 * more nodes than CFA would.
 *
 * Operates based on a work-queue to prevent recursion sickness.
 *
 * Must handle:
 *
 * - Type reference resolution
 *
 * - Field and method reference resolution
 *
 * - General code flow like ControlFlowAnalyzer
 *
 * - GWT.create(), GWT.runAsync(), Impl.getNameOf()
 *
 * - Stitch native methods into JsProgram
 *
 * - Class.desiredAssertionStatus, Class.isClassMetaDataEnabled, GWT.isClient,
 * GWT.isProdMode, GWT.isScript.
 */
// TODO: SOYC correlations.
// TODO(stalcup): perform only binary name based lookups so that libraries
// don't need to index compilation units by both source and binary name
// TODO(stalcup): shrink the translate/flowInto graph for reference only types to eliminate
// unnecessary loading of types and increase performance.
public class UnifyAst {

  /**
   * Embodies the access methods for the compiled class, compilation unit and type for a flavor of
   * type name.
   */
  private abstract class NameBasedTypeLocator {
    private final Map<String, CompiledClass> compiledClassesByTypeName;

    private NameBasedTypeLocator(Map<String, CompiledClass> compiledClassesByTypeName) {
      this.compiledClassesByTypeName = compiledClassesByTypeName;
    }

    protected abstract boolean hasCompileErrors(String typeName);

    protected abstract void logErrorTrace(TreeLogger branch, Type logLevel, String sourceName);

    protected CompilationUnit getCompilationUnitFromSource(String typeName) {
      return compiledClassesByTypeName.get(typeName).getUnit();
    }

    protected JDeclaredType getResolvedType(String typeName) {
      JDeclaredType resolvedType = program.getFromTypeMap(typeName);
      return resolvedType;
    }

    protected boolean resolvedTypeIsAvailable(String typeName) {
      return program.getFromTypeMap(typeName) != null;
    }

    protected boolean sourceCompilationUnitIsAvailable(String typeName) {
      return compiledClassesByTypeName.containsKey(typeName);
    }
  }

  private class UnifyVisitor extends JModVisitor {

    private JMethod currentMethod;

    @Override
    public void endVisit(JArrayType x, Context ctx) {
      assert false : "Should not get here";
    }

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      // Concat ops need to resolve string type.
      x.setType(translate(x.getType().getUnderlyingType()));
    }

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      x.resolve(translate(x.getCastType()));
    }

    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      JType refType = translate(x.getRefType());
      x.resolve(refType);

      // ImplementClassLiteralsAsFields: rescue enumType.values()/valueOf().
      if (refType instanceof JArrayType) {
        refType = ((JArrayType) refType).getLeafType();
      }

      JEnumType enumType = refType.isEnumOrSubclass();
      if (enumType == null) {
        return;
      }
      for (JMethod method : enumType.getMethods()) {
        if (!method.isStatic()) {
          continue;
        }
        if (method.getSignature().startsWith("values()") ||
            method.getSignature().startsWith("valueOf(Ljava/lang/String;)")) {
          flowInto(method);
        }
      }
    }

    @Override
    public void endVisit(JClassType x, Context ctx) {
      assert false : "Should not get here";
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JConstructor x, Context ctx) {
      // Process as method.
      super.endVisit(x, ctx);
      instantiate(x.getEnclosingType());
    }

    @Override
    public void endVisit(JDeclaredType x, Context ctx) {
      assert false : "Should not get here";
    }

    @Override
    public void endVisit(JExpression x, Context ctx) {
      assert !x.getType().isExternal() || errorsFound;
    }

    @Override
    public void endVisit(JExpressionStatement x, Context ctx) {
      if (x.getExpr() instanceof JMethodCall) {
        JMethodCall call = (JMethodCall) x.getExpr();
        JMethod target = call.getTarget();
        if (GWT_DEBUGGER_METHOD_CALLS.contains(target.getQualifiedName())) {
          // We should see all calls here because GWT.debugger() returns void.
          ctx.replaceMe(new JDebuggerStatement(x.getSourceInfo()));
        }
      }
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      assert false : "Should not get here";
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      JField field = translate(x.getField());
      flowInto(field);
      x.resolve(field);
      // Should not have an overridden type at this point.
      assert x.getType() == x.getField().getType();
      assert !x.getEnclosingType().isExternal();
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      x.resolve(translate(x.getTestType()));
    }

    @Override
    public void endVisit(JInterfaceType x, Context ctx) {
      assert false : "Should not get here";
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // Already resolved during visit().
      JMethod target = x.getTarget();
      if (target.isExternal()) {
        assert errorsFound;
        return;
      }
      String targetSignature = target.getQualifiedName();
      if (MAGIC_METHOD_CALLS.contains(targetSignature)) {
        if (GWT_DEBUGGER_METHOD_CALLS.contains(targetSignature)) {
          return; // handled in endVisit for JExpressionStatement
        }
        JExpression result = handleMagicMethodCall(x, targetSignature);
        if (result == null) {
          // Error of some sort.
          result = JNullLiteral.INSTANCE;
        }
        result = this.accept(result);
        ctx.replaceMe(result);
        return;
      }
        // Should not have an overridden type at this point.
      assert x instanceof JNewInstance || x.getType() == target.getType();

      flowInto(target);
    }

    @Override
    public void endVisit(JNameOf x, Context ctx) {
      HasName node = x.getNode();
      if (node instanceof JType) {
        node = translate((JType) node);
      } else if (node instanceof JField) {
        node = translate((JField) node);
      } else if (node instanceof JMethod) {
        node = translate((JMethod) node);
      } else {
        assert false : "Should not get here";
      }
      x.resolve(node, (JClassType) translate(x.getType().getUnderlyingType()));
    }

    @Override
    public void endVisit(JNewArray x, Context ctx) {
      x.setType((JArrayType) translate(x.getArrayType()));
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      JConstructor target = x.getTarget();
      if (target.isExternal()) {
        assert errorsFound;
        return;
      }
      flowInto(target);
    }

    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      endVisit((JFieldRef) x, ctx);
    }

    @Override
    public void endVisit(JsniMethodBody x, Context ctx) {
      JsNestingScope funcScope = (JsNestingScope) x.getFunc().getScope();
      assert funcScope.getParent() == JsRootScope.INSTANCE;
      funcScope.nestInto(jsProgram.getScope());
    }

    @Override
    public void endVisit(JsniMethodRef x, Context ctx) {
      JMethod target = translate(x.getTarget());
      x.resolve(target, program.getJavaScriptObject());
      flowInto(target);
    }

    @Override
    public void endVisit(JsonArray x, Context ctx) {
      x.resolve(translate(x.getType()));
    }

    @Override
    public void endVisit(JStringLiteral x, Context ctx) {
      JClassType stringType = program.getTypeJavaLangString();
      x.resolve(stringType);
      instantiate(stringType);
    }

    @Override
    public void endVisit(JThisRef x, Context ctx) {
      assert !x.getType().isExternal();
    }

    @Override
    public void endVisit(JTryStatement x, Context ctx) {
      // Needs to resolve the Exceptions Types explicitly they are multiple in Java 7 and
      // potentially different from the one in the exception variable.
      for (JTryStatement.CatchClause clause : x.getCatchClauses()) {
        List<JType> types = clause.getTypes();
        for (int i = 0; i <  types.size(); i++) {
          JReferenceType resolvedType = translate((JReferenceType) types.get(i));
          assert resolvedType.replaces(types.get(i));
          types.set(i, resolvedType);
        }
      }
    }

    @Override
    public void endVisit(JVariable x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      // Only visit contents of methods defined in types which are part of this compile. Visit
      // also clinits that are reachable to make sure all the nodes that are needed for
      // propagating compile time constants are available.
      return !program.isReferenceOnly(x.getEnclosingType()) ||
          x == x.getEnclosingType().getClinitMethod();
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      JMethod target = translate(x.getTarget());
      x.resolve(target);
      // Special handling.
      return !MAGIC_METHOD_CALLS.contains(target.getQualifiedName());
    }

    private JExpression handleSystemGetProperty(JMethodCall gwtGetPropertyCall) {
      assert (gwtGetPropertyCall.getArgs().size() == 1 || gwtGetPropertyCall.getArgs().size() == 2);
      JExpression propertyNameExpression = gwtGetPropertyCall.getArgs().get(0);
      boolean defaultVersionCalled = gwtGetPropertyCall.getArgs().size() == 2;
      JExpression defaultValueExpression = defaultVersionCalled ?
          gwtGetPropertyCall.getArgs().get(1) : null;

      if (!(propertyNameExpression instanceof JStringLiteral)) {
        error(gwtGetPropertyCall,
            "Only string constants may be used as property name in System.getProperty()");
        return null;
      }
      String propertyName = ((JStringLiteral) propertyNameExpression).getValue();

      if (!defaultVersionCalled && !isPropertyDefined(propertyName)) {
        error(gwtGetPropertyCall, "Property '" + propertyName + "' is not defined.");
        return null;
      }

      if (isMultivaluedProperty(propertyName)) {
        error(gwtGetPropertyCall,
            "Property '" + propertyName + "' is multivalued. " +
                "Multivalued properties are not supported by System.getProperty().");
        return null;
      }

      if (defaultValueExpression != null) {
        defaultValueExpression = accept(defaultValueExpression);
      }

      return JPermutationDependentValue.createRuntimeProperty(
          program, gwtGetPropertyCall.getSourceInfo(), propertyName, defaultValueExpression);
    }

    private JExpression createRebindExpression(JMethodCall gwtCreateCall) {
      assert (gwtCreateCall.getArgs().size() == 1);
      JExpression arg = gwtCreateCall.getArgs().get(0);
      if (!(arg instanceof JClassLiteral)) {
        error(gwtCreateCall, "Only class literals may be used as arguments to GWT.create()");
        return null;
      }
      JClassLiteral classLiteral = (JClassLiteral) arg;
      if (!(classLiteral.getRefType() instanceof JDeclaredType)) {
        error(gwtCreateCall,
            "Only classes and interfaces may be used as arguments to GWT.create()");
        return null;
      }

      Event event = SpeedTracerLogger.start(CompilerEventType.VISIT_GWT_CREATE,
          "argument", classLiteral.getRefType().getName(),
          "caller", gwtCreateCall.getSourceInfo().getFileName());
      try {
        return createStaticRebindExpression(gwtCreateCall, classLiteral);
      } finally {
        event.end();
      }
    }

    private JExpression createStaticRebindExpression(JMethodCall gwtCreateCall,
        JClassLiteral classLiteral) {
      JDeclaredType type = (JDeclaredType) classLiteral.getRefType();
      String reboundTypeName = type.getName();
      // TODO(stalcup): below a MinimalRebuildCache pattern of "clear cache entries for a type" and
      // "rebuild cache entries for that type" is followed. There is a danger that a compile error
      // could occur between the two stages and leave the cache in an invalid state. Switch to a
      // transactionally safe update pattern like always updating a copy and swapping out the
      // original for the copy at the end of a successful compile.
      if (incrementalCompile) {
        // If this is the first time we've rebound this type during this compile.
        if (reboundTypeNames.add(reboundTypeName)) {
          // The rebinding of this type will accumulate rebound type to input resource associations,
          // but the accumulation should start from scratch, so clear any existing associations that
          // might have been collected in previous compiles.
          minimalRebuildCache.clearReboundTypeAssociations(reboundTypeName);
        }
        minimalRebuildCache.recordRebinderTypeForReboundType(reboundTypeName,
            currentMethod.getEnclosingType().getName());
        rpo.getGeneratorContext().setCurrentRebindBinaryTypeName(reboundTypeName);
      }
      String reqType = BinaryName.toSourceName(reboundTypeName);
      List<String> answers;
      try {
        answers = Lists.newArrayList(rpo.getAllPossibleRebindAnswers(logger, reqType));
        if (incrementalCompile) {
          // Accumulate generated artifacts so that they can be output on recompiles even if no
          // generators are run.
          minimalRebuildCache.addGeneratedArtifacts(rpo.getGeneratorContext().getArtifacts());
        }
        rpo.getGeneratorContext().finish(logger);
        if (incrementalCompile) {
          // There may be more types known to be modified after Generator execution, which would
          // mean the previous stale types calculation was too small. Redo it.
          staleTypeNames =
              minimalRebuildCache.computeAndClearStaleTypesCache(logger, program.typeOracle);
          checkPreambleTypesStillFresh(logger);
          fullFlowIntoRemainingStaleTypes();
        }
      } catch (UnableToCompleteException e) {
        error(gwtCreateCall, "Failed to resolve '" + reqType + "' via deferred binding");
        return null;
      }

      List<JExpression> instantiationExpressions =
          Lists.newArrayListWithCapacity(answers.size());
      for (String answer : answers) {
        JDeclaredType answerType = internalFindType(answer, sourceNameBasedTypeLocator, true);
        if (answerType == null) {
          error(gwtCreateCall, "Rebind result '" + answer + "' could not be found");
          return null;
        }
        if (!(answerType instanceof JClassType)) {
          error(gwtCreateCall, "Rebind result '" + answer + "' must be a class");
          return null;
        }
        if (answerType.isAbstract()) {
          error(gwtCreateCall, "Rebind result '" + answer + "' cannot be abstract");
          return null;
        }
        if (isJso(answerType)) {
          error(gwtCreateCall, "Rebind result '" + answer + "' cannot be a JSO");
          return null;
        }
        JExpression result = JjsUtils
            .createDefaultConstructorInstantiation(gwtCreateCall.getSourceInfo(),
                (JClassType) answerType);
        if (result == null) {
          error(gwtCreateCall,
              "Rebind result '" + answer + "' has no default (zero argument) constructors");
          return null;
        }
        instantiationExpressions.add(result);
      }
      assert answers.size() == instantiationExpressions.size();
      if (answers.size() == 1) {
        return instantiationExpressions.get(0);
      }
      return JPermutationDependentValue
          .createTypeRebind(program, gwtCreateCall.getSourceInfo(), reqType,
              answers, instantiationExpressions);
    }

    private JExpression handleImplNameOf(final JMethodCall x) {
      assert (x.getArgs().size() == 1);
      JExpression arg = x.getArgs().get(0);
      if (!(arg instanceof JStringLiteral)) {
        error(x, "Only string literals may be used as arguments to Impl.getNameOf()");
        return null;
      }
      JStringLiteral stringLiteral = (JStringLiteral) arg;
      String stringValue = stringLiteral.getValue();
      JNode node = null;

      JsniRef ref = JsniRef.parse(stringValue);
      if (ref != null) {
        node = JsniRefLookup.findJsniRefTarget(ref, program, new JsniRefLookup.ErrorReporter() {
          @Override
          public void reportError(String errMsg) {
            error(x, errMsg);
          }
        });
      }
      if (node == null) {
        // Not found, must be null
        return null;
      }

      if (node instanceof JMethod) {
        flowInto((JMethod) node);
        program.addPinnedMethod((JMethod) node);
      }
      return new JNameOf(x.getSourceInfo(), program.getTypeJavaLangString(), (HasName) node);
    }

    private JExpression handleMagicMethodCall(JMethodCall x, String targetSignature) {
      switch (targetSignature) {
        case GWT_CREATE:
        case OLD_GWT_CREATE:
          return createRebindExpression(x);
        case IMPL_GET_NAME_OF:
          return handleImplNameOf(x);
        case SYSTEM_GET_PROPERTY:
        case SYSTEM_GET_PROPERTY_WITH_DEFAULT:
          return handleSystemGetProperty(x);
      }
      throw new InternalCompilerException("Unknown magic method");
    }
  }

  private boolean isMultivaluedProperty(String propertyName) {
    Property property = compilerContext.getModule().getProperties().find(propertyName);
    if (!(property instanceof ConfigurationProperty)) {
      return false;
    }

    return ((ConfigurationProperty) property).allowsMultipleValues();
  }

  private boolean isPropertyDefined(String propertyName) {
    return compilerContext.getModule().getProperties().find(propertyName) != null;
  }

  private static final String CLASS_DESIRED_ASSERTION_STATUS =
      "java.lang.Class.desiredAssertionStatus()Z";

  private static final String CLASS_IS_CLASS_METADATA_ENABLED =
      "java.lang.Class.isClassMetadataEnabled()Z";

  public static final String GWT_CREATE =
      "com.google.gwt.core.shared.GWT.create(Ljava/lang/Class;)Ljava/lang/Object;";

  public static final String SYSTEM_GET_PROPERTY =
      "java.lang.System.getProperty(Ljava/lang/String;)Ljava/lang/String;";

  public static final String SYSTEM_GET_PROPERTY_WITH_DEFAULT =
      "java.lang.System.getProperty(Ljava/lang/String;Ljava/lang/String;)" +
          "Ljava/lang/String;";

  private static final String GWT_DEBUGGER_SHARED = "com.google.gwt.core.shared.GWT.debugger()V";

  private static final String GWT_DEBUGGER_CLIENT = "com.google.gwt.core.client.GWT.debugger()V";

  private static final String GWT_IS_CLIENT = "com.google.gwt.core.shared.GWT.isClient()Z";

  private static final String GWT_IS_PROD_MODE = "com.google.gwt.core.shared.GWT.isProdMode()Z";

  private static final String GWT_IS_SCRIPT = "com.google.gwt.core.shared.GWT.isScript()Z";

  private static final String IMPL_GET_NAME_OF =
      "com.google.gwt.core.client.impl.Impl.getNameOf(Ljava/lang/String;)Ljava/lang/String;";

  public static final String OLD_GWT_CREATE =
      "com.google.gwt.core.client.GWT.create(Ljava/lang/Class;)Ljava/lang/Object;";

  private static final String OLD_GWT_IS_CLIENT = "com.google.gwt.core.client.GWT.isClient()Z";

  private static final String OLD_GWT_IS_PROD_MODE = "com.google.gwt.core.client.GWT.isProdMode()Z";

  private static final String OLD_GWT_IS_SCRIPT = "com.google.gwt.core.client.GWT.isScript()Z";

  /**
   * Methods for which the call site must be replaced with magic AST nodes.
   */
  private static final Set<String> GWT_DEBUGGER_METHOD_CALLS =
      Sets.newLinkedHashSet(Arrays.asList(GWT_DEBUGGER_SHARED, GWT_DEBUGGER_CLIENT));

  /**
   * Methods for which the call site must be replaced with magic AST nodes.
   */
  private static final Set<String> MAGIC_METHOD_CALLS = Sets.newLinkedHashSet(Arrays.asList(
      GWT_CREATE, GWT_DEBUGGER_SHARED, GWT_DEBUGGER_CLIENT, SYSTEM_GET_PROPERTY,
      SYSTEM_GET_PROPERTY_WITH_DEFAULT,
      OLD_GWT_CREATE, IMPL_GET_NAME_OF));

  /**
   * Methods with magic implementations that the compiler must insert.
   */
  private static final Set<String> MAGIC_METHOD_IMPLS = Sets.newLinkedHashSet(Arrays.asList(
      GWT_IS_CLIENT, OLD_GWT_IS_CLIENT, GWT_IS_PROD_MODE, OLD_GWT_IS_PROD_MODE, GWT_IS_SCRIPT,
      OLD_GWT_IS_SCRIPT, CLASS_DESIRED_ASSERTION_STATUS, CLASS_IS_CLASS_METADATA_ENABLED));

  private final CompilationState compilationState;
  private final Map<String, CompiledClass> compiledClassesByInternalName;
  private final Map<String, CompiledClass> compiledClassesBySourceName;
  /**
   * JVisitor interferes with any exceptions thrown inside of a visitor traversal call tree so any
   * time UnifyAst wants to log an error and end operation care it should be done by manually
   * logging an error line and setting errorsFound to true. Adequate checking is already in place to
   * interpret this as ending further exploration and errorsFound = true is already being converted
   * to an UnableToCompleteException at the UnifyAst public function boundaries
   */
  private boolean errorsFound = false;
  private final Set<CompilationUnit> failedUnits = Sets.newIdentityHashSet();
  private final Map<String, JField> fieldMap = Maps.newHashMap();

  /**
   * The set of types currently known to be instantiable. Like
   * {@link ControlFlowAnalyzer#instantiatedTypes}.
   */
  private final Set<JDeclaredType> instantiatedTypes = Sets.newIdentityHashSet();

  private final JsProgram jsProgram;

  /**
   * Fields and methods that are referenceable. Like
   * {@link ControlFlowAnalyzer#liveFieldsAndMethods}.
   */
  private final Set<JNode> liveFieldsAndMethods = Sets.newIdentityHashSet();

  /**
   * Types which have had all of their fields and methods resolved (as opposed to the default
   * behavior of only resolving the reachable ones). Currently only used when performing per-file
   * compilation/recompilation.
   */
  private final Set<String> fullFlowTypes = Sets.newHashSet();

  private final TreeLogger logger;
  private final CompilerContext compilerContext;
  private final Map<String, JMethod> methodMap = Maps.newHashMap();
  private final JProgram program;
  private final RebindPermutationOracle rpo;
  private final Set<String> reboundTypeNames = Sets.newHashSet();

  /**
   * The names of types whose per-file compilation cached Js and StatementRanges are known to no
   * longer be valid.
   * <p>
   * Is initialized to the full initial list at the beginning of exec() and may be recalculated
   * (larger) after Generator executions reveal more modified types.
   */
  private Set<String> staleTypeNames = Sets.newHashSet();

  /**
   * The names of stale types that have been processed (fully traversed) so far.
   */
  private Set<String> processedStaleTypeNames = Sets.newHashSet();

  /**
   * A work queue of methods whose bodies we need to traverse. Prevents
   * excessive stack use.
   */
  private final Queue<JMethod> todo = Lists.newLinkedList();

  private final Set<String> virtualMethodsLive = Sets.newHashSet();
  private final Multimap<String, JMethod> virtualMethodsPending = LinkedListMultimap.create();

  private NameBasedTypeLocator sourceNameBasedTypeLocator;
  private NameBasedTypeLocator binaryNameBasedTypeLocator;
  private NameBasedTypeLocator internalNameBasedTypeLocator;

  private MinimalRebuildCache minimalRebuildCache;
  private boolean incrementalCompile;
  private final List<String> rootTypeSourceNames = Lists.newArrayList();

  public UnifyAst(TreeLogger logger, CompilerContext compilerContext, JProgram program,
      JsProgram jsProgram, PrecompilationContext precompilationContext) {

    this.incrementalCompile = compilerContext.getOptions().isIncrementalCompileEnabled();

    this.logger = logger;
    this.compilerContext = compilerContext;
    this.program = program;
    this.jsProgram = jsProgram;
    this.rpo = precompilationContext.getRebindPermutationOracle();
    this.compilationState = rpo.getCompilationState();
    this.compiledClassesByInternalName = compilationState.getClassFileMap();
    this.compiledClassesBySourceName = compilationState.getClassFileMapBySource();
    initializeNameBasedLocators();
    this.minimalRebuildCache = compilerContext.getMinimalRebuildCache();
    if (incrementalCompile) {
      this.staleTypeNames =
          minimalRebuildCache.computeAndClearStaleTypesCache(logger, program.typeOracle);
      checkPreambleTypesStillFresh(logger);
    }
  }

  public void addRootTypes(Collection<String> rootTypeSourceNames) {
    assert this.rootTypeSourceNames.isEmpty();
    this.rootTypeSourceNames.addAll(rootTypeSourceNames);
  }

  /**
   * Special AST construction, useful for tests. Everything is resolved,
   * translated, and unified.
   */
  public void buildEverything() throws UnableToCompleteException {
    for (String internalName : compiledClassesByInternalName.keySet()) {
      String typeName = InternalName.toBinaryName(internalName);
      internalFindType(typeName, binaryNameBasedTypeLocator, true);
    }

    for (JDeclaredType type : program.getDeclaredTypes()) {
      fullFlowIntoType(type);
    }

    mainLoop();

    computeOverrides();
    if (errorsFound) {
      throw new UnableToCompleteException();
    }
  }

  /**
   * Translates and stitches (unifies) type ASTs into one connected graph.<br />
   *
   * Only types reachable from entry points are traversed. This speeds, saves memory trims
   * unreferenced elements.
   */
  public void exec() throws UnableToCompleteException {
    // Trace execution from entry points and resolve references.
    List<String> entryMethodNames = Lists.newArrayList();
    for (JMethod entryMethod : program.getEntryMethods()) {
      flowInto(entryMethod);
      entryMethodNames.add(entryMethod.getJsniSignature(true, true));
    }

    // Ensure that root types are loaded and possibly (depending on mode) traversed.
    List<String> rootTypeBinaryNames = Lists.newArrayList();
    for (String rootTypeSourceName : rootTypeSourceNames) {
      JDeclaredType rootType =
          internalFindType(rootTypeSourceName, sourceNameBasedTypeLocator, true);
      if (rootType == null) {
        continue;
      }

      rootTypeBinaryNames.add(rootType.getName());
      if (rootType.hasJsInteropEntryPoints()) {
        fullFlowIntoType(rootType);
      }
    }
    minimalRebuildCache.setRootTypeNames(rootTypeBinaryNames);
    minimalRebuildCache.setEntryMethodNames(entryMethodNames);

    // Some fields and methods in codegen types might only become referenced as the result of
    // visitor execution after unification. Since we don't want those fields are methods to be
    // prematurely pruned here we defensively trace them now.
    for (JClassType type : program.codeGenTypes) {
      for (JMethod method : type.getMethods()) {
        flowInto(method);
      }
      for (JField field : type.getFields()) {
        flowInto(field);
      }
    }

    if (incrementalCompile) {
      fullFlowIntoRemainingStaleTypes();
    }

    /*
     * Since we're not actually optimizing here, it's easier to just visit
     * certain things up front instead of duplicating the exacting semantics of
     * ControlFlowAnalyzer.
     */
    // String literals.
    instantiate(program.getTypeJavaLangString());
    // ControlFlowAnalyzer.rescueByConcat().
    flowInto(program.getIndexedMethod(RuntimeConstants.OBJECT_TO_STRING));
    mapApi(program.getTypeJavaLangString());
    flowInto(methodMap.get("java.lang.String.valueOf(C)Ljava/lang/String;"));

    // FixAssignmentsToUnboxOrCast
    AutoboxUtils autoboxUtils = new AutoboxUtils(program);
    for (JMethod method : autoboxUtils.getBoxMethods()) {
      flowInto(method);
    }
    for (JMethod method : autoboxUtils.getUnboxMethods()) {
      flowInto(method);
    }

    // ReplaceRunAsyncs
    if (compilerContext.getOptions().isRunAsyncEnabled()) {
      flowInto(program.getIndexedMethod(RuntimeConstants.ASYNC_FRAGMENT_LOADER_ON_LOAD));
      flowInto(program.getIndexedMethod(RuntimeConstants.ASYNC_FRAGMENT_LOADER_RUN_ASYNC));
    }

    // ImplementClassLiteralsAsFields
    staticInitialize(program.getTypeClassLiteralHolder());
    for (JMethod method : program.getTypeJavaLangClass().getMethods()) {
      if (method.isStatic() && method.getName().startsWith("createFor")) {
        flowInto(method);
      }
    }

    mainLoop();

    if (incrementalCompile) {
      int declaredTypesInModule = program.getModuleDeclaredTypes().size();

      MetricName.DECLARED_TYPES_IN_MODULE.setAmount(logger, declaredTypesInModule);

      logger.log(TreeLogger.INFO, "Unification traversed " + liveFieldsAndMethods.size()
          + " fields and methods and " + program.getDeclaredTypes().size() + " types. "
          + declaredTypesInModule
          + " are considered part of the current module and " + fullFlowTypes.size()
          + " had all of their fields and methods traversed.");

      Set<String> remainingStaleTypeNames = computeRemainingStaleTypeNames();
      if (!remainingStaleTypeNames.isEmpty()) {
        logger.log(TreeLogger.WARN, "Some stale types (" + remainingStaleTypeNames
            + ") were not reprocessed as was expected. This is either a compiler bug or a "
            + "Generator has legitimately stopped creating these types.");
      }
      // Record the list of names of stale types that were processed, for test assertion purposes.
      minimalRebuildCache.setProcessedStaleTypeNames(fullFlowTypes);
    }

    // Compute overrides before pruning, otherwise if a parent class method is pruned an overriding
    // child class method might not look like an override.
    List<JMethod> newStubMethods = computeOverrides();

    // Make sure the created methods have the right liveness computation and don't get incorrectly
    // pruned.
    for (JMethod method : newStubMethods) {
      if (instantiatedTypes.contains(method.getEnclosingType()) &&
          virtualMethodsLive.contains(method.getSignature())) {
        liveFieldsAndMethods.add(method);
      }
    }

    if (!incrementalCompile) {
      // Post-stitching clean-ups.
      pruneDeadFieldsAndMethods();
    }
    if (errorsFound) {
      // Already logged.
      throw new UnableToCompleteException();
    }
  }

  /**
   * Attempts to eagerly load and traverse all remaining known-stale types.
   * <p>
   * Some types may not exist till after some Generator execution so missing types will be
   * temporarily ignored.
   */
  private void fullFlowIntoRemainingStaleTypes() {
    for (String staleTypeName : computeRemainingStaleTypeNames()) {
      JDeclaredType staleType =
          internalFindType(staleTypeName, binaryNameBasedTypeLocator, false);
      if (staleType == null) {
        // The type is Generator output and so is not usually available in the list of types
        // provided from initial JDT compilation. The staleness marking process has already
        // handled this type by cascading the staleness marking onto the types that contain the
        // GWT.create() calls that process that create this type.
        continue;
      }
      // It's possible that the type was previously loaded before it was discovered to be stale (it
      // became stale as a result of a Generator execution). If this happens then the type will have
      // already been marked "reference only" in JProgram. This needs to be undone.
      program.removeReferenceOnlyType(staleType);

      // Make sure that the entire type is traversed.
      fullFlowIntoType(staleType);
    }
  }

  private void pruneDeadFieldsAndMethods() {
    assert !incrementalCompile;
    for (JDeclaredType type : program.getDeclaredTypes()) {
      // Remove dead fields.
      for (int fieldIndex = 0; fieldIndex < type.getFields().size(); ++fieldIndex) {
        JField field = type.getFields().get(fieldIndex);
        if (!liveFieldsAndMethods.contains(field)) {
          type.removeField(fieldIndex);
          --fieldIndex;
        }
      }

      // Empty the body of dead clinits.
      JMethod clinit = type.getClinitMethod();
      if (!liveFieldsAndMethods.contains(clinit)) {
        clinit.setBody(new JMethodBody(SourceOrigin.UNKNOWN));
      }

      // Remove dead methods, but never remove clinit.
      for (int methodIndex = 1; methodIndex < type.getMethods().size(); ++methodIndex) {
        JMethod method = type.getMethods().get(methodIndex);
        // Pruning dead methods from the override list can only be done accurately in
        // non-incremental compiles because of differences in which types are loaded and thus
        // which methods are considered live.
        Iterables.removeIf(method.getOverriddenMethods(),
            Predicates.not(Predicates.in(liveFieldsAndMethods)));
        Iterables.removeIf(method.getOverridingMethods(),
            Predicates.not(Predicates.in(liveFieldsAndMethods)));
        if (!liveFieldsAndMethods.contains(method)) {
          type.removeMethod(methodIndex);
          --methodIndex;
        }
      }
    }
  }

  private void assimilateSourceUnit(CompilationUnit unit, boolean reportErrors) {
    if (unit.isError()) {
      if (failedUnits.add(unit) && reportErrors) {
        CompilationProblemReporter.reportErrors(logger, unit, false);
        CompilationProblemReporter.logErrorTrace(logger, TreeLogger.ERROR,
            compilerContext, unit.getTypeName(), true);
        errorsFound = true;
      }
      return;
    }
    // Staleness calculations need to be able to trace from CompilationUnit name to the names of
    // immediately nested types. So record those associations now.
    if (incrementalCompile) {
      compilerContext.getMinimalRebuildCache().recordNestedTypeNamesPerType(unit);
    }
    // TODO(zundel): ask for a recompile if deserialization fails?
    List<JDeclaredType> types = unit.getTypes();
    assert containsAllTypes(unit, types);
    for (JDeclaredType t : types) {
      program.addType(t);
      // If we're compiling per file and we already have currently valid output for this type.
      if (incrementalCompile && !needsNewJs(t)) {
        // Then make sure we don't output new Js for this type.
        program.addReferenceOnlyType(t);
      }
    }
    for (JDeclaredType t : types) {
      resolveType(t);
    }
    // When compiling per file.
    if (incrementalCompile) {
      // It's possible that a users' edits have made a type referenceable that was not previously
      // referenceable.
      for (JDeclaredType type : types) {
        // Such a type won't have any cached JS and will need a full traversal to ensure it is
        // output (the full type with all fields and methods) as new JS.
        if (needsNewJs(type)) {
          fullFlowIntoType(type);
        }
      }
    }

    for (JDeclaredType type : types) {
      /*
       * Eagerly instantiate any type that requires devirtualization, i.e. String and
       * JavaScriptObject subtypes. That way we don't have to copy the exact semantics of
       * ControlFlowAnalyzer.
       */
      if (requiresDevirtualization(type)) {
        instantiate(type);
      }

      /*
       * We also flow into the types with JsInterop entry point because our first pass on root types
       * with JsInterop entry points are missing these inner classes. For native types this ensures
       * that the constructor is considered reachable as it might be needed later for instanceof
       * and casts.
       */
      if (type.hasJsInteropEntryPoints() || type.isJsNative()) {
        fullFlowIntoType(type);
      }
    }
  }

  /**
   * Ensure that if any preamble types have become stale then adequate steps are taken to ensure the
   * recreation of the entire preamble chunk.
   */
  private void checkPreambleTypesStillFresh(TreeLogger logger) {
    SetView<String> stalePreambleTypes =
        Sets.intersection(staleTypeNames, minimalRebuildCache.getPreambleTypeNames());
    if (!stalePreambleTypes.isEmpty()) {
      // Stale preamble types can't be gracefully replaced. We need to clear all per-file compile
      // related caches to force a full build.
      logger.log(TreeLogger.WARN,
          "Some preamble types became stale. Recreating them is forcing a full "
          + "recompile. Stale preamble types: " + stalePreambleTypes + ".");
      minimalRebuildCache.clearPerTypeJsCache();
      staleTypeNames.clear();
      // TODO: might be able to preserve the cache of all non-stale and non-preamble types.
    }
  }

  /**
    * Compute all overrides.
    */
   private List<JMethod> computeOverrides() {
     return new ComputeOverridesAndImplementDefaultMethods().exec(program);
   }

  private Set<String> computeRemainingStaleTypeNames() {
    return Sets.newHashSet(Sets.difference(staleTypeNames, processedStaleTypeNames));
  }

  private boolean containsAllTypes(CompilationUnit unit, List<JDeclaredType> types) {
    Set<String> binaryTypeNames = Sets.newHashSet();
    for (JDeclaredType type : types) {
      binaryTypeNames.add(type.getName());
    }
    for (CompiledClass cc : unit.getCompiledClasses()) {
      if (!binaryTypeNames.contains(InternalName.toBinaryName(cc.getInternalName()))) {
        return false;
      }
    }
    return true;
  }

  private void error(JNode x, String errorMessage) {
    errorsFound = true;
    TreeLogger branch =
        logger
            .branch(TreeLogger.ERROR, "Errors in '" + x.getSourceInfo().getFileName() + "'", null);
    // Append 'Line #: msg' to the error message.
    StringBuilder msgBuf = new StringBuilder();
    int line = x.getSourceInfo().getStartLine();
    if (line > 0) {
      msgBuf.append("Line ");
      msgBuf.append(line);
      msgBuf.append(": ");
    }
    msgBuf.append(errorMessage);
    branch.log(TreeLogger.ERROR, msgBuf.toString());
  }

  /**
   * Resolves all fields and methods in the given type and marks it instantiable.
   * <p>
   * The net effect is to ensure the entire type is kept and inserted into the unified AST.
   */
  private void fullFlowIntoType(JDeclaredType type) {
    String typeName = type.getName();
    if (fullFlowTypes.contains(typeName) || typeName.endsWith("package-info")) {
      return;
    }
    // The traversal of this type will accumulate rebinder type to rebound type associations, but
    // the accumulation should start from scratch, so clear any existing associations that might
    // have been collected in previous compiles.
    minimalRebuildCache.clearRebinderTypeAssociations(typeName);
    fullFlowTypes.add(typeName);
    // Remove the type from the remaining stale types set so that the fullFlowIntoStaleTypes()
    // attempt is shorter.
    processedStaleTypeNames.add(typeName);
    instantiate(type);
    for (JField field : type.getFields()) {
      flowInto(field);
    }
    for (JMethod method : type.getMethods()) {
      flowInto(method);
    }
  }

  private void flowInto(JField field) {
    if (field.isExternal()) {
      assert errorsFound;
      return;
    }
    if (field == JField.NULL_FIELD) {
      return;
    }
    if (liveFieldsAndMethods.contains(field)) {
      // already flown into.
      return;
    }
    liveFieldsAndMethods.add(field);
    field.setType(translate(field.getType()));
    if (field.isStatic()) {
      staticInitialize(field.getEnclosingType());
    }
  }

  private void flowInto(JMethod method) {
    if (method.isExternal()) {
      assert errorsFound;
      return;
    }
    if (method == JMethod.NULL_METHOD) {
      return;
    }
    if (liveFieldsAndMethods.contains(method)) {
      return;
    }

    liveFieldsAndMethods.add(method);
    JType originalReturnType = translate(method.getOriginalReturnType());
    List<JType> originalParamTypes =
        Lists.newArrayListWithCapacity(method.getOriginalParamTypes().size());
    for (JType originalParamType : method.getOriginalParamTypes()) {
      originalParamTypes.add(translate(originalParamType));
    }
    JType returnType = translate(method.getType());
    List<JClassType> thrownExceptions =
        Lists.newArrayListWithCapacity(method.getThrownExceptions().size());
    for (JClassType thrownException : method.getThrownExceptions()) {
      thrownExceptions.add(translate(thrownException));
    }
    method.resolve(originalReturnType, originalParamTypes, returnType, thrownExceptions);
    if (method.isStatic()) {
      staticInitialize(method.getEnclosingType());
    } else if (method.canBePolymorphic()) {
      String signature = method.getSignature();
      if (!virtualMethodsLive.contains(signature)) {
        virtualMethodsLive.add(signature);
        Iterable<JMethod> pending = virtualMethodsPending.removeAll(signature);
        for (JMethod p : pending) {
          assert instantiatedTypes.contains(p.getEnclosingType());
          flowInto(p);
        }
      }
    }
    resolveSpecialization(method);

    // Queue up visit / resolve on the body.
    todo.add(method);
  }

  private void resolveSpecialization(JMethod method) {
    // TODO (cromwellian): Move to GwtAstBuilder eventually
    if (method.getSpecialization() == null) {
      return;
    }
    Specialization specialization = method.getSpecialization();
    List<JType> resolvedParams = Lists.newArrayList();
    if (specialization.getParams() == null) {
      logger.log(Type.ERROR, "Missing 'params' attribute at @SpecializeMethod for method "
          + method.getQualifiedName());
      errorsFound = true;
      return;
    }

    for (JType param : specialization.getParams()) {
      resolvedParams.add(translate(param));
    }
    JType resolvedReturn = translate(specialization.getReturns());

    String targetMethodSignature = JjsUtils.computeSignature(
        specialization.getTarget(), resolvedParams, resolvedReturn, false);

    JMethod targetMethod = translate(JMethod.getExternalizedMethod(
        method.getEnclosingType().getName(), targetMethodSignature, false));

    if (targetMethod == null) {
      errorsFound = true;
      logger.log(Type.ERROR, "Unable to locate @SpecializeMethod target "
          + targetMethodSignature + " for method " + method.getQualifiedName());
      return;
    }

    flowInto(targetMethod);
    specialization.resolve(resolvedParams, resolvedReturn, targetMethod);
  }

  public NameBasedTypeLocator getSourceNameBasedTypeLocator() {
    return sourceNameBasedTypeLocator;
  }

  private void initializeNameBasedLocators() {
    sourceNameBasedTypeLocator = new NameBasedTypeLocator(compiledClassesBySourceName) {
      @Override
      protected boolean hasCompileErrors(String sourceName) {
        return compilerContext.getCompilationErrorsIndex().hasCompileErrors(sourceName);
      }

      @Override
      protected void logErrorTrace(TreeLogger branch, Type logLevel, String sourceName) {
        CompilationProblemReporter.logErrorTrace(branch, logLevel, compilerContext, sourceName,
            false);
      }
    };
    binaryNameBasedTypeLocator = new NameBasedTypeLocator(null) {
      @Override
      protected CompilationUnit getCompilationUnitFromSource(String binaryName) {
        // There is no binary name based index for this, use the internal name based one instead.
        return internalNameBasedTypeLocator.getCompilationUnitFromSource(
            BinaryName.toInternalName(binaryName));
      }

      @Override
      protected boolean sourceCompilationUnitIsAvailable(String binaryName) {
        // There is no binary name based index for this, use the internal name based one instead.
        return internalNameBasedTypeLocator.sourceCompilationUnitIsAvailable(
            BinaryName.toInternalName(binaryName));
      }

      @Override
      protected boolean hasCompileErrors(String binaryName) {
        return sourceNameBasedTypeLocator.hasCompileErrors(
            BinaryName.toSourceName(binaryName));
      }

      @Override
      protected void logErrorTrace(TreeLogger branch, Type logLevel, String binaryName) {
        sourceNameBasedTypeLocator.logErrorTrace(branch, logLevel,
            BinaryName.toSourceName(binaryName));
      }
    };
    internalNameBasedTypeLocator = new NameBasedTypeLocator(compiledClassesByInternalName) {
      @Override
      protected JDeclaredType getResolvedType(String internalName) {
        // There is no internal name based index for this, use the binary name based one instead.
        return binaryNameBasedTypeLocator.getResolvedType(InternalName.toBinaryName(internalName));
      }

      @Override
      protected boolean resolvedTypeIsAvailable(String internalName) {
        // There is no internal name based index for this, use the binary name based one instead.
        return binaryNameBasedTypeLocator.resolvedTypeIsAvailable(
            InternalName.toBinaryName(internalName));
      }

      @Override
      protected boolean hasCompileErrors(String internalName) {
        return sourceNameBasedTypeLocator.hasCompileErrors(
            InternalName.toSourceName(internalName));
      }

      @Override
      protected void logErrorTrace(TreeLogger branch, Type logLevel, String internalName) {
        sourceNameBasedTypeLocator.logErrorTrace(branch, logLevel,
            BinaryName.toSourceName(internalName));
      }
    };
  }

  private void instantiate(JDeclaredType type) {
    // Don't flow into all the parts of types defined outside this compile; except when the type is
    // requires devirtualization (JSOs, Strings, etc) in which case the original (non devirtualized)
    // methods may not be reachable anymore.
    if (program.isReferenceOnly(type) && !requiresDevirtualization(type)) {
      return;
    }
    if (type.isExternal()) {
      assert errorsFound;
      return;
    }

    if (instantiatedTypes.contains(type)) {
      return;
    }
    instantiatedTypes.add(type);
    if (type.getSuperClass() != null) {
      instantiate(translate(type.getSuperClass()));
    }
    for (JInterfaceType intf : type.getImplements()) {
      instantiate(translate(intf));
    }
    staticInitialize(type);

    // Flow into any reachable virtual methods.
    for (JMethod method : type.getMethods()) {
      if (method.canBeReferencedExternally()) {
        flowInto(method);
        continue;
      }
      if (!method.canBePolymorphic()) {
        continue;
      }

      String signature = method.getSignature();
      if (virtualMethodsLive.contains(signature)) {
        assert !virtualMethodsPending.containsKey(signature);
        flowInto(method);
      } else {
        virtualMethodsPending.put(signature, method);
      }
    }

    for (JField field : type.getFields()) {
      if (field.canBeReferencedExternally()) {
        flowInto(field);
      }
    }
  }

  private boolean requiresDevirtualization(JDeclaredType type) {
    // NOTE: these types are the ones {@link Devirtualizer} handles.
    return isJso(type) || type.isJsNative() ||
        // Use the version that takes names instead of instances as some
        // relevant instances might have not been leaded yet.
        JProgram.isRepresentedAsNative(type.getName());
  }

  private boolean isJso(JDeclaredType type) {
    if (type == null) {
      return false;
    }
    return type == program.getJavaScriptObject() || isJso(type.getSuperClass());
  }

  /**
   * Main loop: run through the queue doing deferred resolution. We could have
   * made this entirely recursive, but a work queue uses much less max stack.
   */
  private void mainLoop() {
    UnifyVisitor visitor = new UnifyVisitor();
    while (!todo.isEmpty()) {
      visitor.accept(todo.poll());
    }
  }

  private void mapApi(JDeclaredType type) {
    assert !type.isExternal();
    for (JField field : type.getFields()) {
      String sig = type.getName() + '.' + field.getSignature();
      fieldMap.put(sig, field);
    }
    for (JMethod method : type.getMethods()) {
      String methodSignature = method.getQualifiedName();
      methodMap.put(methodSignature, method);
      if (!MAGIC_METHOD_IMPLS.contains(methodSignature)) {
        continue;
      }
      if (methodSignature.startsWith("com.google.gwt.core.client.GWT.")
          || methodSignature.startsWith("com.google.gwt.core.shared.GWT.")) {
        // GWT.isClient, GWT.isScript, GWT.isProdMode all true.
        JjsUtils.replaceMethodBody(method, JBooleanLiteral.TRUE);
        continue;
      }
      assert methodSignature.startsWith("java.lang.Class.");
      if (CLASS_DESIRED_ASSERTION_STATUS.equals(methodSignature)) {
        JjsUtils.replaceMethodBody(method,
            JBooleanLiteral.get(compilerContext.getOptions().isEnableAssertions()));
      } else if (CLASS_IS_CLASS_METADATA_ENABLED.equals(methodSignature)) {
        JjsUtils.replaceMethodBody(method,
            JBooleanLiteral.get(!compilerContext.getOptions().isClassMetadataDisabled()));
      } else {
        assert false;
      }
    }
  }

  /**
   * During per file compilation, returns whether the given type has cached JS that can be reused.
   */
  private boolean needsNewJs(JDeclaredType type) {
    String typeName = type.getName();
    boolean hasOwnJs = minimalRebuildCache.hasJs(typeName);
    boolean isPartOfPreamble = minimalRebuildCache.getPreambleTypeNames().contains(typeName);
    return !hasOwnJs && !isPartOfPreamble;
  }

  private void resolveType(JDeclaredType type) {
    assert !type.isExternal();
    if (type.getEnclosingType() != null) {
      type.setEnclosingType(translate(type.getEnclosingType()));
    }
    if (type instanceof JClassType && type.getSuperClass() != null) {
      ((JClassType) type).setSuperClass(translate(type.getSuperClass()));
    }
    List<JInterfaceType> resolvedInterfaces = Lists.newArrayList();
    for (JInterfaceType intf : type.getImplements()) {
      resolvedInterfaces.add((JInterfaceType) translate(intf));
    }
    type.resolve(resolvedInterfaces, findPackageInfo(type));
  }

  private JDeclaredType findPackageInfo(JDeclaredType type) {
    String packagePrefix = type.getName();

    // Package prefix with trailing dot. Empty string if default package.
    packagePrefix = packagePrefix.substring(0,  packagePrefix.lastIndexOf('.') + 1);
    String pkgInfoClassName = StringInterner.get().intern(packagePrefix + "package-info");
    JDeclaredType pkgInfo = internalFindType(pkgInfoClassName, binaryNameBasedTypeLocator, false);
    // package-info classes are loaded only for their package level annotations' possible effect on
    // JsInterop configuration. They are not intended to be included in output.
    if (pkgInfo != null) {
      program.addReferenceOnlyType(pkgInfo);
    }
    return pkgInfo;
  }

  public JDeclaredType findType(String typeName, NameBasedTypeLocator nameBasedTypeLocator)
      throws UnableToCompleteException {
    JDeclaredType type = internalFindType(typeName, nameBasedTypeLocator, true);
    if (errorsFound) {
      // Already logged.
      throw new UnableToCompleteException();
    }
    return type;
  }

  private JDeclaredType internalFindType(String typeName,
      NameBasedTypeLocator nameBasedTypeLocator, boolean reportErrors) {
    if (nameBasedTypeLocator.resolvedTypeIsAvailable(typeName)) {
      // The type was already resolved.
      return nameBasedTypeLocator.getResolvedType(typeName);
    }

    if (nameBasedTypeLocator.sourceCompilationUnitIsAvailable(typeName)) {
      // Resolve from source.
      assimilateSourceUnit(nameBasedTypeLocator.getCompilationUnitFromSource(typeName),
          reportErrors);
      return nameBasedTypeLocator.getResolvedType(typeName);
    }

    if (reportErrors) {
      // The type could not be resolved as source; report the appropriate error.
      if (nameBasedTypeLocator.hasCompileErrors(typeName)) {
        TreeLogger branch = logger.branch(TreeLogger.ERROR, String.format(
            "Type %s could not be referenced because it previously failed to "
            + "compile with errors:", typeName));
        nameBasedTypeLocator.logErrorTrace(branch, TreeLogger.ERROR, typeName);
      } else {
        logger.log(TreeLogger.ERROR, String.format(
            "Could not find %s in types compiled from source. Is the source glob too strict?",
            typeName));
      }
      errorsFound = true;
    }
    return null;
  }

  private void staticInitialize(JDeclaredType type) {
    if (type.isExternal()) {
      assert errorsFound;
      return;
    }
    JMethod clinit = type.getClinitMethod();
    if (!liveFieldsAndMethods.contains(clinit)) {
      flowInto(clinit);
      if (type.getSuperClass() != null) {
        staticInitialize(translate(type.getSuperClass()));
      }
    }
  }

  /**
   * Replaces an external (stub) reference node to a particular class by the actual AST node if
   * necessary.
   */
  private JClassType translate(JClassType type) {
    return (JClassType) translate((JDeclaredType) type);
  }

  /**
   * Replaces an external (stub) reference node to a particular type by the actual AST node if
   * necessary.
   */
  private JDeclaredType translate(JDeclaredType type) {
    if (!type.isExternal()) {
      return type;
    }
    String typeName = type.getName();
    JDeclaredType newType = internalFindType(typeName, binaryNameBasedTypeLocator, true);
    if (newType == null) {
      assert errorsFound;
      return type;
    }
    assert !newType.isExternal();
    return newType;
  }

  /**
   * Replaces an external (stub) reference node to a particular field by the actual AST node if
   * necessary.
   */
  private JField translate(JField field) {
    if (!field.isExternal()) {
      return field;
    }

    JDeclaredType enclosingType = field.getEnclosingType();
    String sig = enclosingType.getName() + '.' + field.getSignature();
    JField newField = fieldMap.get(sig);
    if (newField != null) {
      return newField;
    }

    enclosingType = translate(enclosingType);
    if (enclosingType.isExternal()) {
      assert errorsFound;
      return field;
    }
    mapApi(enclosingType);

    // Now the field should be there.
    field = fieldMap.get(sig);
    if (field == null) {
      // TODO: error logging
      throw new NoSuchFieldError(sig);
    }

    assert !field.isExternal();
    return field;
  }

  /**
   * Replaces an external (stub) reference node to a particular method by the actual AST node if
   * necessary.
   */
  private JMethod translate(JMethod method) {
    if (!method.isExternal()) {
      return method;
    }

    String sig = method.getQualifiedName();
    JMethod newMethod = methodMap.get(sig);
    if (newMethod != null) {
      return newMethod;
    }

    JDeclaredType enclosingType = translate(method.getEnclosingType());
    if (enclosingType.isExternal()) {
      assert errorsFound;
      return method;
    }
    mapApi(enclosingType);

    // Now the method should be there.
    method = methodMap.get(sig);
    if (method == null) {
      // TODO: error logging
      throw new NoSuchMethodError(sig);
    }
    assert !method.isExternal();
    return method;
  }

  /**
   * Replaces an external (stub) reference node to a particular type by the actual AST node if
   * necessary.
   */
  private JReferenceType translate(JReferenceType type) {
    JReferenceType result = type.getUnderlyingType();

    if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      result = program.getTypeArray(translate(arrayType.getElementType()));
    } else  if (type.isExternal()) {
      assert type instanceof JDeclaredType : "Unknown external type " + type.getName();
      result = translate((JDeclaredType) type);
    }
    assert !result.isExternal();

    if (!type.canBeNull()) {
      result = result.strengthenToNonNull();
    }

    return result;
  }

  private JType translate(JType type) {
    if (type.isPrimitiveType()) {
      return type;
    }
    return translate((JReferenceType) type);
  }
}
