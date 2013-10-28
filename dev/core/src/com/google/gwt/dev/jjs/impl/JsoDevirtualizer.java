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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.AccessModifier;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic.CreateStaticImplsVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * JSO devirtualization is the process of converting virtual method calls on instances that might be
 * a JSO in the AST like "someJsoObject.doFoo();" to static calls like
 * "JavaScriptObject.doFoo__devirtual$(someJsoObject);"<br />
 *
 * See https://code.google.com/p/google-web-toolkit/wiki/OverlayTypes for why this is done.<br />
 *
 * To complete the transformation:<br />
 * 1. methods that are defined on JSO subclasses must be turned into static functions.<br />
 * 2. all method calls to the original functions must be rerouted to the new static versions.<br />
 *
 * All functions defined on JSO subclasses are devirtualized. This ensures they're available during
 * separate compilation and it does not bloat optimized output because unused ones are pruned during
 * monolithic compilation.<br />
 *
 * This transform may NOT be run multiple times; it will create ever-expanding replacement
 * expressions.
 */
public class JsoDevirtualizer {

  /**
   * Rewrite any virtual dispatches to Object or JavaScriptObject such that
   * dispatch occurs statically for JSOs. <br />
   *
   * In the following cases JMethodCalls need to be rewritten:
   * <ol>
   * <li>a dual dispatch interface</li>
   * <li>a single dispatch trough single-jso interface</li>
   * <li>a java.lang.Object override from JavaScriptObject</li>
   * <li>in draftMode, a 'static' virtual JSO call that hasn't been made
   * static yet.</li>
   * </ol>
   *
   */
  private class RewriteVirtualDispatches extends JModVisitor {

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (!mightBeJsoMethod(x)) {
        return;
      }
      // The pruning pass will discard devirtualized methods that have not been called in
      // whole program optimizing mode.
      ensureDevirtualVersionExists(x);
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (!mightBeJsoMethod(method)) {
        return;
      }
      JType instanceType = x.getInstance().getType();
      // If the instance can't possibly be a JSO, don't devirtualize the call.
      if (instanceType != program.getTypeJavaLangObject()
          && !program.typeOracle.canBeJavaScriptObject(instanceType)) {
        return;
      }

      ensureDevirtualVersionExists(method);

      // Replaces this virtual method call with a static call to a devirtual version of the method.
      JMethod devirtualMethod = devirtualMethodByMethod.get(method);
      ctx.replaceMe(MakeCallsStatic.makeStaticCall(x, devirtualMethod));
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      // Don't rewrite the polymorphic call inside of the devirtualizing method!
      if (methodByDevirtualMethod.containsValue(x)) {
        return false;
      }
      return true;
    }

    /**
     * Constructs and caches a method that is a new static version of the given method or a
     * trampoline function that wraps a new static version of the given method. It chooses which to
     * construct based on how the given method's defining class relates to the JavascriptObject
     * class.
     */
    private void ensureDevirtualVersionExists(JMethod method) {
      if (devirtualMethodByMethod.containsKey(method)) {
        // already did this one before
        return;
      }

      JDeclaredType targetType = method.getEnclosingType();

      // Separate compilation treats all JSOs as if they are "dualImpl", as the interface might
      // be implemented by a regular Java object in a separate module.

      // TODO(rluble): (Separate compilation) JsoDevirtualizer should be run before optimizations
      // and optimizations need to be strong enough to perform the same kind of size reductions
      // achieved by keeping track of singleImpls.
      if (program.typeOracle.isDualJsoInterface(targetType)) {
        JMethod overridingMethod =
            findOverridingMethod(method, program.typeOracle.getSingleJsoImpl(targetType));
        assert overridingMethod != null;

        JMethod jsoStaticImpl = getStaticImpl(overridingMethod);
        JMethod devirtualMethod = getOrCreateDevirtualMethod(method, jsoStaticImpl);
        devirtualMethodByMethod.put(method, devirtualMethod);
      } else if (program.isJavaScriptObject(targetType)) {
        // It's a virtual JSO dispatch, usually occurs in draftCompile
        JMethod devirtualMethod = getStaticImpl(method);
        devirtualMethodByMethod.put(method, devirtualMethod);
      } else if (program.typeOracle.isSingleJsoImpl(targetType)) {
          JMethod overridingMethod =
              findOverridingMethod(method, program.typeOracle.getSingleJsoImpl(targetType));
          assert overridingMethod != null;

          JMethod jsoStaticImpl = getStaticImpl(overridingMethod);
          devirtualMethodByMethod.put(method, jsoStaticImpl);
      } else if (targetType == program.getTypeJavaLangObject()) {
        // it's a java.lang.Object overridden method in JSO
        JMethod overridingMethod = findOverridingMethod(method, program.getJavaScriptObject());
        if (overridingMethod != null) {
          JMethod jsoStaticImpl = getStaticImpl(overridingMethod);
          JMethod devirtualMethod = getOrCreateDevirtualMethod(method, jsoStaticImpl);
          devirtualMethodByMethod.put(method, devirtualMethod);
        } else {
          assert false : "Object method not overriden by JavaScriptObject";
        }
      } else {
        assert false : "Object method not related to JavaScriptObject";
      }
    }

