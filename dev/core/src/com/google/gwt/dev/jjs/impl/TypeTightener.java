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

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.CanBeAbstract;
import com.google.gwt.dev.jjs.ast.CanBeStatic;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPermutationDependentValue;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * The purpose of this pass is to record "type flow" information and then use
 * the information to infer places where "tighter" (that is, more specific)
 * types can be inferred for locals, fields, parameters, and method return
 * types. We also optimize dynamic casts and instanceof operations.
 *
 * Examples:
 *
 * This declaration of variable foo:
 *
 * <pre>
 * final List foo = new ArrayList();
 * </pre>
 *
 * can be tightened from List to ArrayList because no type other than ArrayList
 * can ever be assigned to foo.
 *
 * The return value of the method bar:
 *
 * <pre>
 * Collection bar() {
 *   return new LinkedHashSet;
 * }
 * </pre>
 *
 * can be tightened from Collection to LinkedHashSet since it
 * will never return any other type.
 *
 * By working in conjunction with {@link MethodCallTightener}, Type tightening
 * can eliminate generating run-time dispatch code for polymorphic methods.
 *
 * Type flow occurs automatically in most JExpressions. But locals, fields,
 * parameters, and method return types serve as "way points" where type
 * information is fixed based on the declared type. Type tightening can be done
 * by analyzing the types "flowing" into each way point, and then updating the
 * declared type of the way point to be a more specific type than it had before.
 *
 * Oddly, it's quite possible to tighten a variable to the Null type, which
 * means either the variable was never assigned, or it was only ever assigned
 * null. This is great for two reasons:
 *
 * 1) Once a variable has been tightened to null, it will no longer impact the
 * variables that depend on it.
 *
 * 2) It creates some very interesting opportunities to optimize later, since we
 * know statically that the value of the variable is always null.
 *
 * Open issue: we don't handle recursion where a method passes (some of) its own
 * args to itself or returns its own call result. With our naive analysis, we
 * can't figure out that tightening might occur.
 *
 * Type flow is not supported for primitive types, only reference types.
 */
public class TypeTightener {
  /**
   * Replaces dangling null references with dummy calls.
   */
  public class FixDanglingRefsVisitor extends JChangeTrackingVisitor {

