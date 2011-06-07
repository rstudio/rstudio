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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.impl.SourceGenerationVisitor;
import com.google.gwt.dev.jjs.impl.ToStringGenerationVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;

import java.io.Serializable;

/**
 * Base class for all visitable AST nodes.
 */
public abstract class JNode implements JVisitable, HasSourceInfo, Serializable {

  private SourceInfo info;

  protected JNode(SourceInfo info) {
    assert info != null : "SourceInfo must be provided for JNodes";
    this.info = info;
  }

  public SourceInfo getSourceInfo() {
    return info;
  }

  public void setSourceInfo(SourceInfo info) {
    assert this.info.getOrigin() == info.getOrigin();
    this.info = info;
  }

  // Causes source generation to delegate to the one visitor
  public final String toSource() {
    DefaultTextOutput out = new DefaultTextOutput(false);
    SourceGenerationVisitor v = new SourceGenerationVisitor(out);
    v.accept(this);
    return out.toString();
  }

  // Causes source generation to delegate to the one visitor
  @Override
  public final String toString() {
    DefaultTextOutput out = new DefaultTextOutput(false);
    ToStringGenerationVisitor v = new ToStringGenerationVisitor(out);
    v.accept(this);
    return out.toString();
  }
}
