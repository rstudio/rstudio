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
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsLiteral;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.HashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Interns conditionally either all literals in a JsProgram, or literals
 * which exceed a certain usage count. Each unique literal will be assigned to a
 * variable in an appropriate program fragment and the JsLiteral will be
 * replaced with a JsNameRef. This optimization is complete in a single pass,
 *
 * It is not safe to run the interner multiple times on the same tree as the names that are
 * assigned to interned literals will collide.
 */
public class JsLiteralInterner {

  /**
   * Counts occurrences of each potentially internable literal.
   */
  private static class OccurrenceCounterVisitor extends JsVisitor {

    private Multiset<JsLiteral> countByLiteral = HashMultiset.create();

    public Multiset<JsLiteral> getLiteralCounts() {
      return countByLiteral;
    }

    /**
     * Implement visit(Js*Literal,...) in a general way as there is no visit(JsLiteral, JsContext)
     * to override in Js*Visitor.
     */
    private boolean doVisitLiteral(JsLiteral x) {
      if (x.isInternable()) {
        countByLiteral.add(x);
        // Literal was internable and counted, do not count its internal literals.
        return false;
      }
      // The literal was not internable but might have some internable constants inside,
      // so count them.
      return true;
    }

    @Override
    public boolean visit(JsBinaryOperation x, JsContext ctx) {
      if (!hasLhsLiteral(x)) {
        // Literal l-values should not arise from valid code, but they are excluded
        // anyway so that errors are not masked away by interning.
        accept(x.getArg1());
      }
      accept(x.getArg2());
      return false;
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsPostfixOperation x, JsContext ctx) {
      return !(x.getArg() instanceof JsLiteral);
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsPrefixOperation x, JsContext ctx) {
      return !(x.getArg() instanceof JsLiteral);
    }

    /**
     * We ignore property initializer labels in object literals, but do process
     * the expression. This is because the LHS is always treated as a string,
     * and never evaluated as an expression.
     */
    @Override
    public boolean visit(JsPropertyInitializer x, JsContext ctx) {
      accept(x.getValueExpr());
      return false;
    }

    /**
     * Count occurences of String literal.
     */
    @Override
    public boolean visit(JsStringLiteral x, JsContext ctx) {
      return doVisitLiteral(x);
    }

    /**
     * Count occurences of Object literal.
     */
    @Override
    public boolean visit(JsObjectLiteral x, JsContext ctx) {
      return doVisitLiteral(x);
    }

    @Override
    public boolean visit(JsRegExp x, JsContext ctx) {
      return doVisitLiteral(x);
    }

    @Override
    public boolean visit(JsNumberLiteral x, JsContext ctx) {
      return doVisitLiteral(x);
    }

    /**
     * Count occurences of Array literal.
     */
    @Override
    public boolean visit(JsArrayLiteral x, JsContext ctx) {
      return doVisitLiteral(x);
    }

    /**
     * This prevents duplicating the intern pool by not traversing JsVar
     * declarations that look like they were created by the interner.
     */
    @Override
    public boolean visit(JsVar x, JsContext ctx) {
      return !(x.getName().getIdent().startsWith(PREFIX));
    }
  }

  /**
   * Replaces internable JsLiterals with JsNameRefs, creating new JsName allocations
   * on the fly.
   */
  private static class LiteralInterningVisitor extends JsModVisitor {

    /**
     * The average length of an obfuscated id. It is OK to overestimate it.
     */
    private static final int AVERAGE_ID_LENGTH = 3;

    /**
     * The number of characters needed to declare an interned literal. As there are many literals we
     * only count the equal sign and the comma.
     * It is OK to overestimate the overhead.
     */
    private static final int INTERNED_LITERAL_DECLARATION_OVERHEAD = AVERAGE_ID_LENGTH + 2;

    /*
     * Minimum number of times a literal must occur to be interned.
     */
    private static final Integer MINIMUM_NUMBER_OF_OCCURRENCES_TO_INTERN = Integer.parseInt(
        System.getProperty("gwt.jjs.literalInternerThreshold", "2"));

