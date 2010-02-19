/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.tree.client;

import com.google.gwt.cells.client.Cell;
import com.google.gwt.sample.tree.shared.SimpleTreeNodeModel;

/**
 * A tree view that uses a single data type for all of its nodes.
 * 
 * @param <T> the data type of each tree node.
 */
public class SimpleTreeNodeView<T> extends TreeNodeView<T,T> {

  private Cell<T> cell;
  private SimpleTreeNodeModel<T> model;

  public SimpleTreeNodeView(T value, final SimpleTreeNodeModel<T> model, Cell<T> cell) {
    super(value, model, cell);
    this.model = model;
    this.cell = cell;
  }

  @Override
  protected TreeNodeView<T, T> createChildView(T value) {
    return new SimpleTreeNodeView<T>(value, model.createChildModel(value), cell);
  }
}
