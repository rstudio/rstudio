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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Interns all String literals in a JsProgram. Each unique String will be
 * assigned to a variable in the program's main block and instances of a
 * JsStringLiteral replaced with a JsNameRef. This optimization is complete in a
 * single pass, although it may be performed multiple times without duplicating
 * the intern pool.
 */
public class JsStringInterner {

  /**
   * Replaces JsStringLiterals with JsNameRefs, creating new JsName allocations
   * on the fly.
   */
  private static class StringVisitor extends JsModVisitor {
    /**
     * Records the scope in which the interned identifiers should be unique.
     */
    final JsScope scope;

    /**
     * This is a TreeMap to ensure consistent iteration order, based on the
     * lexicographical ordering of the string constant.
     */
    final Map<JsStringLiteral, JsName> toCreate =
        new TreeMap<JsStringLiteral, JsName>(new Comparator<JsStringLiteral>() {
          public int compare(JsStringLiteral o1, JsStringLiteral o2) {
            return o1.getValue().compareTo(o2.getValue());
          }
        });

    /**
     * A counter used for assigning ids to Strings. Even though it's unlikely
     * that someone would actually have two billion strings in their
     * application, it doesn't hurt to think ahead.
     */
    long lastId = 0;

    /**
     * Constructor.
     * 
     * @param scope specifies the scope in which the interned strings should be
     *          created.
     */
    public StringVisitor(JsScope scope) {
      this.scope = scope;
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
      return !x.getOperator().isAssignment()
          || !(x.getArg1() instanceof JsStringLiteral);
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
      return !(x.getArg() instanceof JsStringLiteral);
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
      return !(x.getArg() instanceof JsStringLiteral);
    }

    /**
     * We ignore property initializer labels in object literals, but do process
     * the expression. This is because the LHS is always treated as a string,
     * and never evaluated as an expression.
     */
    @Override
    public boolean visit(JsPropertyInitializer x,
        JsContext<JsPropertyInitializer> ctx) {
      x.setValueExpr(accept(x.getValueExpr()));
      return false;
    }

    /**
     * Replace JsStringLiteral instances with JsNameRefs.
     */
    @Override
    public boolean visit(JsStringLiteral x, JsContext<JsExpression> ctx) {
      JsName name = toCreate.get(x);
      if (name == null) {
        String ident = PREFIX + lastId++;
        name = scope.declareName(ident);
        toCreate.put(x, name);
      }

      ctx.replaceMe(name.makeRef());

      return false;
    }

    /**
     * This prevents duplicating the intern pool by not traversing JsVar
     * declarations that look like they were created by the interner.
     */
    @Override
    public boolean visit(JsVar x, JsContext<JsVar> ctx) {
      return !(x.getName().getIdent().startsWith(PREFIX));
    }
  }

  public static final String PREFIX = "$intern_";

  public static boolean exec(JsProgram program) {
    StringVisitor v = new StringVisitor(program.getScope());
    v.accept(program);

    if (v.toCreate.size() > 0) {
      // Create the pool of variable names.
      JsVars vars = new JsVars();
      for (Map.Entry<JsStringLiteral, JsName> entry : v.toCreate.entrySet()) {
        JsVar var = new JsVar(entry.getValue());
        var.setInitExpr(entry.getKey());
        vars.add(var);
      }
      program.getGlobalBlock().getStatements().add(0, vars);
    }

    return v.didChange();
  }

  /**
   * Utility class.
   */
  private JsStringInterner() {
  }
}
