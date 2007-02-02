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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
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
import com.google.gwt.dev.jjs.ast.JSourceInfo;

import java.util.HashSet;
import java.util.IdentityHashMap;
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
  private class CreateStaticImplsVisitor extends JModVisitor {

    // @Override
    public boolean visit(JMethod x, Context ctx) {
      if (!toBeMadeStatic.contains(x)) {
        return false;
      }

      // Let's do it!
      JClassType enclosingType = (JClassType) x.getEnclosingType();
      JType oldReturnType = x.getType();

      // Create the new static method
      String newName = "$" + x.getName();

      /*
       * Don't use the JProgram helper because it auto-adds the new method to
       * its enclosing class, which will break iteration.
       */
      JMethod newMethod = new JMethod(program, x.getSourceInfo(), newName,
          enclosingType, oldReturnType, false, true, true, x.isPrivate());

      // Setup all params and locals; map from the old method to the new method
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

      // Copy all locals over to the new method
      for (int i = 0; i < x.locals.size(); ++i) {
        JLocal oldVar = (JLocal) x.locals.get(i);
        JLocal newVar = program.createLocal(oldVar.getSourceInfo(),
            oldVar.getName().toCharArray(), oldVar.getType(), oldVar.isFinal(),
            newMethod);
        varMap.put(oldVar, newVar);
      }
      x.locals.clear();

      // Move the body of the instance method to the static method
      newMethod.body.statements.addAll(x.body.statements);
      x.body.statements.clear();

      /*
       * Rewrite the method body. Update all thisRefs to paramrefs. Update
       * paramRefs and localRefs to target the params/locals in the new method.
       */
      RewriteMethodBody rewriter = new RewriteMethodBody(thisParam, varMap);
      rewriter.accept(newMethod);

      JSourceInfo bodyInfo = x.body.getSourceInfo();
      // delegate from the instance method to the static method
      JMethodCall newCall = new JMethodCall(program, bodyInfo, null, newMethod);
      newCall.getArgs().add(program.getExprThisRef(bodyInfo, enclosingType));
      for (int i = 0; i < x.params.size(); ++i) {
        JParameter param = (JParameter) x.params.get(i);
        newCall.getArgs().add(new JParameterRef(program, bodyInfo, param));
      }
      JStatement statement;
      if (oldReturnType == program.getTypeVoid()) {
        statement = new JExpressionStatement(program, bodyInfo, newCall);
      } else {
        statement = new JReturnStatement(program, bodyInfo, newCall);
      }
      x.body.statements.add(statement);

      // Add the new method as a static impl of the old method
      program.putStaticImpl(x, newMethod);
      assert (ctx.canInsert());
      ctx.insertAfter(newMethod);
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
      if (method.isNative()) {
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

  /**
   * When code is moved from an instance method to a static method, all this
   * refs must be replaced with param refs to the synthetic this param.
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
    public void endVisit(JLocalRef x, Context ctx) {
      JLocal local = (JLocal) varMap.get(x.getTarget());
      JLocalRef localRef = new JLocalRef(program, x.getSourceInfo(), local);
      ctx.replaceMe(localRef);
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
    creator.accept(program);
    if (!creator.didChange()) {
      return false;
    }

    RewriteCallSites rewriter = new RewriteCallSites();
    rewriter.accept(program);
    assert (rewriter.didChange());
    return true;
  }

}
