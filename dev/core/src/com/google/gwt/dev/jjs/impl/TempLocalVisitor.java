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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.util.collect.HashSet;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Utility base class for visitors that need to replace expressions with temp
 * locals. This class specifically handles allocating temp locals, and inserting
 * a {@link JDeclarationStatement} at the appropriate point. It tracks scopes
 * and assigns any potentially conflicting uses with unique names. Subclasses
 * are only allowed to use a temp within the {@link JBlock} within which it is
 * allocated. Non-conflicting temp locals attempt to reuse as many names as
 * possible in order to produce the most optimal output.
 * 
 * <p>
 * Subclasses must always visit a {@link JMethodBody} naturally, rather than
 * individual blocks.
 * </p>
 */
public abstract class TempLocalVisitor extends JModVisitor {
  /*
   * TODO(scottb): right now our handling of for statements is sub-optimal.
   * Technically, a for statement creates an implicit scope that is not part of
   * any block. This scope is a child of the block containing the for statement,
   * and a parent of the for statement's action block. We don't presently model
   * this scope, instead we just allow the for statement's top-level constructs
   * to bleed up into the containing block, which is correct if sub-optimal for
   * name reuse.
   */

  /**
   * Creates a Scope for each JBlock in the current method body.
   */
  private static class CollectScopes extends JVisitor {

    private Scope curScope = null;

    private final Map<JBlock, Scope> scopes;

    public CollectScopes(Map<JBlock, Scope> scopes) {
      this.scopes = scopes;
    }

    @Override
    public void endVisit(JBlock x, Context ctx) {
      exit(x);
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      JVariable target = x.getVariableRef().getTarget();
      if (target instanceof JLocal) {
        String name = target.getName();
        if (name.startsWith(PREFIX)) {
          curScope.recordTempAllocated(Integer.parseInt(name.substring(PREFIX.length()), 10));
        }
      }
    }

    @Override
    public boolean visit(JBlock x, Context ctx) {
      enter(x);
      return true;
    }

    private void enter(JBlock x) {
      curScope = new Scope(curScope);
      scopes.put(x, curScope);
    }

    private void exit(JBlock x) {
      assert scopes.get(x) == curScope;
      curScope = curScope.parent;
    }
  }

  /**
   * Represents a single logical scope (ie, JBlock), and tracks allocation and
   * usage of any temps.
   */
  private static class Scope {
    /**
     * My containing scope, will be <code>null</code> if this scope corresponds
     * to the top-level method body block.
     */
    public final Scope parent;

    /**
     * Caches which temps have been allocated both in this scope and all parent
     * scopes.
     */
    private transient BitSet allAllocated;

    /**
     * Caches the last temp allocated in this scope, for speedier lookup.
     */
    private transient int lastTemp;

    /**
     * The set of all temps allocated by all my child blocks. This prevents me
     * from allocating a temp one of my children already allocated; however it
     * does not prevent my children from reusing temps also used by their
     * siblings.
     */
    private final BitSet myChildTemps = new BitSet();

    /**
     * The set of temps that have been directly allocated in this scope.
     */
    private final BitSet myTemps = new BitSet();

    public Scope(Scope parent) {
      this.parent = parent;
    }

    /**
     * Acquires the next free temp in this scope.
     */
    public int allocateNextFreeTemp() {
      if (allAllocated == null) {
        allAllocated = new BitSet();
        // Any temps already allocated by my parents are not available to me.
        for (Scope it = this; it != null; it = it.parent) {
          allAllocated.or(it.myTemps);
        }
      }
      // Any temps already allocated by my children are not available to me.
      allAllocated.or(myChildTemps);
      // Find the next free temp.
      lastTemp = allAllocated.nextClearBit(lastTemp);
      recordTempAllocated(lastTemp);
      return lastTemp;
    }

    /**
     * Called when entering this scope, clears transient state.
     */
    public void enter() {
      // Assume dirty, lazy recompute.
      allAllocated = null;
      lastTemp = 0;
    }

    /**
     * Called when exiting this scope.
     */
    public void exit() {
      // Free the memory.
      allAllocated = null;
    }

    /**
     * Record a temp as being allocated in this scope.
     */
    public void recordTempAllocated(int tempNumber) {
      assert !myTemps.get(tempNumber);
      // Record my own usage.
      myTemps.set(tempNumber);
      if (allAllocated != null) {
        allAllocated.set(tempNumber);
      }
      // Tell all my parents I'm now using this one.
      for (Scope it = this.parent; it != null; it = it.parent) {
        assert !it.myTemps.get(tempNumber);
        it.myChildTemps.set(tempNumber);
      }
    }
  }

  /**
   * Prefix for temp locals.
   */
  private static final String PREFIX = "$t";

  /**
   * A set of statements we cannot insert declaration statements into. Currently
   * this is just the "increments" list of a JForStatement.
   */
  private final Set<JStatement> banList = new HashSet<JStatement>();

  private JMethodBody curMethodBody = null;
  private Scope curScope = null;
  private final Stack<Context> insertionStack = new Stack<Context>();
  private Map<JBlock, Scope> scopes = null;

  @Override
  public final void endVisit(JBlock x, Context ctx) {
    exit(x);
    super.endVisit(x, ctx);
  }

  @Override
  public final void endVisit(JMethodBody x, Context ctx) {
    curMethodBody = null;
    scopes = null;
    super.endVisit(x, ctx);
  }

  @Override
  public final void endVisit(JStatement x, Context ctx) {
    if (ctx.canInsert()) {
      if (!banList.remove(x)) {
        Context popped = insertionStack.pop();
        assert popped == ctx;
      }
    }
    super.endVisit(x, ctx);
  }

  @Override
  public final boolean visit(JBlock x, Context ctx) {
    enter(x);
    return super.visit(x, ctx);
  }

  @Override
  public final boolean visit(JMethodBody x, Context ctx) {
    curMethodBody = x;
    scopes = new HashMap<JBlock, Scope>();
    new CollectScopes(scopes).accept(x);
    return super.visit(x, ctx);
  }

  @Override
  public final boolean visit(JStatement x, Context ctx) {
    if (ctx.canInsert() && !banList.contains(x)) {
      insertionStack.push(ctx);
    }
    if (x instanceof JForStatement) {
      // Cannot add decl statements to a for statement increments list.
      JForStatement forStmt = (JForStatement) x;
      banList.addAll(forStmt.getIncrements());
    }
    return super.visit(x, ctx);
  }

  protected JLocal createTempLocal(SourceInfo info, JType type) {
    int tempNum = curScope.allocateNextFreeTemp();
    String name = PREFIX + tempNum;
    JLocal local = JProgram.createLocal(info, name, type, false, curMethodBody);
    JDeclarationStatement init = new JDeclarationStatement(info, new JLocalRef(info, local), null);
    insertionStack.peek().insertBefore(init);
    return local;
  }

  private boolean enter(JBlock x) {
    Scope enterScope = scopes.get(x);
    assert enterScope.parent == curScope;
    curScope = enterScope;
    enterScope.enter();
    return true;
  }

  private void exit(JBlock x) {
    assert scopes.get(x) == curScope;
    curScope.exit();
    curScope = curScope.parent;
  }
}
