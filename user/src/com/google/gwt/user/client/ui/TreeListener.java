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
package com.google.gwt.user.client.ui;

import java.util.EventListener;

/**
 * Event listener interface for tree events.
 * 
 * @deprecated Use {@link com.google.gwt.event.logical.shared.SelectionHandler},
 *             {@link com.google.gwt.event.logical.shared.OpenHandler}, and
 *             {@link com.google.gwt.event.logical.shared.CloseHandler} instead
 */
@Deprecated
public interface TreeListener extends EventListener {

  /**
   * Fired when a tree item is selected.
   * 
   * @param item the item being selected.
   * @deprecated use
   *             {@link com.google.gwt.event.logical.shared.SelectionHandler#onSelection(com.google.gwt.event.logical.shared.SelectionEvent)}
   *             instead
   */
  @Deprecated
  void onTreeItemSelected(TreeItem item);

  /**
   * Fired when a tree item is opened or closed.
   * 
   * @param item the item whose state is changing.
   * @deprecated use
   *             {@link com.google.gwt.event.logical.shared.OpenHandler#onOpen(com.google.gwt.event.logical.shared.OpenEvent)}
   *             and/or
   *             {@link com.google.gwt.event.logical.shared.CloseHandler#onClose(com.google.gwt.event.logical.shared.CloseEvent)}
   */
  @Deprecated
  void onTreeItemStateChanged(TreeItem item);
}
