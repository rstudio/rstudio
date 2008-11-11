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
import com.google.gwt.dev.jjs.ast.CanBeStatic;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.HasEnclosingType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.ast.JsFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
   * Remove assignments to pruned fields, locals and params. Also nullify the
   * return type of methods declared to return a globally uninstantiable type.
   */
  private class CleanupRefsVisitor extends JModVisitor {
    private final Map<JMethod, ArrayList<JParameter>> methodToOriginalParamsMap;
    private final Set<? extends JNode> referencedNonTypes;

    public CleanupRefsVisitor(Set<? extends JNode> referencedNodes,
        Map<JMethod, ArrayList<JParameter>> methodToOriginalParamsMap) {
      this.referencedNonTypes = referencedNodes;
      this.methodToOriginalParamsMap = methodToOriginalParamsMap;
    }

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      // The LHS of assignments may have been pruned.
      if (x.getOp() == JBinaryOperator.ASG) {
        JExpression lhs = x.getLhs();
        if (lhs instanceof JVariableRef) {
          JVariableRef variableRef = (JVariableRef) lhs;
          if (!referencedNonTypes.contains(variableRef.getTarget())) {
            // TODO: better null tracking; we might be missing some NPEs here.
            JExpression replacement = makeReplacementForAssignment(
                x.getSourceInfo(), variableRef, x.getRhs());
            ctx.replaceMe(replacement);
          }
        }
      }
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      // The variable may have been pruned.
      if (!referencedNonTypes.contains(x.getVariableRef().getTarget())) {
        JExpression replacement = makeReplacementForAssignment(
            x.getSourceInfo(), x.getVariableRef(), x.getInitializer());
        ctx.replaceMe(replacement.makeStatement());
      }
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      JType type = x.getType();
      if (type instanceof JReferenceType) {
        if (!program.typeOracle.isInstantiatedType((JReferenceType) type)) {
          x.setType(program.getTypeNull());
        }
      }
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      // Did we prune the parameters of the method we're calling?
      if (methodToOriginalParamsMap.containsKey(method)) {
        // This must be a static method
        assert method.isStatic();

        JMethodCall newCall = new JMethodCall(program, x.getSourceInfo(),
            x.getInstance(), method);
        if (!x.canBePolymorphic()) {
          newCall.setCannotBePolymorphic();
        }

        ArrayList<JExpression> args = x.getArgs();
        ArrayList<JParameter> originalParams = methodToOriginalParamsMap.get(method);

        JMultiExpression currentMulti = null;
        for (int i = 0, c = args.size(); i < c; ++i) {
          JExpression arg = args.get(i);
          JParameter param = null;
          if (i < originalParams.size()) {
            param = originalParams.get(i);
          }

          if (param != null && referencedNonTypes.contains(param)) {
            // If there is an existing multi, terminate it.
            if (currentMulti != null) {
              currentMulti.exprs.add(arg);
              newCall.getArgs().add(currentMulti);
              currentMulti = null;
            } else {
              newCall.getArgs().add(arg);
            }
          } else if (arg.hasSideEffects()) {
            // The argument is only needed for side effects, add it to a multi.
            if (currentMulti == null) {
              currentMulti = new JMultiExpression(program, x.getSourceInfo());
            }
            currentMulti.exprs.add(arg);
          }
        }

        // Add any orphaned parameters on the end. Extra params are OK.
        if (currentMulti != null) {
          newCall.getArgs().add(currentMulti);
        }

        ctx.replaceMe(newCall);
      }
    }

    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      if (isUninstantiable(x.getField())) {
        String ident = x.getIdent();
        JField nullField = program.getNullField();
        program.jsniMap.put(ident, nullField);
        JsniFieldRef nullFieldRef = new JsniFieldRef(program,
            x.getSourceInfo(), ident, nullField, x.getEnclosingType(),
            x.isLvalue());
        ctx.replaceMe(nullFieldRef);
      }
    }

    @Override
    public void endVisit(JsniMethodRef x, Context ctx) {
      // Redirect JSNI refs to uninstantiable types to the null method.
      if (isUninstantiable(x.getTarget())) {
        String ident = x.getIdent();
        JMethod nullMethod = program.getNullMethod();
        program.jsniMap.put(ident, nullMethod);
        JsniMethodRef nullMethodRef = new JsniMethodRef(program,
            x.getSourceInfo(), ident, nullMethod);
        ctx.replaceMe(nullMethodRef);
      }
    }

    private <T extends HasEnclosingType & CanBeStatic> boolean isUninstantiable(
        T node) {
      JReferenceType enclosingType = node.getEnclosingType();
      return !node.isStatic() && enclosingType != null
          && !program.typeOracle.isInstantiatedType(enclosingType);
    }

    private JExpression makeReplacementForAssignment(SourceInfo info,
        JVariableRef variableRef, JExpression rhs) {
      // Replace with a multi, which may wind up empty.
      JMultiExpression multi = new JMultiExpression(program, info);

      // If the lhs is a field ref, evaluate it first.
      if (variableRef instanceof JFieldRef) {
        JFieldRef fieldRef = (JFieldRef) variableRef;
        JExpression instance = fieldRef.getInstance();
        if (instance != null) {
          multi.exprs.add(instance);
        }
      }

      // If there is an initializer, evaluate it second.
      if (rhs != null) {
        multi.exprs.add(rhs);
      }
      if (multi.exprs.size() == 1) {
        return multi.exprs.get(0);
      } else {
        return multi;
      }
    }
  }

  /**
   * Remove any unreferenced classes and interfaces from JProgram. Remove any
   * unreferenced methods and fields from their containing classes.
   */
  private class PruneVisitor extends JModVisitor {
    private boolean didChange = false;
    private final Map<JMethod, ArrayList<JParameter>> methodToOriginalParamsMap = new HashMap<JMethod, ArrayList<JParameter>>();
    private final Set<? extends JNode> referencedNonTypes;

    private final Set<? extends JReferenceType> referencedTypes;

    public PruneVisitor(Set<? extends JReferenceType> referencedTypes,
        Set<? extends JNode> referencedNodes) {
      this.referencedTypes = referencedTypes;
      this.referencedNonTypes = referencedNodes;
    }

    @Override
    public boolean didChange() {
      return didChange;
    }

    public Map<JMethod, ArrayList<JParameter>> getMethodToOriginalParamsMap() {
      return methodToOriginalParamsMap;
    }

    @Override
    public boolean visit(JClassType type, Context ctx) {

      assert (referencedTypes.contains(type));
      boolean isInstantiated = program.typeOracle.isInstantiatedType(type);

      for (Iterator<JField> it = type.fields.iterator(); it.hasNext();) {
        JField field = it.next();
        if (!referencedNonTypes.contains(field)
            || pruneViaNoninstantiability(isInstantiated, field)) {
          it.remove();
          didChange = true;
        }
      }

      for (Iterator<JMethod> it = type.methods.iterator(); it.hasNext();) {
        JMethod method = it.next();
        if (!methodIsReferenced(method)
            || pruneViaNoninstantiability(isInstantiated, method)) {
          it.remove();
          didChange = true;
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

      for (Iterator<JField> it = type.fields.iterator(); it.hasNext();) {
        JField field = it.next();
        // all interface fields are static and final
        if (!isReferenced || !referencedNonTypes.contains(field)) {
          it.remove();
          didChange = true;
        }
      }

      Iterator<JMethod> it = type.methods.iterator();
      if (it.hasNext()) {
        // start at index 1; never prune clinit directly out of the interface
        it.next();
      }
      while (it.hasNext()) {
        JMethod method = it.next();
        // all other interface methods are instance and abstract
        if (!isInstantiated || !methodIsReferenced(method)) {
          it.remove();
          didChange = true;
        }
      }

      return false;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      if (x.isStatic()) {
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
         * okay. Also, it's okay on the final pass since no more
         * devirtualizations will occur.
         */
        JMethod staticImplFor = program.staticImplFor(x);
        // Unless the instance method has already been pruned, of course.
        if (saveCodeGenTypes && staticImplFor != null
            && staticImplFor.getEnclosingType().methods.contains(staticImplFor)) {
          // instance method is still live
          return true;
        }

        JsFunction func = x.isNative()
            ? ((JsniMethodBody) x.getBody()).getFunc() : null;

        ArrayList<JParameter> originalParams = new ArrayList<JParameter>(
            x.params);

        for (int i = 0; i < x.params.size(); ++i) {
          JParameter param = x.params.get(i);
          if (!referencedNonTypes.contains(param)) {
            x.params.remove(i);
            didChange = true;
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
      for (Iterator<JLocal> it = x.locals.iterator(); it.hasNext();) {
        JLocal local = it.next();
        if (!referencedNonTypes.contains(local)) {
          it.remove();
          didChange = true;
        }
      }
      return false;
    }

    @Override
    public boolean visit(JProgram program, Context ctx) {
      for (JMethod method : program.getAllEntryMethods()) {
        accept(method);
      }
      for (Iterator<JReferenceType> it = program.getDeclaredTypes().iterator(); it.hasNext();) {
        JReferenceType type = it.next();
        if (referencedTypes.contains(type)
            || program.typeOracle.isInstantiatedType(type)) {
          accept(type);
        } else {
          it.remove();
          didChange = true;
        }
      }
      return false;
    }

    /**
     * Returns <code>true</code> if a method is referenced.
     */
    private boolean methodIsReferenced(JMethod method) {
      // Is the method directly referenced?
      if (referencedNonTypes.contains(method)) {
        return true;
      }

      /*
       * Special case: if method is the static impl for a live instance method,
       * don't prune it unless this is the final prune.
       * 
       * In some cases, the staticImpl can be inlined into the instance method
       * but still be needed at other call sites.
       */
      JMethod staticImplFor = program.staticImplFor(method);
      if (staticImplFor != null && referencedNonTypes.contains(staticImplFor)) {
        if (saveCodeGenTypes) {
          return true;
        }
      }
      return false;
    }

    private boolean pruneViaNoninstantiability(boolean isInstantiated,
        CanBeStatic it) {
      return (!isInstantiated && !it.isStatic());
    }
  }

  public static boolean exec(JProgram program, boolean noSpecialTypes) {
    return new Pruner(program, noSpecialTypes).execImpl();
  }

  private final JProgram program;
  private final boolean saveCodeGenTypes;

  private Pruner(JProgram program, boolean saveCodeGenTypes) {
    this.program = program;
    this.saveCodeGenTypes = saveCodeGenTypes;
  }

  private boolean execImpl() {
    boolean madeChanges = false;
    while (true) {
      ControlFlowAnalyzer livenessAnalyzer = new ControlFlowAnalyzer(program);
      if (saveCodeGenTypes) {
        /*
         * SPECIAL: Some classes contain methods used by code generation later.
         * Unless those transforms have already been performed, we must rescue
         * all contained methods for later user.
         */
        traverseFromCodeGenTypes(livenessAnalyzer);
      }
      for (JMethod method : program.getAllEntryMethods()) {
        livenessAnalyzer.traverseFrom(method);
      }
      livenessAnalyzer.traverseFromLeftoversFragmentHasLoaded();

      program.typeOracle.setInstantiatedTypes(livenessAnalyzer.getInstantiatedTypes());

      PruneVisitor pruner = new PruneVisitor(
          livenessAnalyzer.getReferencedTypes(),
          livenessAnalyzer.getLiveFieldsAndMethods());
      pruner.accept(program);
      if (!pruner.didChange()) {
        break;
      }

      CleanupRefsVisitor cleaner = new CleanupRefsVisitor(
          livenessAnalyzer.getLiveFieldsAndMethods(),
          pruner.getMethodToOriginalParamsMap());
      cleaner.accept(program.getDeclaredTypes());

      madeChanges = true;
    }
    return madeChanges;
  }

  /**
   * Traverse from all methods in the program's code-gen types. See
   * {@link JProgram#CODEGEN_TYPES_SET}.
   */
  private void traverseFromCodeGenTypes(ControlFlowAnalyzer livenessAnalyzer) {
    for (JReferenceType type : program.codeGenTypes) {
      livenessAnalyzer.traverseFromReferenceTo(type);
      for (int i = 0; i < type.methods.size(); ++i) {
        JMethod method = type.methods.get(i);
        livenessAnalyzer.traverseFrom(method);
      }
    }
  }

}
