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
 * Event listener interface for tab events, used primarily by
 * {@link com.google.gwt.user.client.ui.TabBar} and
 * {@link com.google.gwt.user.client.ui.TabPanel}.
 * 
 * @deprecated use
 *             {@link TabPanel#addBeforeSelectionHandler(com.google.gwt.event.logical.shared.BeforeSelectionHandler)}
 *             and/or
 *             {@link TabPanel#addSelectionHandler(com.google.gwt.event.logical.shared.SelectionHandler)}
 *             instead
 */
@Deprecated
public interface TabListener extends EventListener {

  /**
   * Fired just before a tab is selected.
   * 
   * @param sender the {@link TabBar} or {@link TabPanel} whose tab was
   *          selected.
   * @param tabIndex the index of the tab about to be selected
   * @return <code>false</code> to disallow the selection. If any listener
   *         returns false, then the selection will be disallowed.
   * @deprecated use
   *             {@link TabPanel#addBeforeSelectionHandler(com.google.gwt.event.logical.shared.BeforeSelectionHandler)}
   *             instead
   */
  @Deprecated
  boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex);

  /**
   * Fired when a tab is selected.
   * 
   * @param sender the {@link TabBar} or {@link TabPanel} whose tab was selected
   * @param tabIndex the index of the tab that was selected
   * @deprecated use
   *             {@link TabPanel#addSelectionHandler(com.google.gwt.event.logical.shared.SelectionHandler)}
   *             instead
   */
  @Deprecated
  void onTabSelected(SourcesTabEvents sender, int tabIndex);
}
