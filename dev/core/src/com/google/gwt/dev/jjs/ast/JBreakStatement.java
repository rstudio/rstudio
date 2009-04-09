/*
 * Copyright 2007 Google Inc.
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
 * Java break statement.
 */
public class JBreakStatement extends JStatement {

  private final JLabel label;

  public JBreakStatement(SourceInfo info, JLabel label) {
    super(info);
    this.label = label;
  }

  public JLabel getLabel() {
    return label;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      if (label != null) {
        visitor.accept(label);
      }
    }
    visitor.endVisit(this, ctx);
  }

  @Override
  public boolean unconditionalControlBreak() {
    // It's only an unconditional break if the label is null, because a label
    // can label the break statement itself! We could handle this case.
    return label == null;
  }
}
