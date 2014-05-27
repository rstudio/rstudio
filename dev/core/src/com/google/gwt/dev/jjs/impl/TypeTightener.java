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
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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
  public class FixDanglingRefsVisitor extends JModVisitor {

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      JExpression instance = x.getInstance();
      boolean isStatic = x.getField().isStatic();
      if (isStatic && instance != null) {
        // this doesn't really belong here, but while we're here let's remove
        // non-side-effect qualifiers to statics
        if (!instance.hasSideEffects()) {
          JFieldRef fieldRef =
              new JFieldRef(x.getSourceInfo(), null, x.getField(), x.getEnclosingType());
          ctx.replaceMe(fieldRef);
        }
      } else if (!isStatic && instance.getType() == typeNull
          && x.getField() != program.getNullField()) {
        // Change any dereference of null to use the null field
        ctx.replaceMe(Pruner.transformToNullFieldRef(x, program));
      }
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JExpression instance = x.getInstance();
      JMethod method = x.getTarget();
      boolean isStatic = method.isStatic();
      boolean isStaticImpl = program.isStaticImpl(method);
      if (isStatic && !isStaticImpl && instance != null) {
        // TODO: move to DeadCodeElimination.
        // this doesn't really belong here, but while we're here let's remove
        // non-side-effect qualifiers to statics
        if (!instance.hasSideEffects()) {
          JMethodCall newCall = new JMethodCall(x.getSourceInfo(), null, x.getTarget());
          newCall.addArgs(x.getArgs());
          ctx.replaceMe(newCall);
        }
      } else if (!isStatic && instance.getType() == typeNull) {
        ctx.replaceMe(Pruner.transformToNullMethodCall(x, program));
      } else if (isStaticImpl && method.getParams().size() > 0
          && method.getParams().get(0).isThis() && x.getArgs().size() > 0
          && x.getArgs().get(0).getType() == typeNull) {
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
  public class RecordVisitor extends JVisitor {
    private JMethod currentMethod;

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.isAssignment() && (x.getType() instanceof JReferenceType)) {
        JExpression lhs = x.getLhs();
        if (lhs instanceof JVariableRef) {
          addAssignment(((JVariableRef) lhs).getTarget(), x.getRhs());
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
      if (x.getLiteralInitializer() != null) {
        // TODO: do I still need this?
        addAssignment(x, x.getLiteralInitializer());
      }
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (program.typeOracle.isInstantiatedType(x.getEnclosingType())) {
        for (JMethod method : program.typeOracle.getAllOverriddenMethods(x)) {
          addOverrider(method, x);
        }
      }
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // All of the params in the target method are considered to be assigned by
      // the arguments from the caller
      Iterator<JExpression> argIt = x.getArgs().iterator();
      List<JParameter> params = x.getTarget().getParams();
      for (int i = 0; i < params.size(); ++i) {
        JParameter param = params.get(i);
        JExpression arg = argIt.next();
        if (param.getType() instanceof JReferenceType) {
          addAssignment(param, arg);
        }
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
        addAssignment(param, new JParameterRef(SourceOrigin.UNKNOWN, param));
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
        Collection<JMethod> overrides = program.typeOracle.getAllOverriddenMethods(x);
        if (overrides.isEmpty()) {
          return true;
        }
        for (int j = 0, c = x.getParams().size(); j < c; ++j) {
          JParameter param = x.getParams().get(j);
          Collection<JParameter> set = paramUpRefs.get(param);
          if (set == null) {
            set = new LinkedHashSet<JParameter>();
            paramUpRefs.put(param, set);
          }
          for (JMethod baseMethod : overrides) {
            JParameter baseParam = baseMethod.getParams().get(j);
            set.add(baseParam);
          }
        }
      }
      return true;
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

    private void addOverrider(JMethod target, JMethod overrider) {
      add(target, overrider, overriders);
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
  public class TightenTypesVisitor extends JModVisitor {

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
      JType argType = x.getExpr().getType();
      if (!(x.getCastType() instanceof JReferenceType) || !(argType instanceof JReferenceType)) {
        return;
      }

      JReferenceType toType = getSingleConcreteType(x.getCastType());
      if (toType == null) {
        toType = (JReferenceType) x.getCastType();
      }
      JReferenceType fromType = getSingleConcreteType(argType);
      if (fromType == null) {
        fromType = (JReferenceType) argType;
      }

      boolean triviallyTrue = false;
      boolean triviallyFalse = false;

      JTypeOracle typeOracle = program.typeOracle;
      if (typeOracle.canTriviallyCast(fromType, toType)) {
        triviallyTrue = true;
      } else if (!typeOracle.isInstantiatedType(toType)) {
        triviallyFalse = true;
      } else if (!typeOracle.canTheoreticallyCast(fromType, toType)) {
        triviallyFalse = true;
      }

      if (triviallyTrue) {
        // remove the cast operation
        ctx.replaceMe(x.getExpr());
      } else if (triviallyFalse && toType != program.getTypeNull()) {
        // replace with a placeholder cast to NULL, unless it's already a cast to NULL
        JCastOperation newOp =
            new JCastOperation(x.getSourceInfo(), program.getTypeNull(), x.getExpr());
        ctx.replaceMe(newOp);
      } else {
        // If possible, try to use a narrower cast
        JReferenceType tighterType = getSingleConcreteType(toType);

        if (tighterType != null && tighterType != toType) {
          JCastOperation newOp = new JCastOperation(x.getSourceInfo(), tighterType, x.getExpr());
          ctx.replaceMe(newOp);
        }
      }
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      if (x.getType() instanceof JReferenceType) {
        JReferenceType newType =
            program.generalizeTypes((JReferenceType) x.getThenExpr().getType(), (JReferenceType) x
                .getElseExpr().getType());
        if (newType != x.getType()) {
          x.setType(newType);
          madeChanges();
        }
      }
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      if (!x.isVolatile()) {
        tighten(x);
      }
    }

    @Override
    public void endVisit(JGwtCreate x, Context ctx) {
      List<JReferenceType> typeList = new ArrayList<JReferenceType>();
      for (JExpression expr : x.getInstantiationExpressions()) {
        JReferenceType type = (JReferenceType) expr.getType();
        typeList.add(type);
      }

      JReferenceType resultType = program.generalizeTypes(typeList);
      if (x.getType() != resultType) {
        x.setType(resultType);
        madeChanges();
      }
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      JType argType = x.getExpr().getType();
      if (!(argType instanceof JReferenceType)) {
        // TODO: is this even possible? Replace with assert maybe.
        return;
      }

      JReferenceType toType = getSingleConcreteType(x.getTestType());
      if (toType == null) {
        toType = x.getTestType();
      }
      JReferenceType fromType = getSingleConcreteType(argType);
      if (fromType == null) {
        fromType = (JReferenceType) argType;
      }

      boolean triviallyTrue = false;
      boolean triviallyFalse = false;

      JTypeOracle typeOracle = program.typeOracle;
      if (fromType == program.getTypeNull()) {
        // null is never instanceof anything
        triviallyFalse = true;
      } else if (typeOracle.canTriviallyCast(fromType, toType)) {
        triviallyTrue = true;
      } else if (!typeOracle.isInstantiatedType(toType)) {
        triviallyFalse = true;
      } else if (!typeOracle.canTheoreticallyCast(fromType, toType)) {
        triviallyFalse = true;
      }

      if (triviallyTrue) {
        // replace with a simple null test
        JNullLiteral nullLit = program.getLiteralNull();
        JBinaryOperation neq =
            new JBinaryOperation(x.getSourceInfo(), program.getTypePrimitiveBoolean(),
                JBinaryOperator.NEQ, x.getExpr(), nullLit);
        ctx.replaceMe(neq);
      } else if (triviallyFalse) {
        // replace with a false literal
        ctx.replaceMe(program.getLiteralBoolean(false));
      } else {
        // If possible, try to use a narrower cast
        JReferenceType concreteType = getSingleConcreteType(toType);
        if (concreteType != null) {
          JInstanceOf newOp = new JInstanceOf(x.getSourceInfo(), concreteType, x.getExpr());
          ctx.replaceMe(newOp);
        }
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
    public void endVisit(JMethod x, Context ctx) {
      if (!(x.getType() instanceof JReferenceType)) {
        return;
      }
      JReferenceType refType = (JReferenceType) x.getType();

      if (refType == typeNull) {
        return;
      }

      // tighten based on non-instantiability
      if (!program.typeOracle.isInstantiatedType(refType)) {
        x.setType(typeNull);
        madeChanges();
        return;
      }

      JReferenceType concreteType = getSingleConcreteType(x.getType());
      if (concreteType != null) {
        x.setType(concreteType);
        madeChanges();
      }

      /*
       * The only information that we can infer about native methods is if they
       * are declared to return a leaf type.
       */
      if (x.isNative()) {
        return;
      }

      // tighten based on both returned types and possible overrides
      List<JReferenceType> typeList = new ArrayList<JReferenceType>();

      Collection<JExpression> myReturns = returns.get(x);
      if (myReturns != null) {
        for (JExpression expr : myReturns) {
          typeList.add((JReferenceType) expr.getType());
        }
      }
      Collection<JMethod> myOverriders = overriders.get(x);
      if (myOverriders != null) {
        for (JMethod method : myOverriders) {
          typeList.add((JReferenceType) method.getType());
        }
      }

      JReferenceType resultType;
      if (typeList.isEmpty()) {
        // The method returns nothing
        resultType = typeNull;
      } else {
        resultType = program.generalizeTypes(typeList);
      }
      resultType = program.strongerType(refType, resultType);
      if (refType != resultType) {
        x.setType(resultType);
        madeChanges();
      }
    }

    /**
     * Tighten the target method from the abstract base method to the final
     * implementation.
     */
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      if (x.isVolatile()) {
        return;
      }
      JMethod target = x.getTarget();
      JMethod concreteMethod = getSingleConcreteMethod(target);
      if (concreteMethod != null) {
        JMethodCall newCall = new JMethodCall(x.getSourceInfo(), x.getInstance(), concreteMethod);
        newCall.addArgs(x.getArgs());
        ctx.replaceMe(newCall);
        target = concreteMethod;
        x = newCall;
      }

      /*
       * Mark a call as non-polymorphic if the targeted method is the only
       * possible dispatch, given the qualifying instance type.
       */
      if (x.canBePolymorphic() && !target.isAbstract()) {
        JExpression instance = x.getInstance();
        assert (instance != null);
        JReferenceType instanceType = (JReferenceType) instance.getType();
        Collection<JMethod> myOverriders = overriders.get(target);
        if (myOverriders != null) {
          for (JMethod override : myOverriders) {
            JReferenceType overrideType = override.getEnclosingType();
            if (program.typeOracle.canTheoreticallyCast(instanceType, overrideType)) {
              // This call is truly polymorphic.
              // TODO: composite types! :)
              return;
            }
          }
          // The instance type is incompatible with all overrides.
        }
        x.setCannotBePolymorphic();
        madeChanges();
      }
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      tighten(x);
    }

    @Override
    public boolean visit(JRunAsync x, Context ctx) {
      // JRunAsync's onSuccessCall is not normally traversed but should be here.
      x.traverseOnSuccess(this);
      return true;
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
    public boolean visit(JMethod x, Context ctx) {
      /*
       * Explicitly NOT visiting native methods since we can't infer further
       * type information.
       */
      return !x.isNative();
    }

    /**
     * Find a replacement method. If the original method is abstract, this will
     * return the leaf, final implementation of the method. If the method is
     * already concrete, but enclosed by an abstract type, the overriding method
     * from the leaf concrete type will be returned. If the method is static,
     * return <code>null</code> no matter what.
     */
    private JMethod getSingleConcreteMethod(JMethod method) {
      if (!method.canBePolymorphic()) {
        return null;
      }
      if (getSingleConcreteType(method.getEnclosingType()) != null) {
        return getSingleConcrete(method, overriders);
      } else {
        return null;
      }
    }

    /**
     * Given an abstract type, return the single concrete implementation of that
     * type.
     */
    private JReferenceType getSingleConcreteType(JType type) {
      if (type instanceof JReferenceType) {
        JReferenceType refType = (JReferenceType) type;
        if (refType.isAbstract()) {
          JClassType singleConcrete = getSingleConcrete(refType.getUnderlyingType(), implementors);
          assert (singleConcrete == null || program.typeOracle.isInstantiatedType(singleConcrete));
          if (singleConcrete == null) {
            return null;
          }
          return refType.canBeNull() ? singleConcrete : singleConcrete.getNonNull();
        }
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
      JReferenceType refType = (JReferenceType) x.getType();

      if (refType == typeNull) {
        return;
      }

      // tighten based on non-instantiability
      if (!program.typeOracle.isInstantiatedType(refType)) {
        x.setType(typeNull);
        madeChanges();
        return;
      }

      // tighten based on leaf types
      JReferenceType leafType = getSingleConcreteType(refType);
      if (leafType != null) {
        x.setType(leafType);
        madeChanges();
        return;
      }

      // tighten based on assignment
      List<JReferenceType> typeList = new ArrayList<JReferenceType>();

      /*
       * For fields without an initializer, add a null assignment, because the
       * field might be accessed before initialized. Technically even a field
       * with an initializer might be accessed before initialization, but
       * presumably that is not the programmer's intent, so the compiler cheats
       * and assumes the initial null will not be seen.
       */
      if ((x instanceof JField) && !x.hasInitializer()) {
        typeList.add(typeNull);
      }

      Collection<JExpression> myAssignments = assignments.get(x);
      if (myAssignments != null) {
        for (JExpression expr : myAssignments) {
          JType type = expr.getType();
          if (!(type instanceof JReferenceType)) {
            return; // something fishy is going on, just abort
          }
          typeList.add((JReferenceType) type);
        }
      }

      if (x instanceof JParameter) {
        Collection<JParameter> myParams = paramUpRefs.get(x);
        if (myParams != null) {
          for (JParameter param : myParams) {
            typeList.add((JReferenceType) param.getType());
          }
        }
      }

      JReferenceType resultType;
      if (!typeList.isEmpty()) {
        resultType = program.generalizeTypes(typeList);
        resultType = program.strongerType(refType, resultType);
      } else {
        if (x instanceof JParameter) {
          /*
           * There is no need to tighten unused parameters, because they will be
           * pruned.
           */
          resultType = refType;
        } else {
          resultType = typeNull;
        }
      }

      if (x.getType() != resultType) {
        x.setType(resultType);
        madeChanges();
      }
    }
  }

  private static final String NAME = TypeTightener.class.getSimpleName();

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new TypeTightener(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private static <T, V> void add(T target, V value, Map<T, Collection<V>> map) {
    Collection<V> list = map.get(target);
    if (list == null) {
      list = new LinkedHashSet<V>();
      map.put(target, list);
    }
    list.add(value);
  }

  /**
   * Find exactly one concrete element for a key in a Map of Sets. If there are
   * none or more than one concrete element, return <code>null</code>.
   */
  private static <B, T extends CanBeAbstract> T getSingleConcrete(B x,
      Map<? super B, ? extends Collection<T>> map) {

    Collection<T> collection = map.get(x);
    // No collection, then no concrete version
    if (collection == null) {
      return null;
    }

    T toReturn = null;
    for (T elt : collection) {
      if (elt.isAbstract()) {
        continue;
      }

      // If we already have previously seen a concrete element, fail
      if (toReturn != null) {
        return null;
      } else {
        toReturn = elt;
      }
    }

    return toReturn;
  }

  /**
   * For each program Variable (includes fields, locals and parameters) tracks the set
   * of expressions that are assigned to them. Assignments include parameter instantiations.
   *
   */
  private final Map<JVariable, Collection<JExpression>> assignments =
      new IdentityHashMap<JVariable, Collection<JExpression>>();
  /**
   * For each type tracks all classes the extend or implement it.
   */
  private final Map<JReferenceType, Collection<JClassType>> implementors =
      new IdentityHashMap<JReferenceType, Collection<JClassType>>();
  /**
   * For each method tracks of all the methods that override it.
   */
  private final Map<JMethod, Collection<JMethod>> overriders =
      new IdentityHashMap<JMethod, Collection<JMethod>>();
  /**
   * For each parameter P (in method M) tracks the set of parameters that share its position in all
   * the methods that are overridden by M.
   */
  private final Map<JParameter, Collection<JParameter>> paramUpRefs =
      new IdentityHashMap<JParameter, Collection<JParameter>>();
  /**
   * For each method tracks the set of all expressions that are returned.
   */
  private final Map<JMethod, Collection<JExpression>> returns =
      new IdentityHashMap<JMethod, Collection<JExpression>>();

  private final JProgram program;
  private final JNullType typeNull;

  private TypeTightener(JProgram program) {
    this.program = program;
    typeNull = program.getTypeNull();
  }

  private OptimizerStats execImpl() {
    OptimizerStats stats = new OptimizerStats(NAME);
    RecordVisitor recorder = new RecordVisitor();
    recorder.accept(program);

    /*
     * We must iterate multiple times because each way point we tighten creates
     * more opportunities to do additional tightening for the things that depend
     * on it.
     *
     * TODO(zundel): See if we can remove this loop, or otherwise run to less
     * than completion if we compile with an option for less than 100% optimized
     * output.
     */
    while (true) {
      TightenTypesVisitor tightener = new TightenTypesVisitor();
      tightener.accept(program);
      stats.recordModified(tightener.getNumMods());
      if (!tightener.didChange()) {
        break;
      }
    }

    if (stats.didChange()) {
      FixDanglingRefsVisitor fixer = new FixDanglingRefsVisitor();
      fixer.accept(program);
    }

    return stats;
  }
}
