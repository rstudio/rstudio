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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JVisitor;
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

    private boolean didChange = false;

    public boolean didChange() {
      return didChange;
    }

    // @Override
    public boolean visit(JClassType x, Context ctx) {
      if (!x.isFinal() && !isSubclassed.contains(x)) {
        x.setFinal(true);
        didChange = true;
      }
      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        accept(method);
      }
      return false;
    }

    // @Override
    public boolean visit(JInterfaceType x, Context ctx) {
      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        accept(method);
      }
      return false;
    }

    // @Override
    public boolean visit(JMethod x, Context ctx) {
      if (!x.isFinal() && !isOverriden.contains(x)) {
        x.setFinal(true);
        didChange = true;
      }
      return false;
    }

    // @Override
    public boolean visit(JsniMethod x, Context ctx) {
      return visit((JMethod) x, ctx);
    }
  }
  /**
   * Find all methods and classes that ARE overriden/subclassed.
   */
  private class MarkVisitor extends JVisitor {

    // @Override
    public boolean visit(JClassType x, Context ctx) {
      if (x.extnds != null) {
        isSubclassed.add(x.extnds);
      }

      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        accept(method);
      }
      return false;
    }

    // @Override
    public boolean visit(JInterfaceType x, Context ctx) {
      for (int i = 0; i < x.methods.size(); ++i) {
        JMethod method = (JMethod) x.methods.get(i);
        accept(method);
      }
      return false;
    }

    // @Override
    public boolean visit(JMethod x, Context ctx) {
      for (int i = 0; i < x.overrides.size(); ++i) {
        JMethod it = (JMethod) x.overrides.get(i);
        isOverriden.add(it);
      }
      return false;
    }

    // @Override
    public boolean visit(JProgram x, Context ctx) {
      for (int i = 0; i < x.getDeclaredTypes().size(); ++i) {
        JReferenceType type = (JReferenceType) x.getDeclaredTypes().get(i);
        accept(type);
      }
      return false;
    }

    // @Override
    public boolean visit(JsniMethod x, Context ctx) {
      return visit((JMethod) x, ctx);
    }
  }

  public static boolean exec(JProgram program) {
    return new MethodAndClassFinalizer().execImpl(program);
  }

  private final Set/* <JMethod> */isOverriden = new HashSet/* <JMethod> */();

  private final Set/* <JClassType> */isSubclassed = new HashSet/* <JClassType> */();

  private MethodAndClassFinalizer() {
  }

  private boolean execImpl(JProgram program) {
    MarkVisitor marker = new MarkVisitor();
    marker.accept(program);

    FinalizeVisitor finalizer = new FinalizeVisitor();
    finalizer.accept(program);
    return finalizer.didChange();
  }

}
