/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.CorrelationFactory;
import com.google.gwt.dev.jjs.CorrelationFactory.DummyCorrelationFactory;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;

import java.util.Arrays;
import java.util.Collection;

/**
 * A JavaScript program.
 */
public final class JsProgram extends JsNode {

  public static final String OBJECT_SCOPE_NAME = "Object";
  public static final String GLOBAL_SCOPE_NAME = "Global";
  private final CorrelationFactory correlator;

  private JsProgramFragment[] fragments;

  private final JsScope objectScope;

  private final JsScope topScope;

  public JsProgram() {
    this(DummyCorrelationFactory.INSTANCE);
  }

  /**
   * Constructs a JavaScript program object.
   */
  public JsProgram(CorrelationFactory correlator) {
    super(correlator.makeSourceInfo(SourceOrigin.create(0, JsProgram.class.getName())));

    this.correlator = correlator;

    topScope = new JsNormalScope(JsRootScope.INSTANCE, GLOBAL_SCOPE_NAME);
    objectScope = new JsNormalScope(JsRootScope.INSTANCE, OBJECT_SCOPE_NAME);
    setFragmentCount(1);
  }

  public SourceInfo createSourceInfo(int lineNumber, String location) {
    return correlator.makeSourceInfo(SourceOrigin.create(lineNumber, location));
  }

  public SourceInfo createSourceInfoSynthetic(Class<?> caller) {
    return createSourceInfo(0, caller.getName());
  }

  public JsProgramFragment getFragment(int fragment) {
    if (fragment < 0 || fragment >= fragments.length) {
      throw new IllegalArgumentException("Invalid fragment: " + fragment);
    }
    return fragments[fragment];
  }

  public JsBlock getFragmentBlock(int fragment) {
    if (fragment < 0 || fragment >= fragments.length) {
      throw new IllegalArgumentException("Invalid fragment: " + fragment);
    }
    return fragments[fragment].getGlobalBlock();
  }

  public int getFragmentCount() {
    return this.fragments.length;
  }

  public Collection<JsProgramFragment> getFragments() {
    return Arrays.asList(fragments);
  }

  /**
   * Gets the one and only global block.
   */
  public JsBlock getGlobalBlock() {
    return getFragmentBlock(0);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.PROGRAM;
  }

  public JsScope getObjectScope() {
    return objectScope;
  }

  /**
   * Gets the top level scope. This is the scope of all the statements in the main program.
   */
  public JsScope getScope() {
    return topScope;
  }

  public void setFragmentCount(int fragments) {
    this.fragments = new JsProgramFragment[fragments];
    for (int i = 0; i < fragments; i++) {
      this.fragments[i] = new JsProgramFragment(createSourceInfoSynthetic(JsProgram.class));
    }
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      for (JsProgramFragment fragment : fragments) {
        v.accept(fragment);
      }
    }
    v.endVisit(this, ctx);
  }
}
