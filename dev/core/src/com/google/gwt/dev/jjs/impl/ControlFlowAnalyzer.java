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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * This class finds out what code in a program is live based on starting
 * execution at a specified location.
 */
public class ControlFlowAnalyzer {

  /**
   * A callback for recording control-flow dependencies as they are discovered.
   * See {@link ControlFlowAnalyzer#setDependencyRecorder(DependencyRecorder)}.
   */
  public interface DependencyRecorder {
    /**
     * Used to record the dependencies of a specific method.
     */
    void methodIsLiveBecause(JMethod liveMethod, List<JMethod> dependencyChain);
  }

  /**
   * Marks as "referenced" any types, methods, and fields that are reachable.
   * Also marks as "instantiable" any classes and interfaces that can possibly
   * be instantiated.
   *
   * TODO(later): make RescueVisitor use less stack?
   */
  private class RescueVisitor extends JVisitor {
    private final List<JMethod> curMethodStack = Lists.newArrayList();

    private RescueVisitor() {
      /*
       * Rescue any types that would have been rescued by JCastOperation/JInstanceOf
       * after normalization.
       */
      if (rescuedViaCast != null) {
        for (JReferenceType type : rescuedViaCast) {
          rescue(type, true, true);
        }
        rescuedViaCast.clear();
      }
    }

    @Override
    public boolean visit(JArrayType type, Context ctx) {
      assert (referencedTypes.contains(type));
      boolean isInstantiated = instantiatedTypes.contains(type);

      JType leafType = type.getLeafType();
      int dims = type.getDims();

      // Rescue my super array type
      boolean didSuperType = false;
      if (leafType instanceof JClassType) {
        JClassType superClass = ((JClassType) leafType).getSuperClass();
        if (superClass != null) {
          // FooSub[] -> Foo[]
          rescue(program.getTypeArray(superClass, dims), true, isInstantiated);
          didSuperType = true;
        }
      } else if (leafType instanceof JInterfaceType) {
        // Intf[] -> Object[]
        rescue(program.getTypeArray(program.getTypeJavaLangObject(), dims), true, isInstantiated);
        didSuperType = true;
      }
      if (!didSuperType) {
        if (dims > 1) {
          // anything[][] -> Object[]
          rescue(program.getTypeArray(program.getTypeJavaLangObject(), dims - 1), true,
              isInstantiated);
        } else {
          // anything[] -> Object
          rescue(program.getTypeJavaLangObject(), true, isInstantiated);
        }
      }

      // Rescue super interface array types.
      if (leafType instanceof JDeclaredType) {
        JDeclaredType dLeafType = (JDeclaredType) leafType;
        for (JInterfaceType intfType : dLeafType.getImplements()) {
          JArrayType intfArray = program.getTypeArray(intfType, dims);
          rescue(intfArray, true, isInstantiated);
        }
      }

      return false;
    }

