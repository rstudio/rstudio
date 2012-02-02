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
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
    private class RewriteJsniMethodBody extends JsModVisitor {

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
    private class RewriteMethodBody extends JModVisitor {

      private final JParameter thisParam;
      private final Map<JParameter, JParameter> varMap;

      public RewriteMethodBody(JParameter thisParam, Map<JParameter, JParameter> varMap) {
        this.thisParam = thisParam;
        this.varMap = varMap;
      }

      @Override
      public void endVisit(JParameterRef x, Context ctx) {
        JParameter param = varMap.get(x.getTarget());
        JParameterRef paramRef = new JParameterRef(x.getSourceInfo(), param);
        ctx.replaceMe(paramRef);
      }

      @Override
      public void endVisit(JThisRef x, Context ctx) {
        JParameterRef paramRef = new JParameterRef(x.getSourceInfo(), thisParam);
        ctx.replaceMe(paramRef);
      }
    }

    private final JProgram program;

    CreateStaticImplsVisitor(JProgram program) {
      this.program = program;
    }

    @Override
    public boolean visit(JConstructor x, Context ctx) {
      throw new InternalCompilerException("Should not try to staticify constructors");
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      // Let's do it!
      JClassType enclosingType = (JClassType) x.getEnclosingType();
      JType returnType = x.getType();
      SourceInfo sourceInfo = x.getSourceInfo().makeChild();
      int myIndexInClass = enclosingType.getMethods().indexOf(x);
      assert (myIndexInClass > 0);

      // Create the new static method
      String newName = "$" + x.getName();

      /*
       * Don't use the JProgram helper because it auto-adds the new method to
       * its enclosing class.
       */
      JMethod newMethod =
          new JMethod(sourceInfo, newName, enclosingType, returnType, false, true, true, x
              .getAccess());
      newMethod.setSynthetic();
      newMethod.addThrownExceptions(x.getThrownExceptions());

      // Setup parameters; map from the old params to the new params
      JParameter thisParam =
          JParameter.create(sourceInfo, "this$static", enclosingType.getNonNull(), true, true,
              newMethod);
      Map<JParameter, JParameter> varMap = new IdentityHashMap<JParameter, JParameter>();
      for (int i = 0; i < x.getParams().size(); ++i) {
        JParameter oldVar = x.getParams().get(i);
        JParameter newVar =
            JParameter.create(oldVar.getSourceInfo(), oldVar.getName(), oldVar.getType(), oldVar
                .isFinal(), false, newMethod);
        varMap.put(oldVar, newVar);
      }

      // Set the new original param types based on the old original param types
      List<JType> originalParamTypes = new ArrayList<JType>();
      originalParamTypes.add(enclosingType.getNonNull());
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
        newCall.addArg(new JParameterRef(sourceInfo, param));
      }
      JStatement statement;
      if (returnType == program.getTypeVoid()) {
        statement = newCall.makeStatement();
      } else {
        statement = new JReturnStatement(sourceInfo, newCall);
      }
      newBody.getBlock().addStmt(statement);

      /*
       * Rewrite the method body. Update all thisRefs to paramRefs. Update
       * paramRefs and localRefs to target the params/locals in the new method.
       */
      if (newMethod.isNative()) {
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
        RewriteMethodBody rewriter = new RewriteMethodBody(thisParam, varMap);
        rewriter.accept(movedBody);
      }

      // Add the new method as a static impl of the old method
      program.putStaticImpl(x, newMethod);
      enclosingType.getMethods().add(myIndexInClass + 1, newMethod);
      return false;
    }
  }

  /**
   * Look for any places where instance methods are called in a static manner.
   * Record this fact so we can create static dispatch implementations.
   */
  private class FindStaticDispatchSitesVisitor extends JVisitor {
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      if (method.isExternal()) {
        // Staticifying a method requires modifying the type, which we can't
        // do for external types. Theoretically we could put the static method
        // in some generated code, but what does that really buy us?
        return;
      }

      // Did we already do this one?
      if (program.getStaticImpl(method) != null || toBeMadeStatic.contains(method)) {
        return;
      }

      // Must be instance and final
      if (x.canBePolymorphic()) {
        return;
      }
      if (!method.needsVtable()) {
        return;
      }
      if (method.isAbstract()) {
        return;
      }
      if (method == program.getNullMethod()) {
        // Special case: we don't make calls to this method static.
        return;
      }

      if (!method.getEnclosingType().getMethods().contains(method)) {
        // The target method was already pruned (TypeTightener will fix this).
        return;
      }
  
      // Let's do it!
      toBeMadeStatic.add(method);
    }
  }

  /**
   * For any method calls to methods we updated during
   * CreateStaticMethodVisitor, go and rewrite the call sites to call the static
   * method instead.
   */
  private class RewriteCallSites extends JModVisitor {
    private boolean currentMethodIsInitiallyLive;
    private ControlFlowAnalyzer initiallyLive;

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

      ctx.replaceMe(makeStaticCall(x, newMethod));
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethodIsInitiallyLive = initiallyLive.getLiveFieldsAndMethods().contains(x);
      return true;
    }

    @Override
    public boolean visit(JProgram x, Context ctx) {
      initiallyLive = CodeSplitter2.computeInitiallyLive(x);
      return true;
    }
  }

  private static final String NAME = MakeCallsStatic.class.getSimpleName();

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MakeCallsStatic(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  static JExpression makeStaticCall(JMethodCall x, JMethod newMethod) {
    // Update the call site
    JMethodCall newCall = new JMethodCall(x.getSourceInfo(), null, newMethod);

    /*
     * If the qualifier is a JMultiExpression, invoke on the last value. This
     * ensures that clinits maintain the same execution order relative to
     * parameters in deeply-inlined scenarios.
     */
    // (a, b).foo() --> (a, foo(b))
    if (x.getInstance() instanceof JMultiExpression) {
      JMultiExpression multi = (JMultiExpression) x.getInstance();
      int lastIndex = multi.exprs.size() - 1;
      newCall.addArg(multi.exprs.get(lastIndex));
      newCall.addArgs(x.getArgs());
      multi.exprs.set(lastIndex, newCall);
      return multi;
    } else {
      // The qualifier becomes the first arg
      // a.foo(b) --> foo(a,b)
      newCall.addArg(x.getInstance());
      newCall.addArgs(x.getArgs());
      return newCall;
    }
  }

  protected Set<JMethod> toBeMadeStatic = new HashSet<JMethod>();

  private final JProgram program;

  private MakeCallsStatic(JProgram program) {
    this.program = program;
  }

  private OptimizerStats execImpl() {
    OptimizerStats stats = new OptimizerStats(NAME);
    FindStaticDispatchSitesVisitor finder = new FindStaticDispatchSitesVisitor();
    finder.accept(program);

    CreateStaticImplsVisitor creator = new CreateStaticImplsVisitor(program);
    for (JMethod method : toBeMadeStatic) {
      creator.accept(method);
    }

    /*
     * Run the rewriter even if we didn't make any new static methods; other
     * optimizations can unlock devirtualizations even if no more static impls
     * are created.
     */
    RewriteCallSites rewriter = new RewriteCallSites();
    rewriter.accept(program);
    stats.recordModified(rewriter.getNumMods());
    assert (rewriter.didChange() || toBeMadeStatic.isEmpty());
    return stats;
  }
}