    /**
     * The current fragment being visited.
     */
    private int currentFragment = 0;

    /**
     * This map records which program fragment the variable for this JsName
     * should be created in.
     */
    private final Map<JsLiteral, Integer> fragmentAssignment = Maps.newLinkedHashMap();

    /**
     * A counter used for assigning ids to literals. Even though it's unlikely
     * that someone would actually have two billion literals in their
     * application, it doesn't hurt to think ahead.
     */
    private long lastId = 0;

    /**
     * Count of # of occurences of each literal, or null if
     * count-sensitive interning is off.
     */
    private Multiset<JsLiteral> occurrencesPerLiteral;

    /**
     * Only used to get fragment load order so literals used in multiple
     * fragments are placed in the right fragment.
     */
    private final JProgram program;

    /**
     * Records the scope in which the interned identifiers are declared.
     */
    private final JsScope scope;

    /**
     * Whether to intern all literals without considering occurrence count and profitability.
     */
    private final boolean alwaysIntern;

    /**
     * Maps a literal to be interned to its interned variable name.
     */
    private final Map<JsLiteral, JsName> variableNameForInternedLiteral = Maps.newLinkedHashMap();

    /**
     * This is a set of flags indicating what types of literals are to be interned.
     */
    private final int whatToIntern;

    /**
     * Constructor.
     *
     * @param scope specifies the scope in which the interned literals should be.
     * @param alwaysIntern whether to intern all literals without considering occurrence count and
     *                     profitability
     * @param occurrencesPerLiteral a multiset representing the literal counts.
     * @param whatToIntern what types of literals are to be interned.
     */
    public LiteralInterningVisitor(JProgram program, JsScope scope, boolean alwaysIntern,
        Multiset<JsLiteral> occurrencesPerLiteral, int whatToIntern) {

      assert alwaysIntern || (occurrencesPerLiteral != null);

      this.program = program;
      this.scope = scope;
      this.occurrencesPerLiteral = occurrencesPerLiteral;
      this.whatToIntern = whatToIntern;
      this.alwaysIntern = alwaysIntern;
    }

    @Override
    public void endVisit(JsProgramFragment x, JsContext ctx) {
      currentFragment++;
    }

    /**
     * Replace JsArrayLiteral instances with JsNameRefs.
     */
    @Override
    public boolean visit(JsArrayLiteral x, JsContext ctx) {
      boolean interned = false;
      if ((whatToIntern & INTERN_ARRAY_LITERALS) != 0) {
        interned = maybeInternLiteral(x, ctx);
      }

      // If the array literal is interned do not try to intern any of its contents.
      return !interned;
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsBinaryOperation x, JsContext ctx) {
      if (!hasLhsLiteral(x)) {
        // Literal l-values should not arise from valid code, but they are excluded
        // anyway so that errors are not masked away by interning.
        x.setArg1(accept(x.getArg1()));
      }
      x.setArg2(accept(x.getArg2()));
      return false;
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsPostfixOperation x, JsContext ctx) {
      return !(x.getArg() instanceof JsLiteral);
    }

    /**
     * Prevents 'fixing' an otherwise illegal operation.
     */
    @Override
    public boolean visit(JsPrefixOperation x, JsContext ctx) {
      return !(x.getArg() instanceof JsLiteral);
    }

    /**
     * We ignore property initializer labels in object literals, but do process
     * the expression. This is because the LHS is always treated as a string,
     * and never evaluated as an expression.
     */
    @Override
    public boolean visit(JsPropertyInitializer x, JsContext ctx) {
      x.setValueExpr(accept(x.getValueExpr()));
      return false;
    }

    /**
     * Replace JsStringLiteral instances with JsNameRefs.
     */
    @Override
    public boolean visit(JsStringLiteral x, JsContext ctx) {
      if ((whatToIntern & INTERN_STRINGS) != 0) {
        maybeInternLiteral(x, ctx);
      }
      return false;
    }

