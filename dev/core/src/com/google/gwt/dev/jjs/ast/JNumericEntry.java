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
 * A place order a numeric value in the AST.
 * 
 * It temporary holds an numeric value during compilation. In a later stage,
 * a compiler pass easily replace these place holder. For example,
 * {@link ReplaceRunAsyncs would }
 */
public final class JNumericEntry extends JExpression {

  private final String key;
  private int value;

  public JNumericEntry(SourceInfo info, String key, int value) {
    super(info);
    this.key = key;
    this.value = value;
  }
  
  public String getKey() {
    return key;
  }

  @Override
  public JType getType() {
    return JPrimitiveType.INT;
  }

  public int getValue() {
    return value;
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  public void setValue(int value) {
    this.value = value;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
  
}
