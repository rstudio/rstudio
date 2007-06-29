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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbstractMethodBody;
import com.google.gwt.dev.jjs.ast.JClassType;
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
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsThisRef;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is an interesting "optimization". It's not really an optimization in and
 * of itself, but it opens the door to other optimizations. The basic idea is
 * that you look for calls to instance methods that are not actually
 * polymorphic. In other words, the target method is (effectively) final, not
 * overriden anywhere in the compilation. We rewrite the single instance method
 * as a static method that contains the implementation plus an instance method
 * that delegates to the static method. Then we update any call sites to call
 * the static method instead. This opens the door to further optimizations,
 * reduces use of the long "this" keyword in the resulting JavaScript, and in
 * most cases the polymorphic version can be pruned later.
 * 
 * TODO(later): make this work on JSNI methods!
 */
public class MakeCallsStatic {

  /**
   * For all methods that should be made static, move the contents of the method
   * to a new static method, and have the original (instance) method delegate to
   * it. Sometimes the instance method can be pruned later since we update all
   * non-polymorphic call sites.
   */
  private class CreateStaticImplsVisitor extends JVisitor {

    /**
     * When code is moved from an instance method to a static method, all
     * thisRefs must be replaced with paramRefs to the synthetic this param.
     */
    private class RewriteJsniMethodBody extends JsModVisitor {

      private final JsName thisParam;

      public RewriteJsniMethodBody(JsName thisParam) {
        this.thisParam = thisParam;
      }

      // @Override
      public void endVisit(JsThisRef x, JsContext ctx) {
        ctx.replaceMe(thisParam.makeRef());
      }

      // @Override
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
      private final Map/* <JVariable, JVariable> */varMap;

      public RewriteMethodBody(JParameter thisParam,
          Map/* <JVariable, JVariable> */varMap) {
        this.thisParam = thisParam;
        this.varMap = varMap;
      }

      // @Override
      public void endVisit(JParameterRef x, Context ctx) {
        JParameter param = (JParameter) varMap.get(x.getTarget());
        JParameterRef paramRef = new JParameterRef(program, x.getSourceInfo(),
            param);
        ctx.replaceMe(paramRef);
      }

      // @Override
      public void endVisit(JThisRef x, Context ctx) {
        JParameterRef paramRef = new JParameterRef(program, x.getSourceInfo(),
            thisParam);
        ctx.replaceMe(paramRef);
      }
    }

    // @Override
    public boolean visit(JMethod x, Context ctx) {
      // Let's do it!
      JClassType enclosingType = (JClassType) x.getEnclosingType();
      JType returnType = x.getType();
      SourceInfo sourceInfo = x.getSourceInfo();
      int myIndexInClass = enclosingType.methods.indexOf(x);
      assert (myIndexInClass >= 0);

      // Create the new static method
      String newName = "$" + x.getName();

      /*
       * Don't use the JProgram helper because it auto-adds the new method to
       * its enclosing class.
       */
      JMethod newMethod = new JMethod(program, sourceInfo, newName,
          enclosingType, returnType, false, true, true, x.isPrivate());

      // Setup parameters; map from the old params to the new params
      JParameter thisParam = program.createParameter(null,
          "this$static".toCharArray(), enclosingType, true, newMethod);
      Map/* <JVariable, JVariable> */varMap = new IdentityHashMap();
      for (int i = 0; i < x.params.size(); ++i) {
        JParameter oldVar = (JParameter) x.params.get(i);
        JParameter newVar = program.createParameter(oldVar.getSourceInfo(),
            oldVar.getName().toCharArray(), oldVar.getType(), oldVar.isFinal(),
            newMethod);
        varMap.put(oldVar, newVar);
      }
      newMethod.freezeParamTypes();

      // Move the body of the instance method to the static method
      JAbstractMethodBody movedBody = x.getBody();
      newMethod.setBody(movedBody);

      // Create a new body for the instance method that delegates to the static
      JMethodBody newBody = new JMethodBody(program, sourceInfo);
      x.setBody(newBody);
      JMethodCall newCall = new JMethodCall(program, sourceInfo, null,
          newMethod);
      newCall.getArgs().add(program.getExprThisRef(sourceInfo, enclosingType));
      for (int i = 0; i < x.params.size(); ++i) {
        JParameter param = (JParameter) x.params.get(i);
        newCall.getArgs().add(new JParameterRef(program, sourceInfo, param));
      }
      JStatement statement;
      if (returnType == program.getTypeVoid()) {
        statement = newCall.makeStatement();
      } else {
        statement = new JReturnStatement(program, sourceInfo, newCall);
      }
      newBody.getStatements().add(statement);

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
        jsFunc.getParameters().add(0, new JsParameter(paramName));
        RewriteJsniMethodBody rewriter = new RewriteJsniMethodBody(paramName);
        // Accept the body to avoid the recursion blocker.
        rewriter.accept(jsFunc.getBody());
      } else {
        RewriteMethodBody rewriter = new RewriteMethodBody(thisParam, varMap);
        rewriter.accept(movedBody);
      }

      // Add the new method as a static impl of the old method
      program.putStaticImpl(x, newMethod);
      enclosingType.methods.add(myIndexInClass + 1, newMethod);
      return false;
    }
  }

  /**
   * Look for any places where instance methods are called in a static manner.
   * Record this fact so we can create static dispatch implementations.
   */
  private class FindStaticDispatchSitesVisitor extends JVisitor {

    // @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      // Did we already do this one?
      if (program.getStaticImpl(method) != null
          || toBeMadeStatic.contains(method)) {
        return;
      }

      // Must be instance and final
      if (x.canBePolymorphic()) {
        return;
      }
      if (method.isStatic()) {
        return;
      }
      if (method.isAbstract()) {
        return;
      }
      if (method == program.getNullMethod()) {
        // Special case: we don't make calls to this method static.
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

    /*
     * In cases where callers are directly referencing (effectively) final
     * instance methods, rewrite the call site to reference the newly-generated
     * static method instead.
     */
    // @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod oldMethod = x.getTarget();
      JMethod newMethod = program.getStaticImpl(oldMethod);
      if (newMethod == null || x.canBePolymorphic()) {
        return;
      }

      // Update the call site
      JMethodCall newCall = new JMethodCall(program, x.getSourceInfo(), null,
          newMethod);

      // The qualifier becomes the first arg
      newCall.getArgs().add(x.getInstance());
      // Copy the rest of the args
      for (int i = 0; i < x.getArgs().size(); ++i) {
        newCall.getArgs().add(x.getArgs().get(i));
      }
      ctx.replaceMe(newCall);
    }
  }

  public static boolean exec(JProgram program) {
    return new MakeCallsStatic(program).execImpl();
  }

  public Set toBeMadeStatic = new HashSet();

  private final JProgram program;

  private MakeCallsStatic(JProgram program) {
    this.program = program;
  }

  private boolean execImpl() {
    FindStaticDispatchSitesVisitor finder = new FindStaticDispatchSitesVisitor();
    finder.accept(program);
    if (toBeMadeStatic.isEmpty()) {
      return false;
    }

    CreateStaticImplsVisitor creator = new CreateStaticImplsVisitor();
    for (Iterator it = toBeMadeStatic.iterator(); it.hasNext();) {
      JMethod method = (JMethod) it.next();
      creator.accept(method);
    }

    RewriteCallSites rewriter = new RewriteCallSites();
    rewriter.accept(program);
    assert (rewriter.didChange());
    return true;
  }

}
