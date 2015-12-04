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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbstractMethodBody;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethod.Specialization;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.impl.codesplitter.CodeSplitter;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is an interesting "optimization". It's not really an optimization in and
 * of itself, but it opens the door to other optimizations. The basic idea is
 * that you look for calls to instance methods that are not actually
 * polymorphic. In other words, the target method is (effectively) final, not
 * overridden anywhere in the compilation. We rewrite the single instance method
 * as a static method that contains the implementation plus an instance method
 * that delegates to the static method. Then we update any call sites to call
 * the static method instead. This opens the door to further optimizations,
 * reduces use of the long "this" keyword in the resulting JavaScript, and in
 * most cases the polymorphic version can be pruned later.
 */
public class MakeCallsStatic {
  /**
   * For all methods that should be made static, move the contents of the method
   * to a new static method, and have the original (instance) method delegate to
   * it. Sometimes the instance method can be pruned later since we update all
   * non-polymorphic call sites.
   */
  static class CreateStaticImplsVisitor extends JVisitor {
    /**
     * When code is moved from an instance method to a static method, all
     * thisRefs must be replaced with paramRefs to the synthetic this param.
     */
    private static class RewriteJsniMethodBody extends JsModVisitor {

      private final JsName thisParam;

      public RewriteJsniMethodBody(JsName thisParam) {
        this.thisParam = thisParam;
      }

      @Override
      public void endVisit(JsThisRef x, JsContext ctx) {
        ctx.replaceMe(thisParam.makeRef(x.getSourceInfo()));
      }

      @Override
      public boolean visit(JsFunction x, JsContext ctx) {
        // Don't recurse into nested functions!
        return false;
      }
    }

    /**
     * When code is moved from an instance method to a static method, all
     * thisRefs must be replaced with paramRefs to the synthetic this param.
     * ParameterRefs also need to be targeted to the params in the new method.
     */
    private class RewriteMethodBody extends JChangeTrackingVisitor {

      private final JParameter thisParam;
      private final Map<JParameter, JParameter> varMap;

      public RewriteMethodBody(JParameter thisParam, Map<JParameter, JParameter> varMap,
          OptimizerContext optimizerCtx) {
        super(optimizerCtx);
        this.thisParam = thisParam;
        this.varMap = varMap;
      }

      @Override
      public void endVisit(JParameterRef x, Context ctx) {
        JParameter param = varMap.get(x.getTarget());
        ctx.replaceMe(param.makeRef(x.getSourceInfo()));
      }

      @Override
      public void endVisit(JThisRef x, Context ctx) {
        ctx.replaceMe(thisParam.makeRef(x.getSourceInfo()));
      }
    }

    private final JProgram program;
    private final OptimizerContext optimizerCtx;

    private CreateStaticImplsVisitor(JProgram program, OptimizerContext optimizerCtx) {
      this.program = program;
      this.optimizerCtx = optimizerCtx;
    }

    CreateStaticImplsVisitor(JProgram program) {
      this.program = program;
      this.optimizerCtx = null;
    }

