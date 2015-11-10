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
import com.google.gwt.dev.jjs.ast.CanBeStatic;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.HasName;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNameOf;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRuntimeTypeReference;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.util.collect.Stack;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.ListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Remove globally unreferenced classes, interfaces, methods, parameters, and
 * fields from the AST. This algorithm is based on having known "entry points"
 * into the application which serve as the root(s) from which reachability is
 * determined and everything else is rescued. Pruner determines reachability at
 * a global level based on method calls and new operations; it does not perform
 * any local code flow analysis. But, a local code flow optimization pass that
 * can eliminate method calls would allow Pruner to prune additional nodes.
 *
 * Note: references to pruned types may still exist in the tree after this pass
 * runs, however, it should only be in contexts that do not rely on any code
 * generation for the pruned type. For example, it's legal to have a variable of
 * a pruned type, or to try to cast to a pruned type. These will cause natural
 * failures at run time; or later optimizations might be able to hard-code
 * failures at compile time.
 *
 * Note: this class is limited to pruning parameters of static methods only.
 */
public class Pruner {
  /**
   * Remove assignments to pruned fields, locals and params. Nullify the return
   * type of methods declared to return a globally uninstantiable type. Replace
   * references to pruned variables and methods by references to the null field
   * and null method, assignments to pruned variables, and nullify the type of
   * variable whose type is a pruned type.
   */
  private class CleanupRefsVisitor extends JModVisitorWithTemporaryVariableCreation {
    private final Stack<JExpression> lValues = new Stack<JExpression>();
    private final ListMultimap<JMethod, JParameter> priorParametersByMethod;
    private final Set<? extends JNode> referencedNonTypes;
    {
      // Initialize a sentinel value to avoid having to check for empty stack.
      lValues.push(null);
    }

    public CleanupRefsVisitor(Set<? extends JNode> referencedNodes,
        ListMultimap<JMethod, JParameter> priorParametersByMethod,
        OptimizerContext optimizerCtx) {
      super(optimizerCtx);
      this.referencedNonTypes = referencedNodes;
      this.priorParametersByMethod = priorParametersByMethod;
    }

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp() != JBinaryOperator.ASG) {
        return;
      }
      // The LHS of assignments may have been pruned.
      lValues.pop();
      JExpression lhs = x.getLhs();
      if (!(lhs instanceof JVariableRef)) {
        return;
      }

      JVariableRef variableRef = (JVariableRef) lhs;
      if (isVariablePruned(variableRef.getTarget())) {
        // TODO: better null tracking; we might be missing some NPEs here.
        JExpression replacement =
            makeReplacementForAssignment(x.getSourceInfo(), variableRef, x.getRhs());
        ctx.replaceMe(replacement);
      }
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      super.endVisit(x, ctx);
      lValues.pop();
      // The variable may have been pruned.
      if (isVariablePruned(x.getVariableRef().getTarget())) {
        JExpression replacement =
            makeReplacementForAssignment(x.getSourceInfo(), x.getVariableRef(), x.getInitializer());
        ctx.replaceMe(replacement.makeStatement());
      }
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {
      // Handle l-values at a higher level.
      if (lValues.peek() == x) {
        return;
      }

      if (isPruned(x.getField())) {
        // The field is gone; replace x by a null field reference.
        JFieldRef fieldRef = transformToNullFieldRef(x, program);
        ctx.replaceMe(fieldRef);
      }
    }

    @Override
    public void exit(JMethod x, Context ctx) {
      JType type = x.getType();
      if (type instanceof JReferenceType &&
          !program.typeOracle.isInstantiatedType((JReferenceType) type)) {
        x.setType(JReferenceType.NULL_TYPE);
      }
      Predicate<JMethod> isPruned = new Predicate<JMethod>() {
        @Override
        public boolean apply(JMethod method) {
          return isPruned(method);
        }
      };
      Iterables.removeIf(x.getOverriddenMethods(), isPruned);
      Iterables.removeIf(x.getOverridingMethods(), isPruned);
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      // Is the method pruned entirely?
      if (isPruned(method)) {
        /*
         * We assert that method must be non-static, otherwise it would have
         * been rescued.
         */
        ctx.replaceMe(transformToNullMethodCall(x, program));
        return;
      }

      maybeReplaceForPrunedParameters(x, ctx);
    }

