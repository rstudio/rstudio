/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Rewrite JavaScript to better handle references from one code fragment to
 * another. For any function defined off the initial download and accessed from
 * a different island than the one it's defined on, predefine a variable in the
 * initial download to hold its definition.
 */
public class HandleCrossFragmentReferences {
  /**
   * Find out which islands define and use each named function or variable. This
   * visitor is not smart about which definitions and uses matter. It blindly
   * records all of them.
   */
  private class FindNameReferences extends JsVisitor {
    Map<JsName, Set<Integer>> islandsDefining = new LinkedHashMap<JsName, Set<Integer>>();
    Map<JsName, Set<Integer>> islandsUsing = new LinkedHashMap<JsName, Set<Integer>>();
    private int currentIsland;

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      JsName name = x.getName();
      if (name != null) {
        definitionSeen(name);
      }
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (x.getQualifier() == null) {
        JsName name = x.getName();
        if (name != null) {
          referenceSeen(name);
        }
      }
    }

    @Override
    public void endVisit(JsVars x, JsContext ctx) {
      for (JsVar var : x) {
        JsName name = var.getName();
        if (name != null) {
          definitionSeen(name);
        }
      }
    }

    @Override
    public boolean visit(JsProgram x, JsContext ctx) {
      for (int i = 0; i < x.getFragmentCount(); i++) {
        currentIsland = i;
        accept(x.getFragmentBlock(i));
      }

      return false;
    }

    private void definitionSeen(JsName name) {
      /*
       * Support multiple definitions, because local variables can reuse the
       * same name.
       */
      Set<Integer> defs = islandsDefining.get(name);
      if (defs == null) {
        defs = new LinkedHashSet<Integer>();
        islandsDefining.put(name, defs);
      }
      defs.add(currentIsland);
    }

