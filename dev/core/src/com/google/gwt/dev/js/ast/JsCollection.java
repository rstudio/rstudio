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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a collection of JavaScript language objects.
 */
public class JsCollection extends JsNode {

  private final List/* <JsNode> */list = new ArrayList/* <JsNode> */();

  public void addNode(int index, JsNode o) {
    assert (o != null);
    list.add(index, o);
  }

  public void addNode(JsNode o) {
    assert (o != null);
    list.add(o);
  }

  public JsNode getNode(int index) {
    return (JsNode) list.get(index);
  }

  public Iterator iterator() {
    return list.iterator();
  }

  public void setNode(int index, JsNode o) {
    list.set(index, o);
  }

  public int size() {
    return list.size();
  }

  public void traverse(JsVisitor v) {
    for (int i = 0; i < list.size(); ++i) {
      JsNode node = (JsNode) list.get(i);
      node.traverse(v);
    }
  }
}
