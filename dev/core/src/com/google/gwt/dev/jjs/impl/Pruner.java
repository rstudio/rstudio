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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
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
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
   * and null method, and drop assignments to pruned variables.
   */
  private class CleanupRefsVisitor extends JModVisitor {
    private final Stack<JExpression> lValues = new Stack<JExpression>();
    private final Map<JMethod, ArrayList<JParameter>> methodToOriginalParamsMap;
    private final Set<? extends JNode> referencedNonTypes;
    {
      // Initialize a sentinel value to avoid having to check for empty stack.
      lValues.push(null);
    }

    public CleanupRefsVisitor(Set<? extends JNode> referencedNodes,
        Map<JMethod, ArrayList<JParameter>> methodToOriginalParamsMap) {
      this.referencedNonTypes = referencedNodes;
      this.methodToOriginalParamsMap = methodToOriginalParamsMap;
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
    public void endVisit(JMethod x, Context ctx) {
      JType type = x.getType();
      if (type instanceof JReferenceType &&
          !program.typeOracle.isInstantiatedType((JReferenceType) type)) {
        x.setType(program.getTypeNull());
      }
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

      // Did we prune the parameters of the method we're calling?
      if (methodToOriginalParamsMap.containsKey(method)) {
        JMethodCall newCall = new JMethodCall(x, x.getInstance());
        replaceForPrunedParameters(x, newCall, ctx);
      }
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
      // Did we prune the parameters of the method we're calling?
      if (methodToOriginalParamsMap.containsKey(x.getTarget())) {
        JMethodCall newCall = new JNewInstance(x);
        replaceForPrunedParameters(x, newCall, ctx);
      }
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
    public boolean visit(JBinaryOperation x, Context ctx) {
      if (x.getOp() == JBinaryOperator.ASG) {
        lValues.push(x.getLhs());
      }
      return true;
    }

    @Override
    public boolean visit(JDeclarationStatement x, Context ctx) {
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

    private void replaceForPrunedParameters(JMethodCall x, JMethodCall newCall, Context ctx) {
      assert !x.getTarget().canBePolymorphic();
      List<JParameter> originalParams = methodToOriginalParamsMap.get(x.getTarget());
      JMultiExpression currentMulti = null;
      for (int i = 0, c = x.getArgs().size(); i < c; ++i) {
        JExpression arg = x.getArgs().get(i);
        JParameter param = null;
        if (i < originalParams.size()) {
          param = originalParams.get(i);
        }

        if (param != null && referencedNonTypes.contains(param)) {
          // If there is an existing multi, terminate it.
          if (currentMulti != null) {
            currentMulti.addExpressions(arg);
            newCall.addArg(currentMulti);
            currentMulti = null;
          } else {
            newCall.addArg(arg);
          }
        } else if (arg.hasSideEffects()) {
          // The argument is only needed for side effects, add it to a multi.
          if (currentMulti == null) {
            currentMulti = new JMultiExpression(x.getSourceInfo());
          }
          currentMulti.addExpressions(arg);
        }
      }

      // Add any orphaned parameters on the end. Extra params are OK.
      if (currentMulti != null) {
        newCall.addArg(currentMulti);
      }

      ctx.replaceMe(newCall);
    }
  }

  /**
   * Remove any unreferenced classes and interfaces from JProgram. Remove any
   * unreferenced methods and fields from their containing classes.
   */
  private class PruneVisitor extends JModVisitor {
    private final Map<JMethod, ArrayList<JParameter>> methodToOriginalParamsMap =
        new HashMap<JMethod, ArrayList<JParameter>>();
    private final Set<? extends JNode> referencedNonTypes;
    private final Set<? extends JReferenceType> referencedTypes;

    public PruneVisitor(Set<? extends JReferenceType> referencedTypes,
        Set<? extends JNode> referencedNodes) {
      this.referencedTypes = referencedTypes;
      this.referencedNonTypes = referencedNodes;
    }

    public Map<JMethod, ArrayList<JParameter>> getMethodToOriginalParamsMap() {
      return methodToOriginalParamsMap;
    }

    @Override
    public boolean visit(JClassType type, Context ctx) {
      assert (referencedTypes.contains(type));
      for (int i = 0; i < type.getFields().size(); ++i) {
        JField field = type.getFields().get(i);
        if (!referencedNonTypes.contains(field)) {
          type.removeField(i);
          madeChanges();
          --i;
        }
      }

      for (int i = 0; i < type.getMethods().size(); ++i) {
        JMethod method = type.getMethods().get(i);
        if (!referencedNonTypes.contains(method)) {
          // Never prune clinit directly out of the class.
          if (i > 0) {
            type.removeMethod(i);
            program.removeStaticImplMapping(method);
            madeChanges();
            --i;
          }
        } else {
          accept(method);
        }
      }

      return false;
    }

    @Override
    public boolean visit(JInterfaceType type, Context ctx) {
      boolean isReferenced = referencedTypes.contains(type);
      boolean isInstantiated = program.typeOracle.isInstantiatedType(type);

      for (int i = 0; i < type.getFields().size(); ++i) {
        JField field = type.getFields().get(i);
        // all interface fields are static and final
        if (!isReferenced || !referencedNonTypes.contains(field)) {
          type.removeField(i);
          madeChanges();
          --i;
        }
      }

      // Start at index 1; never prune clinit directly out of the interface.
      for (int i = 1; i < type.getMethods().size(); ++i) {
        JMethod method = type.getMethods().get(i);
        // all other interface methods are instance and abstract
        if (!isInstantiated || !referencedNonTypes.contains(method)) {
          type.removeMethod(i);
          assert program.instanceMethodForStaticImpl(method) == null;
          madeChanges();
          --i;
        }
      }

      return false;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
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

        JsFunction func = x.isNative() ? ((JsniMethodBody) x.getBody()).getFunc() : null;

        ArrayList<JParameter> originalParams = new ArrayList<JParameter>(x.getParams());

        for (int i = 0; i < x.getParams().size(); ++i) {
          JParameter param = x.getParams().get(i);
          if (!referencedNonTypes.contains(param)) {
            x.removeParam(i);
            madeChanges();
            // Remove the associated JSNI parameter
            if (func != null) {
              func.getParameters().remove(i);
            }
            --i;
            methodToOriginalParamsMap.put(x, originalParams);
          }
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
      for (JMethod method : program.getEntryMethods()) {
        accept(method);
      }
      for (Iterator<JDeclaredType> it = program.getDeclaredTypes().iterator(); it.hasNext();) {
        JDeclaredType type = it.next();
        if (referencedTypes.contains(type) || program.typeOracle.isInstantiatedType(type)) {
          accept(type);
        } else {
          it.remove();
          madeChanges();
        }
      }
      return false;
    }
  }

  private static final String NAME = Pruner.class.getSimpleName();

  public static OptimizerStats exec(JProgram program, boolean noSpecialTypes) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new Pruner(program, noSpecialTypes).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
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
            primitiveTypeOrNullType(program, x.getType()));
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

    JMethodCall newCall =
        new JMethodCall(x.getSourceInfo(), instance, program.getNullMethod(),
            primitiveTypeOrNullType(program, x.getType()));
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
  static JType primitiveTypeOrNullType(JProgram program, JType type) {
    if (type instanceof JPrimitiveType) {
      return type;
    }
    return program.getTypeNull();
  }

  private final JProgram program;

  private final boolean saveCodeGenTypes;

  private Pruner(JProgram program, boolean saveCodeGenTypes) {
    this.program = program;
    this.saveCodeGenTypes = saveCodeGenTypes;
  }

  private OptimizerStats execImpl() {
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
            .getLiveFieldsAndMethods());
    pruner.accept(program);
    stats.recordModified(pruner.getNumMods());
    if (!pruner.didChange()) {
      return stats;
    }
    CleanupRefsVisitor cleaner =
        new CleanupRefsVisitor(livenessAnalyzer.getLiveFieldsAndMethods(), pruner
            .getMethodToOriginalParamsMap());
    cleaner.accept(program.getDeclaredTypes());
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
