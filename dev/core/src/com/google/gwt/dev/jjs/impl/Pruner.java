/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Remove globally unreferenced classes, interfaces, methods, and fields from
 * the AST. This algorithm is based on having known "entry points" into the
 * application which serve as the root(s) from which reachability is determined
 * and everything else is rescued. Pruner determines reachability at a global
 * level based on method calls and new operations; it does not perform any local
 * code flow analysis. But, a local code flow optimization pass that can
 * eliminate method calls would allow Pruner to prune additional nodes.
 * 
 * Note: references to pruned types may still exist in the tree after this pass
 * runs, however, it should only be in contexts that do not rely on any code
 * generation for the pruned type. For example, it's legal to have a varable of
 * a pruned type, or to try to cast to a pruned type. These will cause natural
 * failures at run time; or later optimizations might be able to hard-code
 * failures at compile time.
 * 
 * TODO(later): prune params and locals
 */
public class Pruner {

  /**
   * Remove any unreferenced classes and interfaces from JProgram. Remove any
   * unreferenced methods and fields from their containing classes.
   */
  private class PruneVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Prune unreferenced methods, fields, and types.");

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JClassType type) {

      assert (referencedTypes.contains(type));
      boolean isInstantiated = program.typeOracle.isInstantiatedType(type);

      for (int i = 0; i < type.fields.size(); ++i) {
        JField it = (JField) type.fields.get(i);
        if (!referencedNonTypes.contains(it)
            || pruneViaNoninstantiability(isInstantiated, it)) {
          changeList.removeField(it);
        }
      }
      for (int i = 0; i < type.methods.size(); ++i) {
        JMethod it = (JMethod) type.methods.get(i);
        if (!referencedNonTypes.contains(it)
            || pruneViaNoninstantiability(isInstantiated, it)) {
          changeList.removeMethod(it);
        }
      }

      return false;
    }

    // @Override
    public boolean visit(JInterfaceType type) {
      boolean isReferenced = referencedTypes.contains(type);
      boolean isInstantiated = program.typeOracle.isInstantiatedType(type);

      for (int i = 0; i < type.fields.size(); ++i) {
        JField it = (JField) type.fields.get(i);
        // all interface fields are static and final
        if (!isReferenced || !referencedNonTypes.contains(it)) {
          changeList.removeField(it);
        }
      }
      // start at index 1; never prune clinit directly out of the interface
      for (int i = 1; i < type.methods.size(); ++i) {
        JMethod it = (JMethod) type.methods.get(i);
        // all other interface methods are instance and abstract
        if (!isInstantiated || !referencedNonTypes.contains(it)) {
          changeList.removeMethod(it);
        }
      }

      return false;
    }

    // @Override
    public boolean visit(JProgram program) {
      for (int i = 0; i < program.getDeclaredTypes().size(); ++i) {
        JReferenceType type = (JReferenceType) program.getDeclaredTypes().get(i);
        if (referencedTypes.contains(type)
            || program.typeOracle.isInstantiatedType(type)) {
          type.traverse(this);
        } else {
          changeList.removeType(type);
        }
      }
      return false;
    }

    private boolean pruneViaNoninstantiability(boolean isInstantiated, JField it) {
      return (!isInstantiated && !it.isStatic());
    }

    private boolean pruneViaNoninstantiability(boolean isInstantiated,
        JMethod it) {
      return (!isInstantiated && (!it.isStatic() || program.isStaticImpl(it)));
    }
  }
  /**
   * Marks as "referenced" any types, methods, and fields that are reachable.
   * Also marks as "instantiable" any the classes and interfaces that can
   * possibly be instantiated.
   */
  private class RescueVisitor extends JVisitor {

    private final Set/* <JReferenceType> */instantiatedTypes = new HashSet/* <JReferenceType> */();

    public void commitInstantiatedTypes() {
      program.typeOracle.setInstantiatedTypes(instantiatedTypes);
    }

    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      // special string concat handling
      if (x.op == JBinaryOperator.ADD
          && x.getType() == program.getTypeJavaLangString()) {
        rescueByConcat(x.getLhs().getType());
        rescueByConcat(x.getRhs().getType());
      }
    }

    // @Override
    public boolean visit(JArrayType type) {
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
          JInterfaceType intfType = (JInterfaceType) rLeafType.implments.get(i);
          JArrayType intfArray = program.getTypeArray(intfType, dims);
          rescue(intfArray, true, isInstantiated);
        }
      }

      return false;
    }

    // @Override
    public boolean visit(JClassLiteral literal, Mutator mutator) {
      // rescue and instantiate java.lang.Class
      // JLS 12.4.1: do not rescue the target type
      rescue(program.getTypeJavaLangClass(), true, true);
      return true;
    }

    // @Override
    public boolean visit(JClassType type) {
      assert (referencedTypes.contains(type));
      boolean isInstantiated = instantiatedTypes.contains(type);

      /*
       * SPECIAL: Some classes contain methods used by code generation later.
       * Unless those transforms have already been performed, we must rescuse
       * all contained methods for later user.
       */
      if (noSpecialTypes && program.specialTypes.contains(type)) {
        for (int i = 0; i < type.methods.size(); ++i) {
          JMethod it = (JMethod) type.methods.get(i);
          rescue(it);
        }
      }

      // Rescue my super type
      rescue(type.extnds, true, isInstantiated);

      // Rescue my clinit (it won't ever be explicitly referenced
      rescue((JMethod) type.methods.get(0));

      // JLS 12.4.1: don't rescue my super interfaces just because I'm rescued.
      // However, if I'm instantiated, let's mark them as instantiated.
      for (int i = 0; i < type.implments.size(); ++i) {
        JInterfaceType intfType = (JInterfaceType) type.implments.get(i);
        rescue(intfType, false, isInstantiated);
      }

      return false;
    }

    // @Override
    public boolean visit(JFieldRef ref, Mutator mutator) {
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

    // @Override
    public boolean visit(JInterfaceType type) {
      boolean isReferenced = referencedTypes.contains(type);
      boolean isInstantiated = instantiatedTypes.contains(type);
      assert (isReferenced || isInstantiated);

      // Rescue my clinit (it won't ever be explicitly referenced
      rescue((JMethod) type.methods.get(0));

      // JLS 12.4.1: don't rescue my super interfaces just because I'm rescued.
      // However, if I'm instantiated, let's mark them as instantiated.
      if (isInstantiated) {
        for (int i = 0; i < type.implments.size(); ++i) {
          JInterfaceType intfType = (JInterfaceType) type.implments.get(i);
          rescue(intfType, false, true);
        }
      }

      // visit any field initializers
      for (int i = 0; i < type.fields.size(); ++i) {
        JField it = (JField) type.fields.get(i);
        it.traverse(this);
      }

      return false;
    }

    // @Override
    public boolean visit(JMethodCall call, Mutator mutator) {
      JMethod target = call.getTarget();
      // JLS 12.4.1: references to static methods rescue the enclosing class
      if (target.isStatic()) {
        rescue(target.getEnclosingType(), true, false);
      }
      rescue(target);
      return true;
    }

    // @Override
    public boolean visit(JNewArray newArray, Mutator mutator) {
      // rescue and instantiate the array type
      JArrayType arrayType = newArray.getArrayType();
      if (newArray.dims != null) {
        // rescue my type and all the implicitly nested types (with fewer dims)
        int nDims = arrayType.dims;
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

      // also rescue and instantiate the "base" array type
      rescue(program.getSpecialArray(), true, true);
      return true;
    }

    // @Override
    public boolean visit(JNewInstance newInstance, Mutator mutator) {
      // rescue and instantiate the target class!
      rescue(newInstance.getClassType(), true, true);
      return true;
    }

    // @Override
    public boolean visit(JsniFieldRef x) {
      /*
       * SPECIAL: this could be an assignment that passes a value from
       * JavaScript into Java.
       * 
       * TODO: technically we only need to do this if the field is being
       * assigned to.
       */
      maybeRescueJavaScriptObjectPassingIntoJava(x.getField().getType());
      // JsniFieldRef rescues as JFieldRef
      return visit(/* (JFieldRef) */x, null);
    }

    // @Override
    public boolean visit(JsniMethodRef x) {
      /*
       * SPECIAL: each argument of the call passes a value from JavaScript into
       * Java.
       */
      ArrayList params = x.getTarget().params;
      for (int i = 0, c = params.size(); i < c; ++i) {
        JParameter param = (JParameter) params.get(i);
        maybeRescueJavaScriptObjectPassingIntoJava(param.getType());
      }
      // JsniMethodRef rescues as JMethodCall
      return visit(/* (JMethodCall) */x, null);
    }

    // @Override
    public boolean visit(JStringLiteral literal, Mutator mutator) {
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
            program.getSpecialJavaScriptObject())) {
          rescue(refType, true, true);
        }
      }
    }

    private boolean rescue(JField field) {
      if (field == null) {
        return false;
      }

      if (!referencedNonTypes.contains(field)) {
        referencedNonTypes.add(field);
        return true;
      }
      return true;
    }

    private boolean rescue(JMethod method) {
      if (method == null) {
        return false;
      }

      if (!referencedNonTypes.contains(method)) {
        referencedNonTypes.add(method);
        method.traverse(this);
        if (method instanceof JsniMethod) {
          /*
           * SPECIAL: returning from this method passes a value from JavaScript
           * into Java.
           */
          maybeRescueJavaScriptObjectPassingIntoJava(method.getType());
        }
        return true;
      }
      return false;
    }

    private boolean rescue(JReferenceType type, boolean isReferenced,
        boolean isInstantiated) {
      if (type == null) {
        return false;
      }

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
        type.traverse(this);
      }

      return doVisit;
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
         */
        JMethod toStringMethod = program.getSpecialMethod("Object.toString");
        rescue(toStringMethod);
      } else if (type == charType) {
        /*
         * Characters must rescue String.valueOf(char)
         */
        if (stringValueOfChar == null) {
          for (int i = 0; i < stringType.methods.size(); ++i) {
            JMethod meth = (JMethod) stringType.methods.get(i);
            if (meth.getName().equals("valueOf")) {
              List params = meth.getOriginalParamTypes();
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

    private final RescueVisitor rescuer;
    private boolean didRescue = false;

    public UpRefVisitor(RescueVisitor rescuer) {
      this.rescuer = rescuer;
    }

    public boolean didRescue() {
      return didRescue;
    }

    // @Override
    public boolean visit(JMethod x) {
      if (referencedNonTypes.contains(x)) {
        return false;
      }

      for (int i = 0; i < x.overrides.size(); ++i) {
        JMethod ref = (JMethod) x.overrides.get(i);
        if (referencedNonTypes.contains(ref)) {
          didRescue |= rescuer.rescue(x);
        }
      }
      JMethod[] virtualOverrides = program.typeOracle.getAllVirtualOverrides(x);
      for (int i = 0; i < virtualOverrides.length; ++i) {
        JMethod ref = virtualOverrides[i];
        if (referencedNonTypes.contains(ref)) {
          didRescue |= rescuer.rescue(x);
        }
      }
      return false;
    }

    // @Override
    public boolean visit(JProgram x) {
      didRescue = false;
      return true;
    }

    // @Override
    public boolean visit(JsniMethod x) {
      return visit((JMethod) x);
    }
  }

  public static boolean exec(JProgram program, boolean noSpecialTypes) {
    return new Pruner(program, noSpecialTypes).execImpl();
  }

  private final JProgram program;

  private final boolean noSpecialTypes;

  private final Set/* <JReferenceType> */referencedTypes = new HashSet/* <JReferenceType> */();

  private final Set/* <JNode> */referencedNonTypes = new HashSet/* <JNode> */();

  private JMethod stringValueOfChar = null;

  private Pruner(JProgram program, boolean noSpecialTypes) {
    this.program = program;
    this.noSpecialTypes = noSpecialTypes;
  }

  private boolean execImpl() {
    boolean madeChanges = false;
    while (true) {
      RescueVisitor rescuer = new RescueVisitor();

      for (int i = 0; i < program.specialTypes.size(); ++i) {
        JReferenceType type = (JReferenceType) program.specialTypes.get(i);
        rescuer.rescue(type, true, noSpecialTypes);
      }

      for (int i = 0; i < program.entryMethods.size(); ++i) {
        JMethod method = (JMethod) program.entryMethods.get(i);
        rescuer.rescue(method);
      }

      UpRefVisitor upRefer = new UpRefVisitor(rescuer);
      do {
        rescuer.commitInstantiatedTypes();
        program.traverse(upRefer);
      } while (upRefer.didRescue());

      PruneVisitor pruner = new PruneVisitor();
      program.traverse(pruner);

      ChangeList changes = pruner.getChangeList();
      if (changes.empty()) {
        break;
      }
      changes.apply();

      referencedTypes.clear();
      referencedNonTypes.clear();
      madeChanges = true;
    }
    return madeChanges;
  }

}