    /**
     * Replace JsObjectLiteral instances with JsNameRefs.
     */
    @Override
    public boolean visit(JsObjectLiteral x, JsContext ctx) {
      boolean interned = false;
      if ((whatToIntern & INTERN_OBJECT_LITERALS) != 0) {
        interned = maybeInternLiteral(x, ctx);
      }

      // If the object literal is interned do not try to intern any of its contents.
      return !interned;
    }

    /**
     * Replace JsRegExp instances with JsNameRefs.
     */
    @Override
    public boolean visit(JsRegExp x, JsContext ctx) {
      if ((whatToIntern & INTERN_REGEXES) != 0) {
        maybeInternLiteral(x, ctx);
      }
      return false;
    }

    /**
     * Replace JsNumberLiteral instances with JsNameRefs.
     */
    @Override
    public boolean visit(JsNumberLiteral x, JsContext ctx) {
      if ((whatToIntern & INTERN_NUMBERS) != 0) {
        maybeInternLiteral(x, ctx);
      }
      return false;
    }

    /**
     * Returns true if interning {@code literal} will most likely reduce code size.
     */
    private boolean isProfitableToIntern(JsLiteral literal, int occurrences) {
      int literalSize = literal.toSource().length();
      int internedSize = occurrences * AVERAGE_ID_LENGTH + INTERNED_LITERAL_DECLARATION_OVERHEAD +
          literalSize;
      int uninternedSize = occurrences * literalSize;
      return internedSize < uninternedSize;
    }

    /**
     * Interns a literal if deemed profitable or {@code alwaysIntern} is {@code true}.
     */
    private boolean maybeInternLiteral(JsLiteral x, JsContext ctx) {
      if (!x.isInternable()) {
        return false;
      }

      if (!alwaysIntern) {
        int occurrences = occurrencesPerLiteral.count(x);
        if (occurrences < MINIMUM_NUMBER_OF_OCCURRENCES_TO_INTERN) {
          return false;
        }

        boolean alreadyInterned = variableNameForInternedLiteral.containsKey(x);
        if (!alreadyInterned && !isProfitableToIntern(x, occurrences)) {
          return false;
        }
      }

      JsName name = variableNameForInternedLiteral.get(x);
      if (name == null) {
        String ident = PREFIX + lastId++;
        name = scope.declareName(ident);
        variableNameForInternedLiteral.put(x, name);
      }

      Integer currentAssignment = fragmentAssignment.get(x);
      if (currentAssignment == null) {
        // Assign the JsName to the current program fragment
        fragmentAssignment.put(x, currentFragment);

      } else if (currentAssignment != currentFragment) {
        // See if we need to move the assignment to a common ancestor
        Preconditions.checkState(program != null, "JsLiteralInterner cannot be used with "
            + "fragmented JsProgram without an accompanying JProgram");

        int newAssignment = program.getCommonAncestorFragmentId(currentAssignment, currentFragment);
        if (newAssignment != currentAssignment) {
          // Assign the JsName to the common ancestor.
          fragmentAssignment.put(x, newAssignment);
        }
      }

      ctx.replaceMe(name.makeRef(x.getSourceInfo().makeChild()));
      return true;
    }

    /**
     * This prevents duplicating the intern pool by not traversing JsVar
     * declarations that look like they were created by the interner.
     */
    @Override
    public boolean visit(JsVar x, JsContext ctx) {
      return !(x.getName().getIdent().startsWith(PREFIX));
    }
  }

  /**
   * Flags to control what type of literals to intern.
   */
  public static final int INTERN_ARRAY_LITERALS = 0x01;
  public static final int INTERN_NUMBERS = 0x02;
  public static final int INTERN_OBJECT_LITERALS = 0x04;
  public static final int INTERN_REGEXES = 0x08;
  public static final int INTERN_STRINGS = 0x10;
  public static final int INTERN_ALL = INTERN_ARRAY_LITERALS | INTERN_NUMBERS |
      INTERN_OBJECT_LITERALS | INTERN_REGEXES | INTERN_STRINGS;

  private static final String PREFIX = "$intern_";

