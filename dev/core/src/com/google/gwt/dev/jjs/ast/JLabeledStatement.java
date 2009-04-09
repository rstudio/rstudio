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
 * Java statement that has an associated label.
 */
public class JLabeledStatement extends JStatement {

  private JStatement body;
  private final JLabel label;

  public JLabeledStatement(SourceInfo info, JLabel label, JStatement body) {
    super(info);
    this.label = label;
    this.body = body;
  }

  public JStatement getBody() {
    return body;
  }

  public JLabel getLabel() {
    return label;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(label);
      body = visitor.accept(body);
    }
    visitor.endVisit(this, ctx);
  }
}