    private void referenceSeen(JsName name) {
      Set<Integer> refs = islandsUsing.get(name);
      if (refs == null) {
        refs = new HashSet<Integer>();
        islandsUsing.put(name, refs);
      }
      refs.add(currentIsland);
    }
  }

  /**
   * Rewrite var and function declarations as assignments, if their name is
   * accessed cross-island. Rewrite refs to such names correspondingly.
   */
  private class RewriteDeclsAndRefs extends JsModVisitor {
    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      if (namesToPredefine.contains(x.getName())) {
        JsBinaryOperation asg =
            new JsBinaryOperation(x.getSourceInfo(), JsBinaryOperator.ASG, makeRefViaJslink(x
                .getName(), x.getSourceInfo()), x);
        x.setName(null);
        ctx.replaceMe(asg);
      }
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (namesToPredefine.contains(x.getName())) {
        ctx.replaceMe(makeRefViaJslink(x.getName(), x.getSourceInfo()));
      }
    }

    @Override
    public void endVisit(JsVars x, JsContext ctx) {
      if (!ctx.canInsert()) {
        return;
      }

      /*
       * Loop through each var and see if it was predefined. If so, then remove
       * the var. If the var has an initializer, then add back an assignment
       * statement to initialize it. If there is no initializer, then don't add
       * anything back; the var will still have undefined as its initial value,
       * just like before.
       *
       * A complication is that the variables that are predefined might be
       * interspersed with variables that are not. That means the general result
       * of this transformation has alternating var lists and assignment
       * statements. The currentVar variable holds the most recently inserted
       * statement, if that statement was a JsVars; otherwise it holds null.
       */

      JsVars currentVar = null;
      Iterator<JsVar> varsIterator = x.iterator();
      while (varsIterator.hasNext()) {
        JsVar var = varsIterator.next();
        if (namesToPredefine.contains(var.getName())) {
          // The var was predefined
          if (var.getInitExpr() != null) {
            // If it has an initializer, add an assignment statement
            JsBinaryOperation asg =
                new JsBinaryOperation(var.getSourceInfo(), JsBinaryOperator.ASG, makeRefViaJslink(
                    var.getName(), var.getSourceInfo()), var.getInitExpr());
            ctx.insertBefore(asg.makeStmt());
            currentVar = null;
          }
        } else {
          // The var was not predefined; add it to a var list
          if (currentVar == null) {
            currentVar = new JsVars(x.getSourceInfo());
            ctx.insertBefore(currentVar);
          }
          currentVar.add(var);
        }
      }

      ctx.removeMe();
    }

    private JsNameRef makeRefViaJslink(JsName name, SourceInfo sourceInfo) {
      JsNameRef ref = name.makeRef(sourceInfo);
      ref.setQualifier(jslink.makeRef(sourceInfo));
      return ref;
    }
  }

  public static String PROP_PREDECLARE_VARS = "compiler.predeclare.cross.fragment.references";

  public static void exec(TreeLogger logger, JsProgram jsProgram, PropertyOracle[] propertyOracles) {
    new HandleCrossFragmentReferences(logger, jsProgram, propertyOracles).execImpl();
  }

  private static boolean containsOtherThan(Set<Integer> set, int allowed) {
    for (int elem : set) {
      if (elem != allowed) {
        return true;
      }
    }
    return false;
  }

  private JsName jslink;
  private final JsProgram jsProgram;
  private final TreeLogger logger;
  private final Set<JsName> namesToPredefine = new LinkedHashSet<JsName>();
  private final PropertyOracle[] propertyOracles;

  private HandleCrossFragmentReferences(TreeLogger logger, JsProgram jsProgram,
      PropertyOracle[] propertyOracles) {
    this.logger = logger;
    this.jsProgram = jsProgram;
    this.propertyOracles = propertyOracles;
  }

  private void chooseNamesToPredefine(Map<JsName, Set<Integer>> map,
      Map<JsName, Set<Integer>> islandsUsing) {
    for (Entry<JsName, Set<Integer>> entry : map.entrySet()) {
      JsName name = entry.getKey();
      Set<Integer> defIslands = entry.getValue();
      if (defIslands.size() != 1) {
        // Only rewrite global variables, which should have exactly one
        // definition
        continue;
      }
      int defIsland = defIslands.iterator().next();
      if (defIsland == 0) {
        // Variables defined on the base island can be accessed directly from
        // other islands
        continue;
      }
      Set<Integer> useIslands = islandsUsing.get(name);
      if (useIslands == null) {
        // The variable is never used. Leave it alone.
        continue;
      }

      if (containsOtherThan(islandsUsing.get(name), defIsland)) {
        namesToPredefine.add(name);
      }
    }
  }

  /**
   * Define the jslink object that will be used to fix up cross-island
   * references.
   */
  private void defineJsLink() {
    SourceInfo info = jsProgram.createSourceInfoSynthetic(HandleCrossFragmentReferences.class);
    jslink = jsProgram.getScope().declareName("jslink");
    JsVars vars = new JsVars(info);
    JsVar var = new JsVar(info, jslink);
    var.setInitExpr(new JsObjectLiteral(info));
    vars.add(var);
    jsProgram.getFragmentBlock(0).getStatements().add(0, vars);
  }

  private void execImpl() {
    if (jsProgram.getFragmentCount() == 1) {
      return;
    }
    if (!shouldPredeclareReferences()) {
      return;
    }
    defineJsLink();
    FindNameReferences findNameReferences = new FindNameReferences();
    findNameReferences.accept(jsProgram);
    chooseNamesToPredefine(findNameReferences.islandsDefining, findNameReferences.islandsUsing);
    new RewriteDeclsAndRefs().accept(jsProgram);
  }

  /**
   * Check the property oracles for whether references should be predeclared or
   * not. If any of them say yes, then do the rewrite.
   */
  private boolean shouldPredeclareReferences() {
    for (PropertyOracle props : propertyOracles) {
      try {
        String propValue =
            props.getSelectionProperty(logger, PROP_PREDECLARE_VARS).getCurrentValue();
        if (Boolean.parseBoolean(propValue)) {
          return true;
        }
      } catch (BadPropertyValueException e) {
        // Property not defined; don't rewrite
      }
    }

    return false;
  }
}
