/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Describes a fragment and its contents.
 *
 * <p>A fragment contains one or more runAsyncs (split points). Each runAsync in the program is
 * assigned to exactly one fragment.</p>
 */
class Fragment {

  /**
   * Types of fragments:
   * <ul>
   *   <li>- INITIAL fragments are the ones that from part of the initial download.</li>
   *   <li>- EXCLUSIVE fragments are the ones that contain atoms that are only live when runAsyncs
   *         contained in the fragment are activated</li>
   *   <li>- NOT_EXCLUSIVE fragments (only one at this stage) contains all the atoms that are not
   *         part of INITIAL or EXCLUSIVE fragments</li>
   * </ul>
   */
  enum Type {
    INITIAL, EXCLUSIVE, NOT_EXCLUSIVE
  };

  public Fragment(Type type, Fragment... ancestorFragments) {
    this.type = type;
    this.addImmediateAncestors(ancestorFragments);
  }

  /**
   * Assign a runAsync to this fragment.
   */
  public void addRunAsync(JRunAsync runAsync) {
    Preconditions.checkArgument(!this.runAsyncs.contains(runAsync), "Fragment %n already contains "
        + "runAsync %n", runAsync.getRunAsyncId());
    runAsyncs.add(runAsync);
  }

  public void addRunAsyncs(Collection<JRunAsync> runAsyncs) {
    this.runAsyncs.addAll(runAsyncs);
  }

  public void addImmediateAncestors(Fragment... ancestorFragments) {
    for (Fragment ancestorFragment : ancestorFragments) {
      this.immediateAncestors.add(ancestorFragment);
    }
  }

  public void addStatements(List<JsStatement> statements) {
    this.statements.addAll(statements);
  }

  public int getFragmentId() {
    assert fragmentId >= 0;
    return fragmentId;
  }

  /**
   * Splitpoints contained in this fragment.
   */
  public Set<JRunAsync> getRunAsyncs() {
    return runAsyncs;
  }

  public List<JsStatement> getStatements() {
    return statements;
  }

  public Type getType() {
    return type;
  }

  public boolean isExclusive() {
    return type == Type.EXCLUSIVE;
  }

  public boolean isInitial() {
    return type == Type.INITIAL;
  }

  public void setFragmentId(int fragmentId) {
    Preconditions.checkArgument(fragmentId >= 0, "Fragment id  %s is not >= 0", fragmentId);
    Preconditions.checkArgument(fragmentId > 0 || type == Type.INITIAL,
        "Fragment 0 is not INITIAL");
    this.fragmentId = fragmentId;
  }

  public void setStatements(List<JsStatement> statements) {
    this.statements = statements;
  }

  private int fragmentId = -1;
  private Set<JRunAsync> runAsyncs = Sets.newHashSet();
  private List<JsStatement> statements;
  private Type type;
  private Set<Fragment> immediateAncestors = Sets.newHashSet();
}
