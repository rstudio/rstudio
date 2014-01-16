/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * A compiler pass that creates a namespace for each Java package
 * with at least one global variable or function.
 *
 * <p>Prerequisite: JsVarRefs must be resolved.</p>
 */
public class JsNamespaceChooser {

  public static void exec(JsProgram program, JavaToJavaScriptMap jjsmap) {
    new JsNamespaceChooser(program, jjsmap).execImpl();
  }

  private final JsProgram program;
  private final JavaToJavaScriptMap jjsmap;

  public JsNamespaceChooser(JsProgram program, JavaToJavaScriptMap jjsmap) {
    this.program = program;
    this.jjsmap = jjsmap;
  }

  private void execImpl() {

    Map<String, JsName> packageToNamespace = Maps.newHashMap();
    JsVars namespaceVars = new JsVars(SourceOrigin.UNKNOWN);

    // Work from a copy because we will be adding newly-created namespace names.
    ImmutableList<JsName> allNames = ImmutableList.copyOf(program.getScope().getAllNames());
    for (JsName name : allNames) {
      if (name.getNamespace() != null || !name.isObfuscatable()) {
        continue;
      }

      String packageName = findPackage(name);
      if (packageName == null) {
        continue;
      }

      // Find the namespace for this package
      JsName namespace = packageToNamespace.get(packageName);
      if (namespace == null) {
        // Add an initializer for this package
        namespace = program.getScope().declareName(chooseUnusedName(packageName));
        JsVar init = new JsVar(SourceOrigin.UNKNOWN, namespace);
        init.setInitExpr(new JsObjectLiteral(SourceOrigin.UNKNOWN));
        namespaceVars.add(init);
        packageToNamespace.put(packageName, namespace);
      }

      name.setNamespace(namespace);
    }

    fixGlobalFunctions(program);
    new NameFixer().accept(program);

    if (!namespaceVars.isEmpty()) {
      program.getGlobalBlock().getStatements().add(0, namespaceVars);
    }
  }

  private String chooseUnusedName(String packageName) {
    String initials = initialsForPackage(packageName);
    String candidate = initials;
    int counter = 1;
    while (program.getScope().findExistingName(candidate) != null) {
      counter++;
      candidate = initials + counter;
    }
    return candidate;
  }

  /**
   * Find the Java package name for the given JsName, or null
   * if it couldn't be determined.
   */
  private String findPackage(JsName name) {
    JMethod method = jjsmap.nameToMethod(name);
    if (method != null) {
      return findPackage(method.getEnclosingType());
    }
    JField field = jjsmap.nameToField(name);
    if (field != null) {
      return findPackage(field.getEnclosingType());
    }
    return null; // not found
  }

  private static String findPackage(JDeclaredType type) {
    return Util.getPackageName(type.getName());
  }

  /**
   * Find the initials of a package. For example, "java.lang" -> "jl".
   */
  private static String initialsForPackage(String packageName) {
    StringBuilder result = new StringBuilder();

    int end = packageName.length();
    boolean wasDot = true;
    for (int i = 0; i < end; i++) {
      char c = packageName.charAt(i);
      if (c == '.') {
        wasDot = true;
        continue;
      }
      if (wasDot) {
        result.append(c);
      }
      wasDot = false;
    }

    return result.toString();
  }

  /**
   * Fix top-level function definitions that should point to a namespace.
   * (This is not a visitor because we don't need to recurse.)
   * @return true if anything changed
   */
  private static boolean fixGlobalFunctions(JsProgram program) {

    boolean changed = false;
    List<JsStatement> statements = program.getGlobalBlock().getStatements();
    for (int i = 0; i < statements.size(); i++) {
      JsStatement statement = statements.get(i);
      if (!(statement instanceof JsExprStmt)) {
        continue;
      }
      JsExprStmt parent = (JsExprStmt) statement;
      if (!(parent.getExpression() instanceof JsFunction)) {
        continue;
      }
      JsFunction func = (JsFunction) parent.getExpression();
      JsName name = func.getName();
      if (name == null || name.getNamespace() == null) {
        continue;
      }

      JsNameRef newName = name.makeRef(func.getSourceInfo());
      JsBinaryOperation assign =
          new JsBinaryOperation(func.getSourceInfo(), JsBinaryOperator.ASG, newName, func);
      func.setName(null);
      statements.set(i, new JsExprStmt(parent.getSourceInfo(), assign));
      changed = true;
    }
    return changed;
  }

  /**
   * A compiler pass that moves all global variable definitions to the
   * correct namespace and fixes all name references.
   */
  private static class NameFixer extends JsModVisitor {

    /**
     * Replace "name" with "namespace.name".
     */
    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (!x.isLeaf() || x.getQualifier() != null || x.getName() == null) {
        return;
      }

      JsName namespace = x.getName().getNamespace();
      if (namespace == null) {
        return;
      }

      x.setQualifier(new JsNameRef(x.getSourceInfo(), namespace));
      didChange = true;
    }

    /**
     * Replace top-level "var name = " with "namespace.name = ".
     */
    @Override
    public void endVisit(JsVars x, JsContext ctx) {
      if (!ctx.canInsert()) {
        return;
      }

      // Replace each var with a new statement.
      for (JsVar var : x) {
        JsName name = var.getName();
        JsName namespace = var.getName().getNamespace();
        if (namespace == null) {
          // Leave it as a global var.
          // (A separate var is actually better for debugging.)
          JsVars vars = new JsVars(var.getSourceInfo());
          vars.add(var);
          ctx.insertBefore(vars);
        } else {
          // Change to an assignment to a namespace variable.
          JsNameRef newName = name.makeRef(x.getSourceInfo());
          JsExpression init = var.getInitExpr();
          if (init == null) {
            init = JsNullLiteral.INSTANCE;
          }
          JsBinaryOperation assign = new JsBinaryOperation(var.getSourceInfo(),
              JsBinaryOperator.ASG, newName, init);
          ctx.insertBefore(assign.makeStmt());
        }
      }

      ctx.removeMe();
    }
  }
}