    @Override
    public boolean visit(JConstructor x, Context ctx) {
      throw new InternalCompilerException("Should not try to staticify constructors");
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      assert !x.isJsNative() : "Native methods can not be devirtualized";

      // Let's do it!
      JDeclaredType enclosingType = x.getEnclosingType();
      JType returnType = x.getType();
      SourceInfo sourceInfo = x.getSourceInfo().makeChild();
      int myIndexInClass = enclosingType.getMethods().indexOf(x);
      assert (myIndexInClass > 0);

      // Create the new static method
      String newName = getStaticMethodName(x);

      /*
       * Don't use the JProgram helper because it auto-adds the new method to
       * its enclosing class.
       */
      JMethod newMethod =
          new JMethod(sourceInfo, newName, enclosingType, returnType, false, true, true, x
              .getAccess());
      newMethod.setInliningMode(x.getInliningMode());
      newMethod.setHasSideEffects(x.hasSideEffects());
      newMethod.setSynthetic();
      newMethod.addThrownExceptions(x.getThrownExceptions());
      if (x.isJsOverlay()) {
        newMethod.setJsOverlay();
      }

      JType thisParameterType = enclosingType.strengthenToNonNull();
      // Setup parameters; map from the old params to the new params
      JParameter thisParam = newMethod.createThisParameter(sourceInfo, thisParameterType);
      Map<JParameter, JParameter> varMap = Maps.newIdentityHashMap();
      for (JParameter oldVar : x.getParams()) {
        JParameter newVar = newMethod.cloneParameter(oldVar);
        varMap.put(oldVar, newVar);
      }

      // Set the new original param types based on the old original param types
      List<JType> originalParamTypes = Lists.newArrayList();
      originalParamTypes.add(thisParameterType);
      originalParamTypes.addAll(x.getOriginalParamTypes());
      newMethod.setOriginalTypes(x.getOriginalReturnType(), originalParamTypes);

      // Move the body of the instance method to the static method
      JAbstractMethodBody movedBody = x.getBody();
      newMethod.setBody(movedBody);

      JMethodBody newBody = new JMethodBody(sourceInfo);
      x.setBody(newBody);
      JMethodCall newCall = new JMethodCall(sourceInfo, null, newMethod);
      newCall.addArg(new JThisRef(sourceInfo, enclosingType));
      for (int i = 0; i < x.getParams().size(); ++i) {
        JParameter param = x.getParams().get(i);
        newCall.addArg(param.makeRef(sourceInfo));
      }
      newBody.getBlock().addStmt(JjsUtils.makeMethodEndStatement(returnType, newCall));

      /*
       * Rewrite the method body. Update all thisRefs to paramRefs. Update
       * paramRefs and localRefs to target the params/locals in the new method.
       */
      if (newMethod.isJsniMethod()) {
        // For natives, we also need to create the JsParameter for this$static,
        // because the jsFunc already has parameters.
        // TODO: Do we really need to do that in BuildTypeMap?
        JsFunction jsFunc = ((JsniMethodBody) movedBody).getFunc();
        JsName paramName = jsFunc.getScope().declareName("this$static");
        jsFunc.getParameters().add(0, new JsParameter(sourceInfo, paramName));
        RewriteJsniMethodBody rewriter = new RewriteJsniMethodBody(paramName);
        // Accept the body to avoid the recursion blocker.
        rewriter.accept(jsFunc.getBody());
      } else {
        RewriteMethodBody rewriter = new RewriteMethodBody(thisParam, varMap, optimizerCtx);
        rewriter.accept(movedBody);
      }

      // Add the new method as a static impl of the old method
      program.putStaticImpl(x, newMethod);
      enclosingType.getMethods().add(myIndexInClass + 1, newMethod);

      if (optimizerCtx != null) {
        optimizerCtx.markModified(x);
        optimizerCtx.markModified(newMethod);
      }
      return false;
    }

    public JMethod getOrCreateStaticImpl(JProgram program, JMethod method) {
      assert !method.isStatic();
      JMethod staticImpl = program.getStaticImpl(method);
      if (staticImpl == null) {
        accept(method);
        staticImpl = program.getStaticImpl(method);
      }
      return staticImpl;
    }
  }

  private static String getStaticMethodName(JMethod x) {
    return "$" + x.getName();
  }

  /**
   * Look for any places where instance methods are called in a static manner.
   * Record this fact so we can create static dispatch implementations.
   */
  private class FindStaticDispatchSitesVisitor extends JVisitor {
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      if (shouldBeMadeStatic(x, method)) {
        // Let's do it!
        toBeMadeStatic.add(method);
        if (method.getSpecialization() != null &&
            shouldBeMadeStatic(x,
                method.getSpecialization().getTargetMethod())) {
          toBeMadeStatic.add(method.getSpecialization().getTargetMethod());
        }
      }
    }

