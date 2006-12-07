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
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.change.ChangeList;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds all methods and classes are effectively final. That is, methods that
 * are never overriden and classes that are never subclassed. Mark all such
 * methods and classes as final, since it helps us optimize.
 */
public class MethodAndClassFinalizer {

  /**
   * Any method and classes that weren't marked during MarkVisitor can be set
   * final.
   * 
   * Open question: What does it mean if an interface/abstract method becomes
   * final? Is this possible after Pruning? I guess it means that someone tried
   * to make a call to method that wasn't actually implemented anywhere in the
   * program. But if it wasn't implemented, then the enclosing class should have
   * come up as not instantiated and been culled. So I think it's not possible.
   */
  private class FinalizeVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Finalize effectively final methods and types.");

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JClassType x) {
      if (!x.isFinal() && !isSubclassed.contains(x)) {
        changeList.makeFinal(x);
      }
      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        method.traverse(this);
      }
      return false;
    }

    // @Override
    public boolean visit(JInterfaceType x) {
      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        method.traverse(this);
      }
      return false;
    }

    // @Override
    public boolean visit(JMethod x) {
      if (!x.isFinal() && !isOverriden.contains(x)) {
        changeList.makeFinal(x);
      }
      return false;
    }

    // @Override
    public boolean visit(JsniMethod x) {
      return visit((JMethod) x);
    }
  }
  /**
   * Find all methods and classes that ARE overriden/subclassed.
   */
  private class MarkVisitor extends JVisitor {

    // @Override
    public boolean visit(JClassType x) {
      if (x.extnds != null) {
        isSubclassed.add(x.extnds);
      }

      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        method.traverse(this);
      }
      return false;
    }

    // @Override
    public boolean visit(JInterfaceType x) {
      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        method.traverse(this);
      }
      return false;
    }

    // @Override
    public boolean visit(JMethod x) {
      for (int i = 0; i < x.overrides.size(); ++i) {
        JMethod it = (JMethod) x.overrides.get(i);
        isOverriden.add(it);
      }
      return false;
    }

    // @Override
    public boolean visit(JProgram x) {
      for (int i = 0; i < x.getDeclaredTypes().size(); ++i) {
        JReferenceType type = (JReferenceType) x.getDeclaredTypes().get(i);
        type.traverse(this);
      }
      return false;
    }

    // @Override
    public boolean visit(JsniMethod x) {
      return visit((JMethod) x);
    }
  }

  public static boolean exec(JProgram program) {
    return new MethodAndClassFinalizer().execImpl(program);
  }

  private final Set/* <JClassType> */isSubclassed = new HashSet/* <JClassType> */();

  private final Set/* <JMethod> */isOverriden = new HashSet/* <JMethod> */();

  private MethodAndClassFinalizer() {
  }

  private boolean execImpl(JProgram program) {
    MarkVisitor marker = new MarkVisitor();
    program.traverse(marker);
    FinalizeVisitor finalizer = new FinalizeVisitor();
    program.traverse(finalizer);
    ChangeList changes = finalizer.getChangeList();
    if (changes.empty()) {
      return false;
    }
    changes.apply();
    return true;
  }

}
