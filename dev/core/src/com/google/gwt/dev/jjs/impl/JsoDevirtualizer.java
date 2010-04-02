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
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic.CreateStaticImplsVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * All call sites that might result in virtual dispatch to a JSO must be
 * rewritten to force static dispatch. This transform may NOT be run multiple
 * times; it will create ever-expanding replacement expressions.
 */
public class JsoDevirtualizer {

  /**
   * Rewrite any virtual dispatches to Object or JavaScriptObject such that
   * dispatch occurs statically for JSOs.
   */
  private class RewriteVirtualDispatches extends JModVisitor {

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      JMethod newMethod;
      if (virtualJsoMethods.contains(method)) {
        /*
         * Force a JSO call to be static. Really, this should never be necessary
         * as long as MakeCallsStatic runs. However, we would rather not insist
         * that normalization depends on optimization having been done.
         */
        newMethod = program.getStaticImpl(method);
      } else if (objectMethodToJsoMethod.keySet().contains(method)) {
        /*
         * Map the object call to its appropriate devirtualizing method.
         */
        newMethod = objectMethodToJsoMethod.get(method);
      } else {
        return;
      }
      assert (newMethod != null);
      ctx.replaceMe(MakeCallsStatic.makeStaticCall(x, newMethod));
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      // Don't rewrite the polymorphic call inside of the devirtualizing method!
      if (objectMethodToJsoMethod.values().contains(x)) {
        return false;
      }
      return true;
    }
  }

  public static void exec(JProgram program) {
    new JsoDevirtualizer(program).execImpl();
  }

  /**
   * Maps each Object instance methods (ie, {@link Object#equals(Object)}) onto
   * its corresponding devirtualizing method.
   */
  protected Map<JMethod, JMethod> objectMethodToJsoMethod = new HashMap<JMethod, JMethod>();

  /**
   * Contains the set of live instance methods on JavaScriptObject; typically
   * overrides of Object methods.
   */
  protected Set<JMethod> virtualJsoMethods = new HashSet<JMethod>();

  /**
   * Contains the set of devirtualizing methods that replace polymorphic calls
   * to Object methods.
   */
  private Set<JMethod> devirtualMethods = new HashSet<JMethod>();

  /**
   * Contains the Cast.isJavaObject method.
   */
  private final JMethod isJavaObjectMethod;

  private final JProgram program;

  private JsoDevirtualizer(JProgram program) {
    this.program = program;
    this.isJavaObjectMethod = program.getIndexedMethod("Cast.isJavaObject");
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
  private JMethod createDevirtualMethod(JMethod objectMethod, JMethod jsoImpl) {
    JClassType jsoType = program.getJavaScriptObject();
    SourceInfo sourceInfo = jsoType.getSourceInfo();

    // Create the new method.
    String name = objectMethod.getName() + "__devirtual$";
    JMethod newMethod = program.createMethod(sourceInfo.makeChild(
        JsoDevirtualizer.class, "Devirtualized method"), name, jsoType,
        objectMethod.getType(), false, true, true, false, false);
    newMethod.setSynthetic();

    // Setup parameters.
    JParameter thisParam = JProgram.createParameter(sourceInfo, "this$static",
        program.getTypeJavaLangObject(), true, true, newMethod);
    for (JParameter oldParam : objectMethod.getParams()) {
      JProgram.createParameter(sourceInfo, oldParam.getName(),
          oldParam.getType(), true, false, newMethod);
    }
    newMethod.freezeParamTypes();
    newMethod.addThrownExceptions(objectMethod.getThrownExceptions());

    // Build from bottom up.
    JMethodCall condition = new JMethodCall(sourceInfo, null,
        isJavaObjectMethod);
    condition.addArg(new JParameterRef(sourceInfo, thisParam));

    JMethodCall thenValue = new JMethodCall(sourceInfo, new JParameterRef(
        sourceInfo, thisParam), objectMethod);
    for (JParameter param : newMethod.getParams()) {
      if (param != thisParam) {
        thenValue.addArg(new JParameterRef(sourceInfo, param));
      }
    }

    JMethodCall elseValue = new JMethodCall(sourceInfo, null, jsoImpl);
    for (JParameter param : newMethod.getParams()) {
      elseValue.addArg(new JParameterRef(sourceInfo, param));
    }

    JConditional conditional = new JConditional(sourceInfo, objectMethod.getType(),
        condition, thenValue, elseValue);

    JReturnStatement returnStatement = new JReturnStatement(sourceInfo,
        conditional);
    ((JMethodBody) newMethod.getBody()).getBlock().addStmt(returnStatement);
    return newMethod;
  }

  private void execImpl() {
    JClassType jsoType = program.getJavaScriptObject();
    if (jsoType == null) {
      return;
    }

    for (JMethod method : jsoType.getMethods()) {
      if (method.needsVtable()) {
        virtualJsoMethods.add(method);
      }
    }

    if (virtualJsoMethods.isEmpty()) {
      return;
    }

    CreateStaticImplsVisitor creator = new CreateStaticImplsVisitor(program);
    for (JMethod method : virtualJsoMethods) {
      // Ensure staticImpls exist for any instance methods.
      JMethod jsoStaticImpl = program.getStaticImpl(method);
      if (jsoStaticImpl == null) {
        creator.accept(method);
        jsoStaticImpl = program.getStaticImpl(method);
        assert (jsoStaticImpl != null);
      }

      // Find the object method this instance method overrides.
      JMethod objectOverride = findObjectOverride(method);
      if (objectOverride != null) {
        JMethod devirtualizer = createDevirtualMethod(objectOverride,
            jsoStaticImpl);
        devirtualMethods.add(devirtualizer);
        objectMethodToJsoMethod.put(objectOverride, devirtualizer);
      }
    }

    RewriteVirtualDispatches rewriter = new RewriteVirtualDispatches();
    rewriter.accept(program);
    assert (rewriter.didChange());
  }

  /**
   * Finds the object method this method overrides.
   */
  private JMethod findObjectOverride(JMethod method) {
    Set<JMethod> overrides = program.typeOracle.getAllRealOverrides(method);
    JMethod objectOverride = null;
    for (JMethod override : overrides) {
      if (override.getEnclosingType() == program.getTypeJavaLangObject()) {
        objectOverride = override;
        break;
      }
    }
    return objectOverride;
  }

}
