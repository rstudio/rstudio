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
package com.google.gwt.sample.tree.shared;

import com.google.gwt.list.shared.ListModel;

/**
 * A model describing a node in a Tree.
 * 
 * @param <C> the data type contained in the children of the node.
 */
public abstract class TreeNodeModel<C> {
  private ListModel<C> listModel = null;

  public TreeNodeModel() {
  }
  
  public abstract TreeNodeModel<?> createChildModel(C value);

  public ListModel<C> getListModel() {
    if (listModel == null) {
      listModel = createListModel();
    }
    return listModel;
  }
  
  protected abstract ListModel<C> createListModel();
}
