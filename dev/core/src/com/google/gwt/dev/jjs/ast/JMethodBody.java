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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a the body of a method. Can be Java or JSNI.
 */
public class JMethodBody extends JAbstractMethodBody {

  public final ArrayList <JLocal>locals = new ArrayList<JLocal>();
  private JBlock body;

  public JMethodBody(JProgram program, SourceInfo info) {
    super(program, info);
    body = new JBlock(program, info);
  }

  public List<JStatement> getStatements() {
    return body.statements;
  }

  @Override
  public boolean isNative() {
    return false;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(locals);
      body = (JBlock) visitor.accept(body);
    }
    visitor.endVisit(this, ctx);
  }
}
