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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompilerContext;
import com.google.gwt.dev.javac.CompilationProblemReporter;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.CompiledClass;
import com.google.gwt.dev.javac.Shared;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBlock;
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
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNameOf;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.js.JDebuggerStatement;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.js.ast.JsNestingScope;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.collect.IdentityHashSet;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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

    protected abstract CompilationUnit getCompilationUnitFromLibrary(String typeName);

    protected CompilationUnit getCompilationUnitFromSource(String typeName) {
      return compiledClassesByTypeName.get(typeName).getUnit();
    }

    protected JDeclaredType getResolvedType(String typeName) {
      JDeclaredType resolvedType = program.getFromTypeMap(typeName);
      assert resolvedType != null || errorsFound;
      return resolvedType;
    }

    protected boolean libraryCompilationUnitIsAvailable(String typeName) {
      return getCompilationUnitFromLibrary(typeName) != null;
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
      x.setType(translate(x.getType()));
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
        JType leafType = ((JArrayType) refType).getLeafType();
        if (leafType instanceof JReferenceType) {
          refType = leafType;
        }
      }
      if (refType instanceof JClassType) {
        JClassType classType = (JClassType) refType;
        JEnumType enumType = classType.isEnumOrSubclass();
        if (enumType != null) {
          for (JMethod method : enumType.getMethods()) {
            if (method.isStatic()) {
              if (method.getSignature().startsWith("values()")) {
                flowInto(method);
              } else if (method.getSignature().startsWith("valueOf(Ljava/lang/String;)")) {
                flowInto(method);
              }
            }
          }
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
        if (GWT_DEBUGGER_METHOD_CALLS.contains(getMethodTypeSignature(target))) {
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
      String targetSignature = getMethodTypeSignature(target);
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
      if (!(x instanceof JNewInstance)) {
        // Should not have an overridden type at this point.
        assert x.getType() == target.getType();
      }

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
      flowInto(x.getTarget());
      assert !x.getEnclosingType().isExternal();
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
    public void endVisit(JsonObject x, Context ctx) {
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
      // Only visit contents of methods defined in types which are part of this compile.
      return !program.isReferenceOnly(x.getEnclosingType());
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      JMethod target = translate(x.getTarget());
      x.resolve(target);
      // Special handling.
      return !MAGIC_METHOD_CALLS.contains(getMethodTypeSignature(target));
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

      if (compilerContext.shouldCompileMonolithic()) {
        return createStaticRebindExpression(gwtCreateCall, classLiteral);
      } else {
        return createRuntimeRebindExpression(gwtCreateCall, classLiteral);
      }
    }

    private JExpression createRuntimeRebindExpression(JMethodCall gwtCreateCall,
        JClassLiteral classLiteral) {
      // RuntimeRebinder.createInstance(classLiteral);
      JMethod runtimeCreateInstanceMethod =
          program.getIndexedMethod("RuntimeRebinder.createInstance");
      return new JMethodCall(gwtCreateCall.getSourceInfo(), null, runtimeCreateInstanceMethod,
          classLiteral);
    }

    private JExpression createStaticRebindExpression(JMethodCall gwtCreateCall,
        JClassLiteral classLiteral) {
      JDeclaredType type = (JDeclaredType) classLiteral.getRefType();
      String reqType = BinaryName.toSourceName(type.getName());
      List<String> answers;
      try {
        answers = Lists.create(rpo.getAllPossibleRebindAnswers(logger, reqType));
        rpo.getGeneratorContext().finish(logger);
      } catch (UnableToCompleteException e) {
        error(gwtCreateCall, "Failed to resolve '" + reqType + "' via deferred binding");
        return null;
      }

      ArrayList<JExpression> instantiationExpressions = new ArrayList<JExpression>(answers.size());
      for (String answer : answers) {
        JDeclaredType answerType = internalFindType(answer, sourceNameBasedTypeLocator);
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
        JExpression result = JGwtCreate.createInstantiationExpression(gwtCreateCall.getSourceInfo(),
            (JClassType) answerType, currentMethod.getEnclosingType());
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
      } else {
        return new JGwtCreate(gwtCreateCall.getSourceInfo(), reqType, answers,
            program.getTypeJavaLangObject(), instantiationExpressions);
      }
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
      if (GWT_CREATE.equals(targetSignature) || OLD_GWT_CREATE.equals(targetSignature)) {
        return createRebindExpression(x);
      } else if (IMPL_GET_NAME_OF.equals(targetSignature)) {
        return handleImplNameOf(x);
      }
      throw new InternalCompilerException("Unknown magic method");
    }
  }

  private static final String CLASS_DESIRED_ASSERTION_STATUS =
      "java.lang.Class.desiredAssertionStatus()Z";

  private static final String CLASS_IS_CLASS_METADATA_ENABLED =
      "java.lang.Class.isClassMetadataEnabled()Z";

  public static final String GWT_CREATE =
      "com.google.gwt.core.shared.GWT.create(Ljava/lang/Class;)Ljava/lang/Object;";

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
      new LinkedHashSet<String>(Arrays.asList(GWT_DEBUGGER_SHARED, GWT_DEBUGGER_CLIENT));

  /**
   * Methods for which the call site must be replaced with magic AST nodes.
   */
  private static final Set<String> MAGIC_METHOD_CALLS = new LinkedHashSet<String>(Arrays.asList(
      GWT_CREATE, GWT_DEBUGGER_SHARED, GWT_DEBUGGER_CLIENT, OLD_GWT_CREATE, IMPL_GET_NAME_OF));

  /**
   * Methods with magic implementations that the compiler must insert.
   */
  private static final Set<String> MAGIC_METHOD_IMPLS = new LinkedHashSet<String>(Arrays.asList(
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
  private final Set<CompilationUnit> failedUnits = new IdentityHashSet<CompilationUnit>();
  private final Map<String, JField> fieldMap = new HashMap<String, JField>();

  /**
   * The set of types currently known to be instantiable. Like
   * {@link ControlFlowAnalyzer#instantiatedTypes}.
   */
  private final Set<JDeclaredType> instantiatedTypes = new IdentityHashSet<JDeclaredType>();

  private final JsProgram jsProgram;

  /**
   * Fields and methods that are referenceable. Like
   * {@link ControlFlowAnalyzer#liveFieldsAndMethods}.
   */
  private final Set<JNode> liveFieldsAndMethods = new IdentityHashSet<JNode>();

  private final TreeLogger logger;
  private final CompilerContext compilerContext;
  private final Map<String, JMethod> methodMap = new HashMap<String, JMethod>();
  private final JProgram program;
  private final RebindPermutationOracle rpo;

  /**
   * A work queue of methods whose bodies we need to traverse. Prevents
   * excessive stack use.
   */
  private final Queue<JMethod> todo = new LinkedList<JMethod>();

  private final Set<String> virtualMethodsLive = new HashSet<String>();
  private final Map<String, List<JMethod>> virtualMethodsPending =
      new java.util.HashMap<String, List<JMethod>>();

  private NameBasedTypeLocator sourceNameBasedTypeLocator;
  private NameBasedTypeLocator binaryNameBasedTypeLocator;
  private NameBasedTypeLocator internalNameBasedTypeLocator;

  public UnifyAst(TreeLogger logger, CompilerContext compilerContext, JProgram program,
      JsProgram jsProgram, RebindPermutationOracle rpo) {
    this.logger = logger;
    this.compilerContext = compilerContext;
    this.program = program;
    this.jsProgram = jsProgram;
    this.rpo = rpo;
    this.compilationState = rpo.getCompilationState();
    this.compiledClassesByInternalName = compilationState.getClassFileMap();
    this.compiledClassesBySourceName = compilationState.getClassFileMapBySource();

    initializeNameBasedLocators();
  }

  public void addRootTypes(Collection<String> sourceTypeNames) throws UnableToCompleteException {
    for (String sourceTypeName : sourceTypeNames) {
      internalFindType(sourceTypeName, sourceNameBasedTypeLocator);
    }
    if (errorsFound) {
      // Already logged.
      throw new UnableToCompleteException();
    }
  }

  /**
   * Special AST construction, useful for tests. Everything is resolved,
   * translated, and unified.
   */
  public void buildEverything() throws UnableToCompleteException {
    for (String internalName : compiledClassesByInternalName.keySet()) {
      String typeName = InternalName.toBinaryName(internalName);
      internalFindType(typeName, binaryNameBasedTypeLocator);
    }

    for (JDeclaredType type : program.getDeclaredTypes()) {
      instantiate(type);
      for (JField field : type.getFields()) {
        flowInto(field);
      }
      for (JMethod method : type.getMethods()) {
        flowInto(method);
      }
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
   * For normal monolithic compiles only types reachable from entry points are traversed. This
   * speeds, saves memory trims unreferenced elements.<br />
   *
   * Library compiles traverse all types that were supplied as source in the compilation state and
   * no elements are pruned.
   */
  public void exec() throws UnableToCompleteException {
    // Trace execution from entry points and resolve references.
    for (JMethod entryMethod : program.getEntryMethods()) {
      flowInto(entryMethod);
    }

    // Trace execution from compiler code gen types and resolve references.
    for (JClassType type : program.codeGenTypes) {
      for (JMethod method : type.getMethods()) {
        flowInto(method);
      }
    }

    // If this is a library compile.
    if (!compilerContext.shouldCompileMonolithic()) {
      // Trace execution from all types supplied as source and resolve references.
      Set<String> internalNames = ImmutableSet.copyOf(compiledClassesByInternalName.keySet());
      for (String internalName : internalNames) {
        JDeclaredType type = internalFindType(internalName, internalNameBasedTypeLocator);
        instantiate(type);
        for (JField field : type.getFields()) {
          flowInto(field);
        }
        for (JMethod method : type.getMethods()) {
          flowInto(method);
        }
      }
    }

    /*
     * Since we're not actually optimizing here, it's easier to just visit
     * certain things up front instead of duplicating the exacting semantics of
     * ControlFlowAnalyzer.
     */
    // String literals.
    instantiate(program.getTypeJavaLangString());
    // ControlFlowAnalyzer.rescueByConcat().
    flowInto(program.getIndexedMethod("Object.toString"));
    mapApi(program.getTypeJavaLangString());
    flowInto(methodMap.get("java.lang.String.valueOf(C)Ljava/lang/String;"));

    // EnumNameObfuscator
    flowInto(program.getIndexedMethod("Enum.obfuscatedName"));

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
      flowInto(program.getIndexedMethod("AsyncFragmentLoader.onLoad"));
      flowInto(program.getIndexedMethod("AsyncFragmentLoader.runAsync"));
    }

    // ImplementClassLiteralsAsFields
    staticInitialize(program.getTypeClassLiteralHolder());
    for (JMethod method : program.getTypeJavaLangClass().getMethods()) {
      if (method.isStatic() && method.getName().startsWith("createFor")) {
        flowInto(method);
      }
    }

    mainLoop();

    // Post-stitching clean-ups.
    if (compilerContext.shouldCompileMonolithic()) {
      pruneDeadFieldsAndMethods();
    }
    computeOverrides();
    if (errorsFound) {
      // Already logged.
      throw new UnableToCompleteException();
    }
  }

  private void pruneDeadFieldsAndMethods() {
    for (JDeclaredType type : program.getDeclaredTypes()) {
      // Remove dead fields.
      boolean isInstantiated = instantiatedTypes.contains(type);
      for (int fieldIndex = 0; fieldIndex < type.getFields().size(); ++fieldIndex) {
        JField field = type.getFields().get(fieldIndex);
        if (!liveFieldsAndMethods.contains(field) || (!field.isStatic() && !isInstantiated)) {
          type.removeField(fieldIndex);
          --fieldIndex;
        }
      }

      // Empty the body of dead clinits.
      JMethod clinit = type.getClinitMethod();
      if (!liveFieldsAndMethods.contains(clinit)) {
        clinit.setBody(new JMethodBody(SourceOrigin.UNKNOWN));
      }

      // Remove dead methods.
      for (int methodIndex = 1; methodIndex < type.getMethods().size(); ++methodIndex) {
        JMethod method = type.getMethods().get(methodIndex);
        if (!liveFieldsAndMethods.contains(method) || (!method.isStatic() && !isInstantiated)) {
          type.removeMethod(methodIndex);
          --methodIndex;
        }
      }
    }
  }

  private void assimilateLibraryUnit(CompilationUnit referencedCompilationUnit) {
    if (referencedCompilationUnit.isError()) {
      if (failedUnits.add(referencedCompilationUnit)) {
        CompilationProblemReporter.reportErrors(logger, referencedCompilationUnit, false);
        errorsFound = true;
      }
      return;
    }

    compilerContext.getUnitCache().add(referencedCompilationUnit);
    compilationState.addReferencedCompilationUnits(logger, Lists.create(referencedCompilationUnit));
    // Record the types in the JProgram but do *not* flow into them and resolve their internal
    // references. There's no need since they're not part of this library. It's important to call
    // getTypes() only ONCE since each call returns a new copy.
    List<JDeclaredType> types = referencedCompilationUnit.getTypes();
    for (JDeclaredType referenceOnlyType : types) {
      program.addType(referenceOnlyType);
      program.addReferenceOnlyType(referenceOnlyType);
    }
    for (JDeclaredType referenceOnlyType : types) {
      resolveType(referenceOnlyType);
    }
  }

  private void assimilateSourceUnit(CompilationUnit unit) {
    if (unit.isError()) {
      if (failedUnits.add(unit)) {
        CompilationProblemReporter.reportErrors(logger, unit, false);
        errorsFound = true;
      }
      return;
    }
    // TODO(zundel): ask for a recompile if deserialization fails?
    List<JDeclaredType> types = unit.getTypes();
    assert containsAllTypes(unit, types);
    for (JDeclaredType t : types) {
      program.addType(t);
    }
    for (JDeclaredType t : types) {
      resolveType(t);
    }
    /*
     * Eagerly instantiate any JavaScriptObject subtypes. That way we don't have
     * to copy the exact semantics of ControlFlowAnalyzer.
     */
    for (JDeclaredType t : types) {
      if (t instanceof JClassType && isJso((JClassType) t)) {
        instantiate(t);
      }
      if (t instanceof JInterfaceType && ((JInterfaceType) t).isJsInterface()) {
        instantiate(t);
      }
    }
  }

  private boolean canAccessSuperMethod(JDeclaredType type, JMethod method) {
    if (method.isPrivate()) {
      return false;
    }
    if (method.isDefault()) {
      // Check package access.
      String typePackage = Shared.getPackageName(type.getName());
      String methodPackage = Shared.getPackageName(method.getEnclosingType().getName());
      return typePackage.equals(methodPackage);
    }
    return true;
  }

  private void collectUpRefs(JDeclaredType type, Map<String, Set<JMethod>> collected) {
    if (type == null) {
      return;
    }
    for (JMethod method : type.getMethods()) {
      if (method.canBePolymorphic()) {
        Set<JMethod> set = collected.get(method.getSignature());
        if (set != null) {
          set.add(method);
        }
      }
    }
    collectUpRefsInSupers(type, collected);
  }

  private void collectUpRefsInSupers(JDeclaredType type, Map<String, Set<JMethod>> collected) {
    collectUpRefs(type.getSuperClass(), collected);
    for (JInterfaceType intfType : type.getImplements()) {
      collectUpRefs(intfType, collected);
    }
  }

  /**
   * Compute all overrides.
   */
  private void computeOverrides() {
    for (JDeclaredType type : program.getDeclaredTypes()) {
      Map<String, Set<JMethod>> collected = new HashMap<String, Set<JMethod>>();
      for (JMethod method : type.getMethods()) {
        if (method.canBePolymorphic()) {
          collected.put(method.getSignature(), new LinkedHashSet<JMethod>());
        }
      }
      collectUpRefsInSupers(type, collected);
      for (JMethod method : type.getMethods()) {
        if (method.canBePolymorphic()) {
          for (JMethod upref : collected.get(method.getSignature())) {
            if (canAccessSuperMethod(type, upref)) {
              method.addOverriddenMethod(upref);
            }
          }
        }
      }
    }
  }

  private boolean containsAllTypes(CompilationUnit unit, List<JDeclaredType> types) {
    Set<String> binaryTypeNames = new HashSet<String>();
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
    StringBuffer msgBuf = new StringBuffer();
    int line = x.getSourceInfo().getStartLine();
    if (line > 0) {
      msgBuf.append("Line ");
      msgBuf.append(line);
      msgBuf.append(": ");
    }
    msgBuf.append(errorMessage);
    branch.log(TreeLogger.ERROR, msgBuf.toString());
  }

  private void flowInto(JField field) {
    if (field.isExternal()) {
      assert errorsFound;
      return;
    }
    if (field == JField.NULL_FIELD) {
      return;
    }
    if (!liveFieldsAndMethods.contains(field)) {
      liveFieldsAndMethods.add(field);
      field.setType(translate(field.getType()));
      if (field.isStatic()) {
        staticInitialize(field.getEnclosingType());
      }
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
    if (!liveFieldsAndMethods.contains(method)) {
      liveFieldsAndMethods.add(method);
      JType originalReturnType = translate(method.getOriginalReturnType());
      List<JType> originalParamTypes = new ArrayList<JType>(method.getOriginalParamTypes().size());
      for (JType originalParamType : method.getOriginalParamTypes()) {
        originalParamTypes.add(translate(originalParamType));
      }
      JType returnType = translate(method.getType());
      List<JClassType> thrownExceptions =
          new ArrayList<JClassType>(method.getThrownExceptions().size());
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
          List<JMethod> pending = virtualMethodsPending.remove(signature);
          if (pending != null) {
            for (JMethod p : pending) {
              assert instantiatedTypes.contains(p.getEnclosingType());
              flowInto(p);
            }
          }
        }
      }
      // Queue up visit / resolve on the body.
      todo.add(method);
    }
  }

  private String getMethodTypeSignature(JMethod method) {
    return method.getEnclosingType().getName() + '.' + method.getSignature();
  }

  public NameBasedTypeLocator getSourceNameBasedTypeLocator() {
    return sourceNameBasedTypeLocator;
  }

  private void implementMagicMethod(JMethod method, JExpression returnValue) {
    JMethodBody body = (JMethodBody) method.getBody();
    JBlock block = body.getBlock();
    SourceInfo info;
    if (block.getStatements().size() > 0) {
      info = block.getStatements().get(0).getSourceInfo();
    } else {
      info = method.getSourceInfo();
    }
    block.clear();
    block.addStmt(new JReturnStatement(info, returnValue));
  }

  private void initializeNameBasedLocators() {
    sourceNameBasedTypeLocator = new NameBasedTypeLocator(compiledClassesBySourceName) {
      @Override
      protected CompilationUnit getCompilationUnitFromLibrary(String sourceName) {
        return compilerContext.getLibraryGroup().getCompilationUnitByTypeSourceName(sourceName);
      }
    };
    binaryNameBasedTypeLocator = new NameBasedTypeLocator(null) {
      @Override
      protected CompilationUnit getCompilationUnitFromLibrary(String binaryName) {
        return compilerContext.getLibraryGroup().getCompilationUnitByTypeBinaryName(binaryName);
      }

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
    };
    internalNameBasedTypeLocator = new NameBasedTypeLocator(compiledClassesByInternalName) {
      @Override
      protected CompilationUnit getCompilationUnitFromLibrary(String internalName) {
        // There is no internal name based index for this, use the binary name based one instead.
        return binaryNameBasedTypeLocator.getCompilationUnitFromLibrary(
            InternalName.toBinaryName(internalName));
      }

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
    };
  }

  private void instantiate(JDeclaredType type) {
    // Don't flow into all the parts of types defined outside this compile.
    if (program.isReferenceOnly(type)) {
      return;
    }
    if (type.isExternal()) {
      assert errorsFound;
      return;
    }

    if (!instantiatedTypes.contains(type)) {
      instantiatedTypes.add(type);
      if (type.getSuperClass() != null) {
        instantiate(type.getSuperClass());
      }
      for (JInterfaceType intf : type.getImplements()) {
        instantiate(intf);
      }
      staticInitialize(type);
      boolean isJsInterface = type instanceof JInterfaceType ?
          isJsInterface((JInterfaceType) type) : false;

      // Flow into any reachable virtual methods.
      for (JMethod method : type.getMethods()) {
        if (method.canBePolymorphic()) {
          String signature = method.getSignature();
          if (virtualMethodsLive.contains(signature)) {
            assert !virtualMethodsPending.containsKey(signature);
            flowInto(method);
          } else {
            List<JMethod> pending = virtualMethodsPending.get(signature);
            if (pending == null) {
              pending = Lists.create(method);
            } else {
              pending = Lists.add(pending, method);
            }
            virtualMethodsPending.put(signature, pending);
            if (isJsInterface) {
              // Fake a call into the method to keep it around
              flowInto(method);
            }
          }
        } else if (method.getExportName() != null &&
            (method.isStatic() || method.isConstructor())) {
          // rescue any @JsExport methods
          flowInto(method);
        }
      }
    }
  }

  private boolean isJso(JClassType type) {
    if (type == null) {
      return false;
    }
    boolean isJso = type == program.getJavaScriptObject() || isJso(type.getSuperClass());
    if (isJso) {
      return true;
    }

    // if any of the superinterfaces as JsInterfaces, we consider this effectively a JSO
    // for instantiability purposes
    for (JInterfaceType intf : type.getImplements()) {
      if (isJsInterface(intf)) {
        return true;
      }
    }
    return false;
  }

  private boolean isJsInterface(JInterfaceType intf) {
    if (intf.isJsInterface()) {
      return true;
    }
    for (JInterfaceType subIntf : intf.getImplements()) {
      if (isJsInterface(subIntf)) {
        return true;
      }
    }
    return false;
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
      String methodSignature = getMethodTypeSignature(method);
      methodMap.put(methodSignature, method);
      if (MAGIC_METHOD_IMPLS.contains(methodSignature)) {
        if (methodSignature.startsWith("com.google.gwt.core.client.GWT.")
            || methodSignature.startsWith("com.google.gwt.core.shared.GWT.")) {
          // GWT.isClient, GWT.isScript, GWT.isProdMode all true.
          implementMagicMethod(method, JBooleanLiteral.TRUE);
        } else {
          assert methodSignature.startsWith("java.lang.Class.");
          if (CLASS_DESIRED_ASSERTION_STATUS.equals(methodSignature)) {
            implementMagicMethod(method,
                JBooleanLiteral.get(compilerContext.getOptions().isEnableAssertions()));
          } else if (CLASS_IS_CLASS_METADATA_ENABLED.equals(methodSignature)) {
            implementMagicMethod(method,
                JBooleanLiteral.get(!compilerContext.getOptions().isClassMetadataDisabled()));
          } else {
            assert false;
          }
        }
      }
    }
  }

  private void resolveType(JDeclaredType type) {
    assert !type.isExternal();
    if (type instanceof JClassType && type.getSuperClass() != null) {
      ((JClassType) type).setSuperClass(translate(type.getSuperClass()));
    }
    List<JInterfaceType> resolvedInterfaces = new ArrayList<JInterfaceType>();
    for (JInterfaceType intf : type.getImplements()) {
      resolvedInterfaces.add((JInterfaceType) translate(intf));
    }
    List<JNode> resolvedRescues = new ArrayList<JNode>();
    for (JNode node : type.getArtificialRescues()) {
      if (node instanceof JType) {
        node = translate((JType) node);
      } else if (node instanceof JField) {
        node = translate((JField) node);
      } else if (node instanceof JMethod) {
        node = translate((JMethod) node);
      } else {
        assert false : "Unknown artificial rescue node.";
      }
      resolvedRescues.add(node);
    }
    type.resolve(resolvedInterfaces, resolvedRescues);
  }

  public JDeclaredType findType(String typeName, NameBasedTypeLocator nameBasedTypeLocator)
      throws UnableToCompleteException {
    JDeclaredType type = internalFindType(typeName, nameBasedTypeLocator);
    if (errorsFound) {
      // Already logged.
      throw new UnableToCompleteException();
    }
    return type;
  }

  private JDeclaredType internalFindType(String typeName,
      NameBasedTypeLocator nameBasedTypeLocator) {
    if (nameBasedTypeLocator.resolvedTypeIsAvailable(typeName)) {
      return nameBasedTypeLocator.getResolvedType(typeName);
    }

    if (nameBasedTypeLocator.sourceCompilationUnitIsAvailable(typeName)) {
      assimilateSourceUnit(nameBasedTypeLocator.getCompilationUnitFromSource(typeName));
      return nameBasedTypeLocator.getResolvedType(typeName);
    }

    if (compilerContext.shouldCompileMonolithic()) {
      logger.log(TreeLogger.ERROR, String.format("Could not find %s in types compiled from source. "
          + "It was either unavailable or failed to compile.", typeName));
      errorsFound = true;
      return null;
    }

    if (nameBasedTypeLocator.libraryCompilationUnitIsAvailable(typeName)) {
      assimilateLibraryUnit(nameBasedTypeLocator.getCompilationUnitFromLibrary(typeName));
      return nameBasedTypeLocator.getResolvedType(typeName);
    }

    logger.log(TreeLogger.ERROR, String.format(
        "Could not find %s in types compiled from source or in provided dependency libraries. "
        + "Either the source file was unavailable, failed to compile or there is a missing "
        + "dependency.", typeName));
    errorsFound = true;
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
        staticInitialize(type.getSuperClass());
      }
      for (JNode node : type.getArtificialRescues()) {
        if (node instanceof JType) {
          if (node instanceof JDeclaredType) {
            instantiate((JDeclaredType) node);
          }
        } else if (node instanceof JField) {
          JField field = (JField) node;
          flowInto(field);
          if (!field.isFinal()) {
            field.setVolatile();
          }
        } else if (node instanceof JMethod) {
          flowInto((JMethod) node);
        } else {
          assert false : "Unknown artificial rescue node.";
        }
      }
    }
  }

  private JClassType translate(JClassType type) {
    return (JClassType) translate((JDeclaredType) type);
  }

  private JDeclaredType translate(JDeclaredType type) {
    if (!type.isExternal()) {
      return type;
    }
    String typeName = type.getName();
    JDeclaredType newType = internalFindType(typeName, binaryNameBasedTypeLocator);
    if (newType == null) {
      assert errorsFound;
      return type;
    }
    assert !newType.isExternal();
    return newType;
  }

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

  private JMethod translate(JMethod method) {
    if (!method.isExternal()) {
      return method;
    }

    JDeclaredType enclosingType = method.getEnclosingType();
    String sig = enclosingType.getName() + '.' + method.getSignature();
    JMethod newMethod = methodMap.get(sig);
    if (newMethod != null) {
      return newMethod;
    }

    enclosingType = translate(enclosingType);
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

  private JReferenceType translate(JReferenceType type) {
    if (type instanceof JNonNullType) {
      return translate(type.getUnderlyingType()).getNonNull();
    }

    if (type instanceof JArrayType) {
      JArrayType arrayType = (JArrayType) type;
      return program.getTypeArray(translate(arrayType.getElementType()));
    }

    if (type.isExternal()) {
      if (type instanceof JDeclaredType) {
        type = translate((JDeclaredType) type);
      } else {
        assert false : "Unknown external type";
      }
      assert !type.isExternal();
    }

    return type;
  }

  private JType translate(JType type) {
    if (type instanceof JPrimitiveType) {
      return type;
    }
    return translate((JReferenceType) type);
  }
}
