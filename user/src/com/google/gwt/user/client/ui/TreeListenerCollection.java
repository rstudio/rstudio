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
package com.google.gwt.user.client.ui;

import java.util.ArrayList;

/**
 * A helper class for implementers of the SourcesClickEvents interface. This
 * subclass of {@link ArrayList} assumes that all objects added to it will be of
 * type {@link com.google.gwt.user.client.ui.ClickListener}.
 * 
 * @deprecated Widgets should now manage their own handlers via {@link Widget#addDomHandler}
 */
@Deprecated
public class TreeListenerCollection extends ArrayList<TreeListener> {

  /**
   * Fires a "tree item selected" event to all listeners.
   * 
   * @param item the tree item being selected.
   */
  public void fireItemSelected(TreeItem item) {
    for (TreeListener listener : this) {
      listener.onTreeItemSelected(item);
    }
  }

  /**
   * Fires a "tree item state changed" event to all listeners.
   * 
   * @param item the tree item whose state has changed.
   */
  public void fireItemStateChanged(TreeItem item) {
    for (TreeListener listener : this) {
      listener.onTreeItemStateChanged(item);
    }
  }
}
