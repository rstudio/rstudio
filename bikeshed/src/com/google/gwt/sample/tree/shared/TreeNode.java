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

import com.google.gwt.list.shared.ListHandler;
import com.google.gwt.list.shared.ListRegistration;
import com.google.gwt.list.shared.SizeChangeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in a Tree.
 *
 * @param <T> the data type contained in the node.
 */
public abstract class TreeNode<T> {
  
    protected List<ListHandler<TreeNode<T>>> handlers =
      new ArrayList<ListHandler<TreeNode<T>>>();
    protected T nodeData;

    public ListRegistration addListHandler(final ListHandler<TreeNode<T>> handler) {
      handler.onSizeChanged(new SizeChangeEvent(5, true)); // TODO - unhack
      handlers.add(handler);
      
      return new ListRegistration() {
        public void removeHandler() {
          handlers.remove(handler);
        }

        public void setRangeOfInterest(int start, int length) {
          onRangeChanged(start, length);
        }
      };
    }
    
    public T getNodeData() {
      return nodeData;
    }

    protected abstract void onRangeChanged(int start, int length);
}
