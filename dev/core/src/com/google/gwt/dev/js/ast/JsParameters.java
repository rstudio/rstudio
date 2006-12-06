/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.js.ast;

/**
 * A collection of JavaScript parameters.
 */
public final class JsParameters extends JsCollection {

  public void add(JsParameter param) {
    super.addNode(param);
  }

  public void add(int index, JsParameter param) {
    super.addNode(index, param);
  }

  public JsParameter get(int i) {
    return (JsParameter) super.getNode(i);
  }

  public void set(int i, JsParameter param) {
    super.setNode(i, param);
  }

  public void traverse(JsVisitor v) {
    if (v.visit(this)) {
      super.traverse(v);
    }
    v.endVisit(this);
  }

}
