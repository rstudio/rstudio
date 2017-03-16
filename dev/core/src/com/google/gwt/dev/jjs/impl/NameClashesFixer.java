/*
 * Copyright 2015 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The GWT Java AST might contain different local variables with the same name in the same
 * scope. This fixup pass renames variables in the case they clash in a scope.
 */
public class NameClashesFixer {

  private static class FixNameClashesVisitor extends JVisitor {

    /**
     * Represents the scope tree defined by nested statement blocks. It is a temporary structure to
     * track local variable lifetimes.
     */
    private static class Scope {
      private Scope parent;

      // Keeps track what names are used in children.
      private Set<String> usedInChildScope = Sets.newHashSet();

      // Keeps track what names have this scope as its lifetime.
      private Set<String> namesInThisScope = Sets.newHashSet();

      /**
       * The depth at which this scope is in the tree.
       */
      private int level;

      private Scope() {
        this.parent = null;
        this.level = 0;
      }

      private Scope(Scope parent) {
        this.parent = parent;
        this.level = parent.level + 1;
      }

      private static Scope getInnermostEnclosingScope(Scope thisScope, Scope thatScope) {
        if (thisScope == null) {
          return thatScope;
        }

        if (thatScope == null) {
          return thisScope;
        }

        if (thisScope == thatScope) {
          return thisScope;
        }

        if (thisScope.level > thatScope.level) {
          return getInnermostEnclosingScope(thatScope, thisScope);
        }

        if (thisScope.level == thatScope.level) {
          return getInnermostEnclosingScope(thisScope.parent, thatScope.parent);
        }
        return getInnermostEnclosingScope(thisScope, thatScope.parent);
      }

      private void addChildUsage(String name) {
        usedInChildScope.add(name);
        if (parent != null) {
          parent.addChildUsage(name);
        }
      }

      protected void addUsedName(String name) {
        namesInThisScope.add(name);
        if (parent != null) {
          parent.addChildUsage(name);
        }
      }

      private boolean isUsedInParent(String name) {
        return namesInThisScope.contains(name) ||
            (parent != null && parent.isUsedInParent(name));
      }

      protected boolean isConflictingName(String name) {
        return usedInChildScope.contains(name) || isUsedInParent(name);
      }
    }

    private Scope currentScope;
    private Map<JVariable, Scope> scopesByLocal;
    private Multimap<String, JVariable> localsByName;

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      // Start constructing the scope tree.
      currentScope = new Scope();
      scopesByLocal = Maps.newHashMap();
      localsByName = LinkedHashMultimap.create();
      return true;
    }

    @Override
    public boolean visit(JBlock x, Context ctx) {
      currentScope = new Scope(currentScope);
      return true;
    }

    @Override
    public void endVisit(JBlock x, Context ctx) {
      currentScope = currentScope.parent;
    }

    @Override
    public void endVisit(JVariableRef x, Context ctx) {
      // We use the a block scope as a proxy for a lifetime which is safe to do albeit non optimal.
      //
      // Keep track of the scope that encloses a variable lifetime. E.g. assume the following code.
      // { // scope 1
      //   { // scope 1.1
      //     ... a... b...
      //     { // scope 1.1.1
      //        ... a ...
      //     }
      //   }
      //   { // scope 1.2
      //    ... b...
      //   }
      // }
      // Scope 1.1 is the innermost scope that encloses the lifetime of variable a and
      // scope 1 is the innermost scope that encloses the lifetime of variable b.
      if (x instanceof JFieldRef) {
        // Skip fields as they are always qualified in JavaScript and their name resolution logic
        // is in {@link CreateNameAndScopesVisitor}.
        return;
      }
      JVariable local = x.getTarget();
      Scope oldVariableScope = scopesByLocal.get(local);
      Scope newVariableScope = Scope.getInnermostEnclosingScope(oldVariableScope, currentScope);
      newVariableScope.addUsedName(local.getName());
      if (newVariableScope != oldVariableScope) {
        scopesByLocal.put(local, newVariableScope);
      }
      localsByName.put(local.getName(), local);
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      // Fix clashing variables here.  Two locals are clashing if they have the same name and their
      // computed lifetimes are intersecting. By using the scope to model lifetimes two variables
      // clash if their computed scopes are nested.
      for (String name : localsByName.keySet()) {
        Collection<JVariable> localSet = localsByName.get(name);
        if (localSet.size() == 1) {
          continue;
        }

        JVariable[] locals = localSet.toArray(new JVariable[localSet.size()]);
        // TODO(rluble): remove n^2 behaviour in conflict checking.
        // In practice each method has only a handful of locals so this process is not expected
        // to be a performance problem.
        for (int i = 0; i < locals.length; i++) {
          // See if local i conflicts with any local j > i
          for (int j = i + 1; j < locals.length; j++) {
            Scope iLocalScope = scopesByLocal.get(locals[i]);
            Scope jLocalScope = scopesByLocal.get(locals[j]);
            Scope commonAncestor = Scope.getInnermostEnclosingScope(iLocalScope, jLocalScope);
            if (commonAncestor != iLocalScope && commonAncestor != jLocalScope) {
              // no conflict
              continue;
            }
            // conflicting locals => find a unique name rename local i to it;
            int n = 0;
            String baseName = locals[i].getName();
            String newName;
            do {
              // The active namer will clean up these potentially long names.
              newName = baseName + n++;
            } while (iLocalScope.isConflictingName(newName));
            locals[i].setName(newName);
            iLocalScope.addUsedName(newName);
            // There is no need to update the localsByNameMap as newNames are always guaranteed to
            // be clash free.
            break;
          }
        }
      }

      // Only valid for the duration of one method body visit/endVisit pair.
      currentScope = null;
      scopesByLocal = null;
      localsByName = null;
    }
  }

  public static void exec(JProgram program) {
    // Rename variables that should not be shadowed.
    new JModVisitor() {
      @Override
      public boolean visit(JMethod method, Context ctx) {
        return !method.isJsniMethod() && method.isJsMethodVarargs();
      }

      @Override
      public void endVisit(JLocal variable, Context ctx) {
        maybeRename(variable);
      }

      @Override
      public void endVisit(JParameter parameter, Context ctx) {
        maybeRename(parameter);
      }
    }.accept(program);

    // Resolve clashes.
    new FixNameClashesVisitor().accept(program);
  }

  private static Set<String> unshadowableNames = ImmutableSet.of("arguments");

  private static void maybeRename(JVariable variable) {
    // In normal scenarios local variables from our Java programs will shadow existing JavaScript
    // variables. The only exception is the implicit "arguments" variable which should not be
    // shadowed.
    // There is no need to care about collisions here as the JVariable object is what defines a
    // variable, not its name; FixNameClashesVisitor, run immediately after, will ensure that names
    // do not clash.
    if (unshadowableNames.contains(variable.getName())) {
      variable.setName("_" + variable.getName());
    }
  }

  private NameClashesFixer() {
  }
}
