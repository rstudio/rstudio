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

import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * This is an interesting "optimization". It's not really an optimization in and
 * of itself, but it opens the door to other optimizations. The basic idea is
 * that you look for calls to instance methods that are not actually
 * polymorphic. In other words, the target method is (effectively) final, not
 * overriden anywhere in the compilation. We rewrite the single instance method
 * as a static method that contains the implementation plus an instance method
 * that delegates to the static method. Then we update any call sites to call
 * the static method instead. This opens the door to further optimizations,
 * reduces use of the long "this" keyword in the resulting JavaScript, and in
 * most cases the polymorphic version can be pruned later.
 * 
 * TODO: make this work on JSNI methods!
 */
public class MakeCallsStatic {

  /**
   * For any instance methods that are called in a non-polymorphic manner, move
   * the contents of the method to a static method, and have the instance method
   * delegate to it. Sometimes the instance method can be pruned later since we
   * update all non-polymorphic call sites.
   */
  private class CreateStaticMethodVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Create static impls for instance methods");

    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      JMethod oldMethod = x.getTarget();

      // Must be instance and final
      if (x.canBePolymorphic()) {
        return;
      }
      if (oldMethod.isStatic()) {
        return;
      }
      if (oldMethod.isAbstract()) {
        return;
      }
      if (oldMethod.isNative()) {
        return;
      }
      if (oldMethod == program.getNullMethod()) {
        // Special case: we don't make calls to this method static.
        return;
      }

      // Did we already do this one?
      if (program.getStaticImpl(oldMethod) != null) {
        return;
      }

      JClassType enclosingType = (JClassType) oldMethod.getEnclosingType();
      JType oldReturnType = oldMethod.getType();

      // Create the new static method
      String newName = "$" + oldMethod.getName();

      /*
       * Don't use thie JProgram helper because it auto-adds the new method to
       * its enclosing class, which will break iteration.
       */
      JMethod newMethod = new JMethod(program, newName, enclosingType,
          oldReturnType, false, true, true, oldMethod.isPrivate());

      // Setup all params and locals; map from the old method to the new method
      JParameter thisParam = program.createParameter(
          "this$static".toCharArray(), enclosingType, true, newMethod);
      Map/* <JVariable, JVariable> */varMap = new IdentityHashMap();
      for (int i = 0; i < oldMethod.params.size(); ++i) {
        JParameter oldVar = (JParameter) oldMethod.params.get(i);
        JParameter newVar = program.createParameter(
            oldVar.getName().toCharArray(), oldVar.getType(), oldVar.isFinal(),
            newMethod);
        varMap.put(oldVar, newVar);
      }

      newMethod.freezeParamTypes();
      for (int i = 0; i < oldMethod.locals.size(); ++i) {
        JLocal oldVar = (JLocal) oldMethod.locals.get(i);
        JLocal newVar = program.createLocal(oldVar.getName().toCharArray(),
            oldVar.getType(), oldVar.isFinal(), newMethod);
        varMap.put(oldVar, newVar);
      }
      ChangeList myChangeList = new ChangeList("Create a new static method '"
          + newMethod + "' for instance method '" + oldMethod + "'");
      myChangeList.addMethod(newMethod);
      program.putStaticImpl(oldMethod, newMethod);

      // rewrite the method body to update all thisRefs to instance refs
      ChangeList subChangeList = new ChangeList(
          "Update thisrefs as paramrefs; update paramrefs and localrefs to target this method.");
      RewriteMethodBody rewriter = new RewriteMethodBody(thisParam, varMap,
          subChangeList);
      oldMethod.traverse(rewriter);
      myChangeList.add(subChangeList);

      // Move the body of the instance method to the static method
      myChangeList.clear(oldMethod.locals);
      myChangeList.moveBody(oldMethod, newMethod);