    public FixDanglingRefsVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      JExpression instance = x.getInstance();
      JField field = x.getField();
      if (field.isStatic() && instance != null) {
        // this doesn't really belong here, but while we're here let's remove
        // non-side-effect qualifiers to statics
        if (!instance.hasSideEffects()) {
          JFieldRef fieldRef =
              new JFieldRef(x.getSourceInfo(), null, field, x.getEnclosingType());
          ctx.replaceMe(fieldRef);
        }
      } else if (isNullReference(field, instance)
          && field != program.getNullField()) {
        // Change any dereference of null to use the null field
        ctx.replaceMe(Pruner.transformToNullFieldRef(x, program));
      }
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JExpression instance = x.getInstance();
      JMethod method = x.getTarget();
      boolean isStaticImpl = program.isStaticImpl(method);
      if (method.isStatic() && !isStaticImpl && instance != null) {
        // TODO: move to DeadCodeElimination.
        // this doesn't really belong here, but while we're here let's remove
        // non-side-effect qualifiers to statics
        if (!instance.hasSideEffects()) {
          JMethodCall newCall = new JMethodCall(x.getSourceInfo(), null, x.getTarget());
          newCall.addArgs(x.getArgs());
          ctx.replaceMe(newCall);
        }
      } else if (isNullReference(method, instance)) {
        ctx.replaceMe(Pruner.transformToNullMethodCall(x, program));
      } else if (isStaticImpl && method.getParams().size() > 0
          && method.getParams().get(0).isThis() && x.getArgs().size() > 0
          && x.getArgs().get(0).getType().isNullType()) {
        // bind null instance calls to the null method for static impls
        ctx.replaceMe(Pruner.transformToNullMethodCall(x, program));
      }
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      // Do not visit.
    }
  }

  /*
   * TODO(later): handle recursion, self-assignment, arrays, method tightening
   * on invocations from within JSNI blocks
   */

  /**
   * Record "type flow" information. Variables receive type flow via assignment.
   * As a special case, Parameters also receive type flow based on the types of
   * arguments used when calling the containing method (think of this as a kind
   * of assignment). Method return types receive type flow from their contained
   * return statements, plus the return type of any methods that
   * override/implement them.
   *
   * Note that we only have to run this pass ONCE to record the relationships,
   * because type tightening never changes any relationships, only the types of
   * the things related. In my original implementation, I had naively mapped
   * nodes onto sets of JReferenceType directly, which meant I had to rerun this
   * visitor each time.
   */
  private class RecordVisitor extends JVisitor {
    private JMethod currentMethod;
    private Predicate<JField> canUninitializedValueBeObserved;

    /**
     * The call trace invoked by arguments in a method call. It is used to record
     * {@code callersByFieldRefArg} and {@code callersByMethodCallArg}.
     * For example, fun1(fun2(fun3(), fun4()), fun5()); The stack would be ...
     * fun1 -> fun2 -> fun3; (pop fun3, push fun4)
     * fun1 -> fun2 -> fun4; (pop fun4)
     * fun1 -> fun2; (pop fun2, push fun5)
     * fun1 -> fun5; (pop fun5)
     * fun1;
     */
    private Stack<JMethod> nestedCallTrace = new Stack<JMethod>();

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.isAssignment() && (x.getType() instanceof JReferenceType)) {
        JExpression lhs = x.getLhs();
        if (lhs instanceof JVariableRef) {
          addAssignment(((JVariableRef) lhs).getTarget(),
              x.getOp() == JBinaryOperator.ASG ? x.getRhs() : x);
        } else {
          assert lhs instanceof JArrayRef;
        }
      }
    }

    @Override
    public void endVisit(JClassType x, Context ctx) {
      if (program.typeOracle.isInstantiatedType(x)) {
        for (JClassType cur = x; cur != null; cur = cur.getSuperClass()) {
          addImplementor(cur, x);
          addInterfacesImplementorRecursive(cur, x);
        }
      }
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      JExpression initializer = x.getInitializer();
      if (initializer != null) {
        addAssignment(x.getVariableRef().getTarget(), initializer);
      }
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      if (!x.hasInitializer() || canUninitializedValueBeObserved.apply(x)) {
        addAssignment(x, x.getType().getDefaultValue());
      }
      currentMethod = null;
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      if (!nestedCallTrace.empty()) {
        calledMethodsByFieldRefArg.put(x.getField(), nestedCallTrace.peek());
      }
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // All of the params in the target method are considered to be assigned by
      // the arguments from the caller
      Iterator<JExpression> argIt = x.getArgs().iterator();
      List<JParameter> params = x.getTarget().getParams();
      for (JParameter param : params) {
        JExpression arg = argIt.next();
        if (param.getType() instanceof JReferenceType) {
          addAssignment(param, arg);
        }
      }
      nestedCallTrace.pop();
      if (!nestedCallTrace.empty()) {
        calledMethodsByMethodCallArg.put(x.getTarget(), nestedCallTrace.peek());
      }
    }

    @Override
    public void endVisit(JReturnStatement x, Context ctx) {
      if (currentMethod.getType() instanceof JReferenceType) {
        addReturn(currentMethod, x.getExpr());
      }
    }

    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      if (x.isLvalue()) {
        // If this happens in JSNI, we can't make any type-tightening
        // assumptions. Fake an assignment-to-self to prevent tightening.
        addAssignment(x.getTarget(), x);
      }
    }

    @Override
    public void endVisit(JsniMethodRef x, Context ctx) {
      // If this happens in JSNI, we can't make any type-tightening assumptions
      // Fake an assignment-to-self on all args to prevent tightening
      JMethod method = x.getTarget();
      for (JParameter param : method.getParams()) {
        addAssignment(param, param.makeRef(SourceOrigin.UNKNOWN));
      }
    }

    @Override
    public void endVisit(JTryStatement x, Context ctx) {
      // Never tighten args to catch blocks
      // Fake an assignment-to-self to prevent tightening
      for (JTryStatement.CatchClause clause : x.getCatchClauses()) {
        addAssignment(clause.getArg().getTarget(), clause.getArg());
      }
    }

    /**
     * Merge param call args across overriders/implementors. We can't tighten a
     * param type in an overriding method if the declaring method is looser.
     */
    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;

      if (x.canBePolymorphic()) {
        /*
         * Add an assignment to each parameter from that same parameter in every
         * method this method overrides.
         */
        Collection<JMethod> overriddenMethods = x.getOverriddenMethods();
        if (overriddenMethods.isEmpty()) {
          return true;
        }
        for (int j = 0, c = x.getParams().size(); j < c; ++j) {
          JParameter param = x.getParams().get(j);
          for (JMethod baseMethod : overriddenMethods) {
            JParameter baseParam = baseMethod.getParams().get(j);
            add(param, baseParam, paramUpRefs);
          }
        }
      }
      return true;
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      nestedCallTrace.push(x.getTarget());
      return true;
    }

    public void record(JProgram program) {
      canUninitializedValueBeObserved = ComputePotentiallyObservableUninitializedValues
          .analyze(program);
      accept(program);
    }

    private void addAssignment(JVariable target, JExpression rhs) {
      add(target, rhs, assignments);
    }

    private void addImplementor(JReferenceType target, JClassType implementor) {
      add(target, implementor, implementors);
    }

    private void addInterfacesImplementorRecursive(JDeclaredType target, JClassType implementor) {
      for (JInterfaceType implment : target.getImplements()) {
        addImplementor(implment, implementor);
        addInterfacesImplementorRecursive(implment, implementor);
      }
    }

    private void addReturn(JMethod target, JExpression expr) {
      add(target, expr, returns);
    }
  }

  /**
   * Wherever possible, use the type flow information recorded by RecordVisitor
   * to change the declared type of a field, local, parameter, or method to a
   * more specific type.
   *
   * Also optimize dynamic casts and instanceof operations where possible.
   */
  public class TightenTypesVisitor extends JChangeTrackingVisitor {

    public TightenTypesVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    /**
     * Tries to determine a specific concrete type for the cast, then either
     * removes the cast, or tightens the cast to a narrower type.
     *
     * If static analysis determines that a cast is not possible, swap in a cast
     * to a null type. This will later be normalized into throwing an
     * Exception.
     *
     * @see ImplementCastsAndTypeChecks
     */
    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      JType argumentType = x.getExpr().getType();
      if (!(x.getCastType() instanceof JReferenceType) || !(argumentType instanceof JReferenceType)) {
        return;
      }

      JReferenceType toType = getSingleConcreteType(x.getCastType());
      if (toType == null) {
        toType = (JReferenceType) x.getCastType();
      }
      JReferenceType fromType = getSingleConcreteType(argumentType);
      if (fromType == null) {
        fromType = (JReferenceType) argumentType;
      }

      if (program.typeOracle.castSucceedsTrivially(fromType, toType)) {
        // remove the cast operation
        ctx.replaceMe(x.getExpr());
        return;
      }
      if ((!program.typeOracle.isInstantiatedType(toType) ||
          program.typeOracle.castFailsTrivially(fromType, toType))
          && toType != JReferenceType.NULL_TYPE) {
        // replace with a placeholder cast to NULL, unless it's already a cast to NULL
        ctx.replaceMe(new JCastOperation(x.getSourceInfo(), JReferenceType.NULL_TYPE, x.getExpr()));
        return;
      }

      //  If possible, try to use a narrower cast
      JReferenceType tighterType = getSingleConcreteType(toType);
      if (tighterType != null && tighterType != toType) {
        JCastOperation newOp = new JCastOperation(x.getSourceInfo(), tighterType, x.getExpr());
          ctx.replaceMe(newOp);
      }
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      if (!(x.getType() instanceof JReferenceType)) {
        return;
      }
      JReferenceType type = (JReferenceType) x.getType();
      JReferenceType resultType = strongerType(type, (JReferenceType) x.getThenExpr().getType(),
          (JReferenceType) x.getElseExpr().getType());
      if (type != resultType) {
        x.setType(resultType);
        madeChanges();
      }
    }

    @Override
    public void exit(JField x, Context ctx) {
      if (program.codeGenTypes.contains(x.getEnclosingType())
          || x.canBeReferencedExternally() || x.canBeImplementedExternally()) {
        // We cannot tighten this field as we don't see all references or the initial value.
        return;
      }
      if (!x.isVolatile()) {
        tighten(x);
      }
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      JType argType = x.getExpr().getType();
      if (!(argType instanceof JReferenceType)) {
        // TODO: is this even possible? Replace with assert maybe.
        return;
      }

      JReferenceType concreteType = getSingleConcreteType(x.getTestType());
      // If possible, try to use a narrower cast
      if (concreteType != null) {
        ctx.replaceMe(
            new JInstanceOf(x.getSourceInfo(), concreteType.getUnderlyingType(), x.getExpr()));
      }
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      tighten(x);
    }

    /**
     * Tighten based on return types and overrides.
     */
    @Override
    public void exit(JMethod x, Context ctx) {
      if (program.codeGenTypes.contains(x.getEnclosingType())) {
        return;
      }
      if (!(x.getType() instanceof JReferenceType)) {
        return;
      }
      JReferenceType returnType = (JReferenceType) x.getType();

      if (returnType.isNullType()) {
        return;
      }

      // tighten based on non-instantiability
      if (!program.typeOracle.isInstantiatedType(returnType)) {
        x.setType(JReferenceType.NULL_TYPE);
        madeChanges();
        return;
      }

      JReferenceType concreteType = getSingleConcreteType(returnType);
      if (concreteType != null) {
        x.setType(concreteType);
        madeChanges();
      }

      /*
       * The only information that we can infer about native methods is if they
       * are declared to return a leaf type.
       */
      if (x.isJsniMethod() || x.canBeImplementedExternally()) {
        return;
      }

     Iterable<JReferenceType> returnTypes = Iterables.concat(
         JjsUtils.getExpressionTypes(returns.get(x)),
         JjsUtils.getExpressionTypes(x.getOverridingMethods()));

      JReferenceType strengthenedType = strongerType(returnType, returnTypes);
      if (returnType != strengthenedType) {
          x.setType(strengthenedType);
          madeChanges();
      }
    }

    /**
     * Tighten the target method from the abstract base method to the final
     * implementation.
     */
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      if (!x.canBePolymorphic() || x.isVolatile()) {
        return;
      }
      JMethod target = x.getTarget();

      JMethod concreteMethod = getSingleConcreteMethodOverride(target);
      assert concreteMethod != target;
      if (concreteMethod != null) {
        assert !x.isStaticDispatchOnly();
        JMethodCall newCall = new JMethodCall(x.getSourceInfo(), x.getInstance(), concreteMethod);
        newCall.addArgs(x.getArgs());
        newCall.setCannotBePolymorphic();
        ctx.replaceMe(newCall);
      }
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      JMethod currentMethod = getCurrentMethod();
      if (program.codeGenTypes.contains(currentMethod.getEnclosingType())
          || currentMethod.canBeReferencedExternally()) {
        // We cannot tighten this parameter as we don't know all callers.
        return;
      }
      tighten(x);
    }

    @Override
    public void endVisit(JPermutationDependentValue x, Context ctx) {
      throw new IllegalStateException("AST should not contain permutation dependent values at " +
          "this point but contains " + x);
    }

    @Override
    public boolean visit(JRunAsync x, Context ctx) {
      // JRunAsync's onSuccessCall is not normally traversed but should be here.
      x.traverseOnSuccess(this);
      return true;
    }

    /**
     * Find a replacement method. If the original method is abstract, this will
     * return the leaf, final implementation of the method. If the method is
     * already concrete, but enclosed by an abstract type, the overriding method
     * from the leaf concrete type will be returned. If the method is static,
     * return <code>null</code> no matter what.
     */
    private JMethod getSingleConcreteMethodOverride(JMethod method) {
      assert method.canBePolymorphic();

      if (getSingleConcreteType(method.getEnclosingType()) != null) {
        return getSingleConcrete(method.getOverridingMethods());
      }

      return null;
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // don't mess with classes used in code gen
      if (program.codeGenTypes.contains(x)) {
        return false;
      }
      return true;
    }

    @Override
    public boolean enter(JMethod x, Context ctx) {
      /*
       * Explicitly NOT visiting native methods since we can't infer further
       * type information.
       */
      return !x.isJsniMethod();
    }

    /**
     * Given an abstract type, return the single concrete implementation of that
     * type.
     */
    private JReferenceType getSingleConcreteType(JType type) {
      if (!(type instanceof JReferenceType) || type.canBeImplementedExternally()) {
        return null;
      }

      JReferenceType refType = (JReferenceType) type;
      if (refType.isAbstract()) {
        JReferenceType singleConcrete =
            getSingleConcrete(implementors.get(refType.getUnderlyingType()));
        assert (singleConcrete == null || program.typeOracle.isInstantiatedType(singleConcrete));
        if (singleConcrete == null) {
          return null;
        }
        singleConcrete = singleConcrete.strengthenToExact();
        return refType.canBeNull() ? singleConcrete : singleConcrete.strengthenToNonNull();
      }
      return null;
    }

    /**
     * Tighten based on assignment, and for parameters, callArgs as well.
     */
    private void tighten(JVariable x) {
      if (!(x.getType() instanceof JReferenceType)) {
        return;
      }
      JReferenceType varType = (JReferenceType) x.getType();

      if (varType.isNullType()) {
        return;
      }

      // tighten based on non-instantiability
      if (!program.typeOracle.isInstantiatedType(varType)) {
        x.setType(JReferenceType.NULL_TYPE);
        madeChanges();
        return;
      }

      // tighten based on leaf types
      JReferenceType leafType = getSingleConcreteType(varType);
      if (leafType != null) {
        x.setType(leafType);
        madeChanges();
        return;
      }

      // tighten based on assignment
      Collection<JReferenceType> assignmentTypes = getAssignmentsIfValid(x);
      if (assignmentTypes == null) {
        return;
      }

      JReferenceType strengthenedType = strongerType(varType,
          Iterables.concat(assignmentTypes, JjsUtils.getExpressionTypes(paramUpRefs.get(x))));
      if (varType != strengthenedType) {
        x.setType(strengthenedType);
        madeChanges();
      }
    }

    private Collection<JReferenceType> getAssignmentsIfValid(JVariable variable) {
      Collection<JExpression> assignedExpressions = assignments.get(variable);
      if (assignedExpressions == null) {
        return Collections.emptyList();
      }

      Collection<JReferenceType> assignedTypes = Lists.newArrayList();
      for (JExpression expression : assignedExpressions) {
        JType expressionType = expression.getType();
        if (!(expressionType instanceof JReferenceType)) {
          // In some case there will be types that are not JReferenceType; and it is not safe in
          // such a case to replace the type of the lhs. Those cases only arise by AST manipulation,
          // see {@link ImplementCastsAndTypeChecks} and {@link Class#createForClass}.
          return null;
        }
        assignedTypes.add((JReferenceType) expressionType);
      }
      return assignedTypes;
    }
  }

  private static final String NAME = TypeTightener.class.getSimpleName();

  public static OptimizerStats exec(JProgram program, OptimizerContext optimizerCtx) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new TypeTightener(program).execImpl(optimizerCtx);
    optimizerCtx.incOptimizationStep();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  @VisibleForTesting
  static OptimizerStats exec(JProgram program) {
    return exec(program, new FullOptimizerContext(program));
  }

  private static <T, V> void add(T key, V value, Map<T, Collection<V>> map) {
    Collection<V> list = map.get(key);
    if (list == null) {
      list = Sets.newLinkedHashSet();
      map.put(key, list);
    }
    list.add(value);
  }

  /**
   * Find exactly one concrete element in a collection. If there are
   * none or more than one concrete element, return <code>null</code>.
   */
  private static <T extends CanBeAbstract> T getSingleConcrete(Collection<T> collection) {

    // No collection, then no concrete version
    if (collection == null) {
      return null;
    }

    Iterator<T> concreteIterator = FluentIterable.from(collection).filter(
        new Predicate<T>() {
          @Override
          public boolean apply(T element) {
            return !element.isAbstract();
          }
        }).iterator();

    if (!concreteIterator.hasNext()) {
      return null;
    }

    T firstConcrete = concreteIterator.next();
    if (concreteIterator.hasNext()) {
      // multiple concrete elements.
      return null;
    }

    return firstConcrete;
  }

  /**
   * For each program Variable (includes fields, locals and parameters) tracks the set
   * of expressions that are assigned to them. Assignments include parameter instantiations.
   *
   */
  private final Map<JVariable, Collection<JExpression>> assignments = Maps.newIdentityHashMap();
  /**
   * For each type tracks all classes the extend or implement it.
   */
  private final Map<JReferenceType, Collection<JClassType>> implementors =
      Maps.newIdentityHashMap();
  /**
   * For each parameter P (in method M) tracks the set of parameters that share its position in all
   * the methods that are overridden by M.
   */
  private final Map<JParameter, Collection<JParameter>> paramUpRefs = Maps.newIdentityHashMap();
  /**
   * For each method tracks the set of all expressions that are returned.
   */
  private final Map<JMethod, Collection<JExpression>> returns = Maps.newIdentityHashMap();

  /**
   * For each method call, record the method calls and field references in its arguments.
   * When the callee methods or the referenced fields in the arguments are modified,
   * it would be possible for the target method to be type tightened.
   */
  private final Multimap<JMethod, JMethod> calledMethodsByMethodCallArg = HashMultimap.create();
  private final Multimap<JField, JMethod> calledMethodsByFieldRefArg = HashMultimap.create();

  private final JProgram program;

  private TypeTightener(JProgram program) {
    this.program = program;
  }

  private OptimizerStats execImpl(OptimizerContext optimizerCtx) {
    OptimizerStats stats = new OptimizerStats(NAME);
    RecordVisitor recorder = new RecordVisitor();
    recorder.record(program);

    /*
     * We must iterate multiple times because each way point we tighten creates
     * more opportunities to do additional tightening for the things that depend
     * on it.
     *
     * TODO(zundel): See if we can remove this loop, or otherwise run to less
     * than completion if we compile with an option for less than 100% optimized
     * output.
     */
    int lastStep = optimizerCtx.getLastStepFor(NAME);
    /*
     * Set the last step to the step at which TypeTightener does the first iteration. Since the
     * RecordVisitor is run only once, the information in {@code assignments} etc. is not updated.
     * So it is still possible for the type tightened methods/fields to be type tightened for the
     * next time.
     */
    optimizerCtx.setLastStepFor(NAME, optimizerCtx.getOptimizationStep());
    while (true) {
      TightenTypesVisitor tightener = new TightenTypesVisitor(optimizerCtx);

      Set<JMethod> affectedMethods = computeAffectedMethods(optimizerCtx, lastStep);
      Set<JField> affectedFields = computeAffectedFields(optimizerCtx, lastStep);
      optimizerCtx.traverse(tightener, affectedFields);
      optimizerCtx.traverse(tightener, affectedMethods);
      stats.recordModified(tightener.getNumMods());
      lastStep = optimizerCtx.getOptimizationStep();
      optimizerCtx.incOptimizationStep();
      if (!tightener.didChange()) {
        break;
      }
    }

    if (stats.didChange()) {
      FixDanglingRefsVisitor fixer = new FixDanglingRefsVisitor(optimizerCtx);
      fixer.accept(program);
      optimizerCtx.incOptimizationStep();
      JavaAstVerifier.assertProgramIsConsistent(program);
    }
    return stats;
  }

  private Set<JMethod> computeAffectedMethods(OptimizerContext optimizerCtx, int lastStep) {
    Set<JMethod> modifiedMethods = optimizerCtx.getModifiedMethodsSince(lastStep);
    Set<JField> modifiedFields = optimizerCtx.getModifiedFieldsSince(lastStep);
    Set<JMethod> affectedMethods = Sets.newLinkedHashSet();

    // If the return type or parameters' types of a method are changed, its caller methods should be
    // reanalyzed.
    affectedMethods.addAll(optimizerCtx.getCallers(modifiedMethods));

    // If a method is modified, its callee should be reanalyzed.
    affectedMethods.addAll(optimizerCtx.getCallees(modifiedMethods));

    // The removed callee methods (one or more method calls to it are removed) should be reanalyzed.
    affectedMethods.addAll(optimizerCtx.getRemovedCalleeMethodsSince(lastStep));

    // If a method's return type is changed, the called method whose argument calls the method
    // should be reanalyzed.
    for (JMethod method : modifiedMethods) {
      affectedMethods.addAll(calledMethodsByMethodCallArg.get(method));
    }

    // If a method's return type or parameters' types are changed, its overriders and overridden
    // methods should be reanalyzed. The overridden methods and overriders from typeOracle may have
    // been pruned, so we have to check if they are in the AST.
    for (JMethod method : modifiedMethods) {
      affectedMethods.addAll(method.getOverriddenMethods());
      affectedMethods.addAll(method.getOverridingMethods());
    }

    // If a field is changed, the methods that reference to it should be reanalyzed.
    affectedMethods.addAll(optimizerCtx.getMethodsByReferencedFields(modifiedFields));

    // If a field is changed, the caller methods which call it through argument should be
    // reanalyzed.
    for (JField field : modifiedFields) {
      affectedMethods.addAll(calledMethodsByFieldRefArg.get(field));
    }

    // All the methods that are modified by other optimizer should be reanalyzed.
    affectedMethods.addAll(modifiedMethods);
    return affectedMethods;
  }

  private Set<JField> computeAffectedFields(OptimizerContext optimizerCtx, int lastStep) {
    Set<JMethod> modifiedMethods = optimizerCtx.getModifiedMethodsSince(lastStep);
    Set<JField> modifiedFields = optimizerCtx.getModifiedFieldsSince(lastStep);
    Set<JField> affectedFields = Sets.newLinkedHashSet();
    affectedFields.addAll(modifiedFields);
    affectedFields.addAll(optimizerCtx.getReferencedFieldsByMethods(modifiedMethods));
    return affectedFields;
  }

  private boolean isNullReference(CanBeStatic member, JExpression instance) {
    return !member.isStatic() && instance.getType().isNullType();
  }

  /**
   * Computes type ^ (V assignedTypes).
   */
  private JReferenceType strongerType(JReferenceType type, JReferenceType... assignedTypes) {
    return strongerType(type, Arrays.asList(assignedTypes));
  }

  /**
   * Computes type ^ (V assignedTypes).
   */
  private JReferenceType strongerType(JReferenceType type,
      Iterable<JReferenceType> assignedTypes) {
    return program.strengthenType(type, program.generalizeTypes(assignedTypes));
  }
}