    private boolean shouldBeMadeStatic(JMethodCall x, JMethod method) {
      if (method.isExternal()) {
        // Staticifying a method requires modifying the type, which we can't
        // do for external types. Theoretically we could put the static method
        // in some generated code, but what does that really buy us?
        return false;
      }

      if (!method.isDevirtualizationAllowed()) {
        // Method has been specifically excluded from statification.
        return false;
      }

      // Did we already do this one?
      if (program.getStaticImpl(method) != null || toBeMadeStatic.contains(method)) {
        return false;
      }

      // Must be instance and final
      if (x.canBePolymorphic()) {
        return false;
      }
      if (!method.needsDynamicDispatch()) {
        return false;
      }
      if (method.isAbstract()) {
        return false;
      }
      if (method.isJsNative()) {
        return false;
      }
      if (method == program.getNullMethod()) {
        // Special case: we don't make calls to this method static.
        return false;
      }

      if (!method.getEnclosingType().getMethods().contains(method)) {
        // The target method was already pruned (TypeTightener will fix this).
        return false;
      }

      return true;
    }
  }

  /**
   * For any method calls to methods we updated during
   * CreateStaticMethodVisitor, go and rewrite the call sites to call the static
   * method instead.
   */
  private class RewriteCallSites extends JChangeTrackingVisitor {

    private boolean currentMethodIsInitiallyLive;
    private ControlFlowAnalyzer initiallyLive;

    public RewriteCallSites(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    /**
     * In cases where callers are directly referencing (effectively) final
     * instance methods, rewrite the call site to reference the newly-generated
     * static method instead.
     */
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod oldMethod = x.getTarget();
      JMethod newMethod = program.getStaticImpl(oldMethod);

      if (newMethod == null || x.canBePolymorphic()) {
        return;
      }

      if (currentMethodIsInitiallyLive
          && !initiallyLive.getLiveFieldsAndMethods().contains(x.getTarget())) {
        /*
         * Don't devirtualize calls from initial code to non-initial code.
         *
         * TODO(spoon): similar prevention when the callee is exclusive to some
         * split point and the caller is not.
         */
        return;
      }

      ctx.replaceMe(converter.convertCall(x, newMethod));
    }

    @Override
    public boolean enter(JMethod x, Context ctx) {
      currentMethodIsInitiallyLive = initiallyLive.getLiveFieldsAndMethods().contains(x);
      return true;
    }

    @Override
    public boolean visit(JProgram x, Context ctx) {
      // TODO(rluble): This needs to be abstracted out the CodeSplitter.
      initiallyLive = CodeSplitter.computeInitiallyLive(x);
      return true;
    }
  }

  /**
   * Converts instance method calls to equivalent static method calls.
   * Optionally adds a null check on the former "this" parameter.
   */
  static class StaticCallConverter {
    private final JMethod checkNotNull;

    StaticCallConverter(JProgram program, boolean addNullChecksForThis) {
      if (addNullChecksForThis) {
        checkNotNull = program.getIndexedMethod(RuntimeConstants.EXCEPTIONS_CHECK_NOT_NULL);
      } else {
        checkNotNull = null;
      }
    }

    /**
     * Converts an instance method call to the equivalent static method call.
     * @param original the instance method call to convert
     * @param newMethod the static method to call instead
     */
    JExpression convertCall(JMethodCall original, JMethod newMethod) {

      JMethodCall newCall = new JMethodCall(original.getSourceInfo(), null, newMethod);

      /*
       * If the qualifier is a JMultiExpression, invoke on the last value. This
       * ensures that clinits maintain the same execution order relative to
       * parameters in deeply-inlined scenarios.
       */
      //   (a, b).foo() --> (a, foo(b))
      // Or in checked mode:
      //   (a, b).foo() --> (a, foo(checkNotNull(b)))
      if (original.getInstance() instanceof JMultiExpression) {
        JMultiExpression multi = (JMultiExpression) original.getInstance();
        int lastIndex = multi.getNumberOfExpressions() - 1;
        newCall.addArg(makeNullCheck(multi.getExpression(lastIndex), original));
        newCall.addArgs(original.getArgs());
        multi.setExpression(lastIndex, newCall);
        return multi;
      } else {
        // The qualifier becomes the first argument.
        //   a.foo(b) --> foo(a,b)
        // or in checked mode:
        //   a.foo(b) --> foo(checkNotNull(a),b)
        newCall.addArg(makeNullCheck(original.getInstance(), original));
        newCall.addArgs(original.getArgs());
        return newCall;
      }
    }

    private JExpression makeNullCheck(JExpression x, JMethodCall call) {
      if (checkNotNull == null) {
        return x;
      }

      // Existing code plays tricks with JSO's, so don't add the null check.
      if (isJso(call)) {
        return x;
      }

      JMethodCall check = new JMethodCall(x.getSourceInfo(), null, checkNotNull);
      check.addArg(x);
      return check;
    }

    private boolean isJso(JMethodCall call) {
      JDeclaredType type = call.getTarget().getEnclosingType();
      return type != null && type.isJsoType();
    }
  }

  private static final String NAME = MakeCallsStatic.class.getSimpleName();

  public static OptimizerStats exec(JProgram program, boolean addRuntimeChecks,
      OptimizerContext optimizerCtx) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MakeCallsStatic(program, addRuntimeChecks).execImpl(optimizerCtx);
    optimizerCtx.setLastStepFor(NAME, optimizerCtx.getOptimizationStep());
    optimizerCtx.incOptimizationStep();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  @VisibleForTesting
  static OptimizerStats exec(JProgram program,  boolean addRuntimeChecks) {
    return exec(program, addRuntimeChecks, new FullOptimizerContext(program));
  }

  protected Set<JMethod> toBeMadeStatic = Sets.newHashSet();

  private final JProgram program;
  private final StaticCallConverter converter;

  private MakeCallsStatic(JProgram program, boolean addRuntimeChecks) {
    this.program = program;
    this.converter = new StaticCallConverter(program, addRuntimeChecks);
  }

  private OptimizerStats execImpl(OptimizerContext optimizerCtx) {
    OptimizerStats stats = new OptimizerStats(NAME);
    FindStaticDispatchSitesVisitor finder = new FindStaticDispatchSitesVisitor();
    Set<JMethod> modifiedMethods =
        optimizerCtx.getModifiedMethodsSince(optimizerCtx.getLastStepFor(NAME));
    Set<JMethod> affectedMethods = affectedMethods(modifiedMethods, optimizerCtx);
    optimizerCtx.traverse(finder, affectedMethods);

    CreateStaticImplsVisitor creator = new CreateStaticImplsVisitor(program, optimizerCtx);
    for (JMethod method : toBeMadeStatic) {
      creator.accept(method);
    }
    for (JMethod method : toBeMadeStatic) {
      // if method has specialization, add it to the static method
      Specialization specialization = method.getSpecialization();
      if (specialization != null) {
        JMethod staticMethod = program.getStaticImpl(method);
        List<JType> params = Lists.newArrayList(specialization.getParams());
        params.add(0, staticMethod.getParams().get(0).getType());
        staticMethod.setSpecialization(params, specialization.getReturns(),
            staticMethod.getName());
        staticMethod.getSpecialization().resolve(params,
            specialization.getReturns(), program.getStaticImpl(specialization
                .getTargetMethod()));
      }
    }

    /*
     * Run the rewriter even if we didn't make any new static methods; other
     * optimizations can unlock devirtualizations even if no more static impls
     * are created.
     */
    RewriteCallSites rewriter = new RewriteCallSites(optimizerCtx);
    rewriter.accept(program);
    stats.recordModified(rewriter.getNumMods());
    assert (rewriter.didChange() || toBeMadeStatic.isEmpty());
    JavaAstVerifier.assertProgramIsConsistent(program);
    return stats;
  }

  /**
   * Return the set of methods affected (because they are or callers of) by the modifications to the
   * given set functions.
   */
  private Set<JMethod> affectedMethods(Set<JMethod> modifiedMethods,
      OptimizerContext optimizerCtx) {
    assert (modifiedMethods != null);
    Set<JMethod> affectedMethods = Sets.newLinkedHashSet();
    affectedMethods.addAll(modifiedMethods);
    affectedMethods.addAll(optimizerCtx.getCallers(modifiedMethods));
    return affectedMethods;
  }
}
