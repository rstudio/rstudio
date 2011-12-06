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

import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.JsSourceGenerationVisitor;
import com.google.gwt.dev.js.JsToStringGenerationVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;

import java.io.Serializable;

/**
 * Base class for all JS AST elements.
 */
public abstract class JsNode implements JsVisitable, HasSourceInfo, Serializable {

  private SourceInfo sourceInfo;

  protected JsNode(SourceInfo sourceInfo) {
    assert sourceInfo != null : "SourceInfo must be provided for JsNodes";
    this.sourceInfo = sourceInfo;
  }

  public abstract NodeKind getKind();

  @Override
  public SourceInfo getSourceInfo() {
    return sourceInfo;
  }

  public void setSourceInfo(SourceInfo info) {
    assert this.sourceInfo.getOrigin() == info.getOrigin();
    this.sourceInfo = info;
  }

  /**
   * Returns a source code representation of the node using short identifiers.
   */
  public final String toSource() {
    return toSource(false);
  }

  /**
   * Returns a source code representation of the node using short or long identifiers.
   * 
   * @param useLongIdents if true, emit all identifiers in long form
   */
  public final String toSource(boolean useLongIdents) {
    DefaultTextOutput out = new DefaultTextOutput(false);
    JsSourceGenerationVisitor v = new JsSourceGenerationVisitor(out, useLongIdents);
    v.accept(this);
    return out.toString();
  }

  /**
   * Causes source generation to delegate to the one visitor.
   */
  @Override
  public final String toString() {
    DefaultTextOutput out = new DefaultTextOutput(false);
    JsToStringGenerationVisitor v = new JsToStringGenerationVisitor(out);
    v.accept(this);
    return out.toString();
  }
}
