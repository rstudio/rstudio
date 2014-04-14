/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Represents a GWT.runAsync() call.
 */
public class JRunAsync extends JExpression {

  private final String name;
  private final boolean explicitClassLiteral;
  private JExpression onSuccessCall;
  private JExpression runAsyncCall;
  private final int runAsyncId;

  /**
   * Constructs a runAsync call node; explicitClassLiteral is set if the corresponding
   * GWT.runAsync() call has a class literal.
   */
  public JRunAsync(SourceInfo info, int runAsyncId, String name, boolean explicitClassLiteral,
      JExpression runAsyncCall, JExpression onSuccessCall) {
    super(info);
    this.runAsyncId = runAsyncId;
    assert name != null;
    this.name = name;
    this.explicitClassLiteral = explicitClassLiteral;
    this.runAsyncCall = runAsyncCall;
    this.onSuccessCall = onSuccessCall;
  }

  /**
   * Based on either explicit class literal, or the jsni name of the containing
   * method.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns a call expression akin to {@code callback.onSuccess()}.
   * {@link com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer} makes a synthetic
   * visit to this call on the "far" side of the split point, ie, the code that
   * runs when the fragment is through downloading.
   */
  public JExpression getOnSuccessCall() {
    return onSuccessCall;
  }

  /**
   * Returns a call expression akin to
   * {@code AsyncFragmentLoader.runAsync(7, callback)}. This represents the
   * "near" side of the split point, calling into the machinery that queues up
   * the fragment download.
   */
  public JExpression getRunAsyncCall() {
    return runAsyncCall;
  }

  /**
   * Returns a unique id for each runAsync, 1-based.
   *
   * <p>ReplaceRunAsyncs embeds these ids into the Java AST as parameter
   * for a call to {@code }RunAsync.forSplitPointNumber}</p>
   *
   * TODO(rluble): these ids used to be splitpoint/fragment ids back when there was 1-to-1
   * mapping from RunAsyncs to fragment id. This code and the runtime code need to be
   * refactored so that its less coupled and cleaner.
   */
  public int getRunAsyncId() {
    return runAsyncId;
  }

  @Override
  public JType getType() {
    return JPrimitiveType.VOID;
  }

  @Override
  public boolean hasSideEffects() {
    return true;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      /*
       * Normal code flow treats this node like the "near" side call into
       * AsyncFragmentLoader. We only visit the onSuccessCall "far" side
       * explicitly.
       */
      runAsyncCall = visitor.accept(runAsyncCall);
    }
    visitor.endVisit(this, ctx);
  }

  /**
   * Explcitly traverse the onSuccessCall.
   */
  public void traverseOnSuccess(JVisitor visitor) {
    onSuccessCall = visitor.accept(onSuccessCall);
  }

  /**
   * Returns true if the corresponding GWT.runAsync call had a class literal.
   */
  public boolean hasExplicitClassLiteral() {
    return explicitClassLiteral;
  }
}