  /**
   * Apply interning of literals to a JsProgram. The symbol names for the
   * interned literals will be defined within the program's top scope and the
   * symbol declarations will be added as the first statement in the program's
   * global block.
   *
   * @param jprogram the JProgram that has fragment dependency data for {@code program}
   * @param program the JsProgram
   * @param whatToIntern a byte mask indicating what types of literals are interned.
   * @return a map describing the interning that occurred
   */
  public static Map<JsName, JsLiteral> exec(JProgram jprogram, JsProgram program,
      int whatToIntern) {
    LiteralInterningVisitor v = new LiteralInterningVisitor(jprogram, program.getScope(), false,
        computeOccurrenceCounts(program), whatToIntern);
    v.accept(program);

    Map<Integer, Set<JsLiteral>> bins = Maps.newHashMap();
    for (int i = 0, j = program.getFragmentCount(); i < j; i++) {
      bins.put(i, Sets.<JsLiteral>newLinkedHashSet());
    }
    for (Map.Entry<JsLiteral, Integer> entry : v.fragmentAssignment.entrySet()) {
      Set<JsLiteral> set = bins.get(entry.getValue());
      assert set != null;
      set.add(entry.getKey());
    }

    for (Map.Entry<Integer, Set<JsLiteral>> entry : bins.entrySet()) {
      createVars(program, program.getFragmentBlock(entry.getKey()),
          entry.getValue(), v.variableNameForInternedLiteral);
    }

    return reverse(v.variableNameForInternedLiteral);
  }

  /**
   * Intern literals that occur within a JsBlock. The symbol declarations
   * will be added as the first statement in the block.
   *
   * @param block the block to visit.
   * @param scope the JsScope in which to reserve the new identifiers.
   * @param alwaysIntern whether to intern all literals regardless of their occurrence count.
   * @return {@code true} if any changes were made to the block.
   */
  public static boolean exec(JsProgram program, JsBlock block, JsScope scope,
      boolean alwaysIntern) {

    Multiset<JsLiteral> occurrencesPerLiteral = null;
    if (!alwaysIntern) {
      occurrencesPerLiteral = computeOccurrenceCounts(block);
    }

    LiteralInterningVisitor v = new LiteralInterningVisitor(null, scope, alwaysIntern,
        occurrencesPerLiteral, INTERN_ALL);
    v.accept(block);

    createVars(program, block, v.variableNameForInternedLiteral.keySet(),
        v.variableNameForInternedLiteral);

    return v.didChange();
  }

  /**
   * Create variable declarations in {@code block} for literals
   * {@code variableNameForInternedLiteral} using the variable map {@code names}.
   */
  private static void createVars(JsProgram program, JsBlock block,
      Collection<JsLiteral> toCreate, Map<JsLiteral, JsName> names) {
    if (toCreate.size() > 0) {
      // Create the pool of variable names.
      SourceInfo sourceInfo = program.createSourceInfoSynthetic(JsLiteralInterner.class);
      JsVars vars = new JsVars(sourceInfo);
      for (JsLiteral literal : toCreate) {
        JsVar var = new JsVar(sourceInfo, names.get(literal));
        var.setInitExpr(literal);
        vars.add(var);
      }
      block.getStatements().add(0, vars);
    }
  }

  private static Multiset<JsLiteral> computeOccurrenceCounts(JsNode node) {
    OccurrenceCounterVisitor oc = new OccurrenceCounterVisitor();
    oc.accept(node);
    return oc.getLiteralCounts();
  }

  private static Map<JsName, JsLiteral> reverse(Map<JsLiteral, JsName>
      variableNameForInternedLiteral) {
    Map<JsName, JsLiteral> reversed = Maps.newLinkedHashMap();
    for (Entry<JsLiteral, JsName> entry : variableNameForInternedLiteral.entrySet()) {
      reversed.put(entry.getValue(), entry.getKey());
    }
    return reversed;
  }

  private static boolean hasLhsLiteral(JsBinaryOperation x) {
    return x.getOperator().isAssignment()
        && (x.getArg1() instanceof JsLiteral);
  }

  /**
   * Utility class.
   */
  private JsLiteralInterner() {
  }
}
