/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dev.jjs.ast.CanBeStatic;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      // The LHS of assignments may have been pruned.
      if (x.getOp() == JBinaryOperator.ASG) {
        JExpression lhs = x.getLhs();
        if (lhs.hasSideEffects()) {
          return;
        }
        if (lhs instanceof JFieldRef || lhs instanceof JLocalRef
            || lhs instanceof JParameterRef) {
          JVariable var = ((JVariableRef) lhs).getTarget();
          if (!referencedNonTypes.contains(var)) {
            // Just replace with my RHS
            ctx.replaceMe(x.getRhs());
          }
        }
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

    public void endVisit(JLocalDeclarationStatement x, Context ctx) {
      // The variable may have been pruned.
      if (!referencedNonTypes.contains(x.getLocalRef().getTarget())) {
        // If there is an initializer, just replace with that.
        if (x.getInitializer() != null) {
          ctx.replaceMe(x.getInitializer().makeStatement());
        } else {
          // No initializer, prune this entirely.
          if (ctx.canRemove()) {
            // Just remove it if we can.
            ctx.removeMe();
          } else {
            // Replace with empty block.
            ctx.replaceMe(new JBlock(program, x.getSourceInfo()));
          }
        }
      }
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      // Did we prune the parameters of the method we're calling?
      if (removedMethodParamsMap.containsKey(method)) {
        // This must be a static method
        assert method.isStatic();

        JMethodCall newCall = new JMethodCall(program, x.getSourceInfo(),
            x.getInstance(), method);

        JMultiExpression currentMulti = null;

        ArrayList<JParameter> removedParams = removedMethodParamsMap.get(method);

        for (int i = 0; i < x.getArgs().size(); i++) {
          JParameter param = removedParams.get(i);
          JExpression arg = x.getArgs().get(i);

          if (referencedNonTypes.contains(param)) {
            // We rescued this parameter

            // Do we need to add the multi we've been building?
            if (currentMulti == null) {
              newCall.getArgs().add(arg);
            } else {
              currentMulti.exprs.add(arg);
              newCall.getArgs().add(currentMulti);
              currentMulti = null;
            }
          } else if (arg.hasSideEffects()) {
            // We didn't rescue this parameter and it has side-effects. Add it
            // to a new multi
            if (currentMulti == null) {
              currentMulti = new JMultiExpression(program, x.getSourceInfo());
            }
            currentMulti.exprs.add(arg);
          }
        }

        // We have orphaned parameters - add them on the end, wrapped in the
        // multi (JS ignores extra parameters)
        if (currentMulti != null) {
          newCall.getArgs().add(currentMulti);
        }

        ctx.replaceMe(newCall);
      }
    }
  }

  /**
   * Remove any unreferenced classes and interfaces from JProgram. Remove any
   * unreferenced methods and fields from their containing classes.
   */
  private class PruneVisitor extends JVisitor {

    private boolean didChange = false;

    @Override
    public boolean didChange() {
      return didChange;
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

        ArrayList<JParameter> removedParams = new ArrayList<JParameter>(
            x.params);

        for (int i = 0; i < x.params.size(); ++i) {
          JParameter param = x.params.get(i);
          if (!referencedNonTypes.contains(param)) {
            x.params.remove(i);
            didChange = true;
            removedMethodParamsMap.put(x, removedParams);
            // Remove the associated JSNI parameter
            if (func != null) {
              func.getParameters().remove(i);
            }
            --i;
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

  /**
   * Marks as "referenced" any types, methods, and fields that are reachable.
   * Also marks as "instantiable" any the classes and interfaces that can
   * possibly be instantiated.
   * 
   * TODO(later): make RescueVisitor use less stack?
   */
  private class RescueVisitor extends JVisitor {

    private final Set<JReferenceType> instantiatedTypes = new HashSet<JReferenceType>();

    public void commitInstantiatedTypes() {
      program.typeOracle.setInstantiatedTypes(instantiatedTypes);
    }

    @Override
    public boolean visit(JArrayType type, Context ctx) {
      assert (referencedTypes.contains(type));
      boolean isInstantiated = instantiatedTypes.contains(type);

      JType leafType = type.getLeafType();
      int dims = type.getDims();

      // Rescue my super array type
      if (leafType instanceof JReferenceType) {
        JReferenceType rLeafType = (JReferenceType) leafType;
        if (rLeafType.extnds != null) {
          JArrayType superArray = program.getTypeArray(rLeafType.extnds, dims);
          rescue(superArray, true, isInstantiated);
        }

        for (int i = 0; i < rLeafType.implments.size(); ++i) {
          JInterfaceType intfType = rLeafType.implments.get(i);
          JArrayType intfArray = program.getTypeArray(intfType, dims);
          rescue(intfArray, true, isInstantiated);
        }
      }

      return false;
    }

    @Override
    public boolean visit(JBinaryOperation x, Context ctx) {
      // special string concat handling
      if (x.getOp() == JBinaryOperator.ADD
          && x.getType() == program.getTypeJavaLangString()) {
        rescueByConcat(x.getLhs().getType());
        rescueByConcat(x.getRhs().getType());
      } else if (x.getOp() == JBinaryOperator.ASG) {
        // Don't rescue variables that are merely assigned to and never read
        boolean doSkip = false;
        JExpression lhs = x.getLhs();
        if (lhs.hasSideEffects()) {
          // cannot skip
        } else if (lhs instanceof JLocalRef) {
          // locals are ok to skip
          doSkip = true;
        } else if (lhs instanceof JParameterRef) {
          // parameters are ok to skip
          doSkip = true;
        } else if (lhs instanceof JFieldRef) {
          JFieldRef fieldRef = (JFieldRef) lhs;
          /*
           * Whether we can skip depends on what the qualifier is; we have to be
           * certain the qualifier cannot be a null pointer (in which case we'd
           * fail to throw the appropriate exception.
           * 
           * TODO: better non-null tracking!
           */
          JExpression instance = fieldRef.getInstance();
          if (fieldRef.getField().isStatic()) {
            // statics are always okay
            doSkip = true;
          } else if (instance instanceof JThisRef) {
            // a this ref cannot be null
            doSkip = true;
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
    public boolean visit(JClassType type, Context ctx) {
      assert (referencedTypes.contains(type));
      boolean isInstantiated = instantiatedTypes.contains(type);

      /*
       * SPECIAL: Some classes contain methods used by code generation later.
       * Unless those transforms have already been performed, we must rescue all
       * contained methods for later user.
       */
      if (saveCodeGenTypes && program.codeGenTypes.contains(type)) {
        for (int i = 0; i < type.methods.size(); ++i) {
          JMethod it = type.methods.get(i);
          rescue(it);
        }
      }

      // Rescue my super type
      rescue(type.extnds, true, isInstantiated);

      // Rescue my clinit (it won't ever be explicitly referenced
      rescue(type.methods.get(0));

      // JLS 12.4.1: don't rescue my super interfaces just because I'm rescued.
      // However, if I'm instantiated, let's mark them as instantiated.
      for (int i = 0; i < type.implments.size(); ++i) {
        JInterfaceType intfType = type.implments.get(i);
        rescue(intfType, false, isInstantiated);
      }

      return false;
    }

    @Override
    public boolean visit(JFieldRef ref, Context ctx) {
      JField target = ref.getField();

      // JLS 12.4.1: references to static, non-final, or
      // non-compile-time-constant fields rescue the enclosing class.
      // JDT already folds in compile-time constants as literals, so we must
      // rescue the enclosing types for any static fields that make it here.
      if (target.isStatic()) {
        rescue(target.getEnclosingType(), true, false);
      }
      rescue(target);
      return true;
    }

    @Override
    public boolean visit(JInterfaceType type, Context ctx) {
      boolean isReferenced = referencedTypes.contains(type);
      boolean isInstantiated = instantiatedTypes.contains(type);
      assert (isReferenced || isInstantiated);

      // Rescue my clinit (it won't ever be explicitly referenced
      rescue(type.methods.get(0));

      // JLS 12.4.1: don't rescue my super interfaces just because I'm rescued.
      // However, if I'm instantiated, let's mark them as instantiated.
      if (isInstantiated) {
        for (int i = 0; i < type.implments.size(); ++i) {
          JInterfaceType intfType = type.implments.get(i);
          rescue(intfType, false, true);
        }
      }

      // visit any field initializers
      for (int i = 0; i < type.fields.size(); ++i) {
        JField it = type.fields.get(i);
        accept(it);
      }

      return false;
    }

    public boolean visit(JLocalDeclarationStatement x, Context ctx) {
      /*
       * A declaration by itself doesn't rescue a local (even if it has an
       * initializer). Writes don't count, only reads.
       */
      if (x.getInitializer() != null) {
        accept(x.getInitializer());
      }
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
      if (x.isNative()) {
        // Manually rescue native parameter references
        final JsniMethodBody body = (JsniMethodBody) x.getBody();
        final JsFunction func = body.getFunc();

        new JsVisitor() {
          @Override
          public void endVisit(JsNameRef nameRef, JsContext<JsExpression> ctx) {
            JsName ident = nameRef.getName();

            if (ident != null) {
              // If we're referencing a parameter, rescue the associated
              // JParameter
              int index = func.getParameters().indexOf(ident.getStaticRef());
              if (index != -1) {
                rescue(x.params.get(index));
              }
            }
          }
        }.accept(func);
      }

      return true;
    }

    @Override
    public boolean visit(JMethodCall call, Context ctx) {
      JMethod target = call.getTarget();
      // JLS 12.4.1: references to static methods rescue the enclosing class
      if (target.isStatic()) {
        rescue(target.getEnclosingType(), true, false);
      }
      rescue(target);
      return true;
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
    public boolean visit(JNewInstance newInstance, Context ctx) {
      // rescue and instantiate the target class!
      rescue(newInstance.getClassType(), true, true);
      return true;
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
       * 
       * TODO(later): technically we only need to do this if the field is being
       * assigned to.
       */
      maybeRescueJavaScriptObjectPassingIntoJava(x.getField().getType());
      // JsniFieldRef rescues as JFieldRef
      return visit((JFieldRef) x, ctx);
    }

    @Override
    public boolean visit(JsniMethodRef x, Context ctx) {
      /*
       * SPECIAL: each argument of the call passes a value from JavaScript into
       * Java.
       */
      ArrayList<JParameter> params = x.getTarget().params;
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
      // JsniMethodRef rescues as JMethodCall
      return visit((JMethodCall) x, ctx);
    }

    @Override
    public boolean visit(JStringLiteral literal, Context ctx) {
      // rescue and instantiate java.lang.String
      rescue(program.getTypeJavaLangString(), true, true);
      return true;
    }

    /**
     * Subclasses of JavaScriptObject are never instantiated directly. They are
     * created "magically" when a JSNI method passes a reference to an existing
     * JS object into Java code. The point at which a subclass of JSO is passed
     * into Java code constitutes "instantiation". We must identify these points
     * and trigger a rescue and instantiation of that particular JSO subclass.
     * 
     * @param type The type of the value passing from Java to JavaScript.
     * @see com.google.gwt.core.client.JavaScriptObject
     */
    private void maybeRescueJavaScriptObjectPassingIntoJava(JType type) {
      if (type instanceof JReferenceType) {
        JReferenceType refType = (JReferenceType) type;
        if (program.typeOracle.canTriviallyCast(refType,
            program.getJavaScriptObject())) {
          rescue(refType, true, true);
        }
      }
    }

    private boolean rescue(JMethod method) {
      if (method != null) {
        if (!referencedNonTypes.contains(method)) {
          referencedNonTypes.add(method);
          accept(method);
          if (method.isNative()) {
            /*
             * SPECIAL: returning from this method passes a value from
             * JavaScript into Java.
             */
            maybeRescueJavaScriptObjectPassingIntoJava(method.getType());
          }
          return true;
        }
      }
      return false;
    }

    private void rescue(JReferenceType type, boolean isReferenced,
        boolean isInstantiated) {
      if (type != null) {

        boolean doVisit = false;
        if (isInstantiated && !instantiatedTypes.contains(type)) {
          instantiatedTypes.add(type);
          doVisit = true;
        }

        if (isReferenced && !referencedTypes.contains(type)) {
          referencedTypes.add(type);
          doVisit = true;
        }

        if (doVisit) {
          accept(type);
        }
      }
    }

    private void rescue(JVariable var) {
      if (var != null) {
        if (!referencedNonTypes.contains(var)) {
          referencedNonTypes.add(var);
        }
      }
    }

    /**
     * Handle special rescues needed implicitly to support concat.
     */
    private void rescueByConcat(JType type) {
      JClassType stringType = program.getTypeJavaLangString();
      JPrimitiveType charType = program.getTypePrimitiveChar();
      if (type instanceof JReferenceType && type != stringType) {
        /*
         * Any reference types (except String, which works by default) that take
         * part in a concat must rescue java.lang.Object.toString().
         * 
         * TODO: can we narrow the focus by walking up the type heirarchy or
         * doing explicit toString calls?
         */
        JMethod toStringMethod = program.getIndexedMethod("Object.toString");
        rescue(toStringMethod);
      } else if (type == charType) {
        /*
         * Characters must rescue String.valueOf(char)
         */
        if (stringValueOfChar == null) {
          for (int i = 0; i < stringType.methods.size(); ++i) {
            JMethod meth = stringType.methods.get(i);
            if (meth.getName().equals("valueOf")) {
              List<JType> params = meth.getOriginalParamTypes();
              if (params.size() == 1) {
                if (params.get(0) == charType) {
                  stringValueOfChar = meth;
                  break;
                }
              }
            }
          }
          assert (stringValueOfChar != null);
        }
        rescue(stringValueOfChar);
      }
    }
  }

  /**
   * A method that isn't called directly can still be needed, if it overrides or
   * implements any methods that are called.
   */
  private class UpRefVisitor extends JVisitor {

    private boolean didRescue = false;
    private final RescueVisitor rescuer;

    public UpRefVisitor(RescueVisitor rescuer) {
      this.rescuer = rescuer;
    }

    public boolean didRescue() {
      return didRescue;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      if (referencedNonTypes.contains(x)) {
        return false;
      }

      for (int i = 0; i < x.overrides.size(); ++i) {
        JMethod ref = x.overrides.get(i);
        if (referencedNonTypes.contains(ref)) {
          rescuer.rescue(x);
          didRescue = true;
          return false;
        }
      }
      JMethod[] virtualOverrides = program.typeOracle.getAllVirtualOverrides(x);
      for (int i = 0; i < virtualOverrides.length; ++i) {
        JMethod ref = virtualOverrides[i];
        if (referencedNonTypes.contains(ref)) {
          rescuer.rescue(x);
          didRescue = true;
          return false;
        }
      }
      return false;
    }

    @Override
    public boolean visit(JProgram x, Context ctx) {
      didRescue = false;
      return true;
    }
  }

  public static boolean exec(JProgram program, boolean noSpecialTypes) {
    return new Pruner(program, noSpecialTypes).execImpl();
  }

  private final JProgram program;
  private final Set<JNode> referencedNonTypes = new HashSet<JNode>();
  private final Set<JReferenceType> referencedTypes = new HashSet<JReferenceType>();
  private final Map<JMethod, ArrayList<JParameter>> removedMethodParamsMap = new HashMap<JMethod, ArrayList<JParameter>>();
  private final boolean saveCodeGenTypes;
  private JMethod stringValueOfChar = null;

  private Pruner(JProgram program, boolean saveCodeGenTypes) {
    this.program = program;
    this.saveCodeGenTypes = saveCodeGenTypes;
  }

  private boolean execImpl() {
    boolean madeChanges = false;
    while (true) {
      RescueVisitor rescuer = new RescueVisitor();
      // Always rescue and instantiate the "base" array type
      rescuer.rescue(program.getIndexedType("Array"), true, true);
      for (JReferenceType type : program.codeGenTypes) {
        rescuer.rescue(type, true, saveCodeGenTypes);
      }
      for (JMethod method : program.entryMethods) {
        rescuer.rescue(method);
      }

      UpRefVisitor upRefer = new UpRefVisitor(rescuer);
      do {
        rescuer.commitInstantiatedTypes();
        upRefer.accept(program);
      } while (upRefer.didRescue());

      PruneVisitor pruner = new PruneVisitor();
      pruner.accept(program);
      if (!pruner.didChange()) {
        break;
      }

      CleanupRefsVisitor cleaner = new CleanupRefsVisitor();
      cleaner.accept(program);

      referencedTypes.clear();
      referencedNonTypes.clear();
      removedMethodParamsMap.clear();
      madeChanges = true;
    }
    return madeChanges;
  }

}
