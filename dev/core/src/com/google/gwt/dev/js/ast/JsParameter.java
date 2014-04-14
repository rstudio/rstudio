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

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * A JavaScript parameter.
 */
public final class JsParameter extends JsNode implements HasName {

  private final JsName name;

  public JsParameter(SourceInfo sourceInfo, JsName name) {
    super(sourceInfo);
    this.name = name;
    name.setStaticRef(this);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.PARAMETER;
  }

  @Override
  public JsName getName() {
    return name;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }
}
