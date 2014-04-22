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
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic.CreateStaticImplsVisitor;
import com.google.gwt.dev.jjs.impl.MakeCallsStatic.StaticCallConverter;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Devirtualization is the process of converting virtual method calls on instances that might be
 * a JSO, a string or and array (like "obj.doFoo();") to static calls (like
 * "SomeClass.doFoo__devirtual$(obj)).
 *
 * This transformation is done on arrays, strings and JSOs virtual method calls; as this objects
 * do not have the virtual methods in their prototypes. The static version is a trampoline that
 * decides how to dispatch the method.
 *
 * See https://code.google.com/p/google-web-toolkit/wiki/OverlayTypes for why this is done for JSOs.
 * <br />
 *
 * To complete the transformation:
 * <ul>
 *  <li>
 *  1. methods that need to be devirtualized must be turned into static functions.
 *  </li>
 *  <li>
 *  2. all method calls to the original functions must be rerouted either to the new static
 *      version or to a static dispatcher trampoline function that is created by this pass.
 *  </li>
 * </ul>
 * These trampolines are created whether a call to the function exists for separate compiled
 * modules to work. In a globally optimized build unused ones are pruned away. <br />
 *
 * This transform may NOT be run multiple times; it will create ever-expanding replacement
 * expressions.
 */
public class Devirtualizer {
  // TODO(rluble): rename the class as Devirtualizer as it deals with all three instances of
  // devirtualization (arrays, strings and JSOs).

  /**
   * Rewrite any virtual dispatches to Object, Strings or JavaScriptObject such that
   * dispatch occurs statically for JSOs, strings and arrays. <br />
   *
   * In the following cases JMethodCalls need to be rewritten:
   * <ol>
   * <li>a dual dispatch interface</li>
   * <li>a single dispatch trough single-jso interface</li>
   * <li>a java.lang.Object override from JavaScriptObject</li>
   * <li>methods defined at String</li>
   * <li>in draftMode, a 'static' virtual JSO call that hasn't been made
   * static yet.</li>
   * </ol>
   *
   */
  private class RewriteVirtualDispatches extends JModVisitor {

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (!mightNeedDevirtualization(x)) {
        return;
      }
      // The pruning pass will discard devirtualized methods that have not been called in
      // whole program optimizing mode.
      ensureDevirtualVersionExists(x);
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (!mightNeedDevirtualization(method)) {
        return;
      }
      JType instanceType = ((JReferenceType) x.getInstance().getType()).getUnderlyingType();

      if (instanceType instanceof JInterfaceType && ((JInterfaceType) instanceType).isJsInterface()) {
        return;
      }

      // If the instance can't possibly be a JSO, String or an interface implemented by String, do
      // not devirtualize.
      if (instanceType != program.getTypeJavaLangObject()
          && !program.typeOracle.canBeJavaScriptObject(instanceType)
          // not a string
          && instanceType != program.getTypeJavaLangString()
          // not an array
          && !(instanceType instanceof JArrayType)
          // not an interface of String, e.g. CharSequence or Comparable
          && !program.getTypeJavaLangString().getImplements().contains(instanceType)
          // it is a super.m() call and the superclass is not a JSO. (this case is NOT reached if
          // MakeCallsStatic was called).
          || x.isStaticDispatchOnly() && !program.isJavaScriptObject(method.getEnclosingType())) {
        return;
      }

      ensureDevirtualVersionExists(method);

      // Replaces this virtual method call with a static call to a devirtual version of the method.
      JMethod devirtualMethod = devirtualMethodByMethod.get(method);
      ctx.replaceMe(converter.convertCall(x, devirtualMethod));
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

      // TODO(rluble): (Separate compilation) Devirtualizer should be run before optimizations
      // and optimizations need to be strong enough to perform the same kind of size reductions
      // achieved by keeping track of singleImpls.

      //
      if (!program.typeOracle.isDualJsoInterface(targetType) &&
          program.typeOracle.isSingleJsoImpl(targetType)) {
        // Optimize the trampoline away when there is ONLY JSO dispatch.
        // TODO(rluble): verify that this case can not arise in optimized mode and if so
        // remove as is an unnecessary optimization.

        assert targetType instanceof JInterfaceType;
        assert !program.getTypeJavaLangString().getImplements().contains(targetType);

        JMethod overridingMethod =
            findOverridingMethod(method, program.typeOracle.getSingleJsoImpl(targetType));
        assert overridingMethod != null;

        JMethod jsoStaticImpl = getStaticImpl(overridingMethod);
        devirtualMethodByMethod.put(method, jsoStaticImpl);
      } else if (program.isJavaScriptObject(targetType)) {
        // A virtual dispatch on a target that is already known to be a JavaScriptObject, this
        // should have been handled by MakeCallsStatic.
        // TODO(rluble): verify that this case can not arise in optimized mode and if so
        // remove as is an unnecessary optimization.
        JMethod devirtualMethod = getStaticImpl(method);
        devirtualMethodByMethod.put(method, devirtualMethod);
      } else {
        JMethod devirtualMethod = getOrCreateDevirtualMethod(method);
        devirtualMethodByMethod.put(method, devirtualMethod);
      }
    }

    private boolean mightNeedDevirtualization(JMethod method) {
      JDeclaredType targetType = method.getEnclosingType();

      if (targetType == null || !method.needsVtable()) {
        return false;
      } else if (devirtualMethodByMethod.containsKey(method)
          || program.isJavaScriptObject(targetType)
          || program.typeOracle.isSingleJsoImpl(targetType)
          || program.typeOracle.isDualJsoInterface(targetType)
          || targetType == program.getTypeJavaLangObject()
          || targetType == program.getTypeJavaLangString()
          || program.getTypeJavaLangString().getImplements().contains(targetType)) {
        return true;
      }
      return false;
    }
  }

  public static void exec(JProgram program) {
    new Devirtualizer(program).execImpl();
  }

  /**
   * Returns true if getClass() is devirtualized for {@code type}; used in
   * {@link ReplaceGetClassOverrides} to avoid replacing getClass() methods that need
   * trampolines.
   */
  public static boolean isGetClassDevirtualized(JProgram program, JType type) {
    return type == program.getJavaScriptObject() || type == program.getTypeJavaLangString();
  }

  /**
   * Maps each Object instance methods (ie, {@link Object#equals(Object)}) onto
   * its corresponding devirtualizing method.
   */
  protected Map<JMethod, JMethod> devirtualMethodByMethod = Maps.newHashMap();

  /**
   * Contains the Cast.hasJavaObjectVirtualDispatch method.
   */
  private final JMethod hasJavaObjectVirtualDispatch;

  /**
   * Contains the Cast.isJavaString method.
   */
  private final JMethod isJavaStringMethod;

  /**
   * Contains the Cast.instanceofArray method.
   */
  private final JMethod isJavaArray;

  /**
   * Contains the set of devirtualizing methods that replace polymorphic calls
   * to Object methods.
   */
  private final Map<JMethod, JMethod> methodByDevirtualMethod = Maps.newHashMap();

  private final JProgram program;

  private final CreateStaticImplsVisitor staticImplCreator;
  private final StaticCallConverter converter;

  /**
   * Creates and empty devirtualized method for devirtualizing {@code method} in class
   * {@code inclass}.
   */
  private JMethod createDevirtualMethodFor(JMethod method, JDeclaredType inClass) {
    SourceInfo sourceInfo = method.getSourceInfo().makeChild();

    String prefix = computeEscapedSignature(method.getSignature());
    JMethod devirtualMethod = new JMethod(sourceInfo, prefix + "__devirtual$",
        inClass, method.getType(), false, true, true, AccessModifier.PUBLIC);
    devirtualMethod.setBody(new JMethodBody(sourceInfo));
    devirtualMethod.setSynthetic();
    inClass.addMethod(devirtualMethod);
    // Setup parameters.
    JProgram.createParameter(sourceInfo, "this$static", method.getEnclosingType(), true,
        true, devirtualMethod);
    for (JParameter oldParam : method.getParams()) {
      JProgram.createParameter(sourceInfo, oldParam.getName(), oldParam.getType(), true, false,
          devirtualMethod);
    }

    devirtualMethod.freezeParamTypes();
    devirtualMethod.addThrownExceptions(method.getThrownExceptions());
    sourceInfo.addCorrelation(sourceInfo.getCorrelator().by(devirtualMethod));

    return devirtualMethod;
  }

  /**
   * A normal method signature contains characters that are not valid in a method name. If you want
   * to construct a method name based on an existing method signature then those characters need to
   * be escaped.
   */
  private static String computeEscapedSignature(String methodSignature) {
    return methodSignature.replaceAll("[\\<\\>\\(\\)\\;\\/\\[]", "_");
  }

  private Devirtualizer(JProgram program) {
    this.program = program;
    this.isJavaStringMethod = program.getIndexedMethod("Cast.isJavaString");
    this.hasJavaObjectVirtualDispatch =
        program.getIndexedMethod("Cast.hasJavaObjectVirtualDispatch");
    this.isJavaArray = program.getIndexedMethod("Cast.isJavaArray");
    // TODO: consider turning on null checks for "this"?
    // However, for JSO's there is existing code that relies on nulls being okay.
    this.converter = new StaticCallConverter(program, false);
    staticImplCreator = new CreateStaticImplsVisitor(program);
  }

  private void execImpl() {
    JClassType jsoType = program.getJavaScriptObject();
    if (jsoType == null) {
      return;
    }

    RewriteVirtualDispatches rewriter = new RewriteVirtualDispatches();
    rewriter.accept(program);
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
   * Construct conditional expression for dispatch. Handle the cases where a dispatch or check
   * is null indicating impossibility of such operation.
   */
  private static JExpression constructMinimalCondition(JMethod checkMethod, JVariableRef target,
      JMethodCall trueDispatch, JExpression falseDispatch) {
    // TODO(rluble): Maybe we should emit slightly different code in checked mode, so that if
    // no condition is met an exception would be thrown rather than cascading.
    if (falseDispatch == null && trueDispatch == null) {
      return null;
    }
    if (falseDispatch == null) {
      // No need for condition to be evaluated.
      return trueDispatch;
    }
    if (trueDispatch == null || falseDispatch instanceof JMethodCall &&
        ((JMethodCall) falseDispatch).getTarget() == trueDispatch.getTarget()) {
      // Both branches do the same dispatch (or no trueDispatch).
      return falseDispatch;
    }
    JMethodCall condition =
        new JMethodCall(trueDispatch.getSourceInfo(), null, checkMethod, target);

    return new JConditional(condition.getSourceInfo(), trueDispatch.getType(), condition,
        trueDispatch, falseDispatch);
  }

  /**
   * Create a dispatch call taking the arguments from the devirtual method.
   */
  private static JMethodCall maybeCreateDispatch(JMethod dispatchTo,
      JMethod devirtualMethod) {
    if (dispatchTo == null) {
      return null;
    }
    List<JParameter> parameters = Lists.newArrayList(devirtualMethod.getParams());
    SourceInfo sourceInfo = devirtualMethod.getSourceInfo();
    JParameterRef thisParamRef = null;

    if (!dispatchTo.isStatic()) {
      // This is a virtual dispatch, take the first parameter as the receiver.
      thisParamRef = new JParameterRef(sourceInfo, parameters.remove(0));
    }

    JMethodCall dispatchCall = new JMethodCall(sourceInfo, thisParamRef,
        dispatchTo);
    for (JParameter param : parameters) {
      dispatchCall.addArg(new JParameterRef(sourceInfo, param));
    }
    return dispatchCall;
  }

  // Byte mask used by {@link ::getÓrCreateDevirtualMethod} to determine possible dispatches of a
  // method.
  private static final byte STRING = 0x01;
  private static final byte HAS_JAVA_VIRTUAL_DISPATCH = 0x02;
  private static final byte JAVA_ARRAY = 0x04;
  private static final byte JSO = 0x08;

  /**
   * Create a conditional method to discriminate between static and virtual
   * dispatch.
   *
   * <pre>
   * static boolean equals__devirtual$(Object this, Object other) {
   *   return Cast.isJavaString() ? String.equals(other) :
   *       Cast.hasJavaObjectVirtualDispatch(this) ?
   *       this.equals(other) : JavaScriptObject.equals$(this, other);
   * }
   * </pre>
   */
  private JMethod getOrCreateDevirtualMethod(JMethod method) {

    if (methodByDevirtualMethod.containsKey(method)) {
      return methodByDevirtualMethod.get(method);
    }

    /////////////////////////////////////////////////////////////////
    // 1. Determine which types of object are target of this dispatch
    /////////////////////////////////////////////////////////////////
    byte possibleTargetTypes = 0x0;
    JReferenceType enclosingType = method.getEnclosingType();
    if (enclosingType == program.getTypeJavaLangObject()) {
      // Object methods can be dispatched to all four possible classes.
      possibleTargetTypes = STRING | HAS_JAVA_VIRTUAL_DISPATCH | JAVA_ARRAY | JSO;
    } else if (enclosingType == program.getTypeJavaLangString()) {
      // String is final and can not be extended.
      possibleTargetTypes |= STRING;
    }

    if (program.typeOracle.isDualJsoInterface(enclosingType)) {
      // If it is an interface implemented both by JSOs and regular Java Objects;
      possibleTargetTypes = HAS_JAVA_VIRTUAL_DISPATCH | JSO;
    } else if (program.typeOracle.isSingleJsoImpl(enclosingType) ||
        program.isJavaScriptObject(enclosingType)) {
      // If it is either an interface implemented by JSOs or JavaScriptObject or one of its
      // subclasses.
      possibleTargetTypes = JSO;
    }

    if (program.getTypeJavaLangString().getImplements().contains(enclosingType)) {
      // If it is an interface implemented by String.
      possibleTargetTypes |= STRING | HAS_JAVA_VIRTUAL_DISPATCH;
    }

    /////////////////////////////////////////////////////////////////
    // 2. Compute the dispatch to method for each relevant case.
    /////////////////////////////////////////////////////////////////
    Map<Byte, JMethod> dispatchToMethodByTargetType = Maps.newTreeMap();
    if ((possibleTargetTypes & STRING) != 0) {
      JMethod overridingMethod = findOverridingMethod(method, program.getTypeJavaLangString());
      assert overridingMethod != null : method.getEnclosingType().getName() + "::" +
          method.getName() + " not overridden by String";
      dispatchToMethodByTargetType.put(STRING, getStaticImpl(overridingMethod));
    }
    if ((possibleTargetTypes & JSO) != 0) {
      JMethod overridingMethod = findOverridingMethod(method,
          program.typeOracle.getSingleJsoImpl(enclosingType));
      if (overridingMethod == null && enclosingType == program.getTypeJavaLangObject()) {
        overridingMethod = findOverridingMethod(method, program.getJavaScriptObject());
      }
      assert overridingMethod != null : method.getEnclosingType().getName() + "::" +
          method.getName() + " not overridden by JavaScriptObject";
      dispatchToMethodByTargetType.put(JSO, getStaticImpl(overridingMethod));
    }
    if ((possibleTargetTypes & JAVA_ARRAY) != 0) {
      // Arrays only implement Object methods as the Clonable interface is not supported in GWT.
      JMethod overridingMethod = findOverridingMethod(method, program.getTypeJavaLangObject());
      assert overridingMethod != null : method.getEnclosingType().getName() + "::" +
          method.getName() + " not overridden by Object";
      dispatchToMethodByTargetType.put(JAVA_ARRAY, getStaticImpl(overridingMethod));
    }
    if ((possibleTargetTypes & HAS_JAVA_VIRTUAL_DISPATCH) != 0) {
      dispatchToMethodByTargetType.put(HAS_JAVA_VIRTUAL_DISPATCH, method);
    }

    /////////////////////////////////////////////////////////////////
    // 3. Create a devirtualized method.
    /////////////////////////////////////////////////////////////////

    // Decide where to place the devirtual method. Ideally these methods should reside in the
    // declaring type, but some of these will be interfaces and currently GWT does not emit
    // any code for them.
    // TODO(rluble): place interface methods in the corresponding interface once Java 9 defender
    // method support is implemented.
    JClassType devirtualMethodEnclosingClass  = null;
    if (method.getEnclosingType() instanceof JClassType) {
      devirtualMethodEnclosingClass = (JClassType) method.getEnclosingType();
    } else if (dispatchToMethodByTargetType.get(STRING) != null) {
      // Methods from interfaces implemented by String end up in String.
      devirtualMethodEnclosingClass = program.getTypeJavaLangString();
    } else if (dispatchToMethodByTargetType.get(JSO) != null) {
      // This is an interface method implemented by a JSO, place in the JSO class.
      devirtualMethodEnclosingClass = (JClassType)
          dispatchToMethodByTargetType.get(JSO).getEnclosingType();
    } else {
      // It is an interface implemented by String or arrays, place it in Object.
      devirtualMethodEnclosingClass = program.getTypeJavaLangObject();
    }
    // Devirtualization of external methods stays external and devirtualization of internal methods
    // stays internal.
    assert program.isReferenceOnly(devirtualMethodEnclosingClass)
        == program.isReferenceOnly(method.getEnclosingType());
    // TODO(stalcup): devirtualization is modifying both internal and external types. Really
    // external types should never be modified. Change the point at which types are saved into
    // libraries to be after normalization has occurred, so that no further modification is
    // necessary when loading external types.
    JMethod devirtualMethod = createDevirtualMethodFor(method, devirtualMethodEnclosingClass);

    /**
     * Encoding
     */
    SourceInfo sourceInfo = method.getSourceInfo().makeChild();
    JParameter thisParam = devirtualMethod.getParams().get(0);

    // Synthesize the dispatch at a single conditional doing the checks in this order.
    //   isString(obj) ? dispatchToString : (
    //     isRegularJavaObject(obj) ? obj.method : (
    //       isArray(obj) ?
    //         dispatchToArray :
    //         dispatchToJSO
    //     )
    //   )

    // Construct back to fort. Last is JSO.
    JExpression dispatchExpression =
        maybeCreateDispatch(dispatchToMethodByTargetType.get(JSO), devirtualMethod);

    // Dispatch to array
    dispatchExpression = constructMinimalCondition(
        isJavaArray,
        new JParameterRef(thisParam.getSourceInfo(), thisParam),
        maybeCreateDispatch(dispatchToMethodByTargetType.get(JAVA_ARRAY), devirtualMethod),
        dispatchExpression);

    // Dispatch to regular object
    dispatchExpression = constructMinimalCondition(
        hasJavaObjectVirtualDispatch,
        new JParameterRef(thisParam.getSourceInfo(), thisParam),
        maybeCreateDispatch(
            dispatchToMethodByTargetType.get(HAS_JAVA_VIRTUAL_DISPATCH), devirtualMethod),
        dispatchExpression);

    // Dispatch to regular string
    dispatchExpression = constructMinimalCondition(
        isJavaStringMethod,
        new JParameterRef(thisParam.getSourceInfo(), thisParam),
        maybeCreateDispatch(dispatchToMethodByTargetType.get(STRING), devirtualMethod),
        dispatchExpression);

    // return dispatchConditional;
    JReturnStatement returnStatement = new JReturnStatement(sourceInfo, dispatchExpression);

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
