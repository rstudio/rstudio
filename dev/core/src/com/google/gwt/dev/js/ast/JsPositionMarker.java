/*
 * Copyright 2014 Google Inc.
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
 * Represents the starting boundary for statements that make up one class.
 */
public class JsPositionMarker extends JsStatement {

  /**
   * Categories of markers that can be placed in a JS AST.
   */
  public enum Type {
    CLASS_END, CLASS_START, PROGRAM_END, PROGRAM_START
  }

  private String name;
  private Type type;

  public JsPositionMarker(SourceInfo sourceInfo, String name, Type type) {
    super(sourceInfo);
    this.name = name;
    this.type = type;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.POSITION_MARKER;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  @Override
  public boolean shouldRecordPosition() {
    return false;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }
}