    private boolean mightBeJsoMethod(JMethod method) {
      JDeclaredType targetType = method.getEnclosingType();

      if (targetType == null || !method.needsVtable()) {
        return false;
      } else if (devirtualMethodByMethod.containsKey(method)
          || program.isJavaScriptObject(targetType)
          || program.typeOracle.isSingleJsoImpl(targetType)
          || program.typeOracle.isDualJsoInterface(targetType)
          || targetType == program.getTypeJavaLangObject()) {
        return true;
      }
      return false;
    }
  }

  public static void exec(JProgram program) {
    new JsoDevirtualizer(program).execImpl();
  }

  /**
   * Maps each Object instance methods (ie, {@link Object#equals(Object)}) onto
   * its corresponding devirtualizing method.
   */
  protected Map<JMethod, JMethod> devirtualMethodByMethod = new HashMap<JMethod, JMethod>();

  /**
   * Contains the Cast.isJavaObject method.
   */
  private final JMethod isJavaObjectMethod;

  /**
   * Key is the method signature, value is the number of unique instances with
   * the same signature.
   */
  private Map<String, Integer> jsoMethodInstances = new HashMap<String, Integer>();

  /**
   * Contains the set of devirtualizing methods that replace polymorphic calls
   * to Object methods.
   */
  private final Map<JMethod, JMethod> methodByDevirtualMethod = new HashMap<JMethod, JMethod>();

  private final JProgram program;

  private final CreateStaticImplsVisitor staticImplCreator;

  private JsoDevirtualizer(JProgram program) {
    this.program = program;
    this.isJavaObjectMethod = program.getIndexedMethod("Cast.isJavaObject");
    staticImplCreator = new CreateStaticImplsVisitor(program);
  }

  private void execImpl() {
    JClassType jsoType = program.getJavaScriptObject();
    if (jsoType == null) {
      return;
    }

    RewriteVirtualDispatches rewriter = new RewriteVirtualDispatches();
    rewriter.accept(program);
    assert (rewriter.didChange());
  }

  /**
   * Finds the method that overrides this method, starting with the target
   * class.
   */
  private JMethod findOverridingMethod(JMethod method, JClassType target) {
    if (target == null) {
      return null;
    }
    for (JMethod overridingMethod : target.getMethods()) {
      if (JTypeOracle.methodsDoMatch(method, overridingMethod)) {
        return overridingMethod;
      }
    }
    return findOverridingMethod(method, target.getSuperClass());
  }

  /**
   * Create a conditional method to discriminate between static and virtual
   * dispatch.
   *
   * <pre>
    * static boolean equals__devirtual$(Object this, Object other) {
    *   return Cast.isJavaObject(this) ? this.equals(other) : JavaScriptObject.equals$(this, other);
    * }
    * </pre>
   */
  private JMethod getOrCreateDevirtualMethod(JMethod method, JMethod jsoImpl) {
    /**
     * TODO(cromwellian) generate a inlined expression instead of Method Because
     * devirtualization happens after optimization, the devirtual methods don't
     * optimize well in the JS pass. Consider "inlining" a hand optimized
     * devirtual method at callsites instead of a JMethodCall. As a bonus, the
     * inlined code can be specialized for each callsite, for example, if there
     * are no side effects, then there's no need for a temporary. Or, if the
     * instance can't possibly be java.lang.String, then the JSO check becomes a
     * cheaper check for typeMarker.
     */
    if (methodByDevirtualMethod.containsKey(method)) {
      return methodByDevirtualMethod.get(method);
    }

    JClassType jsoType = program.getJavaScriptObject();
    SourceInfo sourceInfo = method.getSourceInfo().makeChild();

    // Create the new method.
    String prefix;
    Integer methodCount;
    methodCount = jsoMethodInstances.get(method.getSignature());
    if (methodCount == null) {
      prefix = method.getName();
      methodCount = 0;
    } else {
      prefix = method.getName() + methodCount;
      methodCount++;
    }
    jsoMethodInstances.put(method.getSignature(), methodCount);
    String devirtualName = prefix + "__devirtual$";
    JMethod devirtualMethod =
        new JMethod(sourceInfo, devirtualName, jsoType, method.getType(), false, true, true,
            AccessModifier.PUBLIC);
    devirtualMethod.setBody(new JMethodBody(sourceInfo));
    jsoType.addMethod(devirtualMethod);
    devirtualMethod.setSynthetic();

    // Setup parameters.
    JParameter thisParam =
        JProgram.createParameter(sourceInfo, "this$static", program.getTypeJavaLangObject(), true,
            true, devirtualMethod);
    for (JParameter oldParam : method.getParams()) {
      JProgram.createParameter(sourceInfo, oldParam.getName(), oldParam.getType(), true, false,
          devirtualMethod);
    }
    devirtualMethod.freezeParamTypes();
    devirtualMethod.addThrownExceptions(method.getThrownExceptions());
    sourceInfo.addCorrelation(sourceInfo.getCorrelator().by(devirtualMethod));

    // maybeJsoInvocation = this$static
    JLocal temp =
        JProgram.createLocal(sourceInfo, "maybeJsoInvocation", thisParam.getType(), true,
            (JMethodBody) devirtualMethod.getBody());
    JMultiExpression multi = new JMultiExpression(sourceInfo);

    // (maybeJsoInvocation = this$static, )
    multi.addExpressions(JProgram.createAssignmentStmt(sourceInfo, new JLocalRef(sourceInfo, temp),
        new JParameterRef(sourceInfo, thisParam)).getExpr());

    // Build from bottom up.
    // isJavaObject(temp)
    JMethodCall isJavaObjectCheck = new JMethodCall(sourceInfo, null, isJavaObjectMethod);
    isJavaObjectCheck.addArg(new JLocalRef(sourceInfo, temp));

    // temp.method(args)
    JMethodCall thenValue =
        new JMethodCall(sourceInfo, new JLocalRef(sourceInfo, temp), method);
    for (JParameter param : devirtualMethod.getParams()) {
      if (param != thisParam) {
        thenValue.addArg(new JParameterRef(sourceInfo, param));
      }
    }

    // jso$method(temp, args)
    JMethodCall elseValue = new JMethodCall(sourceInfo, null, jsoImpl);
    elseValue.addArg(new JLocalRef(sourceInfo, temp));
    for (JParameter param : devirtualMethod.getParams()) {
      if (param != thisParam) {
        elseValue.addArg(new JParameterRef(sourceInfo, param));
      }
    }

    // isJavaObject(temp) ? temp.method(args) : jso$method(temp, args)
    JConditional conditional =
        new JConditional(sourceInfo, method.getType(), isJavaObjectCheck, thenValue, elseValue);

    multi.addExpressions(conditional);

    JReturnStatement returnStatement = new JReturnStatement(sourceInfo, multi);
    ((JMethodBody) devirtualMethod.getBody()).getBlock().addStmt(returnStatement);
    methodByDevirtualMethod.put(method, devirtualMethod);

    return devirtualMethod;
  }

  private JMethod getStaticImpl(JMethod method) {
    assert !method.isStatic();
    JMethod staticImpl = program.getStaticImpl(method);
    if (staticImpl == null) {
      staticImplCreator.accept(method);
      staticImpl = program.getStaticImpl(method);
    }
    return staticImpl;
  }
}