      // delegate from the instance method to the static method
      JMethodCall newCall = new JMethodCall(program, null, newMethod);
      newCall.args.add(program.getExpressionThisRef(enclosingType));
      for (int i = 0; i < oldMethod.params.size(); ++i) {
        JParameter param = (JParameter) oldMethod.params.get(i);
        newCall.args.add(new JParameterRef(program, param));
      }
      JStatement statement;
      if (oldReturnType == program.getTypeVoid()) {
        statement = new JExpressionStatement(program, newCall);
      } else {
        statement = new JReturnStatement(program, newCall);
      }
      myChangeList.addStatement(statement, oldMethod.body);
      changeList.add(myChangeList);
    }

    public ChangeList getChangeList() {
      return changeList;
    }
  }

  /**
   * For any method calls to methods we updated during
   * CreateStaticMethodVisitor, go and rewrite the call sites to call the static
   * method instead.
   */
  private class RewriteCallSites extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Rewrite calls to final instance methods as calls to static impl methods.");

    /*
     * In cases where callers are directly referencing (effectively) final
     * instance methods, rewrite the call site to reference the newly-generated
     * static method instead.
     */
    // @Override
    public void endVisit(JMethodCall x, Mutator m) {
      JMethod oldMethod = x.getTarget();
      JMethod newMethod = program.getStaticImpl(oldMethod);
      if (newMethod == null || x.canBePolymorphic()) {
        return;
      }

      ChangeList changes = new ChangeList("Replace '" + x
          + "' with a static call");

      // Update the call site
      JMethodCall newCall = new JMethodCall(program, null, newMethod);
      changes.replaceExpression(m, newCall);

      // The qualifier becomes the first arg
      changes.addExpression(x.instance, newCall.args);
      // Copy the rest of the args
      for (int i = 0; i < x.args.size(); ++i) {
        Mutator arg = x.args.getMutator(i);
        changes.addExpression(arg, newCall.args);
      }

      changeList.add(changes);
    }

    public ChangeList getChangeList() {
      return changeList;
    }
  }

  /**
   * When code is moved from an instance method to a static method, all this
   * refs must be replaced with param refs to the synthetic this param.
   */
  private class RewriteMethodBody extends JVisitor {

    private final ChangeList changeList;
    private final JParameter thisParam;
    private final Map/* <JVariable, JVariable> */varMap;

    public RewriteMethodBody(JParameter thisParam,
        Map/* <JVariable, JVariable> */varMap, ChangeList changeList) {
      this.changeList = changeList;
      this.thisParam = thisParam;
      this.varMap = varMap;
    }

    // @Override
    public void endVisit(JLocalRef x, Mutator m) {
      JLocal local = (JLocal) varMap.get(x.getTarget());
      JLocalRef localRef = new JLocalRef(program, local);
      changeList.replaceExpression(m, localRef);
    }

    // @Override
    public void endVisit(JParameterRef x, Mutator m) {
      JParameter param = (JParameter) varMap.get(x.getTarget());
      JParameterRef paramRef = new JParameterRef(program, param);
      changeList.replaceExpression(m, paramRef);
    }

    // @Override
    public void endVisit(JThisRef x, Mutator m) {
      JParameterRef paramRef = new JParameterRef(program, thisParam);
      changeList.replaceExpression(m, paramRef);
    }
  }

  public static boolean exec(JProgram program) {
    return new MakeCallsStatic(program).execImpl();
  }

  private final JProgram program;

  private MakeCallsStatic(JProgram program) {
    this.program = program;
  }

  private boolean execImpl() {
    {
      CreateStaticMethodVisitor creator = new CreateStaticMethodVisitor();
      program.traverse(creator);
      ChangeList changes = creator.getChangeList();
      if (changes.empty()) {
        return false;
      }
      changes.apply();
    }

    {
      RewriteCallSites rewriter = new RewriteCallSites();
      program.traverse(rewriter);
      ChangeList changes = rewriter.getChangeList();
      assert (!changes.empty());
      changes.apply();
    }

    return true;
  }

}