    @Override
    public void endVisit(JNameOf x, Context ctx) {
      HasName node = x.getNode();
      boolean pruned;
      if (node instanceof JField) {
        pruned = isPruned((JField) node);
      } else if (node instanceof JMethod) {
        pruned = isPruned((JMethod) node);
      } else if (node instanceof JReferenceType) {
        pruned = !program.typeOracle.isInstantiatedType((JReferenceType) node);
      } else {
        throw new InternalCompilerException("Unhandled JNameOf node: " + node);
      }

      if (pruned) {
        ctx.replaceMe(program.getLiteralNull());
      }
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      maybeReplaceForPrunedParameters(x, ctx);
    }

    @Override
    public void endVisit(JRuntimeTypeReference x, Context ctx) {
      if (!program.typeOracle.isInstantiatedType(x.getReferredType())) {
        ctx.replaceMe(program.getLiteralNull());
      }
    }

    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      if (isPruned(x.getField())) {
        String ident = x.getIdent();
        JField nullField = program.getNullField();
        JsniFieldRef nullFieldRef =
            new JsniFieldRef(x.getSourceInfo(), ident, nullField, x.getEnclosingType(), x
                .isLvalue());
        ctx.replaceMe(nullFieldRef);
      }
    }

    @Override
    public void endVisit(JsniMethodRef x, Context ctx) {
      // Redirect JSNI refs to uninstantiable types to the null method.
      if (isPruned(x.getTarget())) {
        String ident = x.getIdent();
        JMethod nullMethod = program.getNullMethod();
        JsniMethodRef nullMethodRef =
            new JsniMethodRef(x.getSourceInfo(), ident, nullMethod, program.getJavaScriptObject());
        ctx.replaceMe(nullMethodRef);
      }
    }

    @Override
    public void exit(JVariable x, Context ctx) {
      JType type = x.getType();
      if (type instanceof JReferenceType &&
          !program.typeOracle.isInstantiatedType((JReferenceType) type)) {
        x.setType(JReferenceType.NULL_TYPE);
        madeChanges();
      }
    }

    @Override
    public boolean visit(JBinaryOperation x, Context ctx) {
      if (x.getOp() == JBinaryOperator.ASG) {
        lValues.push(x.getLhs());
      }
      return true;
    }

    @Override
    public boolean visit(JDeclarationStatement x, Context ctx) {
      super.visit(x, ctx);
      lValues.push(x.getVariableRef());
      return true;
    }

    private <T extends HasEnclosingType & CanBeStatic> boolean isPruned(T node) {
      if (!referencedNonTypes.contains(node)) {
        return true;
      }
      JReferenceType enclosingType = node.getEnclosingType();
      return !node.isStatic() && enclosingType != null
          && !program.typeOracle.isInstantiatedType(enclosingType);
    }

    private boolean isVariablePruned(JVariable variable) {
      if (variable instanceof JField) {
        return isPruned((JField) variable);
      }
      return !referencedNonTypes.contains(variable);
    }

    private JExpression makeReplacementForAssignment(SourceInfo info, JVariableRef variableRef,
        JExpression rhs) {
      // Replace with a multi, which may wind up empty.
      JMultiExpression multi = new JMultiExpression(info);

      // If the lhs is a field ref, evaluate it first.
      if (variableRef instanceof JFieldRef) {
        JFieldRef fieldRef = (JFieldRef) variableRef;
        JExpression instance = fieldRef.getInstance();
        if (instance != null) {
          multi.addExpressions(instance);
        }
      }

      // If there is an rhs, evaluate it second.
      if (rhs != null) {
        multi.addExpressions(rhs);
      }
      if (multi.getNumberOfExpressions() == 1) {
        return multi.getExpression(0);
      } else {
        return multi;
      }
    }

    // Arguments for pruned parameters will be pushed right into a multiexpression that will be
    // evaluated with the next arg, e.g. m(arg1, (prunnedArg2, prunnedArg3, arg4)).
    private void maybeReplaceForPrunedParameters(JMethodCall x, Context ctx) {
      if (!priorParametersByMethod.containsKey(x.getTarget())) {
        // No parameter was pruned.
        return;
      }

      JMethodCall replacementCall = x.cloneWithoutParameters();

      assert !x.getTarget().canBePolymorphic();
      List<JParameter> originalParams = priorParametersByMethod.get(x.getTarget());

      // The method and the call agree in the number of parameters.
      assert originalParams.size() == x.getArgs().size();

      // Traverse the call arguments left to right.
      SourceInfo sourceInfo = x.getSourceInfo();
      JMultiExpression unevaluatedArgumentsForPrunedParameters =
          new JMultiExpression(sourceInfo);
      List<JExpression> args = x.getArgs();
      for (int currentArgumentIndex = 0; currentArgumentIndex < args.size();
          ++currentArgumentIndex) {
        JExpression arg = args.get(currentArgumentIndex);

        // If the parameter was not pruned .
        if (referencedNonTypes.contains(originalParams.get(currentArgumentIndex))) {
          // Add the current argument to the list of unevaluated arguments and pass the multi
          // expression to the call.
          unevaluatedArgumentsForPrunedParameters.addExpressions(arg);
          replacementCall.addArg(unevaluatedArgumentsForPrunedParameters);
          // Reset the accumulating multi expression.
          unevaluatedArgumentsForPrunedParameters =  new JMultiExpression(sourceInfo);
        } else if (arg.hasSideEffects()) {
          // If the argument was pruned and has sideffects accumulate it; otherwise discard.
          unevaluatedArgumentsForPrunedParameters.addExpressions(arg);
        }
      }

      if (unevaluatedArgumentsForPrunedParameters.isEmpty()) {
        // We are done, all (side effectful) parameters have been evaluated.
        ctx.replaceMe(replacementCall);
        return;
      }

      // If the last few parameters where pruned, we need to evaluate the (side effectful) arguments
      // for those parameters.
      if (replacementCall.getArgs().isEmpty()) {
        // All parameters have been pruned, replace by (prunedArg1, ..., prunedArgn, m()).
        unevaluatedArgumentsForPrunedParameters.addExpressions(replacementCall);
        ctx.replaceMe(unevaluatedArgumentsForPrunedParameters);
        return;
      }
      // Some parameters have been pruned from the end, replace by
      // m(arg1,..., (lastArg = lastUnprunedArg, remainingArgs, lastArg))
      JExpression lastArg = Iterables.getLast(replacementCall.getArgs());
      JLocal tempVar =
          createTempLocal(sourceInfo, Iterables.getLast(
              Iterables.filter(originalParams, Predicates.in(referencedNonTypes))).getType(), "lastArg");
      unevaluatedArgumentsForPrunedParameters.addExpressions(0, JProgram.createAssignment(
          lastArg.getSourceInfo(), tempVar.makeRef(sourceInfo), lastArg));
      unevaluatedArgumentsForPrunedParameters.addExpressions(tempVar.makeRef(sourceInfo));
      replacementCall.setArg(replacementCall.getArgs().size() - 1, unevaluatedArgumentsForPrunedParameters);
      ctx.replaceMe(replacementCall);
    }
  }

  /**
   * Remove any unreferenced classes and interfaces from JProgram. Remove any
   * unreferenced methods and fields from their containing classes.
   */
  private class PruneVisitor extends JChangeTrackingVisitor {
    private final ListMultimap<JMethod, JParameter> priorParametersByMethod =
        ArrayListMultimap.create();
    private final Set<? extends JNode> referencedNonTypes;
    private final Set<? extends JReferenceType> referencedTypes;

    public PruneVisitor(Set<? extends JReferenceType> referencedTypes,
        Set<? extends JNode> referencedNodes, OptimizerContext optimizerCtx) {
      super(optimizerCtx);
      this.referencedTypes = referencedTypes;
      this.referencedNonTypes = referencedNodes;
    }

    public ListMultimap<JMethod, JParameter> getPriorParametersByMethod() {
      return priorParametersByMethod;
    }

    @Override
    public boolean visit(JDeclaredType type, Context ctx) {
      assert referencedTypes.contains(type);
      Predicate<JNode> notReferenced = Predicates.not(Predicates.in(referencedNonTypes));
      removeFields(notReferenced, type);
      removeMethods(notReferenced, type);

      for (JMethod method : type.getMethods()) {
        accept(method);
      }

      return false;
    }

    @Override
    public boolean enter(JMethod x, Context ctx) {
      if (!x.canBePolymorphic()) {
        /*
         * Don't prune parameters on unreferenced methods. The methods might not
         * be reachable through the current method traversal routines, but might
         * be used or checked elsewhere.
         *
         * Basically, if we never actually checked if the method parameters were
         * used or not, don't prune them. Doing so would leave a number of
         * dangling JParameterRefs that blow up in later optimizations.
         */
        if (!referencedNonTypes.contains(x)) {
          return true;
        }

        /*
         * We cannot prune parameters from staticImpls that still have a live
         * instance method, because doing so would screw up any subsequent
         * devirtualizations. If the instance method has been pruned, then it's
         * okay. Also, it's okay on the final pass (saveCodeTypes == false)
         * since no more devirtualizations will occur.
         *
         * TODO: prune params; MakeCallsStatic smarter to account for it.
         */
        JMethod instanceMethod = program.instanceMethodForStaticImpl(x);
        // Unless the instance method has already been pruned, of course.
        if (saveCodeGenTypes && instanceMethod != null &&
            referencedNonTypes.contains(instanceMethod)) {
          // instance method is still live
          return true;
        }

        List<JParameter> originalParameters = ImmutableList.copyOf(x.getParams());

        for (int i = 0; i < x.getParams().size(); ++i) {
          JParameter param = x.getParams().get(i);
          if (!referencedNonTypes.contains(param)) {
            x.removeParam(i);
            madeChanges();
            --i;
          }
        }

        if (x.getParams().size() != originalParameters.size()) {
          // Parameters were pruned. record the original parameters for the cleanup pass.
          priorParametersByMethod.putAll(x, originalParameters);
        }
      }

      return true;
    }

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      for (int i = 0; i < x.getLocals().size(); ++i) {
        if (!referencedNonTypes.contains(x.getLocals().get(i))) {
          x.removeLocal(i--);
          madeChanges();
        }
      }
      return false;
    }

    @Override
    public boolean visit(JProgram program, Context ctx) {
      for (Iterator<JDeclaredType> it = program.getDeclaredTypes().iterator(); it.hasNext();) {
        JDeclaredType type = it.next();
        if (referencedTypes.contains(type)) {
          accept(type);
        } else {
          prunedMethods.addAll(type.getMethods());
          methodsWereRemoved(type.getMethods());
          fieldsWereRemoved(type.getFields());
          it.remove();
          madeChanges();
        }
      }
      return false;
    }

    private void removeFields(Predicate<JNode> shouldRemove, JDeclaredType type) {
      for (int i = 0; i < type.getFields().size(); ++i) {
        JField field = type.getFields().get(i);
        if (!shouldRemove.apply(field)) {
          continue;
        }
        wasRemoved(field);
        type.removeField(i);
        madeChanges();
        --i;
      }
    }

    private void removeMethods(Predicate<JNode> shouldRemove, JDeclaredType type) {
      // Skip method 0 which is clinit and is assumed to exist.
      assert type.getMethods().get(0) == type.getClinitMethod();
      for (int i = 1; i < type.getMethods().size(); ++i) {
        JMethod method = type.getMethods().get(i);
        if (!shouldRemove.apply(method)) {
          continue;
        }
        prunedMethods.add(method);
        wasRemoved(method);
        type.removeMethod(i);
        program.removeStaticImplMapping(method);
        madeChanges();
        --i;
      }
    }
  }

  private static final String NAME = Pruner.class.getSimpleName();

  public static OptimizerStats exec(JProgram program, boolean noSpecialTypes,
      OptimizerContext optimizerCtx) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new Pruner(program, noSpecialTypes).execImpl(optimizerCtx);
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  public static OptimizerStats exec(JProgram program, boolean noSpecialTypes) {
    return exec(program, noSpecialTypes, OptimizerContext.NULL_OPTIMIZATION_CONTEXT);
  }

  /**
   * Transform a reference to a pruned instance field into a reference to the
   * null field, which will be used to replace <code>x</code>.
   */
  public static JFieldRef transformToNullFieldRef(JFieldRef x, JProgram program) {
    JExpression instance = x.getInstance();

    /*
     * We assert that field must be non-static if it's an rvalue, otherwise it
     * would have been rescued.
     */
    // assert !x.getField().isStatic();
    /*
     * HACK HACK HACK: ControlFlowAnalyzer has special hacks for dealing with
     * ClassLiterals, which causes the body of ClassLiteralHolder's clinit to
     * never be rescured. This in turn causes invalid references to static
     * methods, which violates otherwise good assumptions about compiler
     * operation.
     *
     * TODO: Remove this when ControlFlowAnalyzer doesn't special-case
     * CLH.clinit().
     */
    if (x.getField().isStatic() && instance == null) {
      instance = program.getLiteralNull();
    }

    assert instance != null;
    if (!instance.hasSideEffects()) {
      instance = program.getLiteralNull();
    }

    JFieldRef fieldRef =
        new JFieldRef(x.getSourceInfo(), instance, program.getNullField(), x.getEnclosingType(),
            primitiveTypeOrNullTypeOrArray(program, x.getType()));
    return fieldRef;
  }

  /**
   * Transform a call to a pruned instance method (or static impl) into a call
   * to the null method, which will be used to replace <code>x</code>.
   */
  public static JMethodCall transformToNullMethodCall(JMethodCall x, JProgram program) {
    JExpression instance = x.getInstance();
    List<JExpression> args = x.getArgs();
    if (program.isStaticImpl(x.getTarget())) {
      instance = args.get(0);
      args = args.subList(1, args.size());
    } else {
      /*
       * We assert that method must be non-static, otherwise it would have been
       * rescued.
       */
      // assert !x.getTarget().isStatic();
      /*
       * HACK HACK HACK: ControlFlowAnalyzer has special hacks for dealing with
       * ClassLiterals, which causes the body of ClassLiteralHolder's clinit to
       * never be rescured. This in turn causes invalid references to static
       * methods, which violates otherwise good assumptions about compiler
       * operation.
       *
       * TODO: Remove this when ControlFlowAnalyzer doesn't special-case
       * CLH.clinit().
       */
      if (x.getTarget().isStatic() && instance == null) {
        instance = program.getLiteralNull();
      }
    }
    assert (instance != null);
    if (!instance.hasSideEffects()) {
      instance = program.getLiteralNull();
    }

    JMethodCall newCall = new JMethodCall(x.getSourceInfo(), instance, program.getNullMethod());
    newCall.overrideReturnType(primitiveTypeOrNullTypeOrArray(program, x.getType()));
    // Retain the original arguments, they will be evaluated for side effects.
    for (JExpression arg : args) {
      if (arg.hasSideEffects()) {
        newCall.addArg(arg);
      }
    }
    return newCall;
  }

  /**
   * Return the smallest type that is is a subtype of the argument.
   */
  static JType primitiveTypeOrNullTypeOrArray(JProgram program, JType type) {
    if (type instanceof JArrayType) {
      JType leafType = primitiveTypeOrNullTypeOrArray(program, ((JArrayType) type).getLeafType());
      return program.getOrCreateArrayType(leafType, ((JArrayType) type).getDims());
    }
    if (type instanceof JPrimitiveType) {
      return type;
    }
    return JReferenceType.NULL_TYPE;
  }

  private final JProgram program;

  private final boolean saveCodeGenTypes;

  private final Set<JMethod> prunedMethods = Sets.newLinkedHashSet();

  private Pruner(JProgram program, boolean saveCodeGenTypes) {
    this.program = program;
    this.saveCodeGenTypes = saveCodeGenTypes;
  }

  private OptimizerStats execImpl(OptimizerContext optimizerCtx) {
    OptimizerStats stats = new OptimizerStats(NAME);

    ControlFlowAnalyzer livenessAnalyzer = new ControlFlowAnalyzer(program);
    livenessAnalyzer.setForPruning();

    // SPECIAL: Immortal codegen types are never pruned
    traverseTypes(livenessAnalyzer, program.immortalCodeGenTypes);

    if (saveCodeGenTypes) {
      /*
       * SPECIAL: Some classes contain methods used by code generation later.
       * Unless those transforms have already been performed, we must rescue all
       * contained methods for later user.
       */
      traverseTypes(livenessAnalyzer, program.codeGenTypes);
    }
    livenessAnalyzer.traverseEverything();

    program.typeOracle.setInstantiatedTypes(livenessAnalyzer.getInstantiatedTypes());

    PruneVisitor pruner =
        new PruneVisitor(livenessAnalyzer.getReferencedTypes(), livenessAnalyzer
            .getLiveFieldsAndMethods(), optimizerCtx);
    pruner.accept(program);
    stats.recordModified(pruner.getNumMods());

    if (!pruner.didChange()) {
      return stats;
    }
    CleanupRefsVisitor cleaner =
        new CleanupRefsVisitor(livenessAnalyzer.getLiveFieldsAndMethods(), pruner
            .getPriorParametersByMethod(), optimizerCtx);
    cleaner.accept(program.getDeclaredTypes());
    optimizerCtx.incOptimizationStep();
    optimizerCtx.syncDeletedSubCallGraphsSince(optimizerCtx.getLastStepFor(NAME) + 1,
        prunedMethods);
    JavaAstVerifier.assertProgramIsConsistent(program);
    return stats;
  }

  /**
   * Traverse from all methods starting from a set of types.
   */
  private void traverseTypes(ControlFlowAnalyzer livenessAnalyzer,
      List<JClassType> types) {
    for (JClassType type : types) {
      livenessAnalyzer.traverseFromReferenceTo(type);
      for (JMethod method : type.getMethods()) {
        if (method instanceof JConstructor) {
          livenessAnalyzer.traverseFromInstantiationOf(type);
        }
        livenessAnalyzer.traverseFrom(method);
      }
    }
  }
}
