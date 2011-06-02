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
  private JExpression onSuccessCall;
  private JExpression runAsyncCall;
  private final int splitPoint;

  public JRunAsync(SourceInfo info, int splitPoint, String name, JExpression runAsyncCall,
      JExpression onSuccessCall) {
    super(info);
    this.splitPoint = splitPoint;
    assert name != null;
    this.name = name;
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
   * Returns the split point number, 1-based.
   */
  public int getSplitPoint() {
    return splitPoint;
  }

  @Override
  public JType getType() {
    return JPrimitiveType.VOID;
  }

  @Override
  public boolean hasSideEffects() {
    return true;
  }

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
}
