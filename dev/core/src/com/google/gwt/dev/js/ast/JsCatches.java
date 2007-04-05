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
package com.google.gwt.dev.js.ast;

/**
 * Represents a collection of <code>JsCatch</code> objects.
 */
public final class JsCatches extends JsCollection {

  public void add(JsCatch param) {
    super.addNode(param);
  }

  public void add(int index, JsCatch param) {
    super.addNode(index, param);
  }

  public JsCatch get(int i) {
    return (JsCatch) super.getNode(i);
  }

  public void set(int i, JsCatch param) {
    super.setNode(i, param);
  }

}