    @Override
    public boolean visit(JBinaryOperation x, Context ctx) {
      if (x.isAssignment() && x.getLhs() instanceof JFieldRef) {
        fieldsWritten.add(((JFieldRef) x.getLhs()).getField());
      }

      // special string concat handling
      if ((x.getOp() == JBinaryOperator.CONCAT || x.getOp() == JBinaryOperator.ASG_CONCAT)) {
        rescueByConcat(x.getLhs().getType());
        rescueByConcat(x.getRhs().getType());
      } else if (x.getOp() == JBinaryOperator.ASG) {
        // Don't rescue variables that are merely assigned to and never read
        boolean doSkip = false;
        JExpression lhs = x.getLhs();
        if (lhs.hasSideEffects() || isVolatileField(lhs)) {
          /*
           * If the lhs has side effects, skipping it would lose the side
           * effect. If the lhs is volatile, also keep it. This behavior
           * provides a useful idiom for test cases to prevent code from being
           * pruned.
           */
        } else if (lhs instanceof JLocalRef) {
          // locals are ok to skip
          doSkip = true;
        } else if (lhs instanceof JParameterRef) {
          // parameters are ok to skip
          doSkip = true;
        } else if (lhs instanceof JFieldRef) {
          // fields must rescue the qualifier
          doSkip = true;
          JFieldRef fieldRef = (JFieldRef) lhs;
          JExpression instance = fieldRef.getInstance();
          if (instance != null) {
            accept(instance);
          }
        }

        if (doSkip) {
          accept(x.getRhs());
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean visit(JCastOperation x, Context ctx) {
      // Rescue any JavaScriptObject type that is the target of a cast.
      JType targetType = x.getCastType();
      if (!program.typeOracle.canBeInstantiatedInJavascript(targetType)) {
        return true;
      }
      rescue((JReferenceType) targetType, true, true);
      JType exprType = x.getExpr().getType();
      if (program.typeOracle.isSingleJsoImpl(targetType)) {
        /*
         * It's a JSO interface, check if the source expr can be a live JSO:
         * 1) source is java.lang.Object (JSO could have been assigned to it)
         * 2) source is JSO
         * 3) source is SingleJSO interface whose implementor is live
         */
        if (program.getTypeJavaLangObject() == exprType
            || program.typeOracle.canBeJavaScriptObject(exprType)) {
          // source is JSO or SingleJso interface whose implementor is live
          JClassType jsoImplementor =
              program.typeOracle.getSingleJsoImpl((JReferenceType) targetType);
          if (jsoImplementor != null) {
            rescue(jsoImplementor, true, true);
          }
        }
      } else if (program.typeOracle.isJsInterface(targetType)
          && ((JInterfaceType) targetType).getJsPrototype() != null) {
        // keep alive JsInterface with prototype used in cast so it can used in cast checks against JS objects later
        rescue((JReferenceType) targetType, true, true);
      }

      return true;
    }

    @Override
    public boolean visit(JClassLiteral x, Context ctx) {
      JField field = x.getField();
      assert field != null;
      rescue(field);
      return true;
    }

    @Override
    public boolean visit(JClassType type, Context ctx) {
      assert (referencedTypes.contains(type));
      boolean isInstantiated = instantiatedTypes.contains(type);

      // Rescue my super type
      rescue(type.getSuperClass(), true, isInstantiated);

      // Rescue my clinit (it won't ever be explicitly referenced)
      if (type.hasClinit()) {
        rescue(type.getClinitMethod());
      }

      // JLS 12.4.1: don't rescue my super interfaces just because I'm rescued.
      // However, if I'm instantiated, let's mark them as instantiated.
      for (JInterfaceType intfType : type.getImplements()) {
        rescue(intfType, false, isInstantiated);
      }

      rescueMembersIfInstantiable(type);
      return false;
    }

    @Override
    public boolean visit(JDeclarationStatement x, Context ctx) {
      /*
       * A declaration by itself doesn't rescue a local (even if it has an
       * initializer). Writes don't count, only reads.
       */
      if (x.getInitializer() != null &&
          !isStaticFieldInitializedToLiteral(x.getVariableRef().getTarget())) {
        /*
         * Don't traverse literal initializers, because those become live when
         * the variable is accessed, not when its declaration runs.
         */
        accept(x.getInitializer());

        if (x.getVariableRef().getTarget() instanceof JField) {
          fieldsWritten.add((JField) x.getVariableRef().getTarget());
        }
      }

      // If the lhs is a field ref, we have to visit its qualifier.
      JVariableRef variableRef = x.getVariableRef();
      if (variableRef instanceof JFieldRef) {
        JFieldRef fieldRef = (JFieldRef) variableRef;
        JExpression instance = fieldRef.getInstance();
        if (instance != null) {
          accept(instance);
        }
      }
      return false;
    }

    @Override
    public boolean visit(JFieldRef ref, Context ctx) {
      JField target = ref.getField();

      /*
       * JLS 12.4.1: references to static, non-final, or
       * non-compile-time-constant fields rescue the enclosing class. JDT
       * already folds in compile-time constants as literals, so we must rescue
       * the enclosing types for any static fields that make it here.
       */
      if (target.isStatic()) {
        rescue(target.getEnclosingType(), true, false);
      }
      if (target.isStatic() || instantiatedTypes.contains(target.getEnclosingType())) {
        rescue(target);
      } else {
        // It's a field whose class is not instantiable
        if (!liveFieldsAndMethods.contains(target)) {
          membersToRescueIfTypeIsInstantiated.add(target);
        }
      }
      return true;
    }

    @Override
    public boolean visit(JInstanceOf x, Context ctx) {
      JReferenceType targetType = x.getTestType();
      if (program.typeOracle.isJsInterface(targetType)
          && ((JInterfaceType) targetType).getJsPrototype() != null) {
        // keep alive JsInterface with prototype used in cast so it can used in cast checks against JS objects later
        rescue((JReferenceType) targetType, true, true);
      }
      return true;
    }

    @Override
    public boolean visit(JInterfaceType type, Context ctx) {
      boolean isReferenced = referencedTypes.contains(type);
      boolean isInstantiated = instantiatedTypes.contains(type);
      assert (isReferenced || isInstantiated);

      // Rescue my clinit (it won't ever be explicitly referenced)
      if (type.hasClinit()) {
        rescue(type.getClinitMethod());
      }

      // JLS 12.4.1: don't rescue my super interfaces just because I'm rescued.
      // However, if I'm instantiated, let's mark them as instantiated.
      if (isInstantiated) {
        for (JInterfaceType intfType : type.getImplements()) {
          rescue(intfType, false, true);
        }
      }

      rescueMembersIfInstantiable(type);
      return false;
    }

    @Override
    public boolean visit(JLocalRef ref, Context ctx) {
      JLocal target = ref.getLocal();
      rescue(target);
      return true;
    }

    @Override
    public boolean visit(final JMethod x, Context ctx) {
      JReferenceType enclosingType = x.getEnclosingType();
      if (program.isJavaScriptObject(enclosingType)) {
        // Calls to JavaScriptObject types rescue those types.
        boolean instance = !x.isStatic() || program.isStaticImpl(x);
        rescue(enclosingType, true, instance);
      } else if (x.isStatic()) {
        // JLS 12.4.1: references to static methods rescue the enclosing class
        rescue(enclosingType, true, false);
      }

      if (x.isNative()) {
        // Manually rescue native parameter references
        final JsniMethodBody body = (JsniMethodBody) x.getBody();
        final JsFunction func = body.getFunc();

        new JsVisitor() {
          @Override
          public void endVisit(JsNameRef nameRef, JsContext ctx) {
            JsName ident = nameRef.getName();

            if (ident != null) {
              // If we're referencing a parameter, rescue the associated
              // JParameter
              int index = func.getParameters().indexOf(ident.getStaticRef());
              if (index != -1) {
                rescue(x.getParams().get(index));
              }
            }
          }
        }.accept(func);
      }

      return true;
    }

    @Override
    public boolean visit(JMethodCall call, Context ctx) {
      JMethod method = call.getTarget();
      if (call.isVolatile() && method == runAsyncOnsuccess) {
        /*
         * Note: In order to preserve code splitting, don't allow code flow from the
         * AsyncFragmentLoader implementation back into the
         * callback.onSuccess(). If we did, the rescue path would look like
         * JRunAsync -> AsyncFragmentLoader.runAsync() -> callback.onSuccess().
         * This would completely defeat code splitting as all the code on the
         * other side of the barrier would become reachable.
         *
         * Code flow analysis is run separately on methods which implement
         * RunAsyncCallback.onSuccess() as top-level entry points.
         */
        return true;
      }
      if (method.isStatic() || program.isJavaScriptObject(method.getEnclosingType())
          || instantiatedTypes.contains(method.getEnclosingType())) {
        rescue(method);
      } else {
        // It's a virtual method whose class is not instantiable
        if (!liveFieldsAndMethods.contains(method)) {
          membersToRescueIfTypeIsInstantiated.add(method);
        }
      }

      if (argsToRescueIfParameterRead == null || method.canBePolymorphic()
          || call instanceof JsniMethodRef) {
        return true;
      }

      if (program.instanceMethodForStaticImpl(method) != null) {
        // CleanUpRefsVisitor does not prune these params, must rescue.
        return true;
      }

      return rescueArgumentsIfParametersCanBeRead(call, method);
    }

    @Override
    public boolean visit(JNewArray newArray, Context ctx) {
      // rescue and instantiate the array type
      JArrayType arrayType = newArray.getArrayType();
      if (newArray.dims != null) {
        // rescue my type and all the implicitly nested types (with fewer dims)
        int nDims = arrayType.getDims();
        JType leafType = arrayType.getLeafType();
        assert (newArray.dims.size() == nDims);
        for (int i = 0; i < nDims; ++i) {
          if (newArray.dims.get(i) instanceof JAbsentArrayDimension) {
            break;
          }
          rescue(program.getTypeArray(leafType, nDims - i), true, true);
        }
      } else {
        // just rescue my own specific type
        rescue(arrayType, true, true);
      }
      return true;
    }

    @Override
    public boolean visit(JNewInstance x, Context ctx) {
      // rescue and instantiate the target class!
      rescueAndInstantiate(x.getClassType());
      return super.visit(x, ctx);
    }

    @Override
    public boolean visit(JParameterRef x, Context ctx) {
      // rescue the parameter for future pruning purposes
      rescue(x.getParameter());
      return true;
    }

    @Override
    public boolean visit(JsniFieldRef x, Context ctx) {
      /*
       * SPECIAL: this could be an assignment that passes a value from
       * JavaScript into Java.
       */
      if (x.isLvalue()) {
        maybeRescueJavaScriptObjectPassingIntoJava(x.getField().getType());
      }
      // JsniFieldRef rescues as JFieldRef
      return visit((JFieldRef) x, ctx);
    }

    @Override
    public boolean visit(JsniMethodBody body, Context ctx) {
      liveStrings.addAll(body.getUsedStrings());
      return true;
    }

    @Override
    public boolean visit(JsniMethodRef x, Context ctx) {
      /*
       * SPECIAL: each argument of the call passes a value from JavaScript into
       * Java.
       */
      List<JParameter> params = x.getTarget().getParams();
      for (int i = 0, c = params.size(); i < c; ++i) {
        JParameter param = params.get(i);
        maybeRescueJavaScriptObjectPassingIntoJava(param.getType());

        /*
         * Because we're not currently tracking methods through JSNI, we need to
         * assume that it's not safe to prune parameters of a method referenced
         * as such.
         *
         * A better solution would be to perform basic escape analysis to ensure
         * that the function reference never escapes, or at minimum, ensure that
         * the method is immediately called after retrieving the method
         * reference.
         */
        rescue(param);
      }
      // JsniMethodRef rescues as a JMethodCall
      if (x.getTarget() instanceof JConstructor) {
        // But if a constructor is targeted, there is an implicit 'new' op.
        JConstructor ctor = (JConstructor) x.getTarget();
        rescueAndInstantiate(ctor.getEnclosingType());
      }
      return visit((JMethodCall) x, ctx);
    }

    @Override
    public boolean visit(JStringLiteral literal, Context ctx) {
      liveStrings.add(literal.getValue());

      // rescue and instantiate java.lang.String
      rescue(program.getTypeJavaLangString(), true, true);
      return true;
    }

    private boolean canBeInstantiatedInJavaScript(JType type) {
      if (program.typeOracle.canBeInstantiatedInJavascript(type) ||
          program.isJavaLangString(type)) {
        return true;
      }

      if (!(type instanceof JArrayType)) {
        return false;
      }

      /*
       * Hackish: in our own JRE we sometimes create "not quite baked" arrays
       * in JavaScript for expediency.
       */
      JArrayType arrayType = (JArrayType) type;
      JType elementType = arrayType.getElementType();
      return elementType instanceof JPrimitiveType || canBeInstantiatedInJavaScript(elementType);
    }

    private JMethod getStringValueOfCharMethod() {
      JPrimitiveType charType = program.getTypePrimitiveChar();
      JClassType stringType = program.getTypeJavaLangString();
      if (stringValueOfChar != null) {
        return stringValueOfChar;
      }

      for (JMethod method : stringType.getMethods()) {
        if (method.getName().equals("valueOf") &&
            method.getOriginalParamTypes().size() == 1 &&
            method.getOriginalParamTypes().get(0) == charType) {
          stringValueOfChar = method;
          return stringValueOfChar;
        }
      }
      assert false;
      return null;
    }

    private boolean isStaticFieldInitializedToLiteral(JVariable var) {
      if (!(var instanceof JField)) {
        return false;
      }

      JField field = (JField) var;
      return field.isStatic() && field.getLiteralInitializer() != null;
    }

    private boolean isVolatileField(JExpression x) {
      if (!(x instanceof JFieldRef)) {
        return false;
      }

      JFieldRef xFieldRef = (JFieldRef) x;
      return xFieldRef.getField().isVolatile();
    }

    private void maybeRescueClassLiteral(JReferenceType type) {
      if (liveFieldsAndMethods.contains(getClassMethod) ||
          liveFieldsAndMethods.contains(getClassField)) {
        // getClass() already live so rescue class literal immediately
        rescue(program.getClassLiteralField(type));
      } else {
        // getClass() not live yet, so mark for later rescue
        classLiteralsToBeRescuedIfGetClassIsLive.add(type);
      }
    }

    /**
     * Subclasses of JavaScriptObject are never instantiated directly. They are
     * implicitly created when a JSNI method passes a reference to an existing
     * JS object into Java code. If any point in the program can pass a value
     * from JS into Java which could potentially be cast to JavaScriptObject, we
     * must rescue JavaScriptObject.
     *
     * @param type The type of the value passing from Java to JavaScript.
     * @see com.google.gwt.core.client.JavaScriptObject
     */
    private void maybeRescueJavaScriptObjectPassingIntoJava(JType type) {
      if (!canBeInstantiatedInJavaScript(type)) {
        return;
      }
      rescue((JReferenceType) type, true, true);
      if (program.typeOracle.isSingleJsoImpl(type)) {
        // Cast of JSO into SingleJso interface, rescue the implementor if exists
        JClassType singleJsoImpl = program.typeOracle.getSingleJsoImpl((JReferenceType) type);
        if (singleJsoImpl != null) {
          rescue(singleJsoImpl, true, true);
        }
      }
    }

    private void rescue(JMethod method) {
      if (method == null) {
        return;
      }

      if (!liveFieldsAndMethods.add(method)) {
        // Already in the set.
        return;
      }

      membersToRescueIfTypeIsInstantiated.remove(method);
      if (dependencyRecorder != null) {
        curMethodStack.add(method);
        dependencyRecorder.methodIsLiveBecause(method, curMethodStack);
      }
      accept(method);
      if (dependencyRecorder != null) {
        curMethodStack.remove(curMethodStack.size() - 1);
      }
      if (method.isNative()) {
        /*
         * SPECIAL: returning from this method passes a value from
         * JavaScript into Java.
         */
        maybeRescueJavaScriptObjectPassingIntoJava(method.getType());
      }
      rescueOverridingMethods(method);
      if (method == getClassMethod) {
        rescueClassLiteralsIfGetClassIsLive();
      }
      if (JProgram.isJsInterfacePrototype(method.getEnclosingType())) {
        // for JsInterface Prototype methods, rescue all parameters
        for (JParameter param : method.getParams()) {
          rescue(param);
        }
      }
      if (method.getSpecialization() != null) {
        rescue(method.getSpecialization().getTargetMethod());
      }
    }

    private void rescue(JReferenceType type, boolean isReferenced, boolean isInstantiated) {
      if (type == null) {
        return;
      }

      /*
       * Track references and instantiability at the granularity of run-time
       * types. For example, ignore nullness.
       */
      type = type.getUnderlyingType();

      boolean doVisit = false;
      if (isInstantiated && instantiatedTypes.add(type)) {
        maybeRescueClassLiteral(type);
        doVisit = true;
      }

      if (isReferenced && referencedTypes.add(type)) {
        doVisit = true;
      }

      if (!doVisit) {
        return;
      }

      accept(type);
      if (type instanceof JInterfaceType) {
        /*
         * For @JsInterface with a Java implementor, we rescue all interface
         * methods because we don't know if they'll be called from JS or not.
         * That is, the Java implementor may be called because the interface
         * was passed into JS, or it may be called via exported functions.
         *
         * We may be able to tighten this to check for @JsExport as well,
         * since if there is no @JsExport, the only way for JS code to get a
         * reference to the interface is by it being constructed in Java
         * and passed via JSNI into JS, and in that mechanism, the
         * rescue would happen automatically.
         */
        JInterfaceType intfType = (JInterfaceType) type;

        if (intfType.isJsInterface()) {
          for (JMethod method : intfType.getMethods()) {
            rescue(method);
          }
        }
      }
      if (!(type instanceof JDeclaredType)) {
        return;
      }
      for (JNode artificial : ((JDeclaredType) type).getArtificialRescues()) {
        if (artificial instanceof JReferenceType) {
          rescue((JReferenceType) artificial, true, true);
        } else if (artificial instanceof JVariable) {
          rescue((JVariable) artificial);
        } else if (artificial instanceof JMethod) {
          rescue((JMethod) artificial);
        }
      }
    }

    private void rescue(JVariable var) {
      if (var == null) {
        return;
      }
      if (!liveFieldsAndMethods.add(var)) {
        // Already rescued.
        return;
      }
      membersToRescueIfTypeIsInstantiated.remove(var);
      if (var == getClassField) {
        rescueClassLiteralsIfGetClassIsLive();
      }

      if (isStaticFieldInitializedToLiteral(var)) {
        /*
         * Rescue literal initializers when the field is rescued, not when
         * the static initializer runs. This allows fields initialized to
         * string literals to only need the string literals when the field
         * itself becomes live.
         */
        accept(((JField) var).getLiteralInitializer());
      } else if (var instanceof JField
          && (program.getTypeClassLiteralHolder().equals(((JField) var).getEnclosingType()))) {
        /*
         * Rescue just slightly less than what would normally be rescued for
         * a field reference to the literal's field. Rescue the field
         * itself, and its initializer, but do NOT rescue the whole
         * enclosing class. That would pull in the clinit of that class,
         * which has initializers for all the class literals, which in turn
         * have all of the strings of all of the class names.
         *
         * This work is done in rescue() to allow JSNI references to class
         * literals (via the @Foo::class syntax) to correctly rescue class
         * literal initializers.
         *
         * TODO: Model ClassLiteral access a different way to avoid special
         * handling. See
         *  Pruner.transformToNullFieldRef()/transformToNullMethodCall().
         */
        JField field = (JField) var;
        accept(field.getInitializer());
        referencedTypes.add(field.getEnclosingType());
        liveFieldsAndMethods.add(field.getEnclosingType().getClinitMethod());
      } else if (argsToRescueIfParameterRead != null && var instanceof JParameter) {
        List<JExpression> list = argsToRescueIfParameterRead.removeAll(var);
        for (JExpression arg : list) {
          this.accept(arg);
        }
      }
    }

    private void rescueAndInstantiate(JClassType type) {
      rescue(type, true, true);
    }

    /**
     * The code is very tightly tied to the behavior of
     * Pruner.CleanupRefsVisitor. CleanUpRefsVisitor will prune unread
     * parameters, and also prune any matching arguments that don't have side
     * effects. We want to make control flow congruent to pruning, to avoid the
     * need to iterate over Pruner until reaching a stable point, so we avoid
     * actually rescuing such arguments until/unless the parameter is read.
     */
    private boolean rescueArgumentsIfParametersCanBeRead(JMethodCall call, JMethod method) {
      if (call.getInstance() != null) {
        // Explicitly visit instance since we're returning false below.
        this.accept(call.getInstance());
      }
      List<JExpression> args = call.getArgs();
      List<JParameter> params = method.getParams();
      int i = 0;
      for (int c = params.size(); i < c; ++i) {
        JExpression arg = args.get(i);
        JParameter param = params.get(i);
        if (arg.hasSideEffects() || liveFieldsAndMethods.contains(param)
            // rescue any args of JsInterface Prototype methods or JsInterface
            || program.typeOracle.isJsInterfaceMethod(method)
            || JProgram.isJsInterfacePrototype(method.getEnclosingType())) {
          this.accept(arg);
          continue;
        }
        argsToRescueIfParameterRead.put(param, arg);
      }
      // Visit any "extra" arguments that exceed the param list.
      for (int c = args.size(); i < c; ++i) {
        this.accept(args.get(i));
      }
      return false;
    }

    /**
     * Handle special rescues needed implicitly to support concat.
     */
    private void rescueByConcat(JType type) {
      JPrimitiveType charType = program.getTypePrimitiveChar();
      JClassType stringType = program.getTypeJavaLangString();
      if (type instanceof JReferenceType
          && !program.typeOracle.canTriviallyCast((JReferenceType) type, stringType)
          && type != program.getTypeNull()) {
        /*
         * Any reference types (except String, which works by default) that take
         * part in a concat must rescue java.lang.Object.toString().
         *
         * TODO: can we narrow the focus by walking up the type hierarchy or
         * doing explicit toString calls?
         */
        JMethod toStringMethod = program.getIndexedMethod("Object.toString");
        rescue(toStringMethod);
      } else if (type == charType) {
        /*
         * Characters must rescue String.valueOf(char)
         */
        rescue(getStringValueOfCharMethod());
      }
    }

    private void rescueClassLiteralsIfGetClassIsLive() {
      if (classLiteralsToBeRescuedIfGetClassIsLive != null) {
        // guard against re-entrant calls. This only needs to run once.
        Set<JReferenceType> toRescue = classLiteralsToBeRescuedIfGetClassIsLive;
        classLiteralsToBeRescuedIfGetClassIsLive = null;

        for (JReferenceType classLit : toRescue) {
          maybeRescueClassLiteral(classLit);
        }
      }
    }

    /**
     * If the type is instantiable, rescue any of its virtual methods that a
     * previously seen method call could call.
     */
    private void rescueMembersIfInstantiable(JDeclaredType type) {
      if (!instantiatedTypes.contains(type)) {
        return;
      }
      for (JMethod method : type.getMethods()) {
        if (!method.isStatic() && membersToRescueIfTypeIsInstantiated.contains(method)) {
          rescue(method);
        }
      }
      for (JField field : type.getFields()) {
        if (!field.isStatic() && membersToRescueIfTypeIsInstantiated.contains(field)) {
            rescue(field);
        }
      }
    }

    /**
     * Assume that <code>method</code> is live. Rescue any overriding methods
     * that might be called if <code>method</code> is called through virtual
     * dispatch.
     */
    private void rescueOverridingMethods(JMethod method) {
      if (method.isStatic()) {
        return;
      }

      List<JMethod> overriders = methodsThatOverrideMe.get(method);
      if (overriders == null) {
        return;
      }

      for (JMethod overrider : overriders) {
        if (liveFieldsAndMethods.contains(overrider)) {
          // The override is already alive, do nothing.
        } else if (instantiatedTypes.contains(overrider.getEnclosingType())) {
          // The enclosing class is alive, make my override reachable.
          rescue(overrider);
        } else {
          // The enclosing class is not yet alive, put override in limbo.
          membersToRescueIfTypeIsInstantiated.add(overrider);
        }
      }
    }
  }

  /**
   * These are arguments that have not yet been rescued on account of the
   * associated parameter not having been read yet. If the parameter becomes
   * read, we will need to rescue the associated arguments. See comments in
   * {@link #rescueArgumentsIfParametersCanBeRead}.
   */
  private ListMultimap<JParameter, JExpression> argsToRescueIfParameterRead;

  private final JMethod asyncFragmentOnLoad;
  private final JDeclaredType baseArrayType;

  /**
   * Schrodinger set of classLiterals to be rescued if type is instantiated AND getClass()
   * is live.
   */
  private Set<JReferenceType> classLiteralsToBeRescuedIfGetClassIsLive = Sets.newHashSet();

  private DependencyRecorder dependencyRecorder;
  private Set<JField> fieldsWritten = Sets.newHashSet();
  private Set<JReferenceType> instantiatedTypes = Sets.newHashSet();
  private Set<JNode> liveFieldsAndMethods = Sets.newHashSet();
  private Set<String> liveStrings = Sets.newHashSet();

  /**
   * Schrodinger's members... aka "limbo". :) These are instance methods and
   * fields that seem to be reachable, only their enclosing type is
   * uninstantiable. We place these methods into purgatory until/unless the
   * enclosing type is found to be instantiable.
   */
  private Set<JNode> membersToRescueIfTypeIsInstantiated = Sets.newHashSet();

  /**
   * A precomputed map of all instance methods onto a set of methods that
   * override each key method.
   */
  private ListMultimap<JMethod, JMethod> methodsThatOverrideMe;

  private final JField getClassField;
  private final JMethod getClassMethod;
  private final JProgram program;
  private Set<JReferenceType> referencedTypes = Sets.newHashSet();
  private final RescueVisitor rescuer = new RescueVisitor();
  private final Set<JReferenceType> rescuedViaCast = Sets.newHashSet();
  private final JMethod runAsyncOnsuccess;
  private JMethod stringValueOfChar = null;

  public ControlFlowAnalyzer(ControlFlowAnalyzer cfa) {
    program = cfa.program;
    asyncFragmentOnLoad = cfa.asyncFragmentOnLoad;
    runAsyncOnsuccess = cfa.runAsyncOnsuccess;
    baseArrayType = cfa.baseArrayType;
    fieldsWritten = Sets.newHashSet(cfa.fieldsWritten);
    instantiatedTypes = Sets.newHashSet(cfa.instantiatedTypes);
    liveFieldsAndMethods = Sets.newHashSet(cfa.liveFieldsAndMethods);
    referencedTypes = Sets.newHashSet(cfa.referencedTypes);
    stringValueOfChar = cfa.stringValueOfChar;
    liveStrings = Sets.newHashSet(cfa.liveStrings);
    membersToRescueIfTypeIsInstantiated =
        Sets.newHashSet(cfa.membersToRescueIfTypeIsInstantiated);
    if (cfa.argsToRescueIfParameterRead != null) {
      argsToRescueIfParameterRead =
          ArrayListMultimap.create(cfa.argsToRescueIfParameterRead);
    }
    methodsThatOverrideMe = cfa.methodsThatOverrideMe;
    getClassField = program.getIndexedField("Object.___clazz");
    getClassMethod = program.getIndexedMethod("Object.getClass");
  }

  public ControlFlowAnalyzer(JProgram program) {
    this.program = program;
    asyncFragmentOnLoad = program.getIndexedMethod("AsyncFragmentLoader.onLoad");
    runAsyncOnsuccess = program.getIndexedMethod("RunAsyncCallback.onSuccess");
    baseArrayType = program.getIndexedType("Array");
    getClassField = program.getIndexedField("Object.___clazz");
    getClassMethod = program.getIndexedMethod("Object.getClass");
    rescuedViaCast.addAll(program.typeOracle.getInstantiatedJsoTypesViaCast());
    buildMethodsOverriding();
  }

  /**
   * Return the set of all fields that are written.
   */
  public Set<JField> getFieldsWritten() {
    return fieldsWritten;
  }

  /**
   * Return the complete set of types that have been instantiated.
   */
  public Set<JReferenceType> getInstantiatedTypes() {
    return instantiatedTypes;
  }

  /**
   * Return all methods that could be executed, and all variables that could be
   * read, based on the given entry points so far.
   */
  public Set<? extends JNode> getLiveFieldsAndMethods() {
    return liveFieldsAndMethods;
  }

  public Set<String> getLiveStrings() {
    return liveStrings;
  }

  /**
   * Return the complete set of types that have been referenced.
   */
  public Set<? extends JReferenceType> getReferencedTypes() {
    return referencedTypes;
  }

  /**
   * Specify the {@link DependencyRecorder} to be used for future traversals.
   * Specifying <code>null</code> means to stop recording dependencies.
   */
  public void setDependencyRecorder(DependencyRecorder dr) {
    if (dependencyRecorder != null && dr != null) {
      throw new IllegalArgumentException("Attempting to set multiple dependency recorders");
    }
    this.dependencyRecorder = dr;
  }

  public void setForPruning() {
    assert argsToRescueIfParameterRead == null;
    argsToRescueIfParameterRead = ArrayListMultimap.create();
  }

  /**
   * Traverse the program entry points, but don't traverse any runAsync
   * fragments.
   */
  public void traverseEntryMethods() {
    for (JMethod method : program.getEntryMethods()) {
      traverseFrom(method);
    }

    /*
     * All exported methods must be treated as entry points. We need to invent a way to
     * scope this down via flags or module properties.
     */
    for (JMethod method : program.typeOracle.getExportedMethods()) {
      // treat class as instantiated, since a ctor may be called from JS export
      rescuer.rescue(method.getEnclosingType(), true, true);
      traverseFrom(method);
    }

    if (program.getRunAsyncs().size() > 0) {
      /*
       * Explicitly rescue AsyncFragmentLoader.onLoad(). It is never explicitly
       * called anyway, until late code gen. Also, we want it in the initial
       * fragment so all other fragments can share the code.
       */
      traverseFrom(asyncFragmentOnLoad);
    }
  }

  public void traverseEverything() {
    traverseEntryMethods();
    traverseFromRunAsyncs();
    /*
     * Keep callback.onSuccess() from being pruned since we explicitly avoid
     * visiting it.
     */
    liveFieldsAndMethods.add(runAsyncOnsuccess);
  }

  /**
   * Assume <code>method</code> is live, and find out what else might execute.
   */
  public void traverseFrom(JMethod method) {
    rescuer.rescue(method);
  }

  /**
   * Assume <code>type</code> is instantiated, and find out what else will
   * execute as a result.
   */
  public void traverseFromInstantiationOf(JDeclaredType type) {
    rescuer.rescue(type, true, true);
  }

  public void traverseFromReferenceTo(JDeclaredType type) {
    rescuer.rescue(type, true, false);
  }

  /**
   * Traverse the fragment for a specific runAsync.
   */
  public void traverseFromRunAsync(JRunAsync runAsync) {
    runAsync.traverseOnSuccess(rescuer);
  }

  /**
   * Traverse the fragments for all runAsyncs.
   */
  public void traverseFromRunAsyncs() {
    for (JRunAsync runAsync : program.getRunAsyncs()) {
      traverseFromRunAsync(runAsync);
    }
  }

  private void buildMethodsOverriding() {
    methodsThatOverrideMe = ArrayListMultimap.create();
    for (JDeclaredType type : program.getDeclaredTypes()) {
      for (JMethod method : type.getMethods()) {
        for (JMethod overridden : program.typeOracle.getAllOverriddenMethods(method)) {
          methodsThatOverrideMe.put(overridden, method);
        }
      }
    }
  }
}
