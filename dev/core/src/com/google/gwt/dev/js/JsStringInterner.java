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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Interns all String literals in a JsProgram. Each unique String will be
 * assigned to a variable in an appropriate program fragment and the
 * JsStringLiteral will be replaced with a JsNameRef. This optimization is
 * complete in a single pass, although it may be performed multiple times
 * without duplicating the intern pool.
 */
public class JsStringInterner {
  /**
   * Replaces JsStringLiterals with JsNameRefs, creating new JsName allocations
   * on the fly.
   */
  private static class StringVisitor extends JsModVisitor {
    /**
     * The current fragment being visited.
     */
    private int currentFragment = 0;

    /**
     * This map records which program fragment the variable for this JsName
     * should be created in.
     */
    private final SortedMap<JsStringLiteral, Integer> fragmentAssignment = new TreeMap<JsStringLiteral, Integer>(
        LITERAL_COMPARATOR);

    /**
     * A counter used for assigning ids to Strings. Even though it's unlikely
     * that someone would actually have two billion strings in their
     * application, it doesn't hurt to think ahead.
     */
    private long lastId = 0;

    /**
     * Only used to get fragment load order so strings used in multiple
     * fragments need only be downloaded once.
     */
    private final JProgram program;

    /**
     * Records the scope in which the interned identifiers are declared.
     */
    private final JsScope scope;

    /**
     * This is a TreeMap to ensure consistent iteration order, based on the
     * lexicographical ordering of the string constant.
     */
    private final SortedMap<JsStringLiteral, JsName> toCreate = new TreeMap<JsStringLiteral, JsName>(
        LITERAL_COMPARATOR);

    /**
     * Constructor.
     * 
     * @param scope specifies the scope in which the interned strings should be
     *          created.
     */
    public StringVisitor(JProgram program, JsScope scope) {
      this.program = program;
      this.scope = scope;
    }

    @Override
    public void endVisit(JsProgramFragment x, JsContext<JsProgramFragment> ctx) {
      currentFragment++;
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

      Integer currentAssignment = fragmentAssignment.get(x);
      if (currentAssignment == null) {
        // Assign the JsName to the current program fragment
        fragmentAssignment.put(x, currentFragment);

      } else if (currentAssignment != currentFragment) {
        // See if we need to move the assignment to a common ancestor
        assert program != null : "JsStringInterner cannot be used with "
            + "fragmented JsProgram without an accompanying JProgram";

        int newAssignment = program.lastFragmentLoadingBefore(currentFragment,
            currentAssignment);
        if (newAssignment != currentAssignment) {
          // Assign the JsName to the common ancestor
          fragmentAssignment.put(x, newAssignment);
        }
      }

      ctx.replaceMe(name.makeRef(x.getSourceInfo().makeChild(
          JsStringInterner.class, "Interned reference")));

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

  private static final Comparator<JsStringLiteral> LITERAL_COMPARATOR = new Comparator<JsStringLiteral>() {
    public int compare(JsStringLiteral o1, JsStringLiteral o2) {
      return o1.getValue().compareTo(o2.getValue());
    }
  };

  /**
   * Apply interning of String literals to a JsProgram. The symbol names for the
   * interned strings will be defined within the program's top scope and the
   * symbol declarations will be added as the first statement in the program's
   * global block.
   * 
   * @param jprogram the JProgram that has fragment dependency data for
   *          <code>program</code>
   * @param program the JsProgram
   * @return a map describing the interning that occurred
   */
  public static Map<JsName, String> exec(JProgram jprogram, JsProgram program) {
    StringVisitor v = new StringVisitor(jprogram, program.getScope());
    v.accept(program);

    Map<Integer, SortedSet<JsStringLiteral>> bins = new HashMap<Integer, SortedSet<JsStringLiteral>>();
    for (int i = 0, j = program.getFragmentCount(); i < j; i++) {
      bins.put(i, new TreeSet<JsStringLiteral>(LITERAL_COMPARATOR));
    }
    for (Map.Entry<JsStringLiteral, Integer> entry : v.fragmentAssignment.entrySet()) {
      SortedSet<JsStringLiteral> set = bins.get(entry.getValue());
      assert set != null;
      set.add(entry.getKey());
    }

    for (Map.Entry<Integer, SortedSet<JsStringLiteral>> entry : bins.entrySet()) {
      createVars(program, program.getFragmentBlock(entry.getKey()),
          entry.getValue(), v.toCreate);
    }

    return reverse(v.toCreate);
  }

  /**
   * Intern String literals that occur within a JsBlock. The symbol declarations
   * will be added as the first statement in the block.
   * 
   * @param block the block to visit
   * @param scope the JsScope in which to reserve the new identifiers
   * @return <code>true</code> if any changes were made to the block
   */
  public static boolean exec(JsProgram program, JsBlock block, JsScope scope) {
    StringVisitor v = new StringVisitor(null, scope);
    v.accept(block);

    createVars(program, block, v.toCreate.keySet(), v.toCreate);

    return v.didChange();
  }

  /**
   * Create variable declarations in <code>block</code> for literal strings
   * <code>toCreate</code> using the variable map <code>names</code>.
   */
  private static void createVars(JsProgram program, JsBlock block,
      Collection<JsStringLiteral> toCreate, Map<JsStringLiteral, JsName> names) {
    if (toCreate.size() > 0) {
      // Create the pool of variable names.
      JsVars vars = new JsVars(program.createSourceInfoSynthetic(
          JsStringInterner.class, "Interned string pool"));
      SourceInfo sourceInfo = program.createSourceInfoSynthetic(
          JsStringInterner.class, "Interned string assignment");
      for (JsStringLiteral literal : toCreate) {
        JsVar var = new JsVar(sourceInfo, names.get(literal));
        var.setInitExpr(literal);
        vars.add(var);
      }
      block.getStatements().add(0, vars);
    }
  }

  private static Map<JsName, String> reverse(
      SortedMap<JsStringLiteral, JsName> toCreate) {
    Map<JsName, String> reversed = new LinkedHashMap<JsName, String>(
        toCreate.size());
    for (Entry<JsStringLiteral, JsName> entry : toCreate.entrySet()) {
      reversed.put(entry.getValue(), entry.getKey().getValue());
    }
    return reversed;
  }

  /**
   * Utility class.
   */
  private JsStringInterner() {
  }
}
