/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Determines conservatively which classes can potentially see uninitialized values of their
 * subclasses' fields.
 * <p>
 * This simple conservative analysis relies on the fact that when: <ul>
 *   <li> (1) there are no virtual calls on "this" in any of the initialization methods
 *        (constructors, init) of all the superclasses, and </li>
 *   <li> (2) "this" does not escape through a parameter to other methods, and </li>
 *   <li>(3) "this" is not aliased (stored into another field, variable or array)</li>
 * </ul>
 * then uninitialized values for subclass fields can never be seen.
 * <p>
 * This analysis is used to strengthen the nullness analysis performed by {@link TypeTightener} and
 * to hoist initialization of instance fields to the prototype in {@link GenerateJavaScriptAST}.
 */
public class ComputePotentiallyObservableUninitializedValues {

  private static final String NAME =
      ComputePotentiallyObservableUninitializedValues.class.getSimpleName();
  private final JProgram program;
  private final Set<JType> classesWhoseFieldsCanBeObservedUninitialized = Sets.newHashSet();

  private ComputePotentiallyObservableUninitializedValues(JProgram program) {
    this.program = program;
  }

  /**
   * Perform the analysis to compute which fields can be observed uninitialized.
   */
  public static Predicate<JField> analyze(JProgram program) {
    return new ComputePotentiallyObservableUninitializedValues(program).analyzeImpl();
  }

  private Predicate<JField> analyzeImpl() {
    SpeedTracerLogger.Event optimizeEvent =
        SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    CanObserveSubclassUninitializedFieldsVisitor visitor =
        new CanObserveSubclassUninitializedFieldsVisitor();
    visitor.accept(program);
    Set<JType> classesThatCanPotentiallyObserveUninitializedSubclassFields =
        visitor.classesThatCanPotentiallyObserveUninitializedSubclassFields;

    for (JType type : classesThatCanPotentiallyObserveUninitializedSubclassFields) {
      if (classesWhoseFieldsCanBeObservedUninitialized.contains(type)) {
        // Already processed.
        continue;
      }
      classesWhoseFieldsCanBeObservedUninitialized.addAll(program.getSubclasses(type));
    }

    optimizeEvent.end();

    return new Predicate<JField>() {
      @Override
      public boolean apply(JField field) {
        return isUninitializedValueObservable(field);
      }
    };
  }

  private boolean isUninitializedValueObservable(JField x) {
    if (x.getLiteralInitializer() != null && (x.isFinal() || x.isStatic())) {
      // Static and final fields that are initialized to a (value) literal can not be observed in
      // uninitialized state.
      return false;
    }

    if (x.isStatic()) {
      // Static fields can potentially be observed uninitialized if clinit dependencies are
      // cyclical.
      return true;
    }

    return classesWhoseFieldsCanBeObservedUninitialized.contains(x.getEnclosingType());
  }

  private class CanObserveSubclassUninitializedFieldsVisitor extends JVisitor {
    private JClassType currentClass;
    private JParameter devirtualizedThis;
    private Set<JType> classesThatCanPotentiallyObserveUninitializedSubclassFields =
        Sets.newHashSet();

    @Override
    public void endVisit(JClassType x, Context ctx) {
      assert currentClass == x;
      currentClass = null;
    }

    @Override
    public void endVisit(JConstructor x, Context ctx) {
      assert currentClass == x.getEnclosingType();
      assert devirtualizedThis == null;
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      assert currentClass == x.getEnclosingType();
      devirtualizedThis = null;
    }

    @Override
    public void endVisit(JThisRef x, Context ctx) {
      // Seen a reference to "this" that can potentially escape or be used as instance in a
      // method call.
      classesThatCanPotentiallyObserveUninitializedSubclassFields.add(currentClass);
    }

    public void endVisit(JParameterRef x, Context ctx) {
      if (x.getParameter() == devirtualizedThis) {
        // Seen a reference to devirtualized "this" that can potentially escape or be used as
        // instance in a method call.
        classesThatCanPotentiallyObserveUninitializedSubclassFields.add(currentClass);
      }
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      assert currentClass == null;
      currentClass = x;
      return true;
    }

    @Override
    public boolean visit(JConstructor x, Context ctx) {
      // Only look at constructor bodies.
      assert currentClass == x.getEnclosingType();
      return true;
    }

    @Override
    public boolean visit(JFieldRef x, Context ctx) {
      if (isFieldReferenceThroughThis(x) &&  isFieldDeclaredInCurrentClassOrSuper(x)) {
        // Accessing fields through this (or devirtualized this) from the current class or
        // any super is ok, no further checks are needed.
        // A subclass field ref can leak into superclass methods when optimizations are enabled.
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(JInterfaceType x, Context ctx) {
      // No need to examine interfaces.
      return false;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      assert currentClass == x.getEnclosingType();
      if (isInitMethod(x)) {
        return true;
      }

      if (isDevirtualizedInitMethod(x) && x.getParams().size() > 0
          && x.getParams().get(0).getType() == currentClass) {
        devirtualizedThis = x.getParams().get(0);
      }
      // Do not explore the method body if it is not a constructor or the instance initializer.
      return false;
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      // This is a method call inside a constructor.
      assert currentClass != null;
      // Calls to this/super constructors and instance initializers are okay, as they will also
      // get examined.
      if ((x.getTarget().isConstructor()) && x.getInstance() instanceof JThisRef ||
          isInitMethod(x.getTarget())) {
        // Make sure "this" references do not escape through parameters
        accept(x.getArgs());
        return false;
      }

      // The instance initializers are always devirtualized, handle them specially.
      if (isDevirtualizedInitMethod(x.getTarget()) && x.getArgs().size() > 0 &&
          x.getArgs().get(0) instanceof JThisRef) {
        // Make sure "this" references do not escape through parameters other than the first.
        accept(x.getArgs().subList(1, x.getArgs().size()));
        return false;
      }

      if (!x.getTarget().isStatic() && !x.getTarget().isFinal() &&
          x.getInstance() instanceof JThisRef) {
        // This is polymorphic method call on this, hence it is potentially unsafe.
        classesThatCanPotentiallyObserveUninitializedSubclassFields.add(currentClass);
        return false;
      }

      // This is a static call, if there is no "this" references in its parameters then it might
      // be ok.
      return true;
    }

    private boolean isDevirtualizedInitMethod(JMethod method) {
      return method.isStatic() && method.getName().equals(GwtAstBuilder.STATIC_INIT_METHOD_NAME) &&
          method.getEnclosingType() == currentClass;
    }

    private boolean isInitMethod(JMethod method) {
      return !method.isStatic() && method.getName().equals(GwtAstBuilder.INIT_NAME_METHOD_NAME) &&
          method.getEnclosingType() == currentClass;
    }

    private boolean isFieldReferenceThroughThis(JFieldRef x) {
      return x.getInstance() instanceof JThisRef || x.getInstance() instanceof JParameterRef &&
          ((JParameterRef) x.getInstance()).getParameter() == devirtualizedThis;
    }

    private boolean isFieldDeclaredInCurrentClassOrSuper(JFieldRef x) {
      JClassType enclosingClass = (JClassType) x.getField().getEnclosingType();
      return currentClass == enclosingClass ||
          program.typeOracle.isSuperClass(enclosingClass, currentClass);
    }
  }
}